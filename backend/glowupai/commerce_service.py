from __future__ import annotations

import json
import re
import statistics
import uuid
from typing import Any, Callable, Dict, List, Optional

from .catalog import explain, parse_ingredients
from .service import now_iso, row_dict


def uid() -> str:
    return str(uuid.uuid4())


def dump(value: Any) -> str:
    return json.dumps(value, separators=(",", ":"), sort_keys=True)


def load(value: Any, default: Optional[Any] = None) -> Any:
    try:
        return json.loads(value)
    except (TypeError, ValueError):
        return default if default is not None else {}


class CommerceService:
    """Product recommendations, commerce, offers, and purchasing guidance."""

    def __init__(self, db: Any, parent_service: Any) -> None:
        self.db = db
        self.parent = parent_service

    def predict_product(self, user_id: str, product_id: str, require_premium_fn: Optional[Callable] = None, refresh_verdicts_fn: Optional[Callable] = None) -> Dict[str, Any]:
        if require_premium_fn:
            require_premium_fn(user_id, "Pre-purchase prediction")
        product = self.db.fetchone("SELECT * FROM products WHERE id=?", (product_id,))
        if not product:
            raise ValueError("product not found")
        if refresh_verdicts_fn:
            refresh_verdicts_fn(user_id)
        candidate_ingredients = {
            i.casefold() for i in parse_ingredients(product["ingredients_json"])
        }
        own_verdicts = self.db.fetchall(
            """SELECT v.label, p.ingredients_json, p.name FROM verdicts v JOIN products p ON p.id = v.product_id
               WHERE v.user_id=? AND v.generated_at = (SELECT MAX(v2.generated_at) FROM verdicts v2 WHERE v2.user_id=v.user_id AND v2.product_id=v.product_id)""",
            (user_id,),
        )
        investigate_hits, useful_hits = [], []
        for row in own_verdicts:
            other_ingredients = {
                i.casefold() for i in parse_ingredients(row["ingredients_json"])
            }
            overlap = candidate_ingredients & other_ingredients
            if not overlap:
                continue
            if row["label"] == "investigate":
                investigate_hits.append(
                    {"product_name": row["name"], "shared_ingredients": sorted(overlap)}
                )
            elif row["label"] == "likely_useful":
                useful_hits.append(
                    {"product_name": row["name"], "shared_ingredients": sorted(overlap)}
                )
        cohort_rows = self.db.fetchall(
            """SELECT DISTINCT p.name, p.ingredients_json FROM verdicts v JOIN products p ON p.id=v.product_id WHERE v.label='likely_useful' AND v.user_id <> ?""",
            (user_id,),
        )
        cohort_hits = []
        for row in cohort_rows:
            overlap = candidate_ingredients & {
                i.casefold() for i in parse_ingredients(row["ingredients_json"])
            }
            if overlap:
                cohort_hits.append(
                    {"product_name": row["name"], "shared_ingredients": sorted(overlap)}
                )
        if investigate_hits:
            headline = f"{len(investigate_hits)} of your own products that came back 'investigate' share an active ingredient with this one."
        elif useful_hits:
            headline = f"{len(useful_hits)} of your own products that came back 'likely useful' share an active ingredient with this one."
        else:
            headline = "No overlap with your own verdict history yet — this would be a new ingredient profile for you."
        return {
            "product_id": product_id,
            "product_name": product["name"],
            "ingredients": sorted(candidate_ingredients),
            "overlap_with_investigate": investigate_hits[:10],
            "overlap_with_likely_useful": useful_hits[:10],
            "cohort_overlap": cohort_hits[:10],
            "headline": headline,
            "disclaimer": "This is an ingredient-overlap similarity signal from your own and cohort history, not a prediction of efficacy or safety. Only your own capture-based experiment can verify how this product performs for you.",
        }

    def lookup_product(self, barcode: str) -> Optional[Dict[str, Any]]:
        normalized = re.sub(r"[^0-9A-Za-z-]", "", barcode or "").strip()
        if not normalized:
            raise ValueError("barcode is required")
        row = self.db.fetchone("SELECT * FROM products WHERE barcode=?", (normalized,))
        return row_dict(row) if row else None

    def purchase_guidance(
        self,
        user_id: str,
        name: Optional[str] = None,
        barcode: Optional[str] = None,
        ingredients: Optional[Any] = None,
        category: str = "other",
        price_cents: Optional[int] = None,
        currency: str = "INR",
        require_premium_fn: Optional[Callable] = None,
        refresh_verdicts_fn: Optional[Callable] = None,
    ) -> Dict[str, Any]:
        if require_premium_fn:
            require_premium_fn(user_id, "Pre-purchase guidance")
        normalized_barcode = re.sub(r"[^0-9A-Za-z-]", "", barcode or "").strip() or None
        product = (
            self.db.fetchone(
                "SELECT * FROM products WHERE barcode=?", (normalized_barcode,)
            )
            if normalized_barcode
            else None
        )
        candidate_name = (product["name"] if product else (name or "")).strip()
        candidate_ingredients = parse_ingredients(
            product["ingredients_json"] if product else ingredients
        )
        if not candidate_name:
            raise ValueError("product name or a known barcode is required")
        if not candidate_ingredients:
            return {
                "product_id": product["id"] if product else None,
                "product_name": candidate_name,
                "barcode": normalized_barcode,
                "ingredients": [],
                "signal": "missing_ingredients",
                "headline": "Add the ingredient list to compare this product with your history.",
                "next_action": "Paste the INCI list from the box or product page, then run the check again.",
                "overlap_with_investigate": [],
                "overlap_with_likely_useful": [],
                "cohort_overlap": [],
                "estimated_annual_cost_cents": None,
                "currency": currency,
                "disclaimer": "Ingredient overlap is a similarity signal, not a prediction of efficacy or safety.",
            }
        if refresh_verdicts_fn:
            refresh_verdicts_fn(user_id)
        candidate_set = {item.casefold() for item in candidate_ingredients}
        rows = self.db.fetchall(
            """SELECT v.label, p.ingredients_json, p.name FROM verdicts v JOIN products p ON p.id=v.product_id
            WHERE v.user_id=? AND v.generated_at=(SELECT MAX(v2.generated_at) FROM verdicts v2 WHERE v2.user_id=v.user_id AND v2.product_id=v.product_id)""",
            (user_id,),
        )
        investigate_hits, useful_hits = [], []
        for row in rows:
            overlap = candidate_set & {
                item.casefold() for item in parse_ingredients(row["ingredients_json"])
            }
            if not overlap:
                continue
            hit = {"product_name": row["name"], "shared_ingredients": sorted(overlap)}
            if row["label"] == "investigate":
                investigate_hits.append(hit)
            elif row["label"] in {"likely_useful", "keep"}:
                useful_hits.append(hit)
        cohort_rows = self.db.fetchall(
            """SELECT DISTINCT p.name, p.ingredients_json FROM verdicts v JOIN products p ON p.id=v.product_id
            WHERE v.label='likely_useful' AND v.user_id<>?""",
            (user_id,),
        )
        cohort_hits = []
        for row in cohort_rows:
            overlap = candidate_set & {
                item.casefold() for item in parse_ingredients(row["ingredients_json"])
            }
            if overlap:
                cohort_hits.append(
                    {"product_name": row["name"], "shared_ingredients": sorted(overlap)}
                )
        if investigate_hits:
            signal = "caution"
            headline = f"This shares ingredients with {len(investigate_hits)} product(s) that your history marked for investigation."
            next_action = "Do not add it alongside another new product. If you test it, change one variable and wait for the stabilization window."
        elif useful_hits:
            signal = "promising_similarity"
            headline = f"This shares ingredients with {len(useful_hits)} product(s) that looked useful in your history."
            next_action = "It is a reasonable candidate, but only a controlled capture series can verify how it performs for you."
        else:
            signal = "new_profile"
            headline = "No overlap with your own verdict history yet."
            next_action = "Treat it as a new ingredient profile: capture a baseline, start one product, and keep the rest of the routine steady."
        stabilization_days = int(product["stabilization_days"]) if product else 14
        annual_cost = None
        if price_cents is not None:
            annual_cost = round(price_cents * (365 / max(stabilization_days * 3, 60)))
        return {
            "product_id": product["id"] if product else None,
            "product_name": candidate_name,
            "barcode": normalized_barcode,
            "ingredients": sorted(candidate_set),
            "signal": signal,
            "headline": headline,
            "next_action": next_action,
            "overlap_with_investigate": investigate_hits[:10],
            "overlap_with_likely_useful": useful_hits[:10],
            "cohort_overlap": cohort_hits[:10],
            "estimated_annual_cost_cents": annual_cost,
            "currency": currency,
            "disclaimer": "Ingredient overlap is a similarity signal from your own and cohort history, not a prediction of efficacy or safety. Only your own standardized capture series can verify this product for you.",
        }

    def budget_optimizer(self, user_id: str, require_premium_fn: Optional[Callable] = None, refresh_verdicts_fn: Optional[Callable] = None) -> Dict[str, Any]:
        if require_premium_fn:
            require_premium_fn(user_id, "Routine budget optimizer")
        verdicts = refresh_verdicts_fn(user_id) if refresh_verdicts_fn else []
        flagged = []
        total_annual_cents = 0
        for item in verdicts:
            evidence = item.get("evidence", {})
            if item["label"] not in ("keep",) or evidence.get("days_stable", 0) < 30:
                continue
            offer = self.db.fetchone(
                "SELECT AVG(price_cents) AS avg_price, currency FROM affiliate_offers WHERE product_id=? AND price_cents IS NOT NULL",
                (item["product_id"],),
            )
            usage_days = max(int(evidence.get("stabilization_days", 14)) * 3, 60)
            annual_cents = None
            if offer and offer["avg_price"]:
                annual_cents = round(float(offer["avg_price"]) * (365 / usage_days))
                total_annual_cents += annual_cents
            flagged.append(
                {
                    "product_id": item["product_id"],
                    "product_name": item["product_name"],
                    "days_stable": evidence.get("days_stable", 0),
                    "estimated_annual_cost_cents": annual_cents,
                    "currency": (
                        offer["currency"] if offer and offer["avg_price"] else None
                    )
                    or "USD",
                    "reason": "No measurable improvement beyond the capture noise floor after at least 30 days.",
                }
            )
        return {
            "flagged": flagged,
            "estimated_annual_waste_cents": total_annual_cents,
            "currency": "USD",
            "disclaimer": "Estimates use your logged stabilization window and any known offer price; products without a known price are flagged without a cost estimate.",
        }

    def discover(self, user_id: str, require_premium_fn: Optional[Callable] = None) -> Dict[str, Any]:
        if require_premium_fn:
            require_premium_fn(user_id, "Discover")
        rows = self.db.fetchall(
            """SELECT p.id,p.name,p.category,v.label,v.evidence_json,v.user_id FROM verdicts v JOIN products p ON p.id=v.product_id WHERE v.label='likely_useful'"""
        )
        grouped = {}
        for row in rows:
            key = row["id"]
            grouped.setdefault(
                key,
                {
                    "product_id": key,
                    "name": row["name"],
                    "category": row["category"],
                    "users": set(),
                    "effects": [],
                },
            )
            grouped[key]["users"].add(row["user_id"])
            grouped[key]["effects"].append(
                load(row["evidence_json"], {}).get("composite_effect", 0)
            )
        recommendations = []
        for item in grouped.values():
            if len(item["users"]) >= 3:
                recommendations.append(
                    {
                        "product_id": item["product_id"],
                        "name": item["name"],
                        "category": item["category"],
                        "sample_size": len(item["users"]),
                        "average_effect": round(statistics.mean(item["effects"]), 3),
                        "reason": "Observed as likely useful across at least three consenting users; this does not replace your own experiment.",
                    }
                )
        return {
            "recommendations": sorted(
                recommendations, key=lambda item: item["average_effect"], reverse=True
            )[:20],
            "minimum_cohort_size": 3,
            "disclaimer": "Discover is cohort evidence, never a personal verdict and never paid placement.",
        }

    def add_offer(
        self,
        product_id: str,
        merchant: str,
        url: str,
        price_cents: Optional[int] = None,
        currency: str = "USD",
    ) -> Dict[str, Any]:
        if not self.db.fetchone("SELECT id FROM products WHERE id=?", (product_id,)):
            raise ValueError("product not found")
        offer_id = uid()
        self.db.execute(
            "INSERT INTO affiliate_offers (id,product_id,merchant,url,price_cents,currency) VALUES (?,?,?,?,?,?)",
            (offer_id, product_id, merchant, url, price_cents, currency),
        )
        return row_dict(
            self.db.fetchone("SELECT * FROM affiliate_offers WHERE id=?", (offer_id,))
        )

    def offers(self, user_id: str, product_id: Optional[str] = None) -> List[Dict[str, Any]]:
        self.parent.require_user(user_id)
        sql = "SELECT o.*,p.name AS product_name FROM affiliate_offers o JOIN products p ON p.id=o.product_id WHERE o.active=1"
        params = ()
        if product_id:
            sql += " AND o.product_id=?"
            params = (product_id,)
        return [
            row_dict(row)
            for row in self.db.fetchall(sql + " ORDER BY o.created_at DESC", params)
        ]

    def click_offer(self, user_id: str, offer_id: str, record_engagement_fn: Optional[Callable] = None) -> Dict[str, Any]:
        self.parent.require_user(user_id)
        offer = self.db.fetchone(
            "SELECT * FROM affiliate_offers WHERE id=? AND active=1", (offer_id,)
        )
        if not offer:
            raise ValueError("offer not found")
        self.db.execute(
            "INSERT INTO affiliate_clicks (id,user_id,offer_id) VALUES (?,?,?)",
            (uid(), user_id, offer_id),
        )
        if record_engagement_fn:
            record_engagement_fn(user_id, "affiliate_click", offer_id)
        return row_dict(offer)

    def ingredient_explainer(self, user_id: str, product_id: str, require_premium_fn: Optional[Callable] = None) -> Dict[str, Any]:
        if require_premium_fn:
            require_premium_fn(user_id, "Ingredient analysis")
        product = self.db.fetchone("SELECT * FROM products WHERE id=?", (product_id,))
        if not product:
            raise ValueError("product not found")
        return explain(product["name"], product["ingredients_json"])

    def product_detail(self, user_id: str, product_id: str, is_premium: bool, offers_fn: Optional[Callable] = None) -> Dict[str, Any]:
        product = self.db.fetchone("SELECT * FROM products WHERE id=?", (product_id,))
        if not product:
            raise ValueError("product not found")
        result = row_dict(product)
        result["offers"] = offers_fn(user_id, product_id) if offers_fn else []
        result["ingredient_analysis"] = (
            self.ingredient_explainer(user_id, product_id)
            if is_premium
            else None
        )
        return result

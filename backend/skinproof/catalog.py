from __future__ import annotations

import json
import re


INGREDIENTS = {
    "niacinamide": {"purpose": "supports the skin barrier and can help reduce the appearance of uneven tone", "caution": "introduce slowly if your skin is sensitive"},
    "retinol": {"purpose": "a vitamin A derivative commonly used for texture and appearance of fine lines", "caution": "can be irritating; follow the product directions and use daytime sun protection"},
    "salicylic acid": {"purpose": "an oil-soluble exfoliant commonly used for clogged pores", "caution": "may be drying or irritating when combined with other strong exfoliants"},
    "hyaluronic acid": {"purpose": "a humectant that helps attract water to the outer skin layers", "caution": "the feel and benefit depend on the full formula and use with moisturizer as needed"},
    "azelaic acid": {"purpose": "an ingredient commonly used for the appearance of uneven tone and blemishes", "caution": "may cause tingling or irritation when first introduced"},
    "vitamin c": {"purpose": "an antioxidant ingredient often used in formulas targeting the appearance of uneven tone", "caution": "formulas vary substantially in stability and irritation potential"},
}


def parse_ingredients(value) -> list[str]:
    if isinstance(value, list):
        return [str(item).strip() for item in value if str(item).strip()]
    if not value:
        return []
    try:
        decoded = json.loads(value)
        if isinstance(decoded, list):
            return parse_ingredients(decoded)
    except (TypeError, ValueError):
        pass
    return [item.strip() for item in re.split(r",|;", str(value)) if item.strip()]


def explain(name: str, ingredients) -> dict:
    parsed = parse_ingredients(ingredients)
    reviewed = []
    unknown = []
    for ingredient in parsed:
        key = ingredient.casefold()
        match = next((data for known, data in INGREDIENTS.items() if known in key), None)
        if match:
            reviewed.append({"ingredient": ingredient, **match})
        else:
            unknown.append(ingredient)
    return {
        "product_name": name,
        "ingredients": parsed,
        "reviewed": reviewed,
        "unknown": unknown,
        "disclaimer": "This is a cosmetic ingredient explainer, not medical advice. A product formula and individual response matter more than one ingredient in isolation.",
    }

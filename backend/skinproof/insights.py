from __future__ import annotations

from typing import Protocol


class InsightService(Protocol):
    def generate(self, evidence: dict) -> str: ...


class GroundedInsightService:
    """Deterministic language layer grounded in attribution evidence."""

    def generate(self, evidence: dict) -> str:
        label = evidence["label"]
        product = evidence.get("product_name", "this product")
        days = evidence.get("days_stable", 0)
        if label == "likely_useful":
            metric = evidence.get("best_improvement_metric", "your measured skin metrics")
            return f"After {days} days of a stable routine, {product} is likely useful: {metric} improved beyond the capture noise floor. Keep the rest of the routine steady so the signal remains attributable."
        if label == "investigate":
            metric = evidence.get("worst_metric", "one or more metrics")
            return f"After {days} days of a stable routine, {metric} moved in a worse direction for {product}. Hold the rest of the routine steady and investigate this product before adding another change."
        if label == "keep":
            return f"{product} has remained within the expected capture-to-capture noise floor after {days} days. There is no clear measurable improvement, but there is no measured signal requiring removal either."
        wait = evidence.get("days_to_wait", 0)
        reason = evidence.get("reason", "there is not enough clean evidence yet")
        if wait:
            return f"Evidence is unclear for {product}: {reason}. Hold the routine for {wait} more days before making another change."
        return f"Evidence is unclear for {product}: {reason}. Keep the routine stable and capture again before drawing a conclusion."

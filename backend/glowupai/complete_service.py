from __future__ import annotations

from typing import Any, Dict, List, Optional

from .complete_db import FullDatabase
from .google_ai import build_insight_service, build_vision_service
from .insights import GroundedInsightService
from .jobs import JobRunner
from .service import GlowupAIService

# Import the new service modules
from .user_service import UserService
from .capture_service import CaptureService
from .subscription_service import SubscriptionService
from .analytics_service import AnalyticsService
from .guidance_service import GuidanceService
from .commerce_service import CommerceService

VERTICALS = ("skin",)
PREMIUM_FEATURES = {
    "experiments",
    "ingredient_analysis",
    "long_history",
    "qna",
    "discover",
    "root_cause",
    "budget_optimizer",
    "derm_export",
    "product_prediction",
}


class CompleteGlowupAIService(GlowupAIService):
    """Complete local product implementation for every user-facing phase.

    Cloud adapters, mobile SDKs, and payment credentials are injected at the
    deployment boundary; the product behavior itself is implemented here.
    """

    def __init__(self, db: FullDatabase, settings: Any = None, photos: Any = None) -> None:
        super().__init__(
            db, settings=settings, photos=photos, insights=GroundedInsightService()
        )
        self.full_db = db
        self.insights = build_insight_service(self.settings)
        self.vision = build_vision_service(self.settings)
        self.jobs = JobRunner(db)

        # Initialize service modules
        self.user_svc = UserService(db, self)
        self.capture_svc = CaptureService(db, self, photos, settings)
        self.subscription_svc = SubscriptionService(db, self)
        self.analytics_svc = AnalyticsService(db, self)
        self.guidance_svc = GuidanceService(db, self, self.insights, self.vision, self.jobs)
        self.commerce_svc = CommerceService(db, self)

    # ============================================================================
    # USER SERVICE DELEGATION
    # ============================================================================

    def create_user(self, skin_type: Optional[str] = None) -> Dict[str, Any]:
        # Call user service, but prevent infinite recursion by NOT overriding
        # Instead, let UserService call super().create_user() via GlowupAIService
        return self.user_svc.create_user(skin_type)

    def session_for_identity(
        self,
        firebase_uid: str,
        email: Optional[str] = None,
        email_verified: bool = False,
        name: Optional[str] = None,
    ) -> Dict[str, Any]:
        return self.user_svc.session_for_identity(firebase_uid, email, email_verified, name)

    def grant_consent(
        self, user_id: str, facial_data: bool, policy_version: Optional[str] = None
    ) -> Dict[str, Any]:
        return self.user_svc.grant_consent(user_id, facial_data, policy_version)

    def profile(self, user_id: str) -> Dict[str, Any]:
        return self.user_svc.profile(user_id)

    def update_profile(
        self,
        user_id: str,
        display_name: Optional[str] = None,
        skin_type: Optional[str] = None,
        focus_vertical: Optional[str] = None,
        goals: Optional[List[str]] = None,
        experience_level: Optional[str] = None,
        onboarding_complete: Optional[bool] = None,
    ) -> Dict[str, Any]:
        return self.user_svc.update_profile(
            user_id, display_name, skin_type, focus_vertical, goals, experience_level, onboarding_complete
        )

    def export_user(self, user_id: str) -> Dict[str, Any]:
        return self.user_svc.export_user(
            user_id,
            history_fn=self.history,
            context_events_fn=self.context_events,
            check_ins_fn=self.check_ins,
            qna_history_fn=self.qna_history,
            is_premium=self.is_premium(user_id)
        )

    def record_data_collection_consent(
        self, user_id: str, granted: bool, policy_version: str = "1.0"
    ) -> Dict[str, Any]:
        return self.user_svc.record_data_collection_consent(user_id, granted, policy_version)

    def admin_audit(self, limit: int = 100) -> List[Dict[str, Any]]:
        return self.user_svc.admin_audit(limit)

    def _audit(
        self,
        action: str,
        subject_type: Optional[str] = None,
        subject_id: Optional[str] = None,
        actor_id: Optional[str] = None,
        metadata: Optional[Dict[str, Any]] = None,
    ) -> None:
        return self.user_svc._audit(action, subject_type, subject_id, actor_id, metadata)

    # ============================================================================
    # SUBSCRIPTION SERVICE DELEGATION
    # ============================================================================

    def entitlement(self, user_id: str) -> Dict[str, Any]:
        return self.subscription_svc.entitlement(user_id)

    def is_premium(self, user_id: str) -> bool:
        return self.subscription_svc.is_premium(user_id)

    def require_premium(self, user_id: str, feature: str) -> Dict[str, Any]:
        return self.subscription_svc.require_premium(user_id, feature)

    def upgrade(self, user_id: str, source: str = "local_checkout") -> Dict[str, Any]:
        return self.subscription_svc.upgrade(user_id, source, audit_fn=self._audit)

    def downgrade(self, user_id: str) -> Dict[str, Any]:
        return self.subscription_svc.downgrade(user_id, audit_fn=self._audit)

    def list_subscriptions(self, limit: int = 100) -> List[Dict[str, Any]]:
        return self.subscription_svc.list_subscriptions(limit)

    def create_subscription(self, user_id: str, plan: str = "premium", source: str = "api") -> Dict[str, Any]:
        return self.subscription_svc.create_subscription(user_id, plan, source)

    def verdicts_for_user(self, user_id: str) -> List[Dict[str, Any]]:
        return self.subscription_svc.verdicts_for_user(
            user_id,
            refresh_verdicts_fn=self.refresh_verdicts,
            record_engagement_fn=self.record_engagement
        )

    # ============================================================================
    # CAPTURE SERVICE DELEGATION
    # ============================================================================

    def create_capture(
        self,
        user_id: str,
        image_bytes: bytes,
        quality_data: Optional[Dict[str, Any]] = None,
        captured_at: Optional[str] = None,
        device_meta: Optional[Dict[str, Any]] = None,
        is_baseline: bool = False,
        vertical: str = "skin",
        experiment_id: Optional[str] = None,
    ) -> Dict[str, Any]:
        return self.capture_svc.create_capture(
            user_id, image_bytes, quality_data, captured_at, device_meta,
            is_baseline, vertical, experiment_id,
            record_engagement_fn=self.record_engagement
        )

    def history(self, user_id: str, vertical: str = "skin") -> List[Dict[str, Any]]:
        return self.capture_svc.history(user_id, vertical, is_premium=self.is_premium(user_id))

    def capture_guide(self, user_id: str, vertical: str = "skin") -> Dict[str, Any]:
        return self.capture_svc.capture_guide(user_id, vertical, history_fn=self.history)

    def add_measurement_feedback(
        self, user_id: str, capture_id: str, agreement: str, note: Optional[str] = None
    ) -> Dict[str, Any]:
        return self.capture_svc.add_measurement_feedback(
            user_id, capture_id, agreement, note, record_engagement_fn=self.record_engagement
        )

    def measurement_feedback_summary(self) -> Dict[str, Any]:
        return self.capture_svc.measurement_feedback_summary()

    def labels(self, user_id: str) -> List[Dict[str, Any]]:
        return self.capture_svc.labels(user_id)

    def add_label(
        self,
        user_id: str,
        photo_id: str,
        label_type: str,
        value: str,
        confidence: Optional[float] = None,
        notes: Optional[str] = None,
    ) -> Dict[str, Any]:
        return self.capture_svc.add_label(user_id, photo_id, label_type, value, confidence, notes)

    def reprocess(self, user_id: str, model_version: str) -> Dict[str, Any]:
        return self.capture_svc.reprocess(user_id, model_version, self.jobs, self._audit)

    def reprocess_status(self, user_id: str, job_id: str) -> Dict[str, Any]:
        return self.capture_svc.reprocess_status(user_id, job_id, self.jobs)

    def submit_capture_feedback(
        self,
        capture_id: str,
        user_id: str,
        feedback_type: str,
        issues: Optional[List[str]] = None,
        corrections: Optional[Dict[str, float]] = None,
        comment: Optional[str] = None
    ) -> Dict[str, Any]:
        return self.capture_svc.submit_capture_feedback(
            capture_id, user_id, feedback_type, issues, corrections, comment
        )

    def get_feedback_stats(self) -> Dict[str, Any]:
        return self.capture_svc.get_feedback_stats()

    def get_feedback_corrections(self, limit: int = 100) -> List[Dict[str, Any]]:
        return self.capture_svc.get_feedback_corrections(limit)

    def get_metric_accuracy_analysis(self) -> Dict[str, Any]:
        return self.capture_svc.get_metric_accuracy_analysis()

    def get_model_health_status(self) -> Dict[str, Any]:
        return self.capture_svc.get_model_health_status()

    def generate_monitoring_daily_report(self) -> Dict[str, Any]:
        return self.capture_svc.generate_monitoring_daily_report()

    def get_collection_stats(self) -> Dict[str, Any]:
        return self.capture_svc.get_collection_stats()

    def export_training_dataset(
        self,
        output_dir: str,
        min_quality: float = 0.75,
        max_samples: Optional[int] = None
    ) -> Dict[str, Any]:
        return self.capture_svc.export_training_dataset(output_dir, min_quality, max_samples)

    def cleanup_old_data(self, retention_days: int = 365) -> Dict[str, Any]:
        return self.capture_svc.cleanup_old_data(retention_days)

    # ============================================================================
    # ANALYTICS SERVICE DELEGATION
    # ============================================================================

    def add_routine_event(
        self,
        user_id: str,
        product_id: str,
        action: str,
        timestamp: Optional[str] = None,
        slot: str = "unspecified",
        dose: Optional[str] = None,
        frequency: Optional[str] = None,
        notes: Optional[str] = None,
        experiment_id: Optional[str] = None,
    ) -> Dict[str, Any]:
        return self.analytics_svc.add_routine_event(
            user_id, product_id, action, timestamp, slot, dose, frequency, notes, experiment_id,
            audit_fn=self._audit
        )

    def confound_check(
        self, user_id: str, exclude_product_id: Optional[str] = None
    ) -> Dict[str, Any]:
        return self.analytics_svc.confound_check(user_id, exclude_product_id)

    def weekly_recap(
        self, user_id: str, vertical: str = "skin", as_of: Optional[str] = None
    ) -> Dict[str, Any]:
        return self.analytics_svc.weekly_recap(
            user_id, vertical, as_of, history_fn=self.history, check_ins_fn=self.check_ins
        )

    def check_ins(self, user_id: str, limit: int = 30) -> List[Dict[str, Any]]:
        return self.analytics_svc.check_ins(user_id, limit)

    def create_check_in(
        self,
        user_id: str,
        routine_state: str,
        skin_feel: str,
        note: Optional[str] = None,
        occurred_at: Optional[str] = None,
    ) -> Dict[str, Any]:
        return self.analytics_svc.create_check_in(
            user_id, routine_state, skin_feel, note, occurred_at,
            record_engagement_fn=self.record_engagement
        )

    def record_engagement(
        self,
        user_id: str,
        event_type: str,
        reference_id: Optional[str] = None,
        metadata: Optional[Dict[str, Any]] = None,
    ) -> Dict[str, Any]:
        return self.analytics_svc.record_engagement(user_id, event_type, reference_id, metadata)

    def engagement(self, user_id: str) -> Dict[str, Any]:
        return self.analytics_svc.engagement(user_id, capture_guide_fn=self.capture_guide)

    def analytics(self, user_id: str) -> Dict[str, Any]:
        return self.analytics_svc.analytics(user_id, history_fn=self.history)

    def add_context_event(
        self,
        user_id: str,
        event_type: str,
        value: Optional[str] = None,
        occurred_at: Optional[str] = None,
        notes: Optional[str] = None,
    ) -> Dict[str, Any]:
        return self.analytics_svc.add_context_event(user_id, event_type, value, occurred_at, notes)

    def context_events(self, user_id: str) -> List[Dict[str, Any]]:
        return self.analytics_svc.context_events(user_id)

    def root_cause_search(
        self, user_id: str, metric: str = "texture_score"
    ) -> List[Dict[str, Any]]:
        return self.analytics_svc.root_cause_search(
            user_id, metric, history_fn=self.history, require_premium_fn=self.require_premium
        )

    # ============================================================================
    # GUIDANCE SERVICE DELEGATION
    # ============================================================================

    def create_experiment(
        self,
        user_id: str,
        name: str,
        hypothesis: Optional[str],
        product_id: str,
        primary_metric: str = "redness_score",
        target_days: int = 14,
    ) -> Dict[str, Any]:
        return self.guidance_svc.create_experiment(
            user_id, name, hypothesis, product_id, primary_metric, target_days,
            require_premium_fn=self.require_premium,
            require_consent_fn=self.require_consent,
            audit_fn=self._audit
        )

    def experiment(self, experiment_id: str, user_id: str) -> Dict[str, Any]:
        return self.guidance_svc.experiment(experiment_id, user_id, history_fn=self.history)

    def experiments(self, user_id: str) -> List[Dict[str, Any]]:
        return self.guidance_svc.experiments(user_id, require_premium_fn=self.require_premium)

    def set_experiment_status(
        self, user_id: str, experiment_id: str, status: str
    ) -> Dict[str, Any]:
        return self.guidance_svc.set_experiment_status(
            user_id, experiment_id, status, require_premium_fn=self.require_premium
        )

    def ask(self, user_id: str, question: str, thread_id: Optional[str] = None) -> Dict[str, Any]:
        return self.guidance_svc.ask(
            user_id, question, thread_id,
            require_premium_fn=self.require_premium,
            history_fn=self.history,
            record_engagement_fn=self.record_engagement
        )

    def qna_history(self, user_id: str) -> List[Dict[str, Any]]:
        return self.guidance_svc.qna_history(user_id, require_premium_fn=self.require_premium)

    def scan_shelf(self, user_id: str, image_bytes: bytes) -> Dict[str, Any]:
        return self.guidance_svc.scan_shelf(user_id, image_bytes, record_engagement_fn=self.record_engagement)

    def shelf_scan_status(self, user_id: str, job_id: str) -> Dict[str, Any]:
        return self.guidance_svc.shelf_scan_status(user_id, job_id)

    def confirm_shelf_scan(
        self, user_id: str, job_id: str, selections: list[dict]
    ) -> List[Dict[str, Any]]:
        return self.guidance_svc.confirm_shelf_scan(
            user_id, job_id, selections, record_engagement_fn=self.record_engagement
        )

    def predict_product(self, user_id: str, product_id: str) -> Dict[str, Any]:
        return self.commerce_svc.predict_product(
            user_id, product_id,
            require_premium_fn=self.require_premium,
            refresh_verdicts_fn=self.refresh_verdicts
        )

    def lookup_product(self, barcode: str) -> Optional[Dict[str, Any]]:
        return self.commerce_svc.lookup_product(barcode)

    def purchase_guidance(
        self,
        user_id: str,
        name: Optional[str] = None,
        barcode: Optional[str] = None,
        ingredients=None,
        category: str = "other",
        price_cents: Optional[int] = None,
        currency: str = "INR",
    ) -> Dict[str, Any]:
        return self.commerce_svc.purchase_guidance(
            user_id, name, barcode, ingredients, category, price_cents, currency,
            require_premium_fn=self.require_premium,
            refresh_verdicts_fn=self.refresh_verdicts
        )

    def budget_optimizer(self, user_id: str) -> Dict[str, Any]:
        return self.commerce_svc.budget_optimizer(
            user_id,
            require_premium_fn=self.require_premium,
            refresh_verdicts_fn=self.refresh_verdicts
        )

    def dermatologist_report(self, user_id: str) -> Dict[str, Any]:
        return self.guidance_svc.dermatologist_report(
            user_id,
            profile_fn=self.profile,
            history_fn=self.history,
            require_premium_fn=self.require_premium,
            refresh_verdicts_fn=self.refresh_verdicts
        )

    def discover(self, user_id: str) -> Dict[str, Any]:
        return self.commerce_svc.discover(user_id, require_premium_fn=self.require_premium)

    def add_offer(
        self,
        product_id: str,
        merchant: str,
        url: str,
        price_cents: Optional[int] = None,
        currency: str = "USD",
    ) -> Dict[str, Any]:
        return self.commerce_svc.add_offer(product_id, merchant, url, price_cents, currency)

    def offers(self, user_id: str, product_id: Optional[str] = None) -> List[Dict[str, Any]]:
        return self.commerce_svc.offers(user_id, product_id)

    def click_offer(self, user_id: str, offer_id: str) -> Dict[str, Any]:
        return self.commerce_svc.click_offer(user_id, offer_id, record_engagement_fn=self.record_engagement)

    def ingredient_explainer(self, user_id: str, product_id: str) -> Dict[str, Any]:
        return self.commerce_svc.ingredient_explainer(
            user_id, product_id, require_premium_fn=self.require_premium
        )

    def product_detail(self, user_id: str, product_id: str) -> Dict[str, Any]:
        return self.commerce_svc.product_detail(user_id, product_id, self.is_premium(user_id), offers_fn=self.offers)

    def dashboard(self, user_id: str, vertical: str = "skin") -> Dict[str, Any]:
        return self.guidance_svc.dashboard(
            user_id, vertical,
            profile_fn=self.profile,
            history_fn=self.history,
            verdicts_for_user_fn=self.verdicts_for_user,
            experiments_fn=self.experiments,
            engagement_fn=self.engagement,
            analytics_fn=self.analytics,
            weekly_recap_fn=self.weekly_recap,
            check_ins_fn=self.check_ins
        )

    def triage_question(self, text: str) -> Dict[str, Any]:
        return self.guidance_svc.triage_question(text)

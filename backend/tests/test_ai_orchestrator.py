from __future__ import annotations

from pathlib import Path
from types import SimpleNamespace

from glowupai.ai_contracts import DataClass, ProviderResponse, ProviderUsage
from glowupai.ai_orchestrator import AIOrchestrator, AIOrchestrationError
from glowupai.full_db import FullDatabase

VISION = {
    "quality": {"usable": True, "issues": []},
    "observations": [],
    "limitations": [],
    "summary": "Visible appearance only; not a diagnosis.",
}
LANGUAGE = {
    "answer": "Use the stored observation as a limited, non-diagnostic guide.",
    "evidence_ids": ["analysis-1"],
    "limitations": [],
    "suggested_actions": [],
}


class FakeVision:
    def __init__(self, value=VISION):
        self.value = value
        self.calls = []

    def analyze_image(self, image_bytes, **kwargs):
        self.calls.append((image_bytes, kwargs))
        return ProviderResponse(
            output=self.value,
            usage=ProviderUsage(input_tokens=4, output_tokens=5),
            model_id="gpt-5.6-luna",
        )

    def generate_text(self, evidence, **kwargs):
        self.calls.append((evidence, kwargs))
        return ProviderResponse(
            output=LANGUAGE,
            usage=ProviderUsage(input_tokens=2, output_tokens=3),
            model_id="gpt-5.6-luna",
        )


class FakeLanguage:
    def __init__(self, value=LANGUAGE, fail=False):
        self.value = value
        self.fail = fail
        self.calls = []

    def generate_text(self, evidence, **kwargs):
        self.calls.append((evidence, kwargs))
        if self.fail:
            raise RuntimeError("simulated Gemini outage")
        return ProviderResponse(
            output=self.value,
            usage=ProviderUsage(input_tokens=3, output_tokens=4),
            model_id="gemini-3.5-flash-lite",
        )


class FakeSettings(SimpleNamespace):
    luna_enabled = True
    luna_model = "gpt-5.6-luna"
    luna_reasoning_effort = "medium"
    luna_monthly_spend_cap_usd = 1.0
    luna_image_detail = "low"
    gemini_enabled = True
    gemini_model = "gemini-3.5-flash-lite"
    gemini_personal_data_enabled = False
    gemini_eligibility_review_id = None


class PersonalGeminiSettings(FakeSettings):
    gemini_personal_data_enabled = True
    gemini_eligibility_review_id = "test-owner-approval"


def make_orchestrator(
    tmp_path: Path, *, language=None, local_renderer=None, settings=None
):
    db = FullDatabase(tmp_path / "orchestrator.sqlite3")
    db.execute("INSERT INTO users (id) VALUES ('owner')")
    db.execute(
        "INSERT INTO photo_captures (id, user_id, captured_at, raw_ref, capture_quality_json) "
        "VALUES ('capture-1', 'owner', datetime('now'), 'memory://capture-1', '{}')"
    )
    db.execute(
        "INSERT INTO photo_captures (id, user_id, captured_at, raw_ref, capture_quality_json) "
        "VALUES ('capture-2', 'owner', datetime('now'), 'memory://capture-2', '{}')"
    )
    db.execute(
        "INSERT INTO photo_captures (id, user_id, captured_at, raw_ref, capture_quality_json) "
        "VALUES ('capture-busy', 'owner', datetime('now'), 'memory://capture-busy', '{}')"
    )
    return db, AIOrchestrator(
        luna=FakeVision(),
        gemini=language,
        db=db,
        settings=settings or FakeSettings(),
        local_renderer=local_renderer,
    )


def test_vision_then_gemini_success_and_persists_provenance(tmp_path):
    gemini = FakeLanguage({**LANGUAGE, "evidence_ids": []})
    db, orchestrator = make_orchestrator(tmp_path, language=gemini)
    try:
        result = orchestrator.run_capture_analysis(
            user_id="owner",
            capture_id="capture-1",
            image_bytes=b"derivative",
            data_class=DataClass.PRODUCT_ONLY,
        )
        assert result.language_mode.value == "gemini_primary"
        assert len(orchestrator.luna.calls) == 1
        assert len(gemini.calls) == 1
        assert "image_bytes" not in gemini.calls[0][1]
        row = db.fetchone(
            "SELECT * FROM ai_analyses WHERE id=?", (result.analysis["id"],)
        )
        assert row["vision_provider"] == "openai"
        assert row["vision_reasoning_effort"] == "medium"
        assert row["language_mode"] == "gemini_primary"
    finally:
        db.close()


def test_personal_face_runs_luna_vision_then_gemini_text_without_image(tmp_path):
    gemini = FakeLanguage({**LANGUAGE, "evidence_ids": []})
    db, orchestrator = make_orchestrator(
        tmp_path,
        language=gemini,
        settings=PersonalGeminiSettings(),
    )
    try:
        result = orchestrator.run_capture_analysis(
            user_id="owner",
            capture_id="capture-1",
            image_bytes=b"face-derivative",
            data_class=DataClass.PERSONAL_FACE,
        )

        assert result.language_mode.value == "gemini_primary"
        assert result.gemini_policy.allowed
        assert len(orchestrator.luna.calls) == 1
        assert len(gemini.calls) == 1
        evidence, kwargs = gemini.calls[0]
        assert "image_bytes" not in kwargs
        assert evidence["evidence"][0]["type"] == "vision_observation"
    finally:
        db.close()


def test_gemini_failure_uses_one_luna_text_fallback_without_image(tmp_path):
    gemini = FakeLanguage(fail=True)
    db, orchestrator = make_orchestrator(tmp_path, language=gemini)
    try:
        result = orchestrator.run_language(
            user_id="owner",
            evidence={
                "evidence_ids": ["analysis-1"],
                "evidence": [
                    {"id": "analysis-1", "value": {"summary": "Stored evidence"}}
                ],
            },
            task_type="qa",
            data_class=DataClass.PRODUCT_ONLY,
            request_identity="qa-1",
        )
        assert result["language_mode"] == "luna_fallback"
        assert len(gemini.calls) == 1
        assert len(orchestrator.luna.calls) == 1
        evidence, kwargs = orchestrator.luna.calls[0]
        assert "image_bytes" not in kwargs
        assert kwargs["reasoning_effort"] == "medium"
        assert evidence["evidence_ids"] == ["analysis-1"]
    finally:
        db.close()


def test_local_fallback_only_after_gemini_and_luna_fail(tmp_path):
    gemini = FakeLanguage(fail=True)

    class FailingLuna(FakeVision):
        def generate_text(self, evidence, **kwargs):
            self.calls.append((evidence, kwargs))
            raise RuntimeError("simulated Luna outage")

    db, orchestrator = make_orchestrator(
        tmp_path,
        language=gemini,
        local_renderer=lambda evidence: {
            "answer": "Local rendering",
            "evidence_ids": evidence["evidence_ids"],
            "limitations": [],
            "suggested_actions": [],
        },
    )
    orchestrator.luna = FailingLuna()
    try:
        result = orchestrator.run_language(
            user_id="owner",
            evidence={
                "evidence_ids": ["analysis-1"],
                "evidence": [
                    {"id": "analysis-1", "value": {"summary": "Stored evidence"}}
                ],
            },
            task_type="qa",
            data_class=DataClass.PRODUCT_ONLY,
            request_identity="qa-2",
        )
        assert result["language_mode"] == "local_fallback"
        assert len(gemini.calls) == 1
        assert len(orchestrator.luna.calls) == 1
    finally:
        db.close()


def test_completed_capture_request_is_cached_without_provider_calls(tmp_path):
    gemini = FakeLanguage({**LANGUAGE, "evidence_ids": []})
    db, orchestrator = make_orchestrator(tmp_path, language=gemini)
    try:
        first = orchestrator.run_capture_analysis(
            user_id="owner",
            capture_id="capture-2",
            image_bytes=b"same",
            data_class=DataClass.PRODUCT_ONLY,
        )
        second = orchestrator.run_capture_analysis(
            user_id="owner",
            capture_id="capture-2",
            image_bytes=b"same",
            data_class=DataClass.PRODUCT_ONLY,
        )
        assert second.analysis["id"] == first.analysis["id"]
        assert len(orchestrator.luna.calls) == 1
        assert len(gemini.calls) == 1
    finally:
        db.close()


def test_personal_evidence_is_not_sent_without_reviewed_opt_in(tmp_path):
    gemini = FakeLanguage()
    db, orchestrator = make_orchestrator(tmp_path, language=gemini)
    try:
        result = orchestrator.run_language(
            user_id="owner",
            evidence={
                "evidence_ids": ["analysis-1"],
                "evidence": [
                    {"id": "analysis-1", "value": {"summary": "Stored evidence"}}
                ],
            },
            task_type="qa",
            data_class=DataClass.PERSONAL_HISTORY,
            request_identity="qa-3",
        )
        assert result["language_mode"] == "local_fallback"
        assert gemini.calls == []
        assert orchestrator.luna.calls == []
    finally:
        db.close()


def test_duplicate_inflight_capture_does_not_call_luna_twice(tmp_path):
    db, orchestrator = make_orchestrator(tmp_path, language=None)
    try:
        # Seed the same request key as run_capture_analysis, then mark it
        # running to emulate another worker owning the work item.
        import hashlib

        digest = hashlib.sha256(b"busy").hexdigest()
        key = orchestrator._request_key(
            "owner",
            "capture-busy",
            "vision_observation",
            digest,
            "skin-vision-v1",
            "face-derivative-v1",
        )
        orchestrator.storage.create_analysis(
            user_id="owner",
            capture_id="capture-busy",
            task_type="vision_observation",
            request_key=key,
            schema_version="cosmetic-observation-v1",
            vision_provider="openai",
            vision_model_id="gpt-5.6-luna",
            prompt_version="skin-vision-v1",
            preprocessing_version="face-derivative-v1",
            input_digest=digest,
            data_class="product_only",
            policy_decision="pending",
            status="running",
        )
        try:
            orchestrator.run_capture_analysis(
                user_id="owner",
                capture_id="capture-busy",
                image_bytes=b"busy",
                data_class=DataClass.PRODUCT_ONLY,
            )
        except AIOrchestrationError as exc:
            assert exc.code == "analysis_in_progress"
        else:
            raise AssertionError("expected in-flight request to be rejected")
        assert orchestrator.luna.calls == []
    finally:
        db.close()

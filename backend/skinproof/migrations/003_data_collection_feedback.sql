-- Migration: Data Collection, Feedback, and Monitoring Tables
-- Date: 2026-09-01
-- Purpose: Add tables for data collection pipeline, feedback loop, and model monitoring

-- Table: collection_log
-- Tracks anonymized data collected for training
CREATE TABLE IF NOT EXISTS collection_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    face_id TEXT NOT NULL,  -- Anonymized user hash
    anonymous_capture_id TEXT NOT NULL,
    collected_at TEXT NOT NULL,
    quality_score REAL NOT NULL,
    model_version TEXT NOT NULL,
    UNIQUE(face_id, anonymous_capture_id)
);

CREATE INDEX IF NOT EXISTS idx_collection_log_collected_at ON collection_log(collected_at);
CREATE INDEX IF NOT EXISTS idx_collection_log_face_id ON collection_log(face_id);

-- Table: capture_feedback
-- User feedback on capture analysis accuracy
CREATE TABLE IF NOT EXISTS capture_feedback (
    id TEXT PRIMARY KEY,
    capture_id TEXT NOT NULL REFERENCES photo_captures(id) ON DELETE CASCADE,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    feedback_type TEXT NOT NULL CHECK (feedback_type IN ('accurate', 'inaccurate')),
    issues_json TEXT NOT NULL DEFAULT '[]',  -- List of issues: ["blemishes_too_high", "redness_too_low"]
    corrections_json TEXT NOT NULL DEFAULT '{}',  -- User corrections: {"blemish_count": 15, "redness_score": 0.25}
    original_metrics_json TEXT NOT NULL DEFAULT '{}',  -- Original prediction values
    comment TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_feedback_capture ON capture_feedback(capture_id);
CREATE INDEX IF NOT EXISTS idx_feedback_user ON capture_feedback(user_id);
CREATE INDEX IF NOT EXISTS idx_feedback_type ON capture_feedback(feedback_type);
CREATE INDEX IF NOT EXISTS idx_feedback_created_at ON capture_feedback(created_at);

-- Table: model_predictions
-- Track all model predictions for monitoring
CREATE TABLE IF NOT EXISTS model_predictions (
    id TEXT PRIMARY KEY,
    capture_id TEXT NOT NULL,
    predictions_json TEXT NOT NULL,  -- {"blemish_count": 12, "redness_score": 0.34, ...}
    processing_time_ms REAL NOT NULL,
    error TEXT,  -- NULL if successful, error message if failed
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_predictions_capture ON model_predictions(capture_id);
CREATE INDEX IF NOT EXISTS idx_predictions_created_at ON model_predictions(created_at);
CREATE INDEX IF NOT EXISTS idx_predictions_error ON model_predictions(error);

-- Table: model_health_log
-- Log model health check results over time
CREATE TABLE IF NOT EXISTS model_health_log (
    id TEXT PRIMARY KEY,
    status TEXT NOT NULL CHECK (status IN ('healthy', 'degraded', 'critical')),
    variance_json TEXT NOT NULL,  -- Variance scores per metric
    error_rate REAL NOT NULL,
    drift_json TEXT NOT NULL,  -- Drift scores per metric
    issues_json TEXT NOT NULL DEFAULT '[]',  -- List of detected issues
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_health_log_created_at ON model_health_log(created_at);
CREATE INDEX IF NOT EXISTS idx_health_log_status ON model_health_log(status);

-- Add data_collection consent type to existing consent_events table
-- Users must explicitly opt-in to data collection

-- Update users table to track data collection consent
-- (using existing consent_events table with consent_type = 'data_collection')

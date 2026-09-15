"""Google Play subscription verification. Client claims never grant access."""

from __future__ import annotations

import base64
import binascii
import hashlib
import json
import os
from datetime import UTC, datetime
from urllib.parse import quote

from cryptography.hazmat.primitives.ciphers.aead import AESGCM


def account_id(user_id: str) -> str:
    """Return the stable, non-reversible identifier sent to Google Play."""
    return hashlib.sha256(user_id.encode("utf-8")).hexdigest()


class PlayPurchaseNotFound(ValueError):
    """Google Play no longer has a record for a purchase token."""


def token_cipher():
    key = base64.b64decode(os.getenv("GLOWUPAI_BILLING_KEY", ""), validate=True)
    if len(key) != 32:
        raise RuntimeError("GLOWUPAI_BILLING_KEY must be base64 for 32 bytes")
    return AESGCM(key)


def encrypt_token(user_id, token):
    nonce = os.urandom(12)
    return base64.b64encode(
        nonce
        + token_cipher().encrypt(nonce, token.encode("utf-8"), user_id.encode("utf-8"))
    ).decode()


def decrypt_token(user_id, ciphertext):
    blob = base64.b64decode(ciphertext, validate=True)
    return (
        token_cipher()
        .decrypt(blob[:12], blob[12:], user_id.encode("utf-8"))
        .decode("utf-8")
    )


def verify_notification_identity(authorization):
    from google.auth.transport.requests import Request
    from google.oauth2.id_token import verify_oauth2_token

    audience = os.getenv("GLOWUPAI_PLAY_RTDN_AUDIENCE", "").strip()
    email = os.getenv("GLOWUPAI_PLAY_RTDN_EMAIL", "").strip()
    if (
        not audience
        or not email
        or not authorization
        or not authorization.startswith("Bearer ")
    ):
        raise PermissionError("Notification authentication required")
    try:
        claims = verify_oauth2_token(authorization[7:], Request(), audience=audience)
    except Exception:
        raise PermissionError("Invalid notification identity") from None
    if claims.get("email") != email or claims.get("email_verified") is not True:
        raise PermissionError("Unexpected notification identity")


class PlayBilling:
    def __init__(self, *, session=None):
        self.session = session

    def _session(self):
        if self.session is None:
            from google.auth.transport.requests import AuthorizedSession
            from google.oauth2.service_account import Credentials

            scopes = ["https://www.googleapis.com/auth/androidpublisher"]
            encoded_credentials = os.getenv(
                "GLOWUPAI_PLAY_SERVICE_ACCOUNT_JSON_B64", ""
            ).strip()
            if encoded_credentials:
                try:
                    service_account_info = json.loads(
                        base64.b64decode(encoded_credentials, validate=True).decode(
                            "utf-8"
                        )
                    )
                    credentials = Credentials.from_service_account_info(
                        service_account_info, scopes=scopes
                    )
                except (binascii.Error, ValueError, TypeError) as exc:
                    raise RuntimeError(
                        "GLOWUPAI_PLAY_SERVICE_ACCOUNT_JSON_B64 is invalid"
                    ) from exc
            else:
                import google.auth

                credentials, _ = google.auth.default(scopes=scopes)
            self.session = AuthorizedSession(credentials)
        return self.session

    @staticmethod
    def _configuration() -> tuple[str, set[str]]:
        package = os.getenv("GLOWUPAI_PLAY_PACKAGE_NAME", "").strip()
        products = set(
            filter(
                None,
                (
                    s.strip()
                    for s in os.getenv("GLOWUPAI_PLAY_PRODUCT_IDS", "").split(",")
                ),
            )
        )
        if not package or not products:
            raise PermissionError("Google Play subscriptions are not configured yet")
        return package, products

    def get_subscription(self, purchase_token: str) -> dict:
        """Fetch the complete subscription resource from Google's server API."""
        package, _ = self._configuration()
        root = f"https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{quote(package, safe='')}/purchases"
        token = quote(purchase_token, safe="")
        response = self._session().get(
            f"{root}/subscriptionsv2/tokens/{token}", timeout=20
        )
        if response.status_code in (404, 410):
            raise PlayPurchaseNotFound("Google Play purchase was not found")
        if response.status_code != 200:
            raise RuntimeError(
                "Google Play purchase verification is temporarily unavailable"
            )
        try:
            purchase = response.json()
        except (TypeError, ValueError) as exc:
            raise RuntimeError(
                "Google Play returned an invalid purchase response"
            ) from exc
        if not isinstance(purchase, dict):
            raise RuntimeError("Google Play returned an invalid purchase response")
        return purchase

    @staticmethod
    def obfuscated_account_id(purchase: dict) -> str | None:
        identifiers = purchase.get("externalAccountIdentifiers") or {}
        if not isinstance(identifiers, dict):
            return None
        value = identifiers.get("obfuscatedExternalAccountId")
        return value if isinstance(value, str) and value else None

    def verify(self, user_id, purchase_token, *, purchase: dict | None = None):
        package, products = self._configuration()
        purchase = purchase or self.get_subscription(purchase_token)
        bound = self.obfuscated_account_id(purchase)
        if bound != account_id(user_id):
            raise PermissionError("This purchase belongs to a different app account")
        items = [
            item
            for item in purchase.get("lineItems", [])
            if item.get("productId") in products
        ]
        if not items:
            raise ValueError(
                "Purchase does not contain an approved subscription product"
            )
        state = purchase.get("subscriptionState")
        expiry_values = []
        for item in items:
            expiry_time = item.get("expiryTime")
            if not expiry_time:
                continue
            try:
                expiry_values.append(
                    datetime.fromisoformat(expiry_time.replace("Z", "+00:00"))
                )
            except (AttributeError, TypeError, ValueError):
                raise ValueError("Purchase has no valid subscription expiry") from None
        if expiry_values:
            expiry = max(expiry_values)
        elif state in {
            "SUBSCRIPTION_STATE_PENDING",
            "SUBSCRIPTION_STATE_PENDING_PURCHASE_CANCELED",
        }:
            # Pending subscriptions do not necessarily have an expiryTime yet. They
            # are intentionally persisted as inactive and are never acknowledged.
            expiry = datetime.now(UTC)
        else:
            raise ValueError("Purchase has no valid subscription expiry")
        active = state in {
            "SUBSCRIPTION_STATE_ACTIVE",
            "SUBSCRIPTION_STATE_IN_GRACE_PERIOD",
            "SUBSCRIPTION_STATE_CANCELED",
        } and expiry > datetime.now(UTC)
        if (
            active
            and purchase.get("acknowledgementState") == "ACKNOWLEDGEMENT_STATE_PENDING"
        ):
            root = f"https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{quote(package, safe='')}/purchases"
            token = quote(purchase_token, safe="")
            product = quote(items[0]["productId"], safe="")
            acknowledged = self._session().post(
                f"{root}/subscriptions/{product}/tokens/{token}:acknowledge",
                json={},
                timeout=20,
            )
            if acknowledged.status_code not in (200, 204):
                raise ValueError(
                    "Google Play purchase acknowledgement failed; restore purchases to retry"
                )
        return {
            "active": active,
            "expires_at": expiry.isoformat().replace("+00:00", "Z"),
        }

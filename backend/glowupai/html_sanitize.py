"""HTML sanitization for user-generated content and exports.

Prevents XSS attacks when rendering HTML in WebViews or web clients.
"""

from __future__ import annotations

import html
import re
from collections.abc import Collection

# Default allowed HTML tags for dermatologist export
SAFE_TAGS = {
    "p",
    "br",
    "strong",
    "em",
    "b",
    "i",
    "u",
    "h1",
    "h2",
    "h3",
    "h4",
    "h5",
    "h6",
    "ul",
    "ol",
    "li",
    "table",
    "thead",
    "tbody",
    "tr",
    "th",
    "td",
    "div",
    "span",
    "hr",
}

# Default allowed attributes
SAFE_ATTRS = {
    "*": {"class", "style"},  # Allow class and style on all elements
    "table": {"border", "cellpadding", "cellspacing"},
    "td": {"colspan", "rowspan"},
    "th": {"colspan", "rowspan"},
}

# Safe CSS properties (for inline styles)
SAFE_CSS_PROPS = {
    "color",
    "background-color",
    "font-size",
    "font-weight",
    "font-style",
    "text-align",
    "text-decoration",
    "padding",
    "margin",
    "border",
    "border-radius",
    "width",
    "height",
    "display",
}


def sanitize_html(
    html_content: str,
    allowed_tags: Collection[str] | None = None,
) -> str:
    """Sanitize HTML content by removing dangerous tags and attributes.

    This is a simple whitelist-based sanitizer. For production, consider using
    a battle-tested library like `bleach` or `nh3` (Rust-based).

    Args:
        html_content: Raw HTML string
        allowed_tags: Set of allowed HTML tags (defaults to SAFE_TAGS)

    Returns:
        Sanitized HTML string
    """
    if not html_content or not html_content.strip():
        return ""

    allowed = allowed_tags or SAFE_TAGS

    # Remove script and style tags entirely
    html_content = re.sub(
        r"<script\b[^>]*>.*?</script>",
        "",
        html_content,
        flags=re.IGNORECASE | re.DOTALL,
    )
    html_content = re.sub(
        r"<style\b[^>]*>.*?</style>",
        "",
        html_content,
        flags=re.IGNORECASE | re.DOTALL,
    )

    # Remove on* event handlers (onclick, onerror, etc.)
    html_content = re.sub(
        r'\bon\w+\s*=\s*["\'][^"\']*["\']',
        "",
        html_content,
        flags=re.IGNORECASE,
    )
    html_content = re.sub(
        r"\bon\w+\s*=\s*[^>\s]+",
        "",
        html_content,
        flags=re.IGNORECASE,
    )

    # Remove javascript: URLs
    html_content = re.sub(r"javascript:", "", html_content, flags=re.IGNORECASE)

    # Remove data: URLs (can contain base64-encoded scripts)
    html_content = re.sub(r"data:", "", html_content, flags=re.IGNORECASE)

    # Simple tag filtering (not perfect, but better than nothing)
    # For production, use a proper HTML parser like BeautifulSoup or lxml
    def replace_tag(match):
        tag = match.group(1).lower()
        if tag in allowed:
            return match.group(0)  # Keep allowed tags
        return ""  # Remove disallowed tags

    # Remove disallowed tags
    html_content = re.sub(r"<(/?)(\w+)([^>]*)>", replace_tag, html_content)

    return html_content


def sanitize_css(css_value: str) -> str:
    """Sanitize inline CSS to remove dangerous properties.

    Args:
        css_value: Raw CSS string (e.g., "color: red; background: url(...)")

    Returns:
        Sanitized CSS string
    """
    if not css_value or not css_value.strip():
        return ""

    # Remove url() functions (can load external resources)
    css_value = re.sub(r"url\([^)]*\)", "", css_value, flags=re.IGNORECASE)

    # Remove @import
    css_value = re.sub(r"@import[^;]*;", "", css_value, flags=re.IGNORECASE)

    # Remove expression() (IE-specific XSS vector)
    css_value = re.sub(r"expression\([^)]*\)", "", css_value, flags=re.IGNORECASE)

    # Parse and filter properties
    safe_props = []
    for prop in css_value.split(";"):
        if ":" not in prop:
            continue
        name, value = prop.split(":", 1)
        name = name.strip().lower()
        if name in SAFE_CSS_PROPS:
            safe_props.append(f"{name}: {value.strip()}")

    return "; ".join(safe_props)


def escape_html(text: str) -> str:
    """Escape HTML special characters in text content.

    This is for rendering user input as plain text, not HTML.

    Args:
        text: Plain text string

    Returns:
        HTML-escaped string
    """
    return html.escape(text)


# Production-grade sanitizer using bleach (optional dependency)
def sanitize_html_bleach(html_content: str) -> str:
    """Sanitize HTML using bleach library (if available).

    Install: pip install bleach

    This is more robust than the regex-based sanitizer above.
    """
    try:
        import bleach
    except ImportError:
        # Fallback to regex-based sanitizer
        return sanitize_html(html_content)

    result: str = bleach.clean(
        html_content,
        tags=list(SAFE_TAGS),
        attributes=SAFE_ATTRS,
        strip=True,  # Strip disallowed tags instead of escaping
        strip_comments=True,
    )
    return result


# Example usage for dermatologist export
def sanitize_derm_export(html_content: str) -> str:
    """Sanitize dermatologist export HTML.

    This should be called on the backend before sending HTML to the client.
    """
    # Try bleach first (more secure), fall back to regex
    # sanitize_html_bleach already handles ImportError internally
    return sanitize_html_bleach(html_content)

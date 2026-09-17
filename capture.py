"""
mitmproxy addon: capture all HTTP(S) traffic to JSONL + human-readable log.

Usage:
    mitmdump -s capture.py -p 8080
"""
import json
import os
import re
import time
from datetime import datetime, timezone

from mitmproxy import http, ctx

OUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "captures")
os.makedirs(OUT_DIR, exist_ok=True)

JSONL = os.path.join(OUT_DIR, "traffic.jsonl")
TEXTLOG = os.path.join(OUT_DIR, "traffic.log")

# Hosts we don't care about (Apple telemetry, push, etc). Kept but not spammed.
NOISE = re.compile(
    r"(apple\.com|icloud\.com|mzstatic\.com|push\.apple|guzzoni|\.akadns\.|"
    r"gstatic\.com|googleapis\.com|google\.com|doubleclick|criteo|facebook|"
    r"app-measurement|firebase|adjust\.com|amplitude|sentry|bugsnag)",
    re.I,
)

CONTENT_TEXT = re.compile(r"^(text/|application/(json|xml|x-www-form-urlencoded|javascript))", re.I)
MAX_BODY = 200_000


def _decode_body(msg: http.Message) -> str | None:
    try:
        raw = msg.content
    except Exception:
        return None
    if raw is None:
        return None
    if len(raw) > MAX_BODY:
        return f"<body too large: {len(raw)} bytes>"
    ctype = msg.headers.get("content-type", "")
    if CONTENT_TEXT.search(ctype) or not ctype:
        try:
            return raw.decode("utf-8", "replace")
        except Exception:
            return None
    try:
        return raw.decode("utf-8")
    except Exception:
        import base64

        return "<binary base64>" + base64.b64encode(raw[:2000]).decode()


class Capture:
    def __init__(self):
        self.seq = 0

    def _record(self, kind: str, flow: http.HTTPFlow, extra: dict | None = None):
        req = flow.request
        rec = {
            "ts": datetime.now(timezone.utc).isoformat(),
            "kind": kind,
            "method": req.method,
            "scheme": req.scheme,
            "host": req.pretty_host,
            "path": req.path,
            "url": req.pretty_url,
            "client": flow.client_conn.peername[0] if flow.client_conn.peername else None,
            "request_headers": dict(req.headers),
            "request_body": _decode_body(req),
        }
        if flow.response is not None:
            rec["status_code"] = flow.response.status_code
            rec["response_headers"] = dict(flow.response.headers)
            rec["response_body"] = _decode_body(flow.response)
        if extra:
            rec.update(extra)

        with open(JSONL, "a") as f:
            f.write(json.dumps(rec, ensure_ascii=False) + "\n")

        noisy = bool(NOISE.search(req.pretty_host))
        if not noisy or kind == "response":
            status = rec.get("status_code", "-")
            ctx.log.info(f"[{kind}] {req.method} {req.pretty_url} -> {status}")

    def request(self, flow: http.HTTPFlow):
        if flow.request.method == "CONNECT":
            return
        self._record("request", flow)

    def response(self, flow: http.HTTPFlow):
        if flow.request.method == "CONNECT":
            return
        self._record("response", flow)

    def error(self, flow: http.HTTPFlow):
        self._record("error", flow, {"error": str(flow.error)})

    def running(self):
        ctx.log.info(f"Output: {JSONL}")


addons = [Capture()]

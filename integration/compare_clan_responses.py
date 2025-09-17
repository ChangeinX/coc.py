#!/usr/bin/env python3
"""Compare clan JSON between the legacy Python client and the Java client.

Both clients authenticate against the Clash of Clans developer site using the
same credentials supplied via environment variables (see README/MIGRATION).

Usage (from repository root):
    export COC_EMAIL=...
    export COC_PASSWORD=...
    python integration/compare_clan_responses.py

The script uses clan tag #2JLYPYRRY by default, but a different tag can be
provided via the optional COC_CLAN_TAG environment variable.
"""

from __future__ import annotations

import asyncio
import json
import os
import subprocess
import sys
from pathlib import Path
from typing import Any, Dict

REPO_ROOT = Path(__file__).resolve().parents[1]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

DEFAULT_TAG = "#2JLYPYRRY"


def _require_env(name: str) -> str:
    value = os.getenv(name)
    if not value:
        raise SystemExit(f"Missing required environment variable: {name}")
    return value


async def _fetch_python_clan(tag: str, email: str, password: str) -> Dict[str, Any]:
    from coc import Client

    client = Client(raw_attribute=True, key_count=1, throttle_limit=10)
    try:
        await client.login(email, password)
        clan = await client.get_clan(tag)
        raw = getattr(clan, "_raw_data", None)
        if raw is None:
            raise RuntimeError("Python client did not expose raw data; enable raw_attribute")
        return raw
    finally:
        if client.http:
            await client.close()


def _fetch_java_clan(tag: str, env: Dict[str, str], repo_root: Path) -> Dict[str, Any]:
    gradlew = str(repo_root / "gradlew.bat") if os.name == "nt" else str(repo_root / "gradlew")
    cmd = [gradlew, "-q", ":coc-java:runClanRawDump", f"--args={tag}"]
    result = subprocess.run(cmd, capture_output=True, text=True, env=env, cwd=repo_root, check=False)
    if result.returncode != 0:
        sys.stderr.write(result.stderr)
        raise SystemExit(f"Java client execution failed with exit code {result.returncode}")
    output = result.stdout.strip()
    if not output:
        raise SystemExit("Java client did not return any JSON output")
    try:
        return json.loads(output)
    except json.JSONDecodeError as exc:
        raise SystemExit(f"Failed to parse Java client output as JSON: {exc}\nOutput was:\n{output}")


def _compare_dicts(left: Dict[str, Any], right: Dict[str, Any]) -> None:
    ignore_keys = {"builderBaseRank", "status_code", "timestamp"}

    def normalize(value: Any) -> Any:
        if isinstance(value, dict):
            return {
                k: normalize(v)
                for k, v in value.items()
                if not k.startswith("_") and k not in ignore_keys
            }
        if isinstance(value, list):
            return [normalize(item) for item in value]
        return value

    left = normalize(left)
    right = normalize(right)

    if left == right:
        print("Success: Python and Java responses match byte-for-byte.")
        return

    left_text = json.dumps(left, sort_keys=True)
    right_text = json.dumps(right, sort_keys=True)
    if left_text == right_text:
        print("Success: Python and Java responses match after key normalization.")
        return

    def find_difference(a: Any, b: Any, path: str = "root") -> str:
        if type(a) is not type(b):
            return f"{path}: type mismatch {type(a).__name__} vs {type(b).__name__}"
        if isinstance(a, dict):
            keys = sorted(set(a) | set(b))
            for key in keys:
                if key not in a:
                    return f"{path}.{key}: missing from Python payload"
                if key not in b:
                    return f"{path}.{key}: missing from Java payload"
                diff = find_difference(a[key], b[key], f"{path}.{key}")
                if diff:
                    return diff
            return ""
        if isinstance(a, list):
            if len(a) != len(b):
                return f"{path}: list length {len(a)} vs {len(b)}"
            for idx, (aval, bval) in enumerate(zip(a, b)):
                diff = find_difference(aval, bval, f"{path}[{idx}]")
                if diff:
                    return diff
            return ""
        if a != b:
            return f"{path}: value mismatch {a!r} vs {b!r}"
        return ""

    diff_message = find_difference(left, right)
    raise SystemExit(f"Mismatch detected between Python and Java payloads: {diff_message}")


async def main() -> None:
    email = _require_env("COC_EMAIL")
    password = _require_env("COC_PASSWORD")
    tag = os.getenv("COC_CLAN_TAG", DEFAULT_TAG)

    print(f"Fetching clan {tag} using python coc client...", flush=True)
    python_payload = await _fetch_python_clan(tag, email, password)

    repo_root = Path(__file__).resolve().parents[1]
    env = os.environ.copy()
    env.setdefault("COC_EMAIL", email)
    env.setdefault("COC_PASSWORD", password)
    print("Fetching clan using Java client via Gradle...", flush=True)
    java_payload = _fetch_java_clan(tag, env, repo_root)

    _compare_dicts(python_payload, java_payload)


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        raise SystemExit("Cancelled")

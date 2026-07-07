"""Test stage: really runs the Java test suite via the Maven wrapper and gates
on the actual process exit code - not a simulated/narrative pass.
"""
from __future__ import annotations

import glob
import os
import subprocess
import sys

from orchestrator.engine import RunContext, StageResult


def _discover_java_home() -> str | None:
    """Best-effort JDK discovery for environments where JAVA_HOME isn't set
    at the OS level. Never overrides an existing JAVA_HOME."""
    candidates = []
    if sys.platform.startswith("win"):
        for pattern in (r"C:\Program Files\Java\jdk*", r"C:\Program Files\Eclipse Adoptium\jdk*"):
            candidates.extend(glob.glob(pattern))
    else:
        for pattern in ("/usr/lib/jvm/*", "/usr/lib/jvm/java-*"):
            candidates.extend(glob.glob(pattern))
    return sorted(candidates)[-1] if candidates else None


def run(ctx: RunContext, params: dict) -> StageResult:
    test_filter = params.get("test_filter")
    is_windows = sys.platform.startswith("win")
    mvnw_name = "mvnw.cmd" if is_windows else "mvnw"
    mvnw = str(ctx.repo_root / mvnw_name)
    cmd = [mvnw, "-q", "test"]
    if test_filter:
        cmd.append(f"-Dtest={test_filter}")
    cmd.append("-DargLine=-Dnet.bytebuddy.experimental=true")

    env = dict(os.environ)
    if not env.get("JAVA_HOME"):
        discovered = _discover_java_home()
        if discovered:
            env["JAVA_HOME"] = discovered

    proc = subprocess.run(cmd, cwd=ctx.repo_root, capture_output=True, text=True, shell=is_windows, env=env)

    if proc.returncode != 0:
        tail = "\n".join((proc.stdout + proc.stderr).splitlines()[-40:])
        return StageResult(status="fail", notes=f"mvnw test failed (exit {proc.returncode}):\n{tail}")

    ctx.set("test.exit_code", proc.returncode)
    return StageResult(status="pass", output={"exit_code": proc.returncode}, notes="tests passed")

"""Tests for srtool's checks and its status command. They need no server: every outside thing is faked."""
import urllib.error

from srtool import checks, cli

GOOD = {
    "releases": [
        {"version": "2.0.0", "date": "2026-10-20", "sha256": "a", "broken": False},
        {"version": "2.0.1", "date": "2026-11-02", "sha256": "b", "broken": False},
        {"version": "2.0.2", "date": "2026-11-05", "sha256": "c", "broken": True},
    ]
}


def test_a_good_catalog_has_no_problems():
    assert checks.problems(GOOD) == []


def test_an_unquoted_version_is_found():
    bad = {"releases": [{"version": 2.1, "date": "2026-12-01", "sha256": "d", "broken": False}]}
    assert checks.problems(bad) == ["release 1: version must be a string, got float 2.1"]


def test_latest_good_skips_a_broken_release():
    assert checks.latest_good(GOOD) == "2.0.1"


def test_running_version_reads_the_version_file(tmp_path):
    (tmp_path / "current").mkdir()
    (tmp_path / "current" / "VERSION").write_text("2.0.1\n")
    assert checks.running_version(str(tmp_path)) == "2.0.1"


def test_status_says_2_when_the_app_cannot_be_reached(tmp_path, monkeypatch):
    (tmp_path / "current").mkdir()
    (tmp_path / "current" / "VERSION").write_text("2.0.1\n")

    def refused(url, timeout=5.0):
        raise urllib.error.URLError("Connection refused")

    monkeypatch.setattr(checks, "health", refused)
    monkeypatch.setattr(checks, "service_state", lambda unit: ["inactive", "dead"])
    assert cli.main(["--app-dir", str(tmp_path), "status"]) == 2

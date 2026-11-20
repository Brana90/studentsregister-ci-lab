"""The checks behind srtool: each one small, with no printing, so it can be tested on its own."""
import json
import subprocess
import urllib.request

import yaml

REQUIRED = ("version", "date", "sha256", "broken")


def running_version(app_dir):
    """The release that current points at, read from its VERSION file."""
    with open(app_dir + "/current/VERSION") as f:
        return f.read().strip()


def health(url, timeout=5.0):
    """The app's health answer as a dict, for example {"status": "UP", ...}."""
    with urllib.request.urlopen(url, timeout=timeout) as response:
        return json.load(response)


def service_state(unit):
    """ActiveState and SubState of one systemd unit; the name is one argument, never a shell line."""
    result = subprocess.run(
        ["systemctl", "show", unit, "--property=ActiveState,SubState", "--value"],
        capture_output=True, text=True, check=True, timeout=10,
    )
    return result.stdout.split()


def load_catalog(path):
    """The release catalog as plain Python data (safe_load: never objects)."""
    with open(path) as f:
        return yaml.safe_load(f)


def problems(catalog):
    """Every problem in the catalog, as a list of sentences; an empty list means it is fine."""
    found = []
    for number, release in enumerate(catalog.get("releases", []), start=1):
        for key in REQUIRED:
            if key not in release:
                found.append(f"release {number}: {key} is missing")
        for key in ("version", "date"):
            value = release.get(key)
            if value is not None and not isinstance(value, str):
                found.append(f"release {number}: {key} must be a string, "
                             f"got {type(value).__name__} {value}")
    return found


def latest_good(catalog):
    """The newest release that is not marked broken (the catalog lists them oldest first)."""
    good = [release["version"] for release in catalog["releases"] if not release["broken"]]
    return good[-1]

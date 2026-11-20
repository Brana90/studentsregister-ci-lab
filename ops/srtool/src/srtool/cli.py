"""srtool - the platform team's tool for StudentsRegister.

    srtool status              is the app up, which release runs, what systemd says
    srtool releases            the release catalog, with the running one marked
    srtool deploy VERSION      deploy one release with deploy.sh, unless the catalog says no
"""
import argparse
import logging
import subprocess
import sys
import urllib.error

from srtool import __version__, checks

log = logging.getLogger("srtool")

APP_DIR = "/opt/studentsregister"
HEALTH_URL = "http://127.0.0.1:8090/api/students/health"
CATALOG = "/home/dba/studentsregister/ops/releases.yml"
DEPLOY_SH = "/home/dba/studentsregister/ops/deploy.sh"


def cmd_status(args):
    try:
        version = checks.running_version(args.app_dir)
        state = " ".join(checks.service_state("studentsregister"))
        status = checks.health(args.url)["status"]
    except (OSError, urllib.error.URLError, subprocess.SubprocessError, ValueError, KeyError) as e:
        log.error("cannot check: %s", e)
        return 2
    print(f"version  {version}")
    print(f"systemd  {state}")
    print(f"health   {status}")
    return 0 if status == "UP" else 1


def cmd_releases(args):
    catalog = checks.load_catalog(args.catalog)
    found = checks.problems(catalog)
    for problem in found:
        log.error("catalog: %s", problem)
    if found:
        return 1
    running = checks.running_version(args.app_dir)
    for release in catalog["releases"]:
        mark = "*" if release["version"] == running else " "
        note = "BROKEN" if release["broken"] else ""
        print(f"{mark} {release['version']:8} {release['date']}  {note}")
    print(f"latest good: {checks.latest_good(catalog)}")
    return 0


def cmd_deploy(args):
    catalog = checks.load_catalog(args.catalog)
    release = next((r for r in catalog["releases"] if r["version"] == args.version), None)
    if release is None:
        log.error("%s is not in the catalog: refusing", args.version)
        return 1
    if release["broken"]:
        log.error("%s is marked broken in the catalog: refusing", args.version)
        return 1
    log.info("running %s %s", DEPLOY_SH, args.version)
    return subprocess.run([DEPLOY_SH, args.version], timeout=600).returncode


def parse_args(argv=None):
    parser = argparse.ArgumentParser(prog="srtool", description="The team's tool for StudentsRegister.")
    parser.add_argument("--version", action="version", version=f"srtool {__version__}")
    parser.add_argument("-v", "--verbose", action="store_true", help="also log the details (DEBUG)")
    parser.add_argument("--app-dir", default=APP_DIR, help="where the releases live (default: %(default)s)")
    parser.add_argument("--catalog", default=CATALOG, help="the release catalog (default: %(default)s)")
    sub = parser.add_subparsers(dest="command", required=True)

    status = sub.add_parser("status", help="is the app up, and which release runs")
    status.add_argument("--url", default=HEALTH_URL, help="health address (default: %(default)s)")
    status.set_defaults(func=cmd_status)

    releases = sub.add_parser("releases", help="the release catalog, with the running one marked")
    releases.set_defaults(func=cmd_releases)

    deploy = sub.add_parser("deploy", help="deploy one release with deploy.sh")
    deploy.add_argument("version", help="the release, e.g. 2.0.1")
    deploy.set_defaults(func=cmd_deploy)
    return parser.parse_args(argv)


def main(argv=None):
    args = parse_args(argv)
    logging.basicConfig(level=logging.DEBUG if args.verbose else logging.INFO,
                        format="%(asctime)s %(levelname)s %(name)s: %(message)s")
    return args.func(args)


if __name__ == "__main__":
    sys.exit(main())

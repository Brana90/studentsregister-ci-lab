#!/usr/bin/env bash
# deploy.sh - install one StudentsRegister release on this server, check it, roll back if it is not healthy
# v3: run by Jenkins as the deploy user, over ssh; TARGET=staging or production picks the install
# usage: TARGET=staging ./deploy.sh RELEASE        e.g. TARGET=staging ./deploy.sh 2.0.0-14
set -euo pipefail

RELEASE=${1:?usage: deploy.sh RELEASE}
TARGET=${TARGET:-staging}
ARTIFACTS=/srv/artifacts/studentsregister

case "$TARGET" in
    production)                     # two instances behind NGINX (the load-balancing lesson): service:port
        APP=/opt/studentsregister
        INSTANCES="studentsregister:8090 studentsregister-2:8091"
        ;;
    staging)
        APP=/opt/studentsregister-staging
        INSTANCES="studentsregister-staging:8092"
        ;;
    *)
        echo "deploy: unknown TARGET $TARGET" >&2
        exit 2
        ;;
esac

log() {
    echo "$(date '+%F %T') deploy[$TARGET]: $*"
}

healthy() {                         # wait up to 90 s for one instance to answer UP on its port
    for _ in $(seq 1 18); do
        if curl -fs --max-time 2 "http://127.0.0.1:$1/api/students/health" | grep -q '"status":"UP"'; then
            return 0
        fi
        sleep 5
    done
    return 1
}

switch_to() {                       # point current at one release, then restart each instance in turn (rolling)
    ln -sfn "releases/$1" "$APP/current"
    for instance in $INSTANCES; do
        sudo systemctl restart "${instance%%:*}"
        healthy "${instance##*:}"
    done
}

PREVIOUS=$(readlink "$APP/current")
PREVIOUS=${PREVIOUS#releases/}
SWITCHED=no

on_error() {
    log "FAILED at line $1 (exit code $2)"
    if [ "$SWITCHED" = yes ]; then
        log "rolling back to $PREVIOUS"
        switch_to "$PREVIOUS"
    fi
}
trap 'on_error $LINENO $?' ERR

log "deploying $RELEASE (running now: $PREVIOUS)"
cd "$ARTIFACTS/$RELEASE"
sha256sum -c --quiet app.jar.sha256
cp -r "$ARTIFACTS/$RELEASE" "$APP/releases/"
SWITCHED=yes
switch_to "$RELEASE"
log "done: $(cat "$APP/current/VERSION") from commit $(cat "$APP/current/COMMIT") is healthy"

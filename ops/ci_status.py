#!/usr/bin/env python3
"""ci_status.py - the team's morning check: main's last build in Jenkins, the open pull requests in Gitea."""
import base64
import json
import pathlib
import urllib.request

JENKINS = 'http://192.168.75.135'
JOB = '/job/platform/job/studentsregister-mb/job/main'
GITEA = 'http://192.168.75.135:3300'
REPO = 'platform/studentsregister'
TOKEN_FILE = pathlib.Path.home() / '.config' / 'ci' / 'jenkins.token'


def get_json(url, login=None):
    request = urllib.request.Request(url)
    if login:
        request.add_header('Authorization', 'Basic ' + base64.b64encode(login.encode()).decode())
    with urllib.request.urlopen(request, timeout=10) as answer:
        return json.load(answer)


def main():
    pat = TOKEN_FILE.read_text().strip()            # read from the 600 file, never printed
    build = get_json(JENKINS + JOB + '/lastBuild/api/json?tree=number,result,building', login='dba:' + pat)
    state = 'RUNNING' if build['building'] else build['result']
    print(f"jenkins main: build #{build['number']} {state}")
    pulls = get_json(f'{GITEA}/api/v1/repos/{REPO}/pulls?state=open')
    print(f'gitea open pull requests: {len(pulls)}')
    for pull in pulls:
        print(f"  #{pull['number']} {pull['title']} (by {pull['user']['login']})")
    return 0 if state == 'SUCCESS' else 1


if __name__ == '__main__':
    raise SystemExit(main())

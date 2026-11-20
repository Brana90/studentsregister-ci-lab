# db01 runbook

The server: db01 (Ubuntu 24.04), StudentsRegister behind NGINX.
Two app instances: 127.0.0.1:8090 and 127.0.0.1:8091.

## Restart

sudo systemctl restart studentsregister studentsregister-2
Then check: curl -s https://register.lab.internal/api/students/health

## Backups

Nightly at 02:30 UTC: studentsregister-backup.timer.
Dumps: /data/backups/studentsregister, 7 days kept.

## Who to call

The app: Karthik S. The server and the database: Priya Raman.

## CI

Jenkins: http://192.168.75.135/ , folder platform, job studentsregister (Pipeline from SCM, branch main).
The Jenkinsfile in the repository is the build: change it through a pull request, never in the Jenkins form.
A build of main starts by itself after a merge; a red build is read from the first ERROR line of its console.

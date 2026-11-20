# StudentsRegister — Learning Progress & Projects (v2)

Production-style student tracking: **Spring Boot 3 + MySQL** backend, **React** frontend.
Deployment flow unchanged (Jenkins → Docker Hub → K8s prod via Helm; docker-compose for test).

## What's new in v2

- 🔐 **JWT login with roles** — `ADMIN` (manage everything) and `STUDENT` (own portal)
- ✍ **Daily topic signing** — each student logs in and *signs* the topics covered
  that day (topic, notes, hours). Duplicate topics per day are rejected; future dates blocked
- 🔥 **Progress tracking** — day **streaks**, active days, total hours, per-day history;
  admin sees a live cohort progress table (who signed today, streaks, last entry)
- 🚀 **Project portfolio** — students track projects (tech stack, repo link,
  planned → in progress → completed with auto lifecycle dates); admin sees all projects
- 👥 **Student lifecycle** — registering a student auto-creates their login
  (username = email, with a default password the app sets; the student changes it); admin can reset passwords;
  deleting a student cleans up topics/projects/login
- 📊 **Dashboards** — admin: cohort stats, today's learning log, students by course;
  student: personal streak ring + stats
- 🛡️ Security — Spring Security + BCrypt + stateless JWT; role checks on every route;
  the Docker healthcheck path (`GET /api/students`) stays anonymous-safe but returns
  **no student data** without a login

## Run locally

```powershell
# backend (needs MySQL reachable — lab defaults are built into
# application.properties; env vars override them in Docker/K8s)
.\mvnw.cmd spring-boot:run          # http://localhost:8080

# frontend (second terminal)
cd frontend
npm install
npm run dev                         # http://localhost:5174
```

## Database script (`db/`)

| Script | Purpose | How to run |
|---|---|---|
| `db/init.sql` | create db, app user, all tables, sample students (hardcoded host/creds) | `mysql -h 100.105.204.3 -u root -p < db/init.sql` |

> Login accounts are seeded by the app (BCrypt hashes must come from the same
> library). Start the backend once after running the script.
> `spring.jpa.hibernate.ddl-auto=update` also creates the tables automatically,
> so the script is optional — it's for DBAs who prefer explicit DDL.

## API map (all under `/api/`, which nginx proxies)

- `POST /api/auth/login` · `GET /me` · `POST /change-password` · `POST /reset-password` (admin) · `GET /users` (admin)
- `GET|POST /api/students` · `GET|PUT|DELETE /{id}` · `GET /search` · `GET /course/{c}` · `GET /health`
- `POST /api/topics` (sign) · `GET /my` · `GET /my/summary` · `GET /student/{id}` (admin) · `GET /today` (admin) · `DELETE /{id}`
- `GET|POST /api/projects` · `GET /my` · `PUT|DELETE /{id}`
- `GET /api/dashboard/admin` · `GET /api/dashboard/my`

## Self-healing (K8s) — survives pod/DB recreation

This app self-heals with **no extra wiring** — its Helm deployment already has
everything it needs:

- The `mysql:8.0` StatefulSet is passed `MYSQL_USER` / `MYSQL_PASSWORD` /
  `MYSQL_DATABASE` from `mysql-secret`, so the image **auto-creates `student_user`
  (with rights on `student_db`) on a brand-new PVC**. No init script needed
  (unlike Mongo, which does).
- On every startup the Spring app recreates all tables
  (`spring.jpa.hibernate.ddl-auto=update`) and re-seeds the `admin` login
  (`DataSeeder`, idempotent). Student portal logins are back-filled too.

| You do | It heals because |
|---|---|
| Delete all pods / recreate the deployment | Spring startup rebuilds schema + re-seeds admin |
| Wipe the MySQL PVC and recreate | `mysql` image re-creates `student_user`; a backend restart rebuilds tables + admin |

> Wiping only MySQL (backend left running): `kubectl rollout restart deploy/studentregister-backend -n studentregister-prod` — Hibernate only builds the schema at startup, so the backend must restart once. Recreating the pods does this automatically.

## Unchanged (deployment flow)

`Jenkinsfile`, `Dockerfile`, `frontend/Dockerfile`, `docker-compose.yml`,
`frontend/nginx.conf.template`, `.env` files — exactly as before.
`pom.xml` gained app dependencies (security, jjwt, validation) which the existing
Docker build installs automatically; the frontend has **no new npm dependencies**
so `npm ci` keeps working with the existing lockfile.

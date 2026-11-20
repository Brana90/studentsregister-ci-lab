import { useEffect, useState } from "react";
import api, { errMsg, fmtDate, fmtDateTime } from "../api";
import { StatCard, Empty, Spinner } from "./ui";

export default function AdminDashboard({ notify }) {
  const [data, setData] = useState(null);
  const [todayLog, setTodayLog] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      api.get("/api/dashboard/admin"),
      api.get("/api/topics/today"),
    ])
      .then(([d, t]) => { setData(d.data); setTodayLog(t.data); })
      .catch((err) => notify(errMsg(err), "error"))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <Spinner />;
  if (!data) return <Empty text="Could not load the dashboard." />;

  const pbs = data.projectsByStatus || {};
  const progress = [...(data.studentsProgress || [])]
    .sort((a, b) => b.streak - a.streak || b.topicsSigned - a.topicsSigned);

  return (
    <>
      <div className="stat-grid">
        <StatCard label="Students" value={data.totalStudents}
          sub={`${data.activeStudents} active`} />
        <StatCard label="Signed today" value={data.topicsToday} tone="ok" />
        <StatCard label="Topics this week" value={data.topicsThisWeek} />
        <StatCard label="Projects" value={data.totalProjects}
          sub={`${pbs.COMPLETED ?? 0} completed`} />
        <StatCard label="In progress" value={pbs.IN_PROGRESS ?? 0} tone="warn" />
      </div>

      <div className="card">
        <h2>Student progress</h2>
        {progress.length === 0 ? <Empty text="No students yet." /> : (
          <table className="table">
            <thead>
              <tr><th>Student</th><th>Course</th><th>Today</th><th>Streak</th>
                <th>Topics</th><th>Active days</th><th>Projects</th><th>Last entry</th></tr>
            </thead>
            <tbody>
              {progress.map((s) => (
                <tr key={s.studentId}>
                  <td><strong>{s.name}</strong></td>
                  <td className="muted">{s.course || "-"}</td>
                  <td>{s.signedToday
                    ? <span className="badge badge-ok">Signed ✔</span>
                    : <span className="badge badge-off">Not yet</span>}</td>
                  <td>{s.streak > 0
                    ? <span className="streak-chip">🔥 {s.streak}</span>
                    : <span className="muted">0</span>}</td>
                  <td>{s.topicsSigned}</td>
                  <td>{s.activeDays}</td>
                  <td>{s.projects} <span className="muted">({s.projectsCompleted} done)</span></td>
                  <td className="muted">{fmtDate(s.lastEntryDate)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="two-col">
        <div className="card">
          <h2>Today's learning log</h2>
          {todayLog.length === 0 ? <Empty text="Nothing signed today yet." /> : (
            <ul className="entry-list">
              {todayLog.map((t) => (
                <li key={t.id} className="entry-item">
                  <div>
                    <strong>{t.studentName}</strong>
                    <span className="muted"> — {t.topic}</span>
                    {t.hoursSpent ? <span className="chip">{t.hoursSpent}h</span> : null}
                    {t.description && <div className="muted">{t.description}</div>}
                  </div>
                  <span className="muted">{fmtDateTime(t.signedAt)}</span>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="card">
          <h2>Students by course</h2>
          {Object.keys(data.byCourse || {}).length === 0
            ? <Empty text="No data." />
            : (
              <table className="table table-compact">
                <thead><tr><th>Course</th><th>Students</th></tr></thead>
                <tbody>
                  {Object.entries(data.byCourse).map(([c, n]) => (
                    <tr key={c}><td>{c}</td><td>{n}</td></tr>
                  ))}
                </tbody>
              </table>
            )}
        </div>
      </div>
    </>
  );
}

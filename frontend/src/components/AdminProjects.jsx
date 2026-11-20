import { useEffect, useState } from "react";
import api, { errMsg, fmtDate } from "../api";
import { Badge, Empty, Spinner, PROJECT_STATUSES } from "./ui";

export default function AdminProjects({ notify }) {
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState("");
  const [q, setQ] = useState("");

  useEffect(() => {
    api.get("/api/projects")
      .then((res) => setProjects(res.data))
      .catch((err) => notify(errMsg(err), "error"))
      .finally(() => setLoading(false));
  }, []);

  const filtered = projects.filter((p) => {
    if (statusFilter && p.status !== statusFilter) return false;
    if (q) {
      const hay = `${p.title} ${p.studentName} ${p.techStack || ""}`.toLowerCase();
      if (!hay.includes(q.toLowerCase())) return false;
    }
    return true;
  });

  return (
    <>
      <div className="toolbar">
        <input className="input" placeholder="Search title / student / tech…"
          value={q} onChange={(e) => setQ(e.target.value)} />
        <select className="input" value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="">All statuses</option>
          {PROJECT_STATUSES.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
        </select>
        <div className="toolbar-total">{filtered.length} project(s)</div>
      </div>

      <div className="card">
        {loading ? <Spinner /> : filtered.length === 0 ? (
          <Empty text="No projects found." />
        ) : (
          <table className="table">
            <thead>
              <tr><th>Project</th><th>Student</th><th>Tech</th>
                <th>Status</th><th>Started</th><th>Completed</th><th>Link</th></tr>
            </thead>
            <tbody>
              {filtered.map((p) => (
                <tr key={p.id}>
                  <td>
                    <strong>{p.title}</strong>
                    {p.description && <div className="muted">{p.description}</div>}
                  </td>
                  <td>{p.studentName}
                    <div className="muted">{p.course}</div></td>
                  <td className="muted">{p.techStack || "-"}</td>
                  <td><Badge value={p.status} /></td>
                  <td className="muted">{fmtDate(p.startedDate)}</td>
                  <td className="muted">{fmtDate(p.completedDate)}</td>
                  <td>{p.repoUrl
                    ? <a href={p.repoUrl} target="_blank" rel="noreferrer">repo ↗</a>
                    : <span className="muted">-</span>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  );
}

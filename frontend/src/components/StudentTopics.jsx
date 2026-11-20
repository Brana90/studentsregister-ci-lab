import { useEffect, useState } from "react";
import api, { errMsg, fmtDate, fmtDateTime } from "../api";
import { Empty, Spinner } from "./ui";

/** Full learning history, grouped by day. */
export default function StudentTopics({ notify }) {
  const [entries, setEntries] = useState([]);
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [q, setQ] = useState("");

  useEffect(() => {
    Promise.all([
      api.get("/api/topics/my"),
      api.get("/api/topics/my/summary"),
    ])
      .then(([e, s]) => { setEntries(e.data); setSummary(s.data); })
      .catch((err) => notify(errMsg(err), "error"))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <Spinner />;

  const filtered = q
    ? entries.filter((t) =>
        (t.topic + " " + (t.description || "")).toLowerCase().includes(q.toLowerCase()))
    : entries;

  const byDate = {};
  for (const t of filtered) (byDate[t.entryDate] ||= []).push(t);
  const days = Object.keys(byDate).sort().reverse();

  return (
    <>
      {summary && (
        <div className="card progress-strip">
          <span><strong>{summary.totalTopics}</strong> topics</span>
          <span><strong>{summary.activeDays}</strong> active days</span>
          <span><strong>{summary.streak}</strong> day streak</span>
          <span><strong>{Number(summary.totalHours || 0).toFixed(1)}</strong> hours</span>
          <span className="muted">last entry: {fmtDate(summary.lastEntryDate)}</span>
        </div>
      )}

      <div className="toolbar">
        <input className="input" placeholder="Search your topics…"
          value={q} onChange={(e) => setQ(e.target.value)} />
        <div className="toolbar-total">{filtered.length} entries</div>
      </div>

      {days.length === 0 ? (
        <div className="card"><Empty text="No topics signed yet." /></div>
      ) : (
        days.map((d) => (
          <div className="card" key={d}>
            <h2>{fmtDate(d)} <span className="muted">· {byDate[d].length} topic(s)</span></h2>
            <ul className="entry-list">
              {byDate[d].map((t) => (
                <li key={t.id} className="entry-item">
                  <div>
                    <strong>{t.topic}</strong>
                    {t.hoursSpent ? <span className="chip">{t.hoursSpent}h</span> : null}
                    {t.description && <div className="muted">{t.description}</div>}
                  </div>
                  <span className="muted">{fmtDateTime(t.signedAt)}</span>
                </li>
              ))}
            </ul>
          </div>
        ))
      )}
    </>
  );
}

import { useEffect, useState } from "react";
import api, { errMsg, fmtDate, fmtDateTime, todayISO } from "../api";
import { StatCard, Empty, Spinner } from "./ui";

/** Student landing page: progress stats + "sign today's topic". */
export default function StudentHome({ notify }) {
  const [summary, setSummary] = useState(null);
  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState({ topic: "", description: "", hoursSpent: "" });
  const [busy, setBusy] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const [s, e] = await Promise.all([
        api.get("/api/dashboard/my"),
        api.get("/api/topics/my"),
      ]);
      setSummary(s.data);
      setEntries(e.data);
    } catch (err) {
      notify(errMsg(err), "error");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const sign = async (e) => {
    e.preventDefault();
    setBusy(true);
    try {
      await api.post("/api/topics", {
        topic: form.topic,
        description: form.description || null,
        hoursSpent: form.hoursSpent ? parseFloat(form.hoursSpent) : null,
      });
      notify("Topic signed for today ✔");
      setForm({ topic: "", description: "", hoursSpent: "" });
      load();
    } catch (err) {
      notify(errMsg(err), "error");
    } finally {
      setBusy(false);
    }
  };

  const removeToday = async (entry) => {
    if (!window.confirm(`Remove "${entry.topic}" from today's log?`)) return;
    try {
      await api.delete(`/api/topics/${entry.id}`);
      notify("Entry removed");
      load();
    } catch (err) {
      notify(errMsg(err), "error");
    }
  };

  if (loading) return <Spinner />;
  if (!summary) return <Empty text="Could not load your progress." />;

  const today = todayISO();
  const todayEntries = entries.filter((t) => t.entryDate === today);

  return (
    <>
      <div className="hello card">
        <div>
          <h2 className="hello-title">Hi {summary.name || "there"} 👋</h2>
          <p className="muted">
            {summary.course ? `${summary.course} · ` : ""}
            {summary.todaySigned
              ? "You've signed today's progress. Keep the streak alive!"
              : "You haven't signed anything today yet — what did you learn?"}
          </p>
        </div>
        <div className={"streak-ring" + (summary.todaySigned ? " lit" : "")}>
          <span className="streak-num">{summary.streak}</span>
          <span className="streak-word">day streak</span>
        </div>
      </div>

      <div className="stat-grid">
        <StatCard label="Topics signed" value={summary.totalTopics} />
        <StatCard label="Active days" value={summary.activeDays} />
        <StatCard label="Hours logged" value={Number(summary.totalHours || 0).toFixed(1)} />
        <StatCard label="Projects" value={summary.projects}
          sub={`${summary.projectsCompleted} completed`} />
      </div>

      <div className="two-col">
        <div className="card">
          <h2>Sign today's topic</h2>
          <form onSubmit={sign} className="form-grid form-grid-single">
            <label className="field">
              <span>Topic covered *</span>
              <input value={form.topic} required maxLength={200}
                placeholder="e.g. SQL JOINs, React useEffect…"
                onChange={(e) => setForm({ ...form, topic: e.target.value })} />
            </label>
            <label className="field">
              <span>What did you learn? (optional)</span>
              <textarea rows={3} value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })} />
            </label>
            <label className="field">
              <span>Hours spent (optional)</span>
              <input type="number" min="0.5" max="16" step="0.5" value={form.hoursSpent}
                onChange={(e) => setForm({ ...form, hoursSpent: e.target.value })} />
            </label>
            <button className="btn btn-primary" disabled={busy}>
              {busy ? "Signing…" : "✍ Sign it"}
            </button>
          </form>
        </div>

        <div className="card">
          <h2>Signed today ({todayEntries.length})</h2>
          {todayEntries.length === 0 ? (
            <Empty text="Nothing signed yet today." />
          ) : (
            <ul className="entry-list">
              {todayEntries.map((t) => (
                <li key={t.id} className="entry-item">
                  <div>
                    <strong>{t.topic}</strong>
                    {t.hoursSpent ? <span className="chip">{t.hoursSpent}h</span> : null}
                    {t.description && <div className="muted">{t.description}</div>}
                    <div className="muted">signed {fmtDateTime(t.signedAt)}</div>
                  </div>
                  <button className="btn btn-small btn-danger"
                    onClick={() => removeToday(t)}>Remove</button>
                </li>
              ))}
            </ul>
          )}

          <h2 style={{ marginTop: 18 }}>Recent days</h2>
          <RecentDays entries={entries.filter((t) => t.entryDate !== today)} />
        </div>
      </div>
    </>
  );
}

function RecentDays({ entries }) {
  if (entries.length === 0) return <Empty text="No earlier entries yet." />;
  const byDate = {};
  for (const t of entries) (byDate[t.entryDate] ||= []).push(t);
  const days = Object.keys(byDate).sort().reverse().slice(0, 5);
  return (
    <div className="day-groups">
      {days.map((d) => (
        <div key={d} className="day-group">
          <div className="day-head">{fmtDate(d)}</div>
          <ul className="day-topics">
            {byDate[d].map((t) => (
              <li key={t.id}>{t.topic}{t.hoursSpent ? ` · ${t.hoursSpent}h` : ""}</li>
            ))}
          </ul>
        </div>
      ))}
    </div>
  );
}

import { useEffect, useState } from "react";
import api, { errMsg, fmtDate, fmtDateTime } from "../api";
import { Modal, Badge, Empty, Spinner } from "./ui";

const emptyForm = { name: "", email: "", phone: "", course: "", status: "active" };

export default function AdminStudents({ notify }) {
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [q, setQ] = useState("");
  const [form, setForm] = useState(emptyForm);
  const [editing, setEditing] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [progress, setProgress] = useState(null);   // student progress modal
  const [resetFor, setResetFor] = useState(null);   // reset password modal
  const [busy, setBusy] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const res = q.trim()
        ? await api.get("/api/students/search", { params: { name: q.trim() } })
        : await api.get("/api/students");
      setStudents(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      notify(errMsg(err), "error");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const openAdd = () => { setEditing(null); setForm(emptyForm); setShowForm(true); };
  const openEdit = (s) => {
    setEditing(s);
    setForm({ name: s.name, email: s.email, phone: s.phone,
      course: s.course, status: s.status });
    setShowForm(true);
  };

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    try {
      if (editing) {
        await api.put(`/api/students/${editing.id}`, form);
        notify("Student updated");
      } else {
        const res = await api.post("/api/students", form);
        const login = res.data.portalLogin;
        notify(`Student added. Login: ${login.username} / ${login.defaultPassword}`);
      }
      setShowForm(false);
      load();
    } catch (err) {
      notify(errMsg(err), "error");
    } finally {
      setBusy(false);
    }
  };

  const remove = async (s) => {
    if (!window.confirm(
      `Delete ${s.name}? Their topics, projects and login are removed too.`)) return;
    try {
      await api.delete(`/api/students/${s.id}`);
      notify("Student deleted");
      load();
    } catch (err) {
      notify(errMsg(err), "error");
    }
  };

  const openProgress = async (s) => {
    try {
      const res = await api.get(`/api/topics/student/${s.id}`);
      setProgress({ student: s, ...res.data });
    } catch (err) {
      notify(errMsg(err), "error");
    }
  };

  return (
    <>
      <div className="toolbar">
        <input className="input" placeholder="Search by name…"
          value={q} onChange={(e) => setQ(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && load()} />
        <button className="btn" onClick={load}>Search</button>
        <button className="btn" onClick={() => { setQ(""); setTimeout(load, 0); }}>All</button>
        <div className="toolbar-spacer" />
        <button className="btn btn-primary" onClick={openAdd}>+ Register Student</button>
      </div>

      <div className="card">
        {loading ? <Spinner /> : students.length === 0 ? (
          <Empty text="No students found." />
        ) : (
          <table className="table">
            <thead>
              <tr><th>Name</th><th>Email (login)</th><th>Phone</th>
                <th>Course</th><th>Status</th><th>Enrolled</th><th></th></tr>
            </thead>
            <tbody>
              {students.map((s) => (
                <tr key={s.id}>
                  <td><strong>{s.name}</strong></td>
                  <td className="mono">{s.email}</td>
                  <td className="muted">{s.phone}</td>
                  <td>{s.course}</td>
                  <td><Badge value={s.status} /></td>
                  <td className="muted">{fmtDate(s.enrolledDate)}</td>
                  <td className="row-actions">
                    <button className="btn btn-small" onClick={() => openProgress(s)}>Progress</button>
                    <button className="btn btn-small" onClick={() => openEdit(s)}>Edit</button>
                    <button className="btn btn-small" onClick={() => setResetFor(s)}>Reset pwd</button>
                    <button className="btn btn-small btn-danger" onClick={() => remove(s)}>Del</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {showForm && (
        <Modal title={editing ? "Edit student" : "Register student"}
          onClose={() => setShowForm(false)}>
          <form onSubmit={submit} className="form-grid">
            <label className="field">
              <span>Name *</span>
              <input value={form.name} required
                onChange={(e) => setForm({ ...form, name: e.target.value })} />
            </label>
            <label className="field">
              <span>Email * (becomes the login)</span>
              <input type="email" value={form.email} required
                onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </label>
            <label className="field">
              <span>Phone *</span>
              <input value={form.phone} required
                onChange={(e) => setForm({ ...form, phone: e.target.value })} />
            </label>
            <label className="field">
              <span>Course *</span>
              <input value={form.course} required
                onChange={(e) => setForm({ ...form, course: e.target.value })} />
            </label>
            <label className="field">
              <span>Status</span>
              <select value={form.status}
                onChange={(e) => setForm({ ...form, status: e.target.value })}>
                <option value="active">Active</option>
                <option value="inactive">Inactive</option>
              </select>
            </label>
            {!editing && (
              <div className="hint field-full">
                A portal login is auto-created:
                username = email, the default password the app sets.
              </div>
            )}
            <div className="form-actions field-full">
              <button type="button" className="btn" onClick={() => setShowForm(false)}>Cancel</button>
              <button className="btn btn-primary" disabled={busy}>
                {editing ? "Save changes" : "Register"}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {progress && (
        <Modal title={`${progress.student.name} — learning progress`} wide
          onClose={() => setProgress(null)}>
          <div className="progress-strip">
            <span><strong>{progress.totalTopics}</strong> topics</span>
            <span><strong>{progress.activeDays}</strong> active days</span>
            <span><strong>{progress.streak}</strong> day streak</span>
            <span><strong>{Number(progress.totalHours || 0).toFixed(1)}</strong> hours</span>
            <span className="muted">last: {fmtDate(progress.lastEntryDate)}</span>
          </div>
          {(progress.entries || []).length === 0 ? (
            <Empty text="No topics signed yet." />
          ) : (
            <table className="table table-compact">
              <thead>
                <tr><th>Date</th><th>Topic</th><th>Hours</th><th>Signed at</th></tr>
              </thead>
              <tbody>
                {progress.entries.map((t) => (
                  <tr key={t.id}>
                    <td className="muted">{fmtDate(t.entryDate)}</td>
                    <td><strong>{t.topic}</strong>
                      {t.description && <div className="muted">{t.description}</div>}</td>
                    <td>{t.hoursSpent || "-"}</td>
                    <td className="muted">{fmtDateTime(t.signedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Modal>
      )}

      {resetFor && (
        <ResetPassword student={resetFor} notify={notify}
          onClose={() => setResetFor(null)} />
      )}
    </>
  );
}

function ResetPassword({ student, onClose, notify }) {
  const [pwd, setPwd] = useState("");
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    try {
      await api.post("/api/auth/reset-password", {
        username: student.email, newPassword: pwd,
      });
      notify(`Password reset for ${student.email}`);
      onClose();
    } catch (err) {
      notify(errMsg(err), "error");
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal title={`Reset password — ${student.name}`} onClose={onClose}>
      <form onSubmit={submit} className="form-grid form-grid-single">
        <label className="field">
          <span>New password (min 6 chars)</span>
          <input type="text" value={pwd} required minLength={6}
            onChange={(e) => setPwd(e.target.value)} />
        </label>
        <div className="form-actions">
          <button type="button" className="btn" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={busy}>Reset</button>
        </div>
      </form>
    </Modal>
  );
}

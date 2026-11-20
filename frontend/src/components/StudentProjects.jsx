import { useEffect, useState } from "react";
import api, { errMsg, fmtDate } from "../api";
import { Modal, Badge, Empty, Spinner, PROJECT_STATUSES } from "./ui";

const emptyForm = { title: "", description: "", techStack: "", repoUrl: "", status: "PLANNED" };

/** Student's own project portfolio. */
export default function StudentProjects({ notify }) {
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState(emptyForm);
  const [editing, setEditing] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [busy, setBusy] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const res = await api.get("/api/projects/my");
      setProjects(res.data);
    } catch (err) {
      notify(errMsg(err), "error");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const openAdd = () => { setEditing(null); setForm(emptyForm); setShowForm(true); };
  const openEdit = (p) => {
    setEditing(p);
    setForm({ title: p.title, description: p.description || "",
      techStack: p.techStack || "", repoUrl: p.repoUrl || "", status: p.status });
    setShowForm(true);
  };

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    try {
      if (editing) {
        await api.put(`/api/projects/${editing.id}`, form);
        notify("Project updated");
      } else {
        await api.post("/api/projects", form);
        notify("Project added");
      }
      setShowForm(false);
      load();
    } catch (err) {
      notify(errMsg(err), "error");
    } finally {
      setBusy(false);
    }
  };

  const remove = async (p) => {
    if (!window.confirm(`Delete project "${p.title}"?`)) return;
    try {
      await api.delete(`/api/projects/${p.id}`);
      notify("Project deleted");
      load();
    } catch (err) {
      notify(errMsg(err), "error");
    }
  };

  return (
    <>
      <div className="toolbar">
        <div className="toolbar-total">{projects.length} project(s)</div>
        <div className="toolbar-spacer" />
        <button className="btn btn-primary" onClick={openAdd}>+ Add Project</button>
      </div>

      {loading ? <Spinner /> : projects.length === 0 ? (
        <div className="card">
          <Empty text="No projects yet — add the first thing you've built!" />
        </div>
      ) : (
        <div className="project-grid">
          {projects.map((p) => (
            <div className="card project-card" key={p.id}>
              <div className="project-head">
                <strong>{p.title}</strong>
                <Badge value={p.status} />
              </div>
              {p.description && <p className="muted">{p.description}</p>}
              {p.techStack && (
                <div className="chips">
                  {p.techStack.split(",").map((t, i) =>
                    <span className="chip" key={i}>{t.trim()}</span>)}
                </div>
              )}
              <div className="project-meta muted">
                {p.startedDate && <>started {fmtDate(p.startedDate)} · </>}
                {p.completedDate && <>completed {fmtDate(p.completedDate)} · </>}
                {p.repoUrl && (
                  <a href={p.repoUrl} target="_blank" rel="noreferrer">repo ↗</a>
                )}
              </div>
              <div className="project-actions">
                <button className="btn btn-small" onClick={() => openEdit(p)}>Edit</button>
                <button className="btn btn-small btn-danger" onClick={() => remove(p)}>Delete</button>
              </div>
            </div>
          ))}
        </div>
      )}

      {showForm && (
        <Modal title={editing ? "Edit project" : "Add project"} onClose={() => setShowForm(false)}>
          <form onSubmit={submit} className="form-grid form-grid-single">
            <label className="field">
              <span>Title *</span>
              <input value={form.title} required maxLength={150}
                onChange={(e) => setForm({ ...form, title: e.target.value })} />
            </label>
            <label className="field">
              <span>Description</span>
              <textarea rows={3} value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })} />
            </label>
            <label className="field">
              <span>Tech stack (comma separated)</span>
              <input value={form.techStack} placeholder="React, Spring Boot, MySQL"
                onChange={(e) => setForm({ ...form, techStack: e.target.value })} />
            </label>
            <label className="field">
              <span>Repo / demo URL</span>
              <input type="url" value={form.repoUrl} placeholder="https://github.com/…"
                onChange={(e) => setForm({ ...form, repoUrl: e.target.value })} />
            </label>
            <label className="field">
              <span>Status</span>
              <select value={form.status}
                onChange={(e) => setForm({ ...form, status: e.target.value })}>
                {PROJECT_STATUSES.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
              </select>
            </label>
            <div className="form-actions">
              <button type="button" className="btn" onClick={() => setShowForm(false)}>Cancel</button>
              <button className="btn btn-primary" disabled={busy}>
                {editing ? "Save changes" : "Add project"}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </>
  );
}

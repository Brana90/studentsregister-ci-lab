import { useEffect, useState } from "react";
import api, { errMsg, getAuth, setAuth } from "./api";
import { Modal } from "./components/ui";
import Login from "./components/Login";
import StudentHome from "./components/StudentHome";
import StudentTopics from "./components/StudentTopics";
import StudentProjects from "./components/StudentProjects";
import AdminDashboard from "./components/AdminDashboard";
import AdminStudents from "./components/AdminStudents";
import AdminProjects from "./components/AdminProjects";

const NAV = {
  ADMIN: [
    ["dashboard", "Dashboard"],
    ["students", "Students"],
    ["projects", "Projects"],
  ],
  STUDENT: [
    ["home", "My Progress"],
    ["topics", "My Topics"],
    ["myprojects", "My Projects"],
  ],
};

export default function App() {
  const [user, setUser] = useState(getAuth());
  const [tab, setTab] = useState(null);
  const [toast, setToast] = useState(null);
  const [showPwd, setShowPwd] = useState(false);
  const [openAccess, setOpenAccess] = useState(false);
  const [checking, setChecking] = useState(!getAuth());

  // Backend started with OPEN_ACCESS=true (lab / classroom): no sign-in page
  useEffect(() => {
    api.get("/api/auth/mode")
      .then(async (res) => {
        if (!res.data.openAccess) return;
        setOpenAccess(true);
        if (!getAuth()) {
          const session = await api.post("/api/auth/open");
          setAuth(session.data);
          setUser(session.data);
        }
      })
      .catch(() => {})
      .finally(() => setChecking(false));
  }, []);

  useEffect(() => {
    if (user && !tab) {
      setTab(user.role === "ADMIN" ? "dashboard" : "home");
    }
  }, [user]);

  const notify = (text, kind = "ok") => {
    setToast({ text, kind });
    setTimeout(() => setToast(null), kind === "error" ? 6000 : 4000);
  };

  const logout = () => {
    setAuth(null);
    setUser(null);
    setTab(null);
  };

  if (!user) {
    if (checking) return null;
    return <Login onLogin={(u) => setUser(u)} />;
  }

  const nav = NAV[user.role] || NAV.STUDENT;

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-logo">🎓</span>
          <span className="brand-name">Student Register</span>
        </div>
        <nav>
          {nav.map(([key, label]) => (
            <button key={key}
              className={"nav-item" + (tab === key ? " active" : "")}
              onClick={() => setTab(key)}>
              {label}
            </button>
          ))}
        </nav>
        <div className="sidebar-foot">
          <div className="me">
            <div className="me-name">{user.fullName}</div>
            <div className="me-role">{user.role.toLowerCase()}</div>
          </div>
          {openAccess ? (
            <div className="me-role">open access (lab)</div>
          ) : (
            <>
              <button className="nav-item" onClick={() => setShowPwd(true)}>Change password</button>
              <button className="nav-item nav-danger" onClick={logout}>Sign out</button>
            </>
          )}
        </div>
      </aside>

      <main className="content">
        <header className="content-head">
          <h1>{(nav.find(([k]) => k === tab) || [])[1] || ""}</h1>
        </header>

        {tab === "dashboard" && <AdminDashboard notify={notify} />}
        {tab === "students" && <AdminStudents notify={notify} />}
        {tab === "projects" && <AdminProjects notify={notify} />}
        {tab === "home" && <StudentHome notify={notify} />}
        {tab === "topics" && <StudentTopics notify={notify} />}
        {tab === "myprojects" && <StudentProjects notify={notify} />}
      </main>

      {toast && (
        <div className={`toast toast-${toast.kind}`} onClick={() => setToast(null)}>
          {toast.text}
        </div>
      )}

      {showPwd && <ChangePassword onClose={() => setShowPwd(false)} notify={notify} />}
    </div>
  );
}

function ChangePassword({ onClose, notify }) {
  const [current, setCurrent] = useState("");
  const [next, setNext] = useState("");
  const [confirm, setConfirm] = useState("");
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    if (next !== confirm) {
      notify("New passwords do not match", "error");
      return;
    }
    setBusy(true);
    try {
      await api.post("/api/auth/change-password", {
        currentPassword: current, newPassword: next,
      });
      notify("Password changed");
      onClose();
    } catch (err) {
      notify(errMsg(err), "error");
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal title="Change password" onClose={onClose}>
      <form onSubmit={submit} className="form-grid form-grid-single">
        <label className="field">
          <span>Current password</span>
          <input type="password" value={current} required
            onChange={(e) => setCurrent(e.target.value)} />
        </label>
        <label className="field">
          <span>New password (min 6 chars)</span>
          <input type="password" value={next} required minLength={6}
            onChange={(e) => setNext(e.target.value)} />
        </label>
        <label className="field">
          <span>Confirm new password</span>
          <input type="password" value={confirm} required
            onChange={(e) => setConfirm(e.target.value)} />
        </label>
        <div className="form-actions">
          <button type="button" className="btn" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={busy}>Change password</button>
        </div>
      </form>
    </Modal>
  );
}

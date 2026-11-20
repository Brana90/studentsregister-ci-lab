// Shared UI building blocks
export function Modal({ title, onClose, children, wide }) {
  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className={"modal" + (wide ? " modal-wide" : "")}>
        <div className="modal-head">
          <h3>{title}</h3>
          <button className="icon-btn" onClick={onClose} aria-label="Close">✕</button>
        </div>
        <div className="modal-body">{children}</div>
      </div>
    </div>
  );
}

export function StatCard({ label, value, tone, sub }) {
  return (
    <div className={"stat-card" + (tone ? ` stat-${tone}` : "")}>
      <div className="stat-value">{value}</div>
      <div className="stat-label">{label}</div>
      {sub && <div className="stat-sub">{sub}</div>}
    </div>
  );
}

export function Badge({ value }) {
  const map = {
    active: ["badge-ok", "Active"],
    inactive: ["badge-off", "Inactive"],
    PLANNED: ["badge-plan", "Planned"],
    IN_PROGRESS: ["badge-prog", "In progress"],
    COMPLETED: ["badge-ok", "Completed"],
  };
  const [cls, label] = map[value] || ["badge-off", value];
  return <span className={`badge ${cls}`}>{label}</span>;
}

export function Empty({ text }) {
  return <div className="empty">{text}</div>;
}

export function Spinner() {
  return <div className="spinner" />;
}

export const PROJECT_STATUSES = [
  ["PLANNED", "Planned"],
  ["IN_PROGRESS", "In progress"],
  ["COMPLETED", "Completed"],
];

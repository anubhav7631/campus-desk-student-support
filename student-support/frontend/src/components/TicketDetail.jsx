import React, { useState } from "react";
function overdue(ticket) {
  return ticket.status !== "RESOLVED" && new Date(ticket.dueAt) < new Date();
}

export default function TicketDetail({
  ticket,
  events,
  attachments,
  user,
  busy,
  onAction,
  onUpload,
  onDownload,
}) {
  const [note, setNote] = useState("");
  const [staff, setStaff] = useState("staff1");
  const [priority, setPriority] = useState("HIGH");
  const [file, setFile] = useState(null);

  if (!ticket) {
    return (
      <div className="card detailCard placeholder">
        <div className="placeholderIcon">☷</div>
        <h2>Select a ticket</h2>
        <p>Its details and activity will appear here.</p>
      </div>
    );
  }

  const isManager = user.role === "MANAGER";
  const isStaff = user.role === "STAFF";
  const isStudent = user.role === "STUDENT";
  const assignedToMe = ticket.assignee === user.username;
  const canHandle = isManager || (isStaff && assignedToMe);
  const active = ticket.status !== "RESOLVED";

  async function act(action, value = "") {
    const success = await onAction(ticket.id, {
      action,
      value,
      note,
    });

    if (success) setNote("");
  }

  async function upload() {
    if (!file) return;

    const success = await onUpload(ticket.id, file);

    if (success) {
      setFile(null);
      const input = document.getElementById("ticket-file");
      if (input) input.value = "";
    }
  }

  return (
    <div className="card detailCard">
      <div className="sectionHeader">
        <div>
          <div className="eyebrow">
            TICKET #{String(ticket.id).padStart(4, "0")}
          </div>
          <h2>{ticket.title}</h2>
        </div>

        <span className="badge">{ticket.status.replaceAll("_", " ")}</span>
      </div>

      <p className="description">{ticket.description}</p>

      <div className="facts">
        <div>
          <span>Category</span>
          <strong>{ticket.category.replaceAll("_", " ")}</strong>
        </div>
        <div>
          <span>Priority</span>
          <strong>{ticket.priority}</strong>
        </div>
        <div>
          <span>Student</span>
          <strong>{ticket.studentUsername}</strong>
        </div>
        <div>
          <span>Owner</span>
          <strong>{ticket.assignee || "Unassigned"}</strong>
        </div>
        <div>
          <span>Due</span>
          <strong className={overdue(ticket) ? "warning" : ""}>
            {new Date(ticket.dueAt).toLocaleString()}
          </strong>
        </div>
        <div>
          <span>Escalation</span>
          <strong>{ticket.escalated ? "Escalated" : "Normal"}</strong>
        </div>
      </div>

      <section className="detailSection">
        <h3>Next action</h3>

        {isManager && active && (
          <>
            <div className="actionRow">
              <select
                value={staff}
                onChange={(event) => setStaff(event.target.value)}
              >
                <option value="staff1">staff1</option>
                <option value="staff2">staff2</option>
              </select>

              <button disabled={busy} onClick={() => act("ASSIGN", staff)}>
                Assign
              </button>
            </div>

            <div className="actionRow">
              <select
                value={priority}
                onChange={(event) => setPriority(event.target.value)}
              >
                <option value="LOW">LOW</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HIGH">HIGH</option>
              </select>

              <button
                className="outline"
                disabled={busy}
                onClick={() => act("PRIORITY", priority)}
              >
                Change priority
              </button>
            </div>
          </>
        )}

        {isStaff && active && (
          <button disabled={busy} onClick={() => act("ASSIGN", user.username)}>
            Claim ticket
          </button>
        )}

        {active && !ticket.escalated && canHandle && (
          <button
            className="outline"
            disabled={busy}
            onClick={() => act("ESCALATE")}
          >
            Escalate to manager
          </button>
        )}

        {ticket.escalated && isManager && (
          <button
            className="outline"
            disabled={busy}
            onClick={() => act("DEESCALATE")}
          >
            Clear escalation
          </button>
        )}

        {canHandle &&
          ticket.assignee &&
          ["IN_PROGRESS", "WAITING_FOR_STUDENT"].includes(ticket.status) && (
            <div className="actionRow">
              {ticket.status === "IN_PROGRESS" && (
                <button
                  className="outline"
                  disabled={busy}
                  onClick={() => act("STATUS", "WAITING_FOR_STUDENT")}
                >
                  Await student
                </button>
              )}

              {ticket.status === "WAITING_FOR_STUDENT" && (
                <button
                  className="outline"
                  disabled={busy}
                  onClick={() => act("STATUS", "IN_PROGRESS")}
                >
                  Resume
                </button>
              )}

              <button disabled={busy} onClick={() => act("STATUS", "RESOLVED")}>
                Resolve
              </button>
            </div>
          )}

        {!active && (isStudent || isManager) && (
          <button disabled={busy} onClick={() => act("REOPEN")}>
            Reopen request
          </button>
        )}

        {active && (
          <div className="commentBox">
            <textarea
              value={note}
              onChange={(event) => setNote(event.target.value)}
              maxLength={1800}
              placeholder="Write an update"
            />

            <button
              className="outline"
              disabled={busy || !note.trim()}
              onClick={() => act("COMMENT")}
            >
              Post comment
            </button>
          </div>
        )}
      </section>

      <section className="detailSection">
        <h3>Attachments</h3>

        {active && (
          <div className="uploadRow">
            <input
              id="ticket-file"
              type="file"
              accept=".pdf,.png,.jpg,.jpeg"
              onChange={(event) => setFile(event.target.files?.[0] || null)}
            />

            <button disabled={busy || !file} onClick={upload}>
              Upload
            </button>
          </div>
        )}

        {attachments.length === 0 ? (
          <p className="muted">No attachments yet.</p>
        ) : (
          attachments.map((attachment) => (
            <div className="attachmentRow" key={attachment.id}>
              <span>{attachment.filename}</span>
              <button
                className="outline"
                onClick={() => onDownload(ticket.id, attachment)}
              >
                Download
              </button>
            </div>
          ))
        )}
      </section>

      <section className="detailSection">
        <h3>Activity history</h3>

        {events.map((event) => (
          <div className="historyItem" key={event.id}>
            <strong>{event.action.replaceAll("_", " ")}</strong>
            <p>{event.detail}</p>
            <small>
              {event.actor} · {new Date(event.at).toLocaleString()}
            </small>
          </div>
        ))}
      </section>
    </div>
  );
}

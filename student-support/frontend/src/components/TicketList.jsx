
import React, { useState } from "react";
const FILTERS = ["ALL", "OVERDUE", "ESCALATED", "WAITING", "MINE"];

function overdue(ticket) {
  return ticket.status !== "RESOLVED" && new Date(ticket.dueAt) < new Date();
}

function age(createdAt) {
  const hours = Math.max(
    0,
    Math.floor((Date.now() - new Date(createdAt).getTime()) / 3_600_000),
  );

  if (hours < 24) return `${hours}h`;

  return `${Math.floor(hours / 24)}d ${hours % 24}h`;
}

export default function TicketList({
  tickets,
  selectedId,
  username,
  onSelect,
  onRefresh,
}) {
  const [filter, setFilter] = useState("ALL");

  const visible = tickets.filter((ticket) => {
    switch (filter) {
      case "OVERDUE":
        return overdue(ticket);
      case "ESCALATED":
        return ticket.escalated;
      case "WAITING":
        return ticket.status === "WAITING_FOR_STUDENT";
      case "MINE":
        return ticket.assignee === username;
      default:
        return true;
    }
  });

  return (
    <>
      <div className="sectionHeader">
        <div>
          <h2>Requests</h2>
          <p>
            {visible.length} of {tickets.length} tickets
          </p>
        </div>

        <button className="outline" onClick={onRefresh}>
          ↻ Refresh
        </button>
      </div>

      <div className="filters">
        {FILTERS.map((item) => (
          <button
            key={item}
            className={filter === item ? "active" : ""}
            onClick={() => setFilter(item)}
          >
            {item}
          </button>
        ))}
      </div>

      <div className="ticketList">
        {visible.length === 0 ? (
          <div className="empty">No tickets match this filter.</div>
        ) : (
          visible.map((ticket) => (
            <button
              key={ticket.id}
              className={
                "ticketItem " + (selectedId === ticket.id ? "selected" : "")
              }
              onClick={() => onSelect(ticket.id)}
            >
              <div className="ticketLine">
                <span className="muted">
                  #{String(ticket.id).padStart(4, "0")} ·{" "}
                  {ticket.category.replaceAll("_", " ")}
                </span>

                <span className="badge">
                  {ticket.status.replaceAll("_", " ")}
                </span>
              </div>

              <strong>{ticket.title}</strong>

              <div className="ticketLine muted">
                <span>
                  {ticket.studentUsername} · {ticket.assignee || "Unassigned"} ·
                  age {age(ticket.createdAt)}
                </span>

                <span className={overdue(ticket) ? "warning" : ""}>
                  {ticket.escalated ? "▲ Escalated " : ""}
                  {overdue(ticket) ? "● Overdue" : ""}
                </span>
              </div>
            </button>
          ))
        )}
      </div>
    </>
  );
}

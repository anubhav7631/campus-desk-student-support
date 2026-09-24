
import React, { useCallback, useMemo, useState } from "react";
import { createApi } from "./api/client.js";
import Login from "./components/Login.jsx";
import TicketForm from "./components/TicketForm.jsx";
import TicketList from "./components/TicketList.jsx";
import TicketDetail from "./components/TicketDetail.jsx";

export default function App() {
  const [user, setUser] = useState(null);
  const [credentials, setCredentials] = useState(null);

  const [tickets, setTickets] = useState([]);
  const [dashboard, setDashboard] = useState(null);
  const [selectedId, setSelectedId] = useState(null);
  const [events, setEvents] = useState([]);
  const [attachments, setAttachments] = useState([]);

  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const api = useMemo(
    () => (credentials ? createApi(credentials) : null),
    [credentials],
  );

  const selectedTicket =
    tickets.find((ticket) => ticket.id === selectedId) || null;

  async function login(username, password) {
    setBusy(true);
    setError("");

    try {
      const encoded = btoa(`${username}:${password}`);
      const loginApi = createApi(encoded);
      const currentUser = await loginApi.me();
      const allTickets = await loginApi.tickets();

      let stats = null;
      if (currentUser.role !== "STUDENT") {
        stats = await loginApi.dashboard();
      }

      setCredentials(encoded);
      setUser(currentUser);
      setTickets(allTickets);
      setDashboard(stats);
      setSelectedId(null);
      setEvents([]);
      setAttachments([]);
    } catch {
      setError("Login failed. Check your username and password.");
    } finally {
      setBusy(false);
    }
  }

  function logout() {
    setUser(null);
    setCredentials(null);
    setTickets([]);
    setDashboard(null);
    setSelectedId(null);
    setEvents([]);
    setAttachments([]);
    setError("");
  }

  const selectTicket = useCallback(
    async (id) => {
      if (!api) return;

      setError("");

      try {
        const [history, files] = await Promise.all([
          api.events(id),
          api.attachments(id),
        ]);

        setSelectedId(id);
        setEvents(history);
        setAttachments(files);
      } catch (problem) {
        setError(problem.message);
      }
    },
    [api],
  );

  async function refresh(ticketId = selectedId) {
    if (!api || !user) return;

    setError("");

    try {
      const allTickets = await api.tickets();
      setTickets(allTickets);

      if (user.role !== "STUDENT") {
        setDashboard(await api.dashboard());
      }

      if (ticketId) {
        await selectTicket(ticketId);
      }
    } catch (problem) {
      setError(problem.message);
    }
  }

  async function createTicket(form) {
    setBusy(true);
    setError("");

    try {
      const created = await api.createTicket(form);
      await refresh(created.id);
      return true;
    } catch (problem) {
      setError(problem.message);
      return false;
    } finally {
      setBusy(false);
    }
  }

  async function changeTicket(id, change) {
    setBusy(true);
    setError("");

    try {
      await api.changeTicket(id, change);
      await refresh(id);
      return true;
    } catch (problem) {
      setError(problem.message);
      return false;
    } finally {
      setBusy(false);
    }
  }

  async function uploadAttachment(id, file) {
    setBusy(true);
    setError("");

    try {
      await api.uploadAttachment(id, file);
      setAttachments(await api.attachments(id));
      return true;
    } catch (problem) {
      setError(problem.message);
      return false;
    } finally {
      setBusy(false);
    }
  }

  async function downloadAttachment(id, attachment) {
    setError("");

    try {
      await api.downloadAttachment(id, attachment);
    } catch (problem) {
      setError(problem.message);
    }
  }

  if (!user) {
    return <Login onLogin={login} error={error} busy={busy} />;
  }

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="brand">◆ Campus Desk</div>

        <div className="sidebarLabel">WORKSPACE</div>
        <div className="navItem">◫ &nbsp; Ticket overview</div>

        <div className="sidebarFooter">
          <strong>{user.username}</strong>
          <span>{user.role.toLowerCase()} account</span>
          <button onClick={logout}>Sign out →</button>
        </div>
      </aside>

      <main className="main">
        <header className="pageHeader">
          <div>
            <div className="eyebrow">CAMPUS OPERATIONS</div>
            <h1>Support tickets</h1>
            <p>Manage student requests and keep everyone informed.</p>
          </div>

          <div className="avatar">
            {user.username.slice(0, 2).toUpperCase()}
          </div>
        </header>

        {error && (
          <div className="errorBanner" role="alert">
            <span>{error}</span>
            <button onClick={() => setError("")}>×</button>
          </div>
        )}

        {dashboard && (
          <div className="stats">
            {Object.entries(dashboard).map(([name, count]) => (
              <div className="stat" key={name}>
                <span>{name}</span>
                <strong>{count}</strong>
              </div>
            ))}
          </div>
        )}

        <div className="columns">
          <div>
            <TicketList
              tickets={tickets}
              selectedId={selectedId}
              username={user.username}
              onSelect={selectTicket}
              onRefresh={() => refresh()}
            />

            {user.role === "STUDENT" && (
              <TicketForm busy={busy} onCreate={createTicket} />
            )}
          </div>

          <TicketDetail
            ticket={selectedTicket}
            events={events}
            attachments={attachments}
            user={user}
            busy={busy}
            onAction={changeTicket}
            onUpload={uploadAttachment}
            onDownload={downloadAttachment}
          />
        </div>
      </main>
    </div>
  );
}

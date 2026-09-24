import React, { useState } from "react";
const initialForm = {
  title: "",
  description: "",
  category: "FEES",
  priority: "MEDIUM",
};

export default function TicketForm({ onCreate, busy }) {
  const [form, setForm] = useState(initialForm);

  async function submit(event) {
    event.preventDefault();

    const created = await onCreate(form);

    if (created) {
      setForm(initialForm);
    }
  }

  function update(field, value) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  return (
    <div className="card formCard">
      <h2>New request</h2>
      <p>Tell the support team what you need help with.</p>

      <form onSubmit={submit}>
        <label>
          Subject
          <input
            value={form.title}
            onChange={(event) => update("title", event.target.value)}
            maxLength={120}
            placeholder="Brief summary"
            required
          />
        </label>

        <label>
          Description
          <textarea
            value={form.description}
            onChange={(event) => update("description", event.target.value)}
            maxLength={2000}
            placeholder="Add helpful details"
            required
          />
        </label>

        <div className="formRow">
          <label>
            Category
            <select
              value={form.category}
              onChange={(event) => update("category", event.target.value)}
            >
              <option value="FEES">Fees</option>
              <option value="ATTENDANCE">Attendance</option>
              <option value="ID_CARD">ID card</option>
              <option value="DOCUMENTS">Documents</option>
              <option value="OTHER">Other</option>
            </select>
          </label>

          <label>
            Priority
            <select
              value={form.priority}
              onChange={(event) => update("priority", event.target.value)}
            >
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
            </select>
          </label>
        </div>

        <button disabled={busy}>
          {busy ? "Submitting..." : "Submit request"}
        </button>
      </form>
    </div>
  );
}

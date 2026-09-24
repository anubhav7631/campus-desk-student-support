const BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8080/api";

async function handleResponse(response) {
  if (!response.ok) {
    let message = `Request failed (${response.status})`;

    try {
      const error = await response.json();
      message = error.message || message;
    } catch {
      // Keep the HTTP status message if the response has no JSON body.
    }

    throw new Error(message);
  }

  if (response.status === 204) return null;

  return response.json();
}

export function createApi(credentials) {
  const authorization = `Basic ${credentials}`;

  async function request(path, options = {}) {
    const headers = {
      Authorization: authorization,
      ...options.headers,
    };

    if (options.body && !(options.body instanceof FormData)) {
      headers["Content-Type"] = "application/json";
    }

    const response = await fetch(`${BASE_URL}${path}`, {
      ...options,
      headers,
    });

    return handleResponse(response);
  }

  return {
    me: () => request("/me"),

    tickets: () => request("/tickets"),

    ticket: (id) => request(`/tickets/${id}`),

    events: (id) => request(`/tickets/${id}/events`),

    dashboard: () => request("/dashboard"),

    createTicket: (ticket) =>
      request("/tickets", {
        method: "POST",
        body: JSON.stringify(ticket),
      }),

    changeTicket: (id, change) =>
      request(`/tickets/${id}`, {
        method: "PATCH",
        body: JSON.stringify(change),
      }),

    attachments: (id) => request(`/tickets/${id}/attachments`),

    uploadAttachment: (id, file) => {
      const data = new FormData();
      data.append("file", file);

      return request(`/tickets/${id}/attachments`, {
        method: "POST",
        body: data,
      });
    },

    async downloadAttachment(ticketId, attachment) {
      const response = await fetch(
        `${BASE_URL}/tickets/${ticketId}/attachments/${attachment.id}`,
        {
          headers: { Authorization: authorization },
        },
      );

      if (!response.ok) {
        throw new Error(`Download failed (${response.status})`);
      }

      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");

      link.href = url;
      link.download = attachment.filename;
      document.body.appendChild(link);
      link.click();
      link.remove();

      setTimeout(() => URL.revokeObjectURL(url), 1000);
    },
  };
}

# Campus Desk — Student Support & Ticket Management

Edumerge Solutions pre-drive assignment **Option 4**. A React + Spring Boot + MySQL prototype for student requests, staff ownership, manager assignment, due dates, escalation, and activity entries. The frontend build passed; backend startup still requires local validation.

## Run locally

Requirements: Java 17+, Maven 3.9+, MySQL 8+, Node 18+ and npm.

1. Start MySQL. The app creates the `student_support` database when the configured user has permission. Alternatively run `CREATE DATABASE student_support;` first.
2. In `backend/`, configure `DB_USER` and `DB_PASSWORD` environment variables. The default URL is `jdbc:mysql://localhost:3306/student_support?createDatabaseIfNotExist=true`; set `DB_URL` if needed. Run `mvn spring-boot:run`.
3. In `frontend/`, run `npm install` then `npm run dev`. Open `http://localhost:5173`.
4. Sign in using **student1 / student123**, **student2 / student123**, **staff1 / staff123**, **staff2 / staff123**, or **manager / manager123**. These are development accounts only. Passwords can be overridden with `DEMO_STUDENT_PASSWORD`, `DEMO_STAFF_PASSWORD`, and `DEMO_MANAGER_PASSWORD`.

If the backend runs elsewhere set `VITE_API_URL` to the full API base, e.g. `http://localhost:8080/api`. Set `FRONTEND_ORIGIN` in the backend when changing the frontend origin.

## Demo flow

1. As `student1`, create a HIGH priority fee request, add a comment, and inspect its history.
2. As `manager`, assign it to `staff1` and inspect the dashboard.
3. As `staff1`, move it to Waiting for Student, resume it, then resolve it with a note.
4. As `student1`, reopen it. Confirm the old resolution remains in the history and its new deadline is 24 hours away.
5. As `student2`, confirm that `student1`'s ticket is absent and direct access by ID returns 403.

6. As manager, change a ticket priority and observe its recalculated due date. As assigned staff, escalate it; as manager, clear escalation. Try the overdue, escalated, waiting, and mine filters.

## Rules and design

- Students create and see only their own tickets. Staff see the queue, claim tickets for themselves, and can update only tickets assigned to them. Managers assign/reassign and update tickets.
- States: OPEN → IN_PROGRESS → WAITING_FOR_STUDENT / RESOLVED; WAITING_FOR_STUDENT → IN_PROGRESS / RESOLVED; RESOLVED → OPEN on student or manager reopening. Reopening clears the owner and starts a new 24-hour due window.
- Initial SLA: HIGH 24 hours, MEDIUM 48 hours, LOW 72 hours, measured continuously in UTC from creation; the dashboard marks unresolved tickets overdue when `dueAt` passes. Waiting time counts toward SLA. Staff must explicitly resolve requests; a note alone does not change the state.
- Managers can adjust priority, recalculating the due date from creation. Assigned staff and managers can escalate; managers can clear escalation. The queue shows age. Student replies to waiting tickets resume processing. No automatic escalation or external notification is implemented.
- Two tables: `tickets` holds current state and deadline, `ticket_events` holds append-only actions and comments. Hibernate creates or updates tables for this prototype. `@Version` detects concurrent ticket updates.
- HTTP Basic authentication and in-memory demo accounts keep the prototype straightforward; MySQL persists tickets and history across backend restarts. Run behind HTTPS if deployed. Before production, use persistent user accounts, proper credential management, migrations, pagination, and database-backed dashboard aggregates.
- Categories are FEES, ATTENDANCE, ID_CARD, DOCUMENTS, OTHER. Students select priority for this prototype. No email notifications or attachments are implemented.

## API

All routes require Basic authentication. `GET /api/me`, `GET /api/tickets`, `POST /api/tickets`, `GET /api/tickets/{id}`, `GET /api/tickets/{id}/events`, `PATCH /api/tickets/{id}`, `GET /api/dashboard` (staff and manager).

Create body: `{"title":"Fee receipt missing","description":"Paid yesterday","category":"FEES","priority":"HIGH"}`.

Update body: `{"action":"ASSIGN","value":"staff1","note":"Finance team"}`; actions are ASSIGN, PRIORITY (LOW, MEDIUM, HIGH), ESCALATE, DEESCALATE, STATUS (value IN_PROGRESS, WAITING_FOR_STUDENT, or RESOLVED as permitted), REOPEN, COMMENT.

## Validation checklist

- Student isolation: student2 cannot list or fetch student1's tickets or events.
- Invalid category/priority and invalid status transitions return 400; unauthorized roles return 403.
- Staff cannot assign another staff member or resolve another staff member's ticket.
- Resolved tickets cannot receive comments until reopened; reopening resets due date and owner.
- Overdue counts exclude resolved tickets; activity rows preserve actor, event, and timestamp.

See `APPROACH.md` and `AI_USAGE_REPORT.md`. Replace development passwords before sharing a hosted instance.

# Approach and assumptions

## Problem

Student requests can be lost when ownership, deadlines, and follow-up are unclear. Campus Desk centralizes requests and shows the next responsible person and a full action history.

## Scope

Three roles: student, staff, manager. The student submits, comments, tracks, and reopens their own requests. Staff claims and handles requests assigned to them. The manager assigns/reassigns and views workload and overdue counts. The UI shows current state, owner, due time, and event history.

## Engineering choices

- Java 17 + Spring Boot for clear service rules and transactional writes; MySQL for durable ticket and event records; React for the task-oriented dashboard.
- Basic authentication with in-memory users supports a reproducible demonstration. Authorization is enforced by the backend; React only hides unavailable actions for usability.
- A ticket update and event entry share one transaction. An optimistic version field protects conflicting edits; a concurrent request may return an error and the user can refresh.
- Deadlines are stored as UTC instants. A request counts as overdue only while unresolved and after its due time. SLA remains active while awaiting the student.

## Trade-offs and next steps

This prototype prioritizes an explainable workflow over integrations. Escalation is a manager-visible flag set by assigned staff or managers, without automated alerts. It has two demo staff accounts, no admin account management, attachments, automated escalation, notifications, or holidays/business-hour calendars. The manager dashboard currently loads all tickets, suitable only for a small demo dataset. Production work would add migrations, persistent identity, server-side paging and aggregation, SLA policy tables, automated escalation and tests around concurrent updates and state rules.

package net.officefloor.hq.app;

import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Per-spec data setup for the harness (DESIGN.md §9). Profile-guarded so it exists ONLY under the
 * harness launch (bin/start sets spring.profiles.active=harness) — never in a real deploy. This is
 * APP CODE and EVOLVES with the schema (NOT pinned); a change that breaks a prior spec's seed is a
 * seed-path regression. Tests call these to ARRANGE data; they ASSERT only through the UI.
 */
@Profile("harness")
@RestController
@RequestMapping("/__test__")
public class TestSupportController {

    private final Audit audit;
    private final JdbcTemplate jdbc;

    public TestSupportController(Audit audit, JdbcTemplate jdbc) {
        this.audit = audit;
        this.jdbc = jdbc;
    }

    /** Truncate all domain tables and clear the audit file so each spec starts clean. */
    @PostMapping("/reset")
    public void reset() {
        audit.clear();
        // invoices references projects references clients, so H2 refuses to TRUNCATE a parent while
        // the FK exists — drop referential integrity for the duration, truncate all, then restore.
        jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
        try {
            jdbc.execute("TRUNCATE TABLE dashboard_reference");
            jdbc.execute("TRUNCATE TABLE notes RESTART IDENTITY");
            jdbc.execute("TRUNCATE TABLE project_tags RESTART IDENTITY");
            jdbc.execute("TRUNCATE TABLE tags RESTART IDENTITY");
            jdbc.execute("TRUNCATE TABLE tasks RESTART IDENTITY");
            jdbc.execute("TRUNCATE TABLE invoice_payments RESTART IDENTITY");
            jdbc.execute("TRUNCATE TABLE invoice_line_items RESTART IDENTITY");
            jdbc.execute("TRUNCATE TABLE invoices RESTART IDENTITY");
            jdbc.execute("TRUNCATE TABLE contacts RESTART IDENTITY");
            jdbc.execute("TRUNCATE TABLE projects RESTART IDENTITY");
            jdbc.execute("TRUNCATE TABLE clients RESTART IDENTITY");
        } finally {
            jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
    }

    /** Insert the fixture a spec needs; the payload shape evolves with the schema. */
    @PostMapping("/seed")
    @SuppressWarnings("unchecked")
    public void seed(@RequestBody Map<String, Object> fixture) {
        // The reference date the dashboard measures "overdue" against (single row, id = 1). Optional:
        // a spec that does not care leaves it out, so the dashboard falls back to today.
        Object asOf = fixture.get("asOf");
        if (asOf != null) {
            jdbc.update("INSERT INTO dashboard_reference (id, as_of) VALUES (1, ?)",
                    java.sql.Date.valueOf(java.time.LocalDate.parse(asOf.toString())));
        }
        List<Map<String, Object>> clients =
                (List<Map<String, Object>>) fixture.getOrDefault("clients", List.of());
        // Explicit ids from the fixture (the spec asserts rows by these ids), via JdbcTemplate.
        for (Map<String, Object> c : clients) {
            jdbc.update("INSERT INTO clients (id, name, email) VALUES (?, ?, ?)",
                    ((Number) c.get("id")).longValue(), c.get("name"), c.get("email"));
        }
        List<Map<String, Object>> projects =
                (List<Map<String, Object>>) fixture.getOrDefault("projects", List.of());
        for (Map<String, Object> p : projects) {
            // status is optional in the fixture — a spec that only cares a project exists leaves it
            // out, defaulting to ACTIVE (the same state a fresh project gets, V22__project_status.sql).
            Object status = p.getOrDefault("status", "ACTIVE");
            // archived is optional too — a spec that only cares a project exists leaves it out,
            // defaulting to not-archived (the same state a fresh project gets, V16__project_archived.sql).
            boolean archived = Boolean.TRUE.equals(p.getOrDefault("archived", Boolean.FALSE));
            // budget is optional in the fixture — a spec that does not care about the budget leaves
            // it out, so the column stays NULL (the same state a fresh project gets until one is set,
            // V23__project_budget.sql).
            java.math.BigDecimal budget = p.get("budget") != null
                    ? new java.math.BigDecimal(p.get("budget").toString())
                    : null;
            jdbc.update(
                    "INSERT INTO projects (id, name, client_id, status, archived, budget)"
                            + " VALUES (?, ?, ?, ?, ?, ?)",
                    ((Number) p.get("id")).longValue(), p.get("name"),
                    ((Number) p.get("clientId")).longValue(), status.toString(), archived, budget);
        }
        List<Map<String, Object>> contacts =
                (List<Map<String, Object>>) fixture.getOrDefault("contacts", List.of());
        for (Map<String, Object> c : contacts) {
            // primary is optional in the fixture — a spec that does not care leaves it out, so the
            // contact defaults to not-primary (the same state a fresh contact gets, V26__contact_primary.sql).
            boolean primary = Boolean.TRUE.equals(c.getOrDefault("primary", Boolean.FALSE));
            jdbc.update(
                    "INSERT INTO contacts (id, name, email, contact_role, client_id, is_primary)"
                            + " VALUES (?, ?, ?, ?, ?, ?)",
                    ((Number) c.get("id")).longValue(), c.get("name"), c.get("email"),
                    c.get("role"), ((Number) c.get("clientId")).longValue(), primary);
        }
        List<Map<String, Object>> invoices =
                (List<Map<String, Object>>) fixture.getOrDefault("invoices", List.of());
        for (Map<String, Object> inv : invoices) {
            Object status = inv.getOrDefault("status", "DRAFT");
            // Dates are optional in the fixture — a spec that only cares about the lifecycle leaves
            // them out, so default issued=today / due=+30 days (the same rule CreateInvoice applies).
            java.time.LocalDate issued = inv.get("issuedDate") != null
                    ? java.time.LocalDate.parse(inv.get("issuedDate").toString())
                    : java.time.LocalDate.now();
            java.time.LocalDate due = inv.get("dueDate") != null
                    ? java.time.LocalDate.parse(inv.get("dueDate").toString())
                    : issued.plusDays(30);
            // An invoice is built from line items; its amount is the sum of each line's qty times
            // unit price. A fixture gives the lines (not a figure), so compute the amount from them
            // here — the same derivation CreateLineItem keeps in step on every add. A legacy fixture
            // that still supplies an explicit amount (and no lines) is honoured as-is.
            List<Map<String, Object>> lineItems =
                    (List<Map<String, Object>>) inv.getOrDefault("lineItems", List.of());
            java.math.BigDecimal amount;
            if (inv.get("amount") != null) {
                amount = new java.math.BigDecimal(inv.get("amount").toString());
            } else {
                amount = java.math.BigDecimal.ZERO;
                for (Map<String, Object> li : lineItems) {
                    amount = amount.add(new java.math.BigDecimal(li.get("unitPrice").toString())
                            .multiply(new java.math.BigDecimal(li.get("qty").toString())));
                }
            }
            jdbc.update(
                    "INSERT INTO invoices (id, project_id, amount, status, issued_date, due_date)"
                            + " VALUES (?, ?, ?, ?, ?, ?)",
                    ((Number) inv.get("id")).longValue(),
                    ((Number) inv.get("projectId")).longValue(),
                    amount,
                    status.toString(),
                    java.sql.Date.valueOf(issued),
                    java.sql.Date.valueOf(due));
            // Insert the invoice's lines with their explicit fixture ids (the spec asserts rows by
            // these ids), owned by this invoice.
            for (Map<String, Object> li : lineItems) {
                jdbc.update(
                        "INSERT INTO invoice_line_items"
                                + " (id, invoice_id, description, quantity, unit_price)"
                                + " VALUES (?, ?, ?, ?, ?)",
                        ((Number) li.get("id")).longValue(),
                        ((Number) inv.get("id")).longValue(),
                        li.get("description"),
                        ((Number) li.get("qty")).intValue(),
                        new java.math.BigDecimal(li.get("unitPrice").toString()));
            }
        }
        // Payments a client has made against an invoice (amount + the date paid), with their
        // explicit fixture ids (the spec asserts rows by these ids), owned by a seeded invoice.
        List<Map<String, Object>> payments =
                (List<Map<String, Object>>) fixture.getOrDefault("payments", List.of());
        long maxPaymentId = 0;
        for (Map<String, Object> pay : payments) {
            long paymentId = ((Number) pay.get("id")).longValue();
            jdbc.update(
                    "INSERT INTO invoice_payments (id, invoice_id, amount, paid_date)"
                            + " VALUES (?, ?, ?, ?)",
                    paymentId,
                    ((Number) pay.get("invoiceId")).longValue(),
                    new java.math.BigDecimal(pay.get("amount").toString()),
                    java.sql.Date.valueOf(
                            java.time.LocalDate.parse(pay.get("date").toString())));
            maxPaymentId = Math.max(maxPaymentId, paymentId);
        }
        // Explicit ids (GENERATED BY DEFAULT) do NOT advance H2's identity sequence, so a payment
        // recorded through the API would collide on id 1. Bump the sequence past the seeded rows.
        if (maxPaymentId > 0) {
            jdbc.execute(
                    "ALTER TABLE invoice_payments ALTER COLUMN id RESTART WITH " + (maxPaymentId + 1));
        }
        List<Map<String, Object>> tasks =
                (List<Map<String, Object>>) fixture.getOrDefault("tasks", List.of());
        for (Map<String, Object> t : tasks) {
            // done is optional in the fixture — a spec that only cares a task exists leaves it out,
            // so it defaults to not-done (the same state CreateTask would give a fresh task).
            boolean done = Boolean.TRUE.equals(t.getOrDefault("done", Boolean.FALSE));
            jdbc.update("INSERT INTO tasks (id, project_id, title, done) VALUES (?, ?, ?, ?)",
                    ((Number) t.get("id")).longValue(),
                    ((Number) t.get("projectId")).longValue(),
                    t.get("title"), done);
        }
        List<Map<String, Object>> tags =
                (List<Map<String, Object>>) fixture.getOrDefault("tags", List.of());
        for (Map<String, Object> tag : tags) {
            jdbc.update("INSERT INTO tags (id, name) VALUES (?, ?)",
                    ((Number) tag.get("id")).longValue(), tag.get("name"));
        }
        // Notes are written against a generic target (type + id); the fixture pins each note's
        // write time so the newest-first order is deterministic. `at` is optional — a spec that
        // only cares a note exists leaves it out, defaulting to now().
        List<Map<String, Object>> notes =
                (List<Map<String, Object>>) fixture.getOrDefault("notes", List.of());
        long maxNoteId = 0;
        for (Map<String, Object> n : notes) {
            java.time.OffsetDateTime at = n.get("at") != null
                    ? java.time.OffsetDateTime.parse(n.get("at").toString())
                    : java.time.OffsetDateTime.now();
            long noteId = ((Number) n.get("id")).longValue();
            jdbc.update(
                    "INSERT INTO notes (id, target_type, target_id, text, created_at)"
                            + " VALUES (?, ?, ?, ?, ?)",
                    noteId,
                    n.get("targetType"),
                    ((Number) n.get("targetId")).longValue(),
                    n.get("text"),
                    at);
            maxNoteId = Math.max(maxNoteId, noteId);
        }
        // Explicit ids (GENERATED BY DEFAULT) do NOT advance H2's identity sequence, so the app's
        // next create would collide on id 1. Bump the sequence past the seeded rows so a note added
        // through the API gets a fresh id above them.
        if (maxNoteId > 0) {
            jdbc.execute("ALTER TABLE notes ALTER COLUMN id RESTART WITH " + (maxNoteId + 1));
        }
        // The many-to-many links: which seeded tag sits on which seeded project.
        List<Map<String, Object>> projectTags =
                (List<Map<String, Object>>) fixture.getOrDefault("projectTags", List.of());
        for (Map<String, Object> pt : projectTags) {
            jdbc.update("INSERT INTO project_tags (project_id, tag_id) VALUES (?, ?)",
                    ((Number) pt.get("projectId")).longValue(),
                    ((Number) pt.get("tagId")).longValue());
        }
    }
}

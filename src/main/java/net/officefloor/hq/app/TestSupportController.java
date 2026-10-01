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
        // projects -> clients foreign key: H2 refuses TRUNCATE on a referenced table, so drop
        // referential integrity for the truncate and restore it immediately after.
        jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbc.execute("TRUNCATE TABLE project_tags RESTART IDENTITY");
        jdbc.execute("TRUNCATE TABLE tags RESTART IDENTITY");
        jdbc.execute("TRUNCATE TABLE notes RESTART IDENTITY");
        jdbc.execute("TRUNCATE TABLE tasks RESTART IDENTITY");
        jdbc.execute("TRUNCATE TABLE invoice_line_items RESTART IDENTITY");
        jdbc.execute("TRUNCATE TABLE invoices RESTART IDENTITY");
        jdbc.execute("TRUNCATE TABLE contacts RESTART IDENTITY");
        jdbc.execute("TRUNCATE TABLE projects RESTART IDENTITY");
        jdbc.execute("TRUNCATE TABLE clients RESTART IDENTITY");
        jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    /** Insert the fixture a spec needs; the payload shape evolves with the schema. */
    @PostMapping("/seed")
    @SuppressWarnings("unchecked")
    public void seed(@RequestBody Map<String, Object> fixture) {
        List<Map<String, Object>> clients = (List<Map<String, Object>>) fixture.get("clients");
        if (clients != null) {
            for (Map<String, Object> client : clients) {
                // Explicit fixture id (JPA save() would ignore it on an IDENTITY column).
                jdbc.update("INSERT INTO clients (id, name, email) VALUES (?, ?, ?)",
                        ((Number) client.get("id")).longValue(), client.get("name"),
                        client.get("email"));
            }
        }
        List<Map<String, Object>> projects = (List<Map<String, Object>>) fixture.get("projects");
        if (projects != null) {
            for (Map<String, Object> project : projects) {
                jdbc.update("INSERT INTO projects (id, name, client_id) VALUES (?, ?, ?)",
                        ((Number) project.get("id")).longValue(), project.get("name"),
                        ((Number) project.get("clientId")).longValue());
            }
        }
        List<Map<String, Object>> contacts = (List<Map<String, Object>>) fixture.get("contacts");
        if (contacts != null) {
            for (Map<String, Object> contact : contacts) {
                jdbc.update("INSERT INTO contacts (id, client_id, name, email, contact_role)"
                        + " VALUES (?, ?, ?, ?, ?)",
                        ((Number) contact.get("id")).longValue(),
                        ((Number) contact.get("clientId")).longValue(), contact.get("name"),
                        contact.get("email"), contact.get("role"));
            }
        }
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) fixture.get("tasks");
        if (tasks != null) {
            for (Map<String, Object> task : tasks) {
                // done is optional in a fixture; default to false (OPEN) when absent.
                Object done = task.get("done");
                jdbc.update("INSERT INTO tasks (id, project_id, title, done) VALUES (?, ?, ?, ?)",
                        ((Number) task.get("id")).longValue(),
                        ((Number) task.get("projectId")).longValue(), task.get("title"),
                        done != null && Boolean.parseBoolean(done.toString()));
            }
        }
        List<Map<String, Object>> notes = (List<Map<String, Object>>) fixture.get("notes");
        if (notes != null) {
            long maxId = 0;
            for (Map<String, Object> note : notes) {
                long id = ((Number) note.get("id")).longValue();
                maxId = Math.max(maxId, id);
                // `at` is an ISO instant (e.g. "2026-01-05T09:00:00Z"); store it as a UTC
                // timestamp so notes sort newest-first by the instant they were written.
                jdbc.update("INSERT INTO notes (id, target_type, target_id, note_text, at)"
                        + " VALUES (?, ?, ?, ?, ?)",
                        id, note.get("targetType"),
                        ((Number) note.get("targetId")).longValue(), note.get("text"),
                        java.time.OffsetDateTime.ofInstant(
                                java.time.Instant.parse(note.get("at").toString()),
                                java.time.ZoneOffset.UTC));
            }
            // Explicit-id inserts don't advance H2's IDENTITY counter, so a later JPA save() would
            // regenerate a seeded id and collide. Restart the counter past the seeded ids.
            if (maxId > 0) {
                jdbc.execute("ALTER TABLE notes ALTER COLUMN id RESTART WITH " + (maxId + 1));
            }
        }
        List<Map<String, Object>> tags = (List<Map<String, Object>>) fixture.get("tags");
        if (tags != null) {
            for (Map<String, Object> tag : tags) {
                // Explicit fixture id (JPA save() would ignore it on an IDENTITY column).
                jdbc.update("INSERT INTO tags (id, name) VALUES (?, ?)",
                        ((Number) tag.get("id")).longValue(), tag.get("name"));
            }
        }
        List<Map<String, Object>> projectTags =
                (List<Map<String, Object>>) fixture.get("projectTags");
        if (projectTags != null) {
            for (Map<String, Object> link : projectTags) {
                // The join row's own id is IDENTITY-generated; the fixture names only the pair.
                jdbc.update("INSERT INTO project_tags (project_id, tag_id) VALUES (?, ?)",
                        ((Number) link.get("projectId")).longValue(),
                        ((Number) link.get("tagId")).longValue());
            }
        }
        List<Map<String, Object>> invoices = (List<Map<String, Object>>) fixture.get("invoices");
        if (invoices != null) {
            for (Map<String, Object> invoice : invoices) {
                // status is optional in a fixture; default to UNPAID (matching Flyway V5) when absent.
                Object status = invoice.get("status");
                // issuedDate/dueDate are optional; fall back to the Flyway V7 column defaults when absent.
                Object issuedDate = invoice.get("issuedDate");
                Object dueDate = invoice.get("dueDate");
                // An invoice is built from line items (Flyway V13). Its amount is the worked-out sum
                // of qty * unitPrice across them (zero when there are none yet) — a fixture no longer
                // types a figure, it lists what is charged. (An explicit `amount` still wins if a
                // fixture gives one, for back-compatibility with lump-sum invoices.)
                List<Map<String, Object>> lineItems =
                        (List<Map<String, Object>>) invoice.get("lineItems");
                java.math.BigDecimal amount = java.math.BigDecimal.ZERO;
                if (lineItems != null) {
                    for (Map<String, Object> line : lineItems) {
                        java.math.BigDecimal unitPrice =
                                new java.math.BigDecimal(line.get("unitPrice").toString());
                        int qty = ((Number) line.get("qty")).intValue();
                        amount = amount.add(unitPrice.multiply(java.math.BigDecimal.valueOf(qty)));
                    }
                }
                if (invoice.get("amount") != null) {
                    amount = new java.math.BigDecimal(invoice.get("amount").toString());
                }
                long invoiceId = ((Number) invoice.get("id")).longValue();
                jdbc.update("INSERT INTO invoices (id, project_id, amount, status, issued_date, due_date)"
                        + " VALUES (?, ?, ?, ?, COALESCE(?, CURRENT_DATE),"
                        + " COALESCE(?, DATEADD('DAY', 30, CURRENT_DATE)))",
                        invoiceId,
                        ((Number) invoice.get("projectId")).longValue(),
                        amount,
                        status != null ? status.toString() : "UNPAID",
                        issuedDate != null ? java.sql.Date.valueOf(issuedDate.toString()) : null,
                        dueDate != null ? java.sql.Date.valueOf(dueDate.toString()) : null);
                if (lineItems != null) {
                    for (Map<String, Object> line : lineItems) {
                        jdbc.update("INSERT INTO invoice_line_items"
                                + " (id, invoice_id, description, qty, unit_price) VALUES (?, ?, ?, ?, ?)",
                                ((Number) line.get("id")).longValue(), invoiceId,
                                line.get("description"), ((Number) line.get("qty")).intValue(),
                                new java.math.BigDecimal(line.get("unitPrice").toString()));
                    }
                }
            }
        }
    }
}

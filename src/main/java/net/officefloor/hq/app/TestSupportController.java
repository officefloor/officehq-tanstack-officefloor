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
        // invoice references project references client, so drop referential integrity around the
        // truncates: RESTART IDENTITY on each, then restore it. Order-independent and clears every
        // FK-referenced table.
        jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbc.execute("TRUNCATE TABLE invoice RESTART IDENTITY");
        jdbc.execute("TRUNCATE TABLE contact RESTART IDENTITY");
        jdbc.execute("TRUNCATE TABLE project RESTART IDENTITY");
        jdbc.execute("TRUNCATE TABLE client RESTART IDENTITY");
        jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    /** Insert the fixture a spec needs; the payload shape evolves with the schema. */
    @SuppressWarnings("unchecked")
    @PostMapping("/seed")
    public void seed(@RequestBody Map<String, Object> fixture) {
        List<Map<String, Object>> clients = (List<Map<String, Object>>) fixture.get("clients");
        if (clients != null) {
            for (Map<String, Object> c : clients) {
                jdbc.update("INSERT INTO client (id, name, email) VALUES (?, ?, ?)",
                        ((Number) c.get("id")).longValue(), c.get("name"), c.get("email"));
            }
        }
        List<Map<String, Object>> projects = (List<Map<String, Object>>) fixture.get("projects");
        if (projects != null) {
            for (Map<String, Object> p : projects) {
                jdbc.update("INSERT INTO project (id, name, client_id) VALUES (?, ?, ?)",
                        ((Number) p.get("id")).longValue(), p.get("name"),
                        ((Number) p.get("clientId")).longValue());
            }
        }
        List<Map<String, Object>> contacts = (List<Map<String, Object>>) fixture.get("contacts");
        if (contacts != null) {
            for (Map<String, Object> c : contacts) {
                jdbc.update("INSERT INTO contact (id, client_id, name, email, role) VALUES (?, ?, ?, ?, ?)",
                        ((Number) c.get("id")).longValue(),
                        ((Number) c.get("clientId")).longValue(),
                        c.get("name"), c.get("email"), c.get("role"));
            }
        }
        List<Map<String, Object>> invoices = (List<Map<String, Object>>) fixture.get("invoices");
        if (invoices != null) {
            for (Map<String, Object> i : invoices) {
                Object status = i.get("status");
                jdbc.update("INSERT INTO invoice (id, project_id, amount, status, issued_date, due_date) "
                                + "VALUES (?, ?, ?, ?, ?, ?)",
                        ((Number) i.get("id")).longValue(),
                        ((Number) i.get("projectId")).longValue(),
                        ((Number) i.get("amount")).doubleValue(),
                        status == null ? "UNPAID" : status,
                        i.get("issuedDate"),
                        i.get("dueDate"));
            }
        }
    }
}

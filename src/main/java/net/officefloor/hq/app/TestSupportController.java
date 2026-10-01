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
    }
}

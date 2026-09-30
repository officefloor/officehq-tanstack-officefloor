package net.officefloor.hq.app;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
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

    /**
     * Every domain table, child-before-parent. Referential integrity is dropped around the
     * TRUNCATE so the order is only for readability of the FK chain (invoices -> projects ->
     * clients). Add a new table here (and a matching {@code seedTable} call) to extend the harness.
     */
    private static final List<String> TABLES =
            List.of("invoices", "tasks", "contacts", "projects", "clients");

    /** Truncate all domain tables and clear the audit file so each spec starts clean. */
    @PostMapping("/reset")
    public void reset() {
        audit.clear();
        jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
        for (String table : TABLES) {
            jdbc.execute("TRUNCATE TABLE " + table + " RESTART IDENTITY");
        }
        jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    /** Insert the fixture a spec needs; the payload shape evolves with the schema. */
    @PostMapping("/seed")
    public void seed(@RequestBody Map<String, Object> fixture) {
        seedTable(fixture, "clients", "INSERT INTO clients (id, name, email) VALUES (?, ?, ?)",
                c -> new Object[] {id(c, "id"), c.get("name"), c.get("email")});
        seedTable(fixture, "projects", "INSERT INTO projects (id, name, client_id) VALUES (?, ?, ?)",
                p -> new Object[] {id(p, "id"), p.get("name"), id(p, "clientId")});
        seedTable(fixture, "contacts",
                "INSERT INTO contacts (id, name, email, role, client_id) VALUES (?, ?, ?, ?, ?)",
                c -> new Object[] {id(c, "id"), c.get("name"), c.get("email"), c.get("role"),
                        id(c, "clientId")});
        seedTable(fixture, "tasks", "INSERT INTO tasks (id, title, done, project_id) VALUES (?, ?, ?, ?)",
                t -> new Object[] {id(t, "id"), t.get("title"), Boolean.TRUE.equals(t.get("done")),
                        id(t, "projectId")});
        seedTable(fixture, "invoices",
                "INSERT INTO invoices (id, project_id, amount, status, issued_date, due_date)"
                        + " VALUES (?, ?, ?, ?, ?, ?)",
                i -> new Object[] {id(i, "id"), id(i, "projectId"),
                        ((Number) i.get("amount")).doubleValue(), i.getOrDefault("status", "DRAFT"),
                        i.get("issuedDate"), i.get("dueDate")});
    }

    /**
     * Insert every row the fixture supplies under {@code key}, mapping each to the {@code sql}
     * parameters with {@code toParams}. Absent key = nothing to seed. One call per table keeps
     * seeding a new table a single additive line.
     */
    @SuppressWarnings("unchecked")
    private void seedTable(Map<String, Object> fixture, String key, String sql,
            Function<Map<String, Object>, Object[]> toParams) {
        List<Map<String, Object>> rows = (List<Map<String, Object>>) fixture.get(key);
        if (rows == null) {
            return;
        }
        for (Map<String, Object> row : rows) {
            // Explicit fixture id (JPA save() would ignore it — the spec asserts by these ids).
            jdbc.update(sql, toParams.apply(row));
        }
    }

    /** Read a fixture field as a table id (JSON numbers arrive as {@link Number}). */
    private static long id(Map<String, Object> row, String key) {
        return ((Number) row.get(key)).longValue();
    }
}

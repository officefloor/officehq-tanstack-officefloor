package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The statements taken of each person, kept per sheet in the order they were taken. */
@Repository
public class StatementStore {

    private final JdbcTemplate jdbc;

    public StatementStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void add(String sheetId, Statement statement, String user) {
        jdbc.update("INSERT INTO statement (sheet_id, person_id, taken_on, due_total, claims, taken_by) VALUES (?, ?, ?, ?, ?, ?)",
                sheetId, statement.personId(), Date.valueOf(statement.takenOn()), statement.dueTotal(),
                statement.claims(), user);
    }

    /** Removes the person's last statement and returns it, or null when they have none. */
    public Statement removeLast(String sheetId, long personId) {
        var ids = jdbc.queryForList("SELECT id FROM statement WHERE sheet_id = ? AND person_id = ? ORDER BY id DESC LIMIT 1 FOR UPDATE",
                Long.class, sheetId, personId);
        if (ids.isEmpty()) {
            return null;
        }
        Statement last = jdbc.queryForObject("SELECT taken_on, due_total, claims FROM statement WHERE id = ?",
                (rs, i) -> new Statement(personId, rs.getObject("taken_on", LocalDate.class), rs.getBigDecimal("due_total"),
                        rs.getInt("claims")), ids.get(0));
        jdbc.update("DELETE FROM statement WHERE id = ?", ids.get(0));
        return last;
    }

    /** Each person on the sheet with a statement: their count and last statement. */
    public Map<Long, StatementSummary> summaries(String sheetId) {
        Map<Long, StatementSummary> found = new HashMap<>();
        jdbc.query("SELECT s.person_id, s.taken_on, s.due_total, c.n FROM statement s JOIN "
                + "(SELECT person_id, COUNT(*) AS n, MAX(id) AS last_id FROM statement WHERE sheet_id = ? GROUP BY person_id) c "
                + "ON s.id = c.last_id", rs -> {
                    BigDecimal total = rs.getBigDecimal("due_total");
                    found.put(rs.getLong("person_id"), new StatementSummary(rs.getInt("n"),
                            total == null ? "n/a" : total.setScale(2).toPlainString(),
                            rs.getObject("taken_on", LocalDate.class).toString()));
                }, sheetId);
        return found;
    }

    public StatementSummary summary(String sheetId, long personId) {
        return summaries(sheetId).getOrDefault(personId, StatementSummary.NONE);
    }
}

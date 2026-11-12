package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.history.History;
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

    /** Gives the statements of {@code fromId} on {@code fromSheet} to {@code intoId} on {@code toSheet}, after the
     *  ones {@code intoId} has, but keeps the last of {@code intoId}'s statements their last (when they have none,
     *  the last of the others becomes theirs). The statements of {@code fromId} are dropped with them (see
     *  PersonStore). */
    public void moveAll(String fromSheet, long fromId, String toSheet, long intoId) {
        var last = jdbc.queryForList("SELECT id FROM statement WHERE sheet_id = ? AND person_id = ? ORDER BY id DESC LIMIT 1",
                Long.class, toSheet, intoId);
        jdbc.update("INSERT INTO statement (sheet_id, person_id, taken_on, due_total, claims, taken_by) "
                + "SELECT ?, ?, taken_on, due_total, claims, taken_by FROM statement WHERE sheet_id = ? AND person_id = ? "
                + "ORDER BY id", toSheet, intoId, fromSheet, fromId);
        if (!last.isEmpty()) {
            // listed by id: taken again after the others, so it stays their last
            jdbc.update("INSERT INTO statement (sheet_id, person_id, taken_on, due_total, claims, taken_by) "
                    + "SELECT sheet_id, person_id, taken_on, due_total, claims, taken_by FROM statement WHERE id = ?",
                    last.get(0));
            jdbc.update("DELETE FROM statement WHERE id = ?", last.get(0));
        }
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
        return summariesFrom(sheetId, "statement");
    }

    /** Each person on the sheet with a statement as the statements stood at the end of the date (see
     *  {@link History}): their count and last statement then. */
    public Map<Long, StatementSummary> summariesAsOf(String sheetId, LocalDate date) {
        return summariesFrom(sheetId, History.asOf("statement", "id", date));
    }

    private Map<Long, StatementSummary> summariesFrom(String sheetId, String table) {
        Map<Long, List<StatementSummary.Line>> lines = new HashMap<>();
        jdbc.query("SELECT person_id, taken_on, due_total FROM " + table + " WHERE sheet_id = ? ORDER BY id",
                rs -> {
                    lines.computeIfAbsent(rs.getLong("person_id"), k -> new ArrayList<>()).add(new StatementSummary.Line(
                            rs.getObject("taken_on", LocalDate.class).toString(), figure(rs.getBigDecimal("due_total"))));
                }, sheetId);
        Map<Long, StatementSummary> found = new HashMap<>();
        jdbc.query("SELECT s.person_id, s.taken_on, s.due_total, c.n FROM (SELECT * FROM " + table + ") s JOIN "
                + "(SELECT person_id, COUNT(*) AS n, MAX(id) AS last_id FROM " + table + " WHERE sheet_id = ? GROUP BY person_id) c "
                + "ON s.id = c.last_id", rs -> {
                    long personId = rs.getLong("person_id");
                    found.put(personId, new StatementSummary(rs.getInt("n"), figure(rs.getBigDecimal("due_total")),
                            rs.getObject("taken_on", LocalDate.class).toString(),
                            List.copyOf(lines.getOrDefault(personId, List.of()))));
                }, sheetId);
        return found;
    }

    private static String figure(BigDecimal total) {
        return total == null ? "n/a" : total.setScale(2).toPlainString();
    }

    public StatementSummary summary(String sheetId, long personId) {
        return summaries(sheetId).getOrDefault(personId, StatementSummary.NONE);
    }
}

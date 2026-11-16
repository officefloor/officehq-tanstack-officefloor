package net.officefloor.hq.app.statement;

import java.sql.Date;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The stored statements, kept per sheet. Ids are assigned in the order statements are taken. */
@Repository
public class StatementRepository {

    private final JdbcTemplate jdbc;

    public StatementRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void add(Statement s) {
        jdbc.update("INSERT INTO statement (sheet_id, person_id, taken_on, due_total, claims, taken_by) VALUES (?, ?, ?, ?, ?, ?)",
                s.sheetId(), s.personId(), Date.valueOf(s.takenOn()), s.dueTotal(), s.claims(), s.by());
    }

    /** The person's last statement, or null when they have none. */
    public Statement last(String sheetId, long personId) {
        List<Statement> found = jdbc.query("SELECT sheet_id, person_id, taken_on, due_total, claims, taken_by FROM statement"
                + " WHERE sheet_id = ? AND person_id = ? ORDER BY id DESC LIMIT 1",
                (rs, n) -> new Statement(rs.getString("sheet_id"), rs.getLong("person_id"),
                        rs.getDate("taken_on").toLocalDate(), rs.getString("due_total"), rs.getInt("claims"),
                        rs.getString("taken_by")),
                sheetId, personId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Removes the person's last statement; false when they have none. */
    public boolean removeLast(String sheetId, long personId) {
        return jdbc.update("DELETE FROM statement WHERE id = (SELECT MAX(id) FROM statement WHERE sheet_id = ? AND person_id = ?)",
                sheetId, personId) > 0;
    }

    /** For each person on the sheet with a statement: their count, and their last statement's due-total and date. */
    public List<StatementSummary> summaries(String sheetId) {
        return jdbc.query("SELECT s.person_id, c.n, s.due_total, s.taken_on FROM statement s"
                + " JOIN (SELECT person_id, COUNT(*) n, MAX(id) last_id FROM statement WHERE sheet_id = ? GROUP BY person_id) c"
                + " ON s.id = c.last_id ORDER BY s.person_id",
                (rs, n) -> new StatementSummary(rs.getLong("person_id"), rs.getInt("n"), rs.getString("due_total"),
                        rs.getDate("taken_on").toLocalDate().toString()),
                sheetId);
    }
}

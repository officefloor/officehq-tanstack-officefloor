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

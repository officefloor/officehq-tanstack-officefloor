package net.officefloor.hq.app.statement;

import java.sql.Date;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The store of statements, kept per sheet. */
@Repository
public class Statements {

    private final JdbcTemplate jdbc;

    public Statements(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Locks the person's row until the transaction commits, so two statements of them are taken one after the other. */
    public void lock(long personId) {
        jdbc.queryForList("SELECT id FROM person WHERE id = ? FOR UPDATE", personId);
    }

    public void add(Statement statement) {
        jdbc.update("INSERT INTO statement (sheet_id, person_id, due_total, claims, stated_on) VALUES (?, ?, ?, ?, ?)",
                statement.sheetId(), statement.personId(), statement.dueTotal(), statement.claims(),
                Date.valueOf(statement.statedOn()));
    }

    /** Each person of the sheet who has a statement: their count, and their last statement's due-total and date. */
    public List<StatementResponse> list(String sheetId) {
        return jdbc.query("SELECT s.person_id, c.n, s.due_total, s.stated_on FROM statement s "
                + "JOIN (SELECT person_id, COUNT(*) AS n, MAX(id) AS last_id FROM statement WHERE sheet_id = ? "
                + "GROUP BY person_id) c ON s.id = c.last_id ORDER BY s.person_id",
                (rs, n) -> new StatementResponse(rs.getLong(1), rs.getInt(2), rs.getString(3),
                        rs.getDate(4).toLocalDate().toString()),
                sheetId);
    }

    public StatementResponse find(String sheetId, long personId) {
        return list(sheetId).stream().filter(s -> s.personId() == personId).findFirst()
                .orElse(new StatementResponse(personId, 0, null, null));
    }
}

package net.officefloor.hq.app.statement;

import java.sql.Date;
import java.util.List;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The stored statements, kept per sheet. Ids are assigned in the order statements are taken. A cancelled statement is
 *  kept with the sheet's today when it was cancelled, so a read as of an earlier date still counts it. */
@Repository
public class StatementRepository {

    private final JdbcTemplate jdbc;

    private final SheetRepository sheets;

    public StatementRepository(JdbcTemplate jdbc, SheetRepository sheets) {
        this.jdbc = jdbc;
        this.sheets = sheets;
    }

    public void add(Statement s) {
        jdbc.update("INSERT INTO statement (sheet_id, person_id, taken_on, due_total, claims, taken_by, as_of, made_on) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                s.sheetId(), s.personId(), Date.valueOf(s.takenOn()), s.dueTotal(), s.claims(), s.by(),
                s.asOf() == null ? null : Date.valueOf(s.asOf()), Date.valueOf(s.madeOn()));
    }

    /** The person's last statement, or null when they have none. */
    public Statement last(String sheetId, long personId) {
        List<Statement> found = jdbc.query("SELECT sheet_id, person_id, taken_on, due_total, claims, taken_by, as_of, made_on FROM statement"
                + " WHERE sheet_id = ? AND person_id = ? AND cancelled_on IS NULL ORDER BY id DESC LIMIT 1",
                (rs, n) -> new Statement(rs.getString("sheet_id"), rs.getLong("person_id"),
                        rs.getDate("taken_on").toLocalDate(), rs.getString("due_total"), rs.getInt("claims"),
                        rs.getString("taken_by"), rs.getObject("as_of", java.time.LocalDate.class),
                        rs.getObject("made_on", java.time.LocalDate.class)),
                sheetId, personId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Cancels the person's last statement on the sheet's today; false when they have none. */
    public boolean removeLast(String sheetId, long personId) {
        return jdbc.update("UPDATE statement SET cancelled_on = ? WHERE id = (SELECT MAX(id) FROM statement"
                + " WHERE sheet_id = ? AND person_id = ? AND cancelled_on IS NULL)",
                Date.valueOf(sheets.today(sheetId)), sheetId, personId) > 0;
    }

    /** For each person on the sheet with a statement: their count, and their last statement's due-total and date. */
    public List<StatementSummary> summaries(String sheetId) {
        return jdbc.query("SELECT s.person_id, c.n, s.due_total, s.taken_on FROM statement s"
                + " JOIN (SELECT person_id, COUNT(*) n, MAX(id) last_id FROM statement WHERE sheet_id = ?"
                + " AND cancelled_on IS NULL GROUP BY person_id) c"
                + " ON s.id = c.last_id ORDER BY s.person_id",
                (rs, n) -> new StatementSummary(rs.getLong("person_id"), rs.getInt("n"), rs.getString("due_total"),
                        rs.getDate("taken_on").toLocalDate().toString()),
                sheetId);
    }

    /** The summaries as they stood at the end of the date: only the statements taken while the sheet's today was on or
     *  before it, and not cancelled by then, count. */
    public List<StatementSummary> summaries(String sheetId, java.time.LocalDate asOf) {
        return jdbc.query("SELECT s.person_id, c.n, s.due_total, s.taken_on FROM statement s"
                + " JOIN (SELECT person_id, COUNT(*) n, MAX(id) last_id FROM statement WHERE sheet_id = ?"
                + " AND COALESCE(made_on, taken_on) <= ? AND (cancelled_on IS NULL OR cancelled_on > ?) GROUP BY person_id) c"
                + " ON s.id = c.last_id ORDER BY s.person_id",
                (rs, n) -> new StatementSummary(rs.getLong("person_id"), rs.getInt("n"), rs.getString("due_total"),
                        rs.getDate("taken_on").toLocalDate().toString()),
                sheetId, Date.valueOf(asOf), Date.valueOf(asOf));
    }
}

package net.officefloor.hq.app.statement;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
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
                + " WHERE sheet_id = ? AND person_id = ? AND cancelled_on IS NULL ORDER BY COALESCE(ord, id) DESC LIMIT 1",
                (rs, n) -> new Statement(rs.getString("sheet_id"), rs.getLong("person_id"),
                        rs.getDate("taken_on").toLocalDate(), rs.getString("due_total"), rs.getInt("claims"),
                        rs.getString("taken_by"), rs.getObject("as_of", java.time.LocalDate.class),
                        rs.getObject("made_on", java.time.LocalDate.class)),
                sheetId, personId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Cancels the person's last statement on the sheet's today; false when they have none. */
    public boolean removeLast(String sheetId, long personId) {
        return jdbc.update("UPDATE statement SET cancelled_on = ? WHERE id = (SELECT id FROM statement"
                + " WHERE sheet_id = ? AND person_id = ? AND cancelled_on IS NULL ORDER BY COALESCE(ord, id) DESC LIMIT 1)",
                Date.valueOf(sheets.today(sheetId)), sheetId, personId) > 0;
    }

    /** For each person on the sheet with a statement: their count, their last statement's due-total and date,
     *  and their statement lines. */
    public List<StatementSummary> summaries(String sheetId) {
        Map<Long, List<String>> lines = lines(sheetId, "cancelled_on IS NULL");
        return jdbc.query("SELECT s.person_id, c.n, s.due_total, s.taken_on FROM statement s"
                + " JOIN (SELECT person_id, COUNT(*) n, MAX(COALESCE(ord, id)) last_key FROM statement WHERE sheet_id = ?"
                + " AND cancelled_on IS NULL GROUP BY person_id) c"
                + " ON s.person_id = c.person_id AND COALESCE(s.ord, s.id) = c.last_key AND s.sheet_id = ? ORDER BY s.person_id",
                (rs, n) -> new StatementSummary(rs.getLong("person_id"), rs.getInt("n"), rs.getString("due_total"),
                        rs.getDate("taken_on").toLocalDate().toString(), lines.getOrDefault(rs.getLong("person_id"), List.of())),
                sheetId, sheetId);
    }

    /** The summaries as they stood at the end of the date: only the statements taken while the sheet's today was on or
     *  before it, and not cancelled by then, count. */
    public List<StatementSummary> summaries(String sheetId, java.time.LocalDate asOf) {
        Map<Long, List<String>> lines = lines(sheetId, "COALESCE(made_on, taken_on) <= ? AND (cancelled_on IS NULL OR cancelled_on > ?)",
                Date.valueOf(asOf), Date.valueOf(asOf));
        return jdbc.query("SELECT s.person_id, c.n, s.due_total, s.taken_on FROM statement s"
                + " JOIN (SELECT person_id, COUNT(*) n, MAX(COALESCE(ord, id)) last_key FROM statement WHERE sheet_id = ?"
                + " AND COALESCE(made_on, taken_on) <= ? AND (cancelled_on IS NULL OR cancelled_on > ?) GROUP BY person_id) c"
                + " ON s.person_id = c.person_id AND COALESCE(s.ord, s.id) = c.last_key AND s.sheet_id = ? ORDER BY s.person_id",
                (rs, n) -> new StatementSummary(rs.getLong("person_id"), rs.getInt("n"), rs.getString("due_total"),
                        rs.getDate("taken_on").toLocalDate().toString(), lines.getOrDefault(rs.getLong("person_id"), List.of())),
                sheetId, Date.valueOf(asOf), Date.valueOf(asOf), sheetId);
    }

    /** Each person's statement lines, "<statement date> <due-total>", in the order taken, so the last line is
     *  their last statement (those a merge or a transfer brought over before the person's own). */
    private Map<Long, List<String>> lines(String sheetId, String where, Object... args) {
        Map<Long, List<String>> lines = new HashMap<>();
        Object[] params = new Object[args.length + 1];
        params[0] = sheetId;
        System.arraycopy(args, 0, params, 1, args.length);
        jdbc.query("SELECT person_id, taken_on, due_total FROM statement WHERE sheet_id = ? AND " + where
                + " ORDER BY person_id, COALESCE(ord, id), id",
                (RowCallbackHandler) rs -> lines.computeIfAbsent(rs.getLong("person_id"), k -> new ArrayList<>())
                        .add(rs.getDate("taken_on").toLocalDate() + " " + rs.getString("due_total")),
                params);
        return lines;
    }

    /**
     * Moves the statements of person {@code from} on sheet {@code fromSheet} to person {@code to} on sheet
     * {@code toSheet}, kept in their order and ordered before to's own statements, so to's count is their own plus
     * from's and to's last statement stays theirs (or, having none, becomes from's last).
     */
    public void moveAll(String fromSheet, long from, String toSheet, long to) {
        List<Long> moving = jdbc.queryForList("SELECT id FROM statement WHERE sheet_id = ? AND person_id = ? ORDER BY COALESCE(ord, id), id",
                Long.class, fromSheet, from);
        Long first = jdbc.queryForObject("SELECT MIN(COALESCE(ord, id)) FROM statement WHERE sheet_id = ? AND person_id = ?",
                Long.class, toSheet, to);
        for (int i = 0; i < moving.size(); i++) {
            if (first == null) {
                jdbc.update("UPDATE statement SET sheet_id = ?, person_id = ? WHERE id = ?", toSheet, to, moving.get(i));
            } else {
                jdbc.update("UPDATE statement SET sheet_id = ?, person_id = ?, ord = ? WHERE id = ?", toSheet, to,
                        first - moving.size() + i, moving.get(i));
            }
        }
    }
}

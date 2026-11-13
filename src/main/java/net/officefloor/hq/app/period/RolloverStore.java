package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The rollovers made on each sheet, with the periods each generated and the opening balances each set, so the
 *  last one can be undone. */
@Repository
public class RolloverStore {

    /** A rollover: the old fiscal year (yyyy), who made it and on which sheet-today, and whether it is undone. */
    public record Rollover(long id, String sheetId, int fy, String madeBy, LocalDate madeOn, boolean undone) {
    }

    /** An opening balance a rollover set: the amount it added to the person's opening for the period. */
    public record RolloverOpening(String periodName, long personId, BigDecimal amount) {
    }

    private final JdbcTemplate jdbc;

    public RolloverStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Records a rollover and the periods it generated; answers its id. */
    public long add(String sheetId, int fy, String user, LocalDate today, List<String> generated) {
        jdbc.update("INSERT INTO rollover (sheet_id, fy, made_by, made_on) VALUES (?, ?, ?, ?)", sheetId, fy, user,
                today);
        long id = jdbc.queryForObject("SELECT MAX(id) FROM rollover WHERE sheet_id = ?", Long.class, sheetId);
        for (String name : generated) {
            jdbc.update("INSERT INTO rollover_period (rollover_id, period_name) VALUES (?, ?)", id, name);
        }
        return id;
    }

    /** Records an amount the rollover added to a person's opening balance for the period. */
    public void addOpening(long rolloverId, String periodName, long personId, BigDecimal amount) {
        jdbc.update("INSERT INTO rollover_opening (rollover_id, period_name, person_id, amount) VALUES (?, ?, ?, ?)",
                rolloverId, periodName, personId, amount);
    }

    /** The last rollover made on the sheet, or null when none was. */
    public Rollover last(String sheetId) {
        List<Rollover> found = jdbc.query("SELECT * FROM rollover WHERE sheet_id = ? ORDER BY id DESC LIMIT 1",
                (rs, i) -> new Rollover(rs.getLong("id"), rs.getString("sheet_id"), rs.getInt("fy"),
                        rs.getString("made_by"), rs.getObject("made_on", LocalDate.class), rs.getBoolean("undone")),
                sheetId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The names of the periods the rollover generated. */
    public List<String> periods(long rolloverId) {
        return jdbc.queryForList("SELECT period_name FROM rollover_period WHERE rollover_id = ? ORDER BY period_name",
                String.class, rolloverId);
    }

    /** The opening balances the rollover set. */
    public List<RolloverOpening> openings(long rolloverId) {
        return jdbc.query("SELECT * FROM rollover_opening WHERE rollover_id = ?",
                (rs, i) -> new RolloverOpening(rs.getString("period_name"), rs.getLong("person_id"),
                        rs.getBigDecimal("amount")),
                rolloverId);
    }

    /** Whether a claim (other than a deleted one) on the sheet is dated in from..to (inclusive). */
    public boolean claimIn(String sheetId, LocalDate from, LocalDate to) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM claim WHERE sheet_id = ? AND claim_date >= ? "
                + "AND claim_date <= ? AND status <> 'DELETED'", Integer.class, sheetId, from, to);
        return n != null && n > 0;
    }

    /** Marks the rollover undone; false when it already is (another request undid it first). */
    public boolean markUndone(long rolloverId) {
        return jdbc.update("UPDATE rollover SET undone = TRUE WHERE id = ? AND NOT undone", rolloverId) == 1;
    }

    /** Removes a generated period from the sheet, with any figures fixed in it. */
    public void removePeriod(String sheetId, String name) {
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND period_name = ?", sheetId, name);
        jdbc.update("DELETE FROM period_pool_cut WHERE sheet_id = ? AND period_name = ?", sheetId, name);
        jdbc.update("DELETE FROM period WHERE sheet_id = ? AND name = ?", sheetId, name);
    }

    /** Takes the amount back off the person's opening balance for the period, dropping it when nothing is left. */
    public void takeOpeningBack(String sheetId, RolloverOpening opening) {
        jdbc.update("UPDATE period_person_opening SET amount = amount - ? WHERE sheet_id = ? AND period_name = ? "
                + "AND person_id = ?", opening.amount(), sheetId, opening.periodName(), opening.personId());
        jdbc.update("DELETE FROM period_person_opening WHERE sheet_id = ? AND period_name = ? AND person_id = ? "
                + "AND amount = 0", sheetId, opening.periodName(), opening.personId());
    }
}

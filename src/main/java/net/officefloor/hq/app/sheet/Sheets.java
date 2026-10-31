package net.officefloor.hq.app.sheet;

import java.sql.Date;
import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * The sheets and their "today". A sheet starts with the real date the first time it is loaded;
 * from then on {@link #today(String)} is the only date the app uses for that sheet — never the
 * real clock.
 */
@Repository
public class Sheets {

    private final JdbcTemplate jdbc;

    public Sheets(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's today, starting the sheet at the real date when it has never been used. */
    public LocalDate today(String sheetId) {
        jdbc.update("MERGE INTO sheet (id, today) KEY (id) SELECT ?, ? FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM sheet WHERE id = ?)",
                sheetId, Date.valueOf(LocalDate.now()), sheetId);
        return jdbc.queryForObject("SELECT today FROM sheet WHERE id = ?", Date.class, sheetId)
                .toLocalDate();
    }

    /**
     * The sheet's next claim number: 1 for its first, then 2, and so on. The sheet's row stays
     * locked until the transaction ends, so two claims opened at once get different numbers, and a
     * number is never given twice.
     */
    public long nextClaimNumber(String sheetId) {
        today(sheetId);
        jdbc.update("UPDATE sheet SET claim_numbers = claim_numbers + 1 WHERE id = ?", sheetId);
        return jdbc.queryForObject("SELECT claim_numbers FROM sheet WHERE id = ?", Long.class, sheetId);
    }

    /**
     * The next number of one of the sheet's period sequences (a period's name, or "X" for claims in
     * no period): 1 for its first, then 2, and so on. Locks the sheet's row as {@link #nextClaimNumber}
     * does, so a number is never given twice.
     */
    public long nextPeriodNumber(String sheetId, String seq) {
        today(sheetId);
        jdbc.update("UPDATE sheet SET claim_numbers = claim_numbers WHERE id = ?", sheetId);
        jdbc.update("MERGE INTO period_number (sheet_id, seq, given) KEY (sheet_id, seq) "
                + "VALUES (?, ?, COALESCE((SELECT given FROM period_number WHERE sheet_id = ? AND seq = ?), 0) + 1)",
                sheetId, seq, sheetId, seq);
        return jdbc.queryForObject("SELECT given FROM period_number WHERE sheet_id = ? AND seq = ?", Long.class, sheetId, seq);
    }

    /** Whether a steward has archived the sheet: then every change to it is refused. */
    public boolean archived(String sheetId) {
        today(sheetId);
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT archived FROM sheet WHERE id = ?", Boolean.class, sheetId));
    }

    public void archive(String sheetId) {
        today(sheetId);
        jdbc.update("UPDATE sheet SET archived = TRUE, archived_on = today WHERE id = ?", sheetId);
    }

    public void setToday(String sheetId, LocalDate today) {
        jdbc.update("MERGE INTO sheet (id, today) KEY (id) VALUES (?, ?)", sheetId, Date.valueOf(today));
    }
}

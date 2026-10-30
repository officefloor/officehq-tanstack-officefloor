package net.officefloor.hq.app.sheet;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * The sheet store. {@link #today(String)} is THE date for a sheet: every feature that decides
 * anything by date, or fills in a default date, asks it — never {@code LocalDate.now()}.
 */
@Repository
public class Sheets {

    private final JdbcTemplate jdbc;

    public Sheets(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's today; a sheet seen for the first time starts at the real date, kept from then on. */
    public LocalDate today(String sheetId) {
        List<LocalDate> found = jdbc.query("SELECT today FROM sheet WHERE id = ?",
                (rs, i) -> rs.getDate(1).toLocalDate(), sheetId);
        if (!found.isEmpty()) {
            return found.get(0);
        }
        LocalDate now = LocalDate.now();
        try {
            jdbc.update("INSERT INTO sheet (id, today) VALUES (?, ?)", sheetId, Date.valueOf(now));
        } catch (DuplicateKeyException raced) {
            // another request started this sheet first; its date stands
        }
        return jdbc.queryForObject("SELECT today FROM sheet WHERE id = ?",
                (rs, i) -> rs.getDate(1).toLocalDate(), sheetId);
    }

    /**
     * The sheet's next claim number. The counter row stays locked until the transaction ends, so
     * claims opened at the same moment get distinct numbers, and a rolled-back open gives none away.
     */
    public long nextClaimNumber(String sheetId) {
        today(sheetId); // the sheet's row holds the counter
        jdbc.update("UPDATE sheet SET last_claim_number = last_claim_number + 1 WHERE id = ?", sheetId);
        return jdbc.queryForObject("SELECT last_claim_number FROM sheet WHERE id = ?", Long.class, sheetId);
    }

    /** The sheet's next bulk settle batch number, counted like {@link #nextClaimNumber(String)}. */
    public int nextSettleBatch(String sheetId) {
        today(sheetId); // the sheet's row holds the counter
        jdbc.update("UPDATE sheet SET last_settle_batch = last_settle_batch + 1 WHERE id = ?", sheetId);
        return jdbc.queryForObject("SELECT last_settle_batch FROM sheet WHERE id = ?", Integer.class, sheetId);
    }

    /** The number of the sheet's last bulk settle batch; 0 when there has been none. */
    public int lastSettleBatch(String sheetId) {
        today(sheetId);
        return jdbc.queryForObject("SELECT last_settle_batch FROM sheet WHERE id = ?", Integer.class, sheetId);
    }

    public void setToday(String sheetId, LocalDate today) {
        jdbc.update("MERGE INTO sheet (id, today) KEY (id) VALUES (?, ?)", sheetId, Date.valueOf(today));
    }

    /** Whether the sheet is archived: everything on it stays visible and every change to it is refused. */
    public boolean archived(String sheetId) {
        today(sheetId); // the sheet's row holds the flag
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT archived FROM sheet WHERE id = ?", Boolean.class, sheetId));
    }

    public void archive(String sheetId) {
        today(sheetId);
        jdbc.update("UPDATE sheet SET archived = TRUE, archived_on = today WHERE id = ?", sheetId);
    }
}

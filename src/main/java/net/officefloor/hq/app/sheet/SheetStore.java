package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * The per-sheet settings. {@link #today(String)} is the date every date-dependent decision and
 * default in the app uses — never the real clock.
 */
@Repository
public class SheetStore {

    private final JdbcTemplate jdbc;

    public SheetStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's today: as set on the sheet, else the real date. While a read is answered for a screen
     *  with a preview date set (see {@link PreviewFilter}), that date stands in for it. */
    public LocalDate today(String sheetId) {
        LocalDate preview = PreviewFilter.current();
        return preview != null ? preview : realToday(sheetId);
    }

    /** The sheet's own today, whatever the screen's preview date. */
    public LocalDate realToday(String sheetId) {
        List<LocalDate> found = jdbc.query("SELECT today FROM sheet WHERE id = ?",
                (rs, i) -> rs.getObject(1, LocalDate.class), sheetId);
        return found.isEmpty() ? LocalDate.now() : found.get(0);
    }

    public void setToday(String sheetId, LocalDate today) {
        jdbc.update("MERGE INTO sheet (id, today) KEY (id) VALUES (?, ?)", sheetId, today);
    }

    /** Records that the sheet has been loaded: from then on it exists. */
    public void markLoaded(String sheetId) {
        jdbc.update("MERGE INTO sheet_loaded (id) KEY (id) VALUES (?)", sheetId);
    }

    /** Whether the sheet exists: it has been loaded (or changed). */
    public boolean exists(String sheetId) {
        Integer found = jdbc.queryForObject("SELECT (SELECT COUNT(*) FROM sheet_loaded WHERE id = ?)"
                + " + (SELECT COUNT(*) FROM sheet WHERE id = ?)", Integer.class, sheetId, sheetId);
        return found != null && found > 0;
    }

    /** Whether the sheet is archived: visible, but every change to it is refused. */
    public boolean archived(String sheetId) {
        List<Boolean> found = jdbc.query("SELECT archived FROM sheet WHERE id = ?",
                (rs, i) -> rs.getBoolean(1), sheetId);
        return !found.isEmpty() && found.get(0);
    }

    /** Whether the sheet was archived while its today was on or before the date. */
    public boolean archivedBy(String sheetId, LocalDate date) {
        List<Boolean> found = jdbc.query("SELECT archived AND archived_on <= ? FROM sheet WHERE id = ?",
                (rs, i) -> rs.getBoolean(1), date, sheetId);
        return !found.isEmpty() && found.get(0);
    }

    /** Archives the sheet, keeping its today as it reads now, and recording that today as the day it was archived. */
    public void archive(String sheetId) {
        LocalDate today = today(sheetId);
        jdbc.update("MERGE INTO sheet (id, today, archived, archived_on) KEY (id) VALUES (?, ?, TRUE, ?)", sheetId,
                today, today);
    }

    /** Refuses any change to an archived sheet. */
    public void refuseArchived(String sheetId) throws InvalidSheetException {
        if (archived(sheetId)) {
            throw new InvalidSheetException("The sheet is archived");
        }
    }
}

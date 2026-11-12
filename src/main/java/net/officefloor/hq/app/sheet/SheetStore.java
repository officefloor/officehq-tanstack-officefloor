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

    /** The sheet's today: as set on the sheet, else the real date. */
    public LocalDate today(String sheetId) {
        List<LocalDate> found = jdbc.query("SELECT today FROM sheet WHERE id = ?",
                (rs, i) -> rs.getObject(1, LocalDate.class), sheetId);
        return found.isEmpty() ? LocalDate.now() : found.get(0);
    }

    public void setToday(String sheetId, LocalDate today) {
        jdbc.update("MERGE INTO sheet (id, today) KEY (id) VALUES (?, ?)", sheetId, today);
    }

    /** Whether the sheet is archived: visible, but every change to it is refused. */
    public boolean archived(String sheetId) {
        List<Boolean> found = jdbc.query("SELECT archived FROM sheet WHERE id = ?",
                (rs, i) -> rs.getBoolean(1), sheetId);
        return !found.isEmpty() && found.get(0);
    }

    /** Archives the sheet, keeping its today as it reads now. */
    public void archive(String sheetId) {
        jdbc.update("MERGE INTO sheet (id, today, archived) KEY (id) VALUES (?, ?, TRUE)", sheetId, today(sheetId));
    }

    /** Refuses any change to an archived sheet. */
    public void refuseArchived(String sheetId) throws InvalidSheetException {
        if (archived(sheetId)) {
            throw new InvalidSheetException("The sheet is archived");
        }
    }
}

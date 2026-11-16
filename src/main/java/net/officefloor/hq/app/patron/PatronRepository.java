package net.officefloor.hq.app.patron;

import java.util.List;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The stored patrons, kept per sheet. */
@Repository
public class PatronRepository {

    private final JdbcTemplate jdbc;
    private final SheetRepository sheets;

    public PatronRepository(JdbcTemplate jdbc, SheetRepository sheets) {
        this.jdbc = jdbc;
        this.sheets = sheets;
    }

    public List<Patron> list(String sheetId) {
        return jdbc.query("SELECT person_id, patron_id FROM patron WHERE sheet_id = ? ORDER BY person_id, patron_id",
                (rs, n) -> new Patron(rs.getLong("person_id"), rs.getLong("patron_id")), sheetId);
    }

    /** Names the patron for the person; false when they already had them (the primary key refuses a second). */
    public boolean add(String sheetId, long personId, long patronId) {
        try {
            jdbc.update("INSERT INTO patron (sheet_id, person_id, patron_id, added_on) VALUES (?, ?, ?, ?)", sheetId, personId, patronId,
                    java.sql.Date.valueOf(sheets.today(sheetId)));
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /** As they stood at the end of the date: only those started while the sheet's today was on or before it. */
    public List<Patron> listAsOf(String sheetId, java.time.LocalDate date) {
        return jdbc.query("SELECT person_id, patron_id FROM patron WHERE sheet_id = ? AND (added_on IS NULL OR added_on <= ?) ORDER BY person_id, patron_id",
                (rs, n) -> new Patron(rs.getLong("person_id"), rs.getLong("patron_id")), sheetId, java.sql.Date.valueOf(date));
    }
}

package net.officefloor.hq.app.patron;

import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The stored patrons, kept per sheet. */
@Repository
public class PatronRepository {

    private final JdbcTemplate jdbc;

    public PatronRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Patron> list(String sheetId) {
        return jdbc.query("SELECT person_id, patron_id FROM patron WHERE sheet_id = ? ORDER BY person_id, patron_id",
                (rs, n) -> new Patron(rs.getLong("person_id"), rs.getLong("patron_id")), sheetId);
    }

    /** Names the patron for the person; false when they already had them (the primary key refuses a second). */
    public boolean add(String sheetId, long personId, long patronId) {
        try {
            jdbc.update("INSERT INTO patron (sheet_id, person_id, patron_id) VALUES (?, ?, ?)", sheetId, personId, patronId);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }
}

package net.officefloor.hq.app.person;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PatronRepository {

    private final JdbcTemplate jdbc;

    public PatronRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every patron named on the sheet, by person, in the order they were named. */
    public List<Patron> list(String sheetId) {
        return jdbc.query("SELECT person_id, patron_id FROM patron WHERE sheet_id = ? ORDER BY person_id, seq",
                (rs, i) -> new Patron(rs.getLong("person_id"), rs.getLong("patron_id")), sheetId);
    }

    /** Whether the person has already named this patron. */
    public boolean named(String sheetId, long personId, long patronId) {
        Integer known = jdbc.queryForObject(
                "SELECT COUNT(*) FROM patron WHERE sheet_id = ? AND person_id = ? AND patron_id = ?", Integer.class,
                sheetId, personId, patronId);
        return known != null && known > 0;
    }

    public void add(String sheetId, long personId, long patronId) {
        jdbc.update("INSERT INTO patron (sheet_id, person_id, patron_id) VALUES (?, ?, ?)", sheetId, personId,
                patronId);
    }
}

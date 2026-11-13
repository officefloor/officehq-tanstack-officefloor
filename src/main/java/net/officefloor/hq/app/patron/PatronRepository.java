package net.officefloor.hq.app.patron;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PatronRepository {

    private final JdbcTemplate jdbc;

    public PatronRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's people's patrons, by person and then in the order they were named. */
    public List<Patron> findAll(String sheetId) {
        return jdbc.query(
                "SELECT sheet_id, person_id, patron_id FROM person_patron WHERE sheet_id = ? ORDER BY person_id, patron_id",
                (rs, n) -> new Patron(rs.getString("sheet_id"), rs.getLong("person_id"), rs.getLong("patron_id")),
                sheetId);
    }

    /** Whether the person already names this patron. */
    public boolean exists(String sheetId, long personId, long patronId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person_patron WHERE sheet_id = ? AND person_id = ? AND patron_id = ?",
                Integer.class, sheetId, personId, patronId);
        return n != null && n > 0;
    }

    public void save(Patron patron) {
        jdbc.update("INSERT INTO person_patron (sheet_id, person_id, patron_id) VALUES (?, ?, ?)", patron.sheetId(),
                patron.personId(), patron.patronId());
    }
}

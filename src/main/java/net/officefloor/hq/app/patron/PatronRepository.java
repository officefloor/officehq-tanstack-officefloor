package net.officefloor.hq.app.patron;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** People's patrons by sheet. */
@Repository
public class PatronRepository {

    private static final RowMapper<Patron> ROW = (rs, i) -> new Patron(rs.getString("sheet_id"),
            rs.getLong("person_id"), rs.getLong("patron_id"), rs.getString("name"));

    private final JdbcTemplate jdbc;

    public PatronRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every person's patrons on the sheet. */
    public List<Patron> list(String sheetId) {
        return jdbc.query("SELECT pp.sheet_id, pp.person_id, pp.patron_id, p.name FROM person_patron pp "
                + "JOIN person p ON p.id = pp.patron_id WHERE pp.sheet_id = ? ORDER BY pp.person_id, p.name", ROW, sheetId);
    }

    /** Whether the person already has this patron. */
    public boolean exists(String sheetId, long personId, long patronId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person_patron WHERE sheet_id = ? AND person_id = ? AND patron_id = ?",
                Integer.class, sheetId, personId, patronId);
        return n != null && n > 0;
    }

    public void insert(Patron patron) {
        jdbc.update("INSERT INTO person_patron (sheet_id, person_id, patron_id) VALUES (?, ?, ?)",
                patron.sheetId(), patron.personId(), patron.patronId());
    }
}

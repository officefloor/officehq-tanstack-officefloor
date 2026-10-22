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

    /** Every person's patrons on the sheet, each person's in the order they were named. */
    public List<Patron> list(String sheetId) {
        return jdbc.query("SELECT pp.sheet_id, pp.person_id, pp.patron_id, p.name FROM person_patron pp "
                + "JOIN person p ON p.id = pp.patron_id WHERE pp.sheet_id = ? ORDER BY pp.person_id, pp.named", ROW, sheetId);
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

    /** On merging one person into another: the first's patrons are added to the other's (not the other themselves,
     *  no repeats), anyone naming the first as patron names the other instead (no repeats), and the first has none. */
    public void merge(String sheetId, long fromId, long intoId) {
        jdbc.update("INSERT INTO person_patron (sheet_id, person_id, patron_id) SELECT sheet_id, ?, patron_id "
                + "FROM person_patron pp WHERE sheet_id = ? AND person_id = ? AND patron_id <> ? AND NOT EXISTS "
                + "(SELECT 1 FROM person_patron q WHERE q.person_id = ? AND q.patron_id = pp.patron_id)",
                intoId, sheetId, fromId, intoId, intoId);
        jdbc.update("DELETE FROM person_patron WHERE sheet_id = ? AND person_id = ?", sheetId, fromId);
        jdbc.update("UPDATE person_patron pp SET patron_id = ? WHERE sheet_id = ? AND patron_id = ? AND person_id <> ? "
                + "AND NOT EXISTS (SELECT 1 FROM person_patron q WHERE q.person_id = pp.person_id AND q.patron_id = ?)",
                intoId, sheetId, fromId, intoId, intoId);
        jdbc.update("DELETE FROM person_patron WHERE sheet_id = ? AND patron_id = ?", sheetId, fromId);
    }
}

package net.officefloor.hq.app.patron;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The patrons each person on a sheet has named. */
@Repository
public class Patrons {

    private static final RowMapper<Patron> ROW = (rs, i) -> new Patron(rs.getLong("person_id"), rs.getLong("patron_id"),
            rs.getLong("named"));

    private final JdbcTemplate jdbc;

    public Patrons(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every patron named on the sheet, each person's in the order they were named. */
    public List<Patron> list(String sheet) {
        return jdbc.query("SELECT person_id, patron_id, named FROM patron WHERE sheet = ? ORDER BY person_id, named", ROW,
                sheet);
    }

    /** Whether the person has already named this patron. */
    public boolean has(String sheet, long person, long patron) {
        Integer found = jdbc.queryForObject(
                "SELECT COUNT(*) FROM patron WHERE sheet = ? AND person_id = ? AND patron_id = ?", Integer.class, sheet,
                person, patron);
        return found != null && found > 0;
    }

    public void add(String sheet, long person, long patron) {
        jdbc.update("INSERT INTO patron (sheet, person_id, patron_id) VALUES (?, ?, ?)", sheet, person, patron);
    }
}

package net.officefloor.hq.app.patron;

import net.officefloor.hq.app.history.AsOf;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The store of who names whom as their patron, kept per sheet through the people. */
@Repository
public class Patrons {

    private final JdbcTemplate jdbc;

    public Patrons(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every person-to-patron link on the sheet, each person's in the order they named them. */
    public List<PatronResponse> list(String sheetId) {
        return jdbc.query("SELECT pp.person_id, pp.patron_id FROM " + AsOf.table("person_patron", "pp")
                + " JOIN " + AsOf.table("person", "p") + " ON p.id = pp.person_id WHERE p.sheet_id = ? ORDER BY pp.person_id, pp.named, pp.patron_id",
                (rs, n) -> new PatronResponse(rs.getLong("person_id"), rs.getLong("patron_id")), sheetId);
    }

    public boolean has(long personId, long patronId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + AsOf.table("person_patron") + " WHERE person_id = ? AND patron_id = ?",
                Integer.class, personId, patronId);
        return n != null && n > 0;
    }

    public void add(long personId, long patronId) {
        jdbc.update("INSERT INTO person_patron (person_id, patron_id) VALUES (?, ?)", personId, patronId);
    }
}

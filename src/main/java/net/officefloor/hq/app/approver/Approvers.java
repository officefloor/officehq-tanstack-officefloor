package net.officefloor.hq.app.approver;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The store of the people's approvers, kept per sheet through the people. */
@Repository
public class Approvers {

    private static final RowMapper<Approver> ROW = (rs, n) -> new Approver(rs.getLong("person_id"),
            rs.getString("user_name"));

    private final JdbcTemplate jdbc;

    public Approvers(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every approver of the sheet's people. */
    public List<Approver> list(String sheetId) {
        return jdbc.query(
                "SELECT a.* FROM person_approver a JOIN person p ON p.id = a.person_id WHERE p.sheet_id = ?", ROW,
                sheetId);
    }

    /** Whether the user is the person's approver. */
    public boolean isApprover(long personId, String user) {
        List<Approver> found = jdbc.query("SELECT * FROM person_approver WHERE person_id = ?", ROW, personId);
        return !found.isEmpty() && found.get(0).user().equals(user);
    }

    /** Make the user the person's approver, replacing any approver they had. */
    public void set(long personId, String user) {
        jdbc.update("DELETE FROM person_approver WHERE person_id = ?", personId);
        jdbc.update("INSERT INTO person_approver (person_id, user_name) VALUES (?, ?)", personId, user);
    }
}

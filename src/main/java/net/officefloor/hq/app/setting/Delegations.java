package net.officefloor.hq.app.setting;

import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The store of the delegations of the steward role, kept per sheet. */
@Repository
public class Delegations {

    private final JdbcTemplate jdbc;

    public Delegations(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's delegations, in the order they were made. */
    public List<Delegation> all(String sheetId) {
        return jdbc.query("SELECT * FROM delegation WHERE sheet_id = ? ORDER BY id",
                (rs, n) -> new Delegation(rs.getString("sheet_id"), rs.getString("user_name"),
                        rs.getDate("from_date").toLocalDate(), rs.getDate("to_date").toLocalDate(),
                        rs.getString("delegated_by")), sheetId);
    }

    /** The steward who delegated the role to the user for the date (the latest such delegation); null when none did. */
    public String delegator(String sheetId, String user, LocalDate date) {
        List<String> by = jdbc.queryForList("SELECT delegated_by FROM delegation WHERE sheet_id = ? AND user_name = ?"
                + " AND from_date <= ? AND to_date >= ? ORDER BY id DESC LIMIT 1", String.class,
                sheetId, user, java.sql.Date.valueOf(date), java.sql.Date.valueOf(date));
        return by.isEmpty() ? null : by.get(0);
    }

    public void save(Delegation delegation) {
        jdbc.update("INSERT INTO delegation (sheet_id, user_name, from_date, to_date, delegated_by) VALUES (?, ?, ?, ?, ?)",
                delegation.sheetId(), delegation.user(), java.sql.Date.valueOf(delegation.from()),
                java.sql.Date.valueOf(delegation.to()), delegation.by());
    }
}

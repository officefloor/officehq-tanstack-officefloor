package net.officefloor.hq.app.delegation;

import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Delegations of the steward role by sheet. One is in force while the sheet's today is inside its dates. */
@Repository
public class DelegationRepository {

    private static final RowMapper<Delegation> ROW = (rs, i) -> new Delegation(rs.getLong("id"),
            rs.getString("sheet_id"), rs.getString("user_name"), rs.getObject("from_date", LocalDate.class),
            rs.getObject("to_date", LocalDate.class), rs.getString("delegated_by"));

    private final JdbcTemplate jdbc;

    public DelegationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's delegations, in the order they were made. */
    public List<Delegation> list(String sheetId) {
        return jdbc.query("SELECT * FROM sheet_delegation WHERE sheet_id = ? ORDER BY id", ROW, sheetId);
    }

    public Delegation add(String sheetId, DelegationInput input) {
        jdbc.update("INSERT INTO sheet_delegation (sheet_id, user_name, from_date, to_date, delegated_by) "
                + "VALUES (?, ?, ?, ?, ?)", sheetId, input.name(), input.from(), input.to(), input.user());
        Long id = jdbc.queryForObject("SELECT MAX(id) FROM sheet_delegation WHERE sheet_id = ?", Long.class, sheetId);
        return new Delegation(id, sheetId, input.name(), input.from(), input.to(), input.user());
    }

    /** The steward whose delegation to the user is in force by the sheet's today (the latest made, when several
     *  are), or null when none is. */
    public String delegator(String sheetId, String user) {
        List<String> found = jdbc.queryForList("SELECT d.delegated_by FROM sheet_delegation d "
                + "JOIN sheet s ON s.id = d.sheet_id WHERE d.sheet_id = ? AND d.user_name = ? "
                + "AND s.today BETWEEN d.from_date AND d.to_date ORDER BY d.id DESC", String.class, sheetId, user);
        return found.isEmpty() ? null : found.get(0);
    }
}

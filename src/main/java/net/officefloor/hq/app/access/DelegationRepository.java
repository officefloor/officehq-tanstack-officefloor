package net.officefloor.hq.app.access;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The stewards' delegations of the steward role on each sheet. */
@Repository
public class DelegationRepository {

    private static final RowMapper<Delegation> ROW = (rs, n) -> new Delegation(rs.getString("sheet_id"),
            rs.getString("user_name"), rs.getObject("from_date", LocalDate.class),
            rs.getObject("to_date", LocalDate.class), rs.getString("by_user"));

    private final JdbcTemplate jdbc;

    public DelegationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's delegations, in the order they were made. */
    public List<Delegation> list(String sheetId) {
        return jdbc.query("SELECT sheet_id, user_name, from_date, to_date, by_user FROM sheet_delegation"
                + " WHERE sheet_id = ? ORDER BY id", ROW, sheetId);
    }

    public Delegation insert(String sheetId, String user, LocalDate from, LocalDate to, String by) {
        jdbc.update("INSERT INTO sheet_delegation (sheet_id, user_name, from_date, to_date, by_user) VALUES (?, ?, ?, ?, ?)",
                sheetId, user, from, to, by);
        return new Delegation(sheetId, user, from, to, by);
    }

    /**
     * The steward the user acts for on the sheet's today: the latest delegation to the user whose range holds the
     * sheet's today and whose steward is still one. Empty when none does, or the user is a steward in their own right.
     */
    public Optional<String> actingFor(String sheetId, String user) {
        return jdbc.queryForList("SELECT d.by_user FROM sheet_delegation d JOIN sheet s ON s.id = d.sheet_id"
                + " JOIN sheet_role r ON r.sheet_id = d.sheet_id AND r.user_name = d.by_user AND r.role = 'steward'"
                + " WHERE d.sheet_id = ? AND d.user_name = ? AND s.today BETWEEN d.from_date AND d.to_date"
                + " AND NOT EXISTS (SELECT 1 FROM sheet_role o WHERE o.sheet_id = d.sheet_id AND o.user_name = d.user_name"
                + " AND o.role = 'steward') ORDER BY d.id DESC LIMIT 1", String.class, sheetId, user)
                .stream().findFirst();
    }
}

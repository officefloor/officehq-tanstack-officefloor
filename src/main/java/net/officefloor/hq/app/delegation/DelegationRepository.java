package net.officefloor.hq.app.delegation;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The delegations store, kept per sheet in the order they were made. */
@Repository
public class DelegationRepository {

    private static final RowMapper<Delegation> ROW = (rs, n) -> new Delegation(rs.getLong("id"),
            rs.getString("sheet_id"), rs.getString("user_name"), rs.getString("by_user"),
            rs.getDate("from_date").toLocalDate(), rs.getDate("to_date").toLocalDate());

    private final JdbcTemplate jdbc;

    public DelegationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's delegations, in the order they were made. */
    public List<Delegation> findAll(String sheetId) {
        return jdbc.query("SELECT id, sheet_id, user_name, by_user, from_date, to_date FROM sheet_delegation"
                + " WHERE sheet_id = ? ORDER BY id", ROW, sheetId);
    }

    /**
     * The steward who delegated the steward role to the user for the sheet's today (the latest made, when
     * more than one covers it), or null when none does.
     */
    public String delegatorOf(String sheetId, String user) {
        List<String> found = jdbc.queryForList("SELECT d.by_user FROM sheet_delegation d JOIN sheet s ON s.id = d.sheet_id"
                + " WHERE d.sheet_id = ? AND d.user_name = ? AND s.today BETWEEN d.from_date AND d.to_date"
                + " ORDER BY d.id DESC", String.class, sheetId, user);
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * Whether the steward role was delegated to the user for {@code day}, by a delegation made while the
     * sheet's today was on or before it.
     */
    public boolean delegatedOn(String sheetId, String user, LocalDate day) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM sheet_delegation WHERE sheet_id = ? AND user_name = ?"
                + " AND ? BETWEEN from_date AND to_date AND (made_on IS NULL OR made_on <= ?)", Integer.class,
                sheetId, user, Date.valueOf(day), Date.valueOf(day));
        return n != null && n > 0;
    }

    public Delegation save(Delegation delegation) {
        jdbc.update("INSERT INTO sheet_delegation (sheet_id, user_name, by_user, from_date, to_date, made_on)"
                + " SELECT ?, ?, ?, ?, ?, today FROM sheet WHERE id = ?",
                delegation.sheetId(), delegation.user(), delegation.by(),
                Date.valueOf(delegation.from()), Date.valueOf(delegation.to()), delegation.sheetId());
        Long id = jdbc.queryForObject("SELECT MAX(id) FROM sheet_delegation WHERE sheet_id = ?", Long.class,
                delegation.sheetId());
        return new Delegation(id, delegation.sheetId(), delegation.user(), delegation.by(), delegation.from(),
                delegation.to());
    }
}

package net.officefloor.hq.app.setting;

import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class DelegationRepository {

    private static final RowMapper<Delegation> ROW = (rs, n) -> new Delegation(rs.getLong("id"),
            rs.getString("sheet_id"), rs.getString("user_name"),
            rs.getObject("from_date", LocalDate.class).toString(), rs.getObject("to_date", LocalDate.class).toString(),
            rs.getString("delegated_by"));

    private final JdbcTemplate jdbc;

    public DelegationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's delegations, newest first. */
    public List<Delegation> list(String sheetId) {
        return jdbc.query("SELECT id, sheet_id, user_name, from_date, to_date, delegated_by FROM delegation "
                + "WHERE sheet_id = ? ORDER BY id DESC", ROW, sheetId);
    }

    public Delegation add(String sheetId, String user, LocalDate from, LocalDate to, String by) {
        jdbc.update("INSERT INTO delegation (sheet_id, user_name, from_date, to_date, delegated_by, delegated_at) "
                + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)", sheetId, user, from, to, by);
        Long id = jdbc.queryForObject("SELECT MAX(id) FROM delegation WHERE sheet_id = ? AND user_name = ?",
                Long.class, sheetId, user);
        return new Delegation(id, sheetId, user, from.toString(), to.toString(), by);
    }
}

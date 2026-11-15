package net.officefloor.hq.app.voidreason;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class VoidReasonRepository {

    private static final RowMapper<VoidReason> ROW = (rs, n) -> new VoidReason(rs.getString("sheet_id"),
            rs.getString("reason"), rs.getString("added_by"));

    private final JdbcTemplate jdbc;

    public VoidReasonRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's void reasons, in the order they were added. */
    public List<VoidReason> list(String sheetId) {
        return jdbc.query("SELECT sheet_id, reason, added_by FROM void_reason WHERE sheet_id = ? ORDER BY id",
                ROW, sheetId);
    }

    public boolean any(String sheetId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM void_reason WHERE sheet_id = ?", Integer.class, sheetId);
        return n != null && n > 0;
    }

    public boolean contains(String sheetId, String reason) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM void_reason WHERE sheet_id = ? AND reason = ?",
                Integer.class, sheetId, reason);
        return n != null && n > 0;
    }

    public VoidReason add(String sheetId, String reason, String by) {
        jdbc.update("INSERT INTO void_reason (sheet_id, reason, added_by, added_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                sheetId, reason, by);
        return new VoidReason(sheetId, reason, by);
    }
}

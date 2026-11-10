package net.officefloor.hq.app.voidreason;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The void reasons store, kept per sheet. */
@Repository
public class VoidReasonRepository {

    private final JdbcTemplate jdbc;

    public VoidReasonRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's void reasons, in the order they were added. */
    public List<VoidReason> findAll(String sheetId) {
        return jdbc.query("SELECT sheet_id, reason FROM sheet_void_reason WHERE sheet_id = ? ORDER BY id",
                (rs, n) -> new VoidReason(rs.getString("sheet_id"), rs.getString("reason")), sheetId);
    }

    public boolean exists(String sheetId, String reason) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM sheet_void_reason WHERE sheet_id = ? AND reason = ?",
                Integer.class, sheetId, reason);
        return n != null && n > 0;
    }

    public void save(VoidReason reason) {
        jdbc.update("INSERT INTO sheet_void_reason (sheet_id, reason) VALUES (?, ?)", reason.sheetId(), reason.reason());
    }
}

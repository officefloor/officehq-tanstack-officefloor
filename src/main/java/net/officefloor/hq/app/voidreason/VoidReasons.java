package net.officefloor.hq.app.voidreason;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The void reasons of each sheet, in the order added: a reason is on a sheet's list at most once. */
@Repository
public class VoidReasons {

    private final JdbcTemplate jdbc;

    public VoidReasons(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every void reason on the sheet, in the order added. */
    public List<String> onSheet(String sheetId) {
        return jdbc.query("SELECT reason FROM sheet_void_reason WHERE sheet_id = ? ORDER BY position",
                (rs, i) -> rs.getString(1), sheetId);
    }

    /** Whether `reason` is on the sheet's list. */
    public boolean has(String sheetId, String reason) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM sheet_void_reason WHERE sheet_id = ? AND reason = ?",
                Integer.class, sheetId, reason);
        return n != null && n > 0;
    }

    public void add(String sheetId, String reason) {
        jdbc.update("INSERT INTO sheet_void_reason (sheet_id, reason, position) VALUES (?, ?, "
                + "(SELECT COALESCE(MAX(position), 0) + 1 FROM sheet_void_reason WHERE sheet_id = ?))",
                sheetId, reason, sheetId);
    }
}

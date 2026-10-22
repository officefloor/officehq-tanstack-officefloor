package net.officefloor.hq.app.voidreason;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The void reasons by sheet, and the reason each claim was voided with. */
@Repository
public class VoidReasonRepository {

    private static final RowMapper<VoidReason> ROW = (rs, i) -> new VoidReason(rs.getLong("id"),
            rs.getString("sheet_id"), rs.getString("reason"), rs.getString("added_by"));

    private final JdbcTemplate jdbc;

    public VoidReasonRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's void reasons, in the order they were added. */
    public List<VoidReason> list(String sheetId) {
        return jdbc.query("SELECT * FROM sheet_void_reason WHERE sheet_id = ? ORDER BY id", ROW, sheetId);
    }

    public boolean exists(String sheetId, String reason) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM sheet_void_reason WHERE sheet_id = ? AND reason = ?",
                Integer.class, sheetId, reason);
        return n != null && n > 0;
    }

    /** Whether the sheet's list has any reason on it. */
    public boolean any(String sheetId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM sheet_void_reason WHERE sheet_id = ?", Integer.class,
                sheetId);
        return n != null && n > 0;
    }

    public VoidReason add(String sheetId, VoidReasonInput input) {
        jdbc.update("INSERT INTO sheet_void_reason (sheet_id, reason, added_by) VALUES (?, ?, ?)", sheetId,
                input.reason(), input.user());
        Long id = jdbc.queryForObject("SELECT id FROM sheet_void_reason WHERE sheet_id = ? AND reason = ?",
                Long.class, sheetId, input.reason());
        return new VoidReason(id, sheetId, input.reason(), input.user());
    }

    /** Record the reason the claim was voided with. */
    public void voided(String sheetId, long claimId, String reason) {
        jdbc.update("DELETE FROM claim_void_reason WHERE sheet_id = ? AND claim_id = ?", sheetId, claimId);
        jdbc.update("INSERT INTO claim_void_reason (sheet_id, claim_id, reason) VALUES (?, ?, ?)", sheetId, claimId,
                reason);
    }

    /** Per reason, how many of the sheet's claims that are VOID now were voided with it. */
    public Map<String, Integer> voided(String sheetId) {
        Map<String, Integer> counts = new HashMap<>();
        jdbc.query("SELECT v.reason, COUNT(*) AS n FROM claim_void_reason v JOIN claim c ON c.sheet_id = v.sheet_id "
                + "AND c.id = v.claim_id WHERE v.sheet_id = ? AND c.status = 'VOID' GROUP BY v.reason",
                rs -> {
                    counts.put(rs.getString("reason"), rs.getInt("n"));
                }, sheetId);
        return counts;
    }
}

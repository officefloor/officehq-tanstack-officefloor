package net.officefloor.hq.app.voidreason;

import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The stored void reasons, kept per sheet, and the reason each voided claim was given. */
@Repository
public class VoidReasonRepository {

    private final JdbcTemplate jdbc;

    public VoidReasonRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's void reasons in the order added, each with the count of its VOID claims voided with it. */
    public List<VoidReason> list(String sheetId) {
        return jdbc.query("SELECT r.reason, (SELECT COUNT(*) FROM claim_void_reason v JOIN claim c ON c.id = v.claim_id"
                + " WHERE v.sheet_id = r.sheet_id AND v.reason = r.reason AND c.status = 'VOID') AS voided"
                + " FROM void_reason r WHERE r.sheet_id = ? ORDER BY r.id",
                (rs, n) -> new VoidReason(rs.getString("reason"), rs.getLong("voided")), sheetId);
    }

    /** The reason each claim of the sheet was voided with, by claim id (a claim voided more than once: its last). */
    public java.util.Map<Long, String> reasons(String sheetId) {
        java.util.Map<Long, String> out = new java.util.HashMap<>();
        jdbc.query("SELECT claim_id, reason FROM claim_void_reason WHERE sheet_id = ?",
                (rs) -> { out.put(rs.getLong("claim_id"), rs.getString("reason")); }, sheetId);
        return out;
    }

    /** Whether the sheet has any void reason (then voiding a claim needs one). */
    public boolean any(String sheetId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM void_reason WHERE sheet_id = ?", Integer.class, sheetId);
        return n != null && n > 0;
    }

    /** Whether the reason is on the sheet's list. */
    public boolean has(String sheetId, String reason) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM void_reason WHERE sheet_id = ? AND reason = ?",
                Integer.class, sheetId, reason);
        return n != null && n > 0;
    }

    /** Adds the reason; false when it is already on the list (the unique key refuses a second). */
    public boolean add(String sheetId, String reason) {
        try {
            jdbc.update("INSERT INTO void_reason (sheet_id, reason) VALUES (?, ?)", sheetId, reason);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /** Keeps the reason the claim was voided with. */
    public void voided(String sheetId, long claimId, String reason) {
        jdbc.update("INSERT INTO claim_void_reason (claim_id, sheet_id, reason) VALUES (?, ?, ?)", claimId, sheetId,
                reason);
    }
}

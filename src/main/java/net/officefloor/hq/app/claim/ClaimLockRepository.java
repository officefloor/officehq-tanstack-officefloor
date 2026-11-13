package net.officefloor.hq.app.claim;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The claims whose editor a user has open: each is locked for every other user. */
@Repository
public class ClaimLockRepository {

    private final JdbcTemplate jdbc;

    public ClaimLockRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The user holding the claim's lock, or null when it is not locked. */
    public String lockedBy(String sheetId, long claimId) {
        List<String> found = jdbc.queryForList("SELECT locked_by FROM claim_lock WHERE sheet_id = ? AND claim_id = ?",
                String.class, sheetId, claimId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The holder of each locked claim on the sheet, by claim id. */
    public Map<Long, String> findAll(String sheetId) {
        Map<Long, String> locks = new HashMap<>();
        jdbc.query("SELECT claim_id, locked_by FROM claim_lock WHERE sheet_id = ?",
                (rs) -> {
                    locks.put(rs.getLong("claim_id"), rs.getString("locked_by"));
                }, sheetId);
        return locks;
    }

    /** Lock the claim for the user (it is not locked by anyone else). */
    public void lock(String sheetId, long claimId, String user) {
        jdbc.update("DELETE FROM claim_lock WHERE sheet_id = ? AND claim_id = ?", sheetId, claimId);
        jdbc.update("INSERT INTO claim_lock (sheet_id, claim_id, locked_by) VALUES (?, ?, ?)", sheetId, claimId, user);
    }

    /** Release the claim's lock when the user holds it. */
    public void unlock(String sheetId, long claimId, String user) {
        jdbc.update("DELETE FROM claim_lock WHERE sheet_id = ? AND claim_id = ? AND locked_by = ?", sheetId, claimId,
                user);
    }

    /** Release every lock on the sheet. */
    public void unlockAll(String sheetId) {
        jdbc.update("DELETE FROM claim_lock WHERE sheet_id = ?", sheetId);
    }
}

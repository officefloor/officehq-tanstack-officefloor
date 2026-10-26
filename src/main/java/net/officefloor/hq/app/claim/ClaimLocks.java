package net.officefloor.hq.app.claim;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The claims whose editor a user has open, kept per sheet. A user holds at most one lock on a sheet. */
@Repository
public class ClaimLocks {

    private final JdbcTemplate jdbc;

    public ClaimLocks(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The user holding the claim's lock; null when it is not locked. */
    public String lockedBy(Claim claim) {
        List<String> by = jdbc.query("SELECT locked_by FROM claim_lock WHERE claim_id = ?",
                (rs, n) -> rs.getString("locked_by"), claim.id());
        return by.isEmpty() ? null : by.get(0);
    }

    /** The sheet's locks: the user holding each, by claim id. */
    public Map<Long, String> list(String sheetId) {
        Map<Long, String> locks = new HashMap<>();
        jdbc.query("SELECT * FROM claim_lock WHERE sheet_id = ?",
                rs -> {
                    locks.put(rs.getLong("claim_id"), rs.getString("locked_by"));
                }, sheetId);
        return locks;
    }

    /**
     * Locks the claim for {@code user}, releasing any other lock they hold on the sheet, and returns true; false when
     * another user holds it.
     */
    public boolean lock(Claim claim, String user) {
        jdbc.update("DELETE FROM claim_lock WHERE sheet_id = ? AND locked_by = ? AND claim_id <> ?", claim.sheetId(),
                user, claim.id());
        try {
            jdbc.update("INSERT INTO claim_lock (claim_id, sheet_id, locked_by) VALUES (?, ?, ?)", claim.id(),
                    claim.sheetId(), user);
            return true;
        } catch (DuplicateKeyException e) {
            return user.equals(lockedBy(claim));
        }
    }

    /** Releases the claim's lock if {@code user} holds it. */
    public void release(Claim claim, String user) {
        jdbc.update("DELETE FROM claim_lock WHERE claim_id = ? AND locked_by = ?", claim.id(), user);
    }

    /** Releases every lock on the sheet. */
    public void releaseAll(String sheetId) {
        jdbc.update("DELETE FROM claim_lock WHERE sheet_id = ?", sheetId);
    }
}

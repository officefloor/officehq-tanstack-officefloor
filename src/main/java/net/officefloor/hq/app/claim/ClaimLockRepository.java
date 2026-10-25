package net.officefloor.hq.app.claim;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Which user's open editor holds each claim: at most one per claim, so two users never edit it at once. */
@Repository
public class ClaimLockRepository {

    private final JdbcTemplate jdbc;

    public ClaimLockRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The user holding the claim's lock; null when no one does. */
    public String holder(long claimId) {
        return jdbc.query("SELECT locked_by FROM claim_lock WHERE claim_id = ?",
                rs -> rs.next() ? rs.getString(1) : null, claimId);
    }

    /** Take the claim's lock for {@code user}, in one statement so two users never both take it. The holder after: the
     *  user when they took it (or already held it), else whoever holds it. */
    public String lock(Claim claim, String user) {
        try {
            jdbc.update("INSERT INTO claim_lock (claim_id, sheet_id, locked_by) VALUES (?, ?, ?)",
                    claim.getId(), claim.getSheetId(), user);
            return user;
        } catch (DuplicateKeyException held) {
            return holder(claim.getId());
        }
    }

    /** Drop the claim's lock when {@code user} holds it. */
    public void release(long claimId, String user) {
        jdbc.update("DELETE FROM claim_lock WHERE claim_id = ? AND locked_by = ?", claimId, user);
    }

    /** Drop every lock on the sheet. */
    public void releaseAll(String sheetId) {
        jdbc.update("DELETE FROM claim_lock WHERE sheet_id = ?", sheetId);
    }
}

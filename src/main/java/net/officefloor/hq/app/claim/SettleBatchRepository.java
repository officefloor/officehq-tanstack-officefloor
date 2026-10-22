package net.officefloor.hq.app.claim;

import java.util.HashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The sheet's settle batches: each bulk settle that went through (one left PENDING counts), numbered
 *  1, 2, ... on the sheet in the order they happened, and the batch that settled each claim (none for a claim settled
 *  one at a time). */
@Repository
public class SettleBatchRepository {

    private final JdbcTemplate jdbc;

    public SettleBatchRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The number of the sheet's last batch; null when there has been none. */
    public Integer last(String sheetId) {
        return jdbc.query("SELECT MAX(batch) FROM settle_batch WHERE sheet_id = ?",
                rs -> rs.next() ? (Integer) rs.getObject(1, Integer.class) : null, sheetId);
    }

    /** Start the sheet's next batch: its number. */
    public int start(String sheetId) {
        Integer last = last(sheetId);
        int next = last != null ? last + 1 : 1;
        jdbc.update("INSERT INTO settle_batch (sheet_id, batch) VALUES (?, ?)", sheetId, next);
        return next;
    }

    /** Record the claim as settled by the batch. */
    public void settledBy(String sheetId, long claimId, int batch) {
        jdbc.update("MERGE INTO claim_batch KEY (sheet_id, claim_id) VALUES (?, ?, ?)", sheetId, claimId, batch);
    }

    /** Record the claim as settled one at a time: by no batch. */
    public void settledAlone(String sheetId, long claimId) {
        jdbc.update("DELETE FROM claim_batch WHERE sheet_id = ? AND claim_id = ?", sheetId, claimId);
    }

    /** The batch that settled each claim on the sheet a bulk settle settled, by claim id. */
    public Map<Long, Integer> byClaim(String sheetId) {
        Map<Long, Integer> batches = new HashMap<>();
        jdbc.query("SELECT claim_id, batch FROM claim_batch WHERE sheet_id = ?",
                rs -> {
                    batches.put(rs.getLong(1), rs.getInt(2));
                }, sheetId);
        return batches;
    }
}

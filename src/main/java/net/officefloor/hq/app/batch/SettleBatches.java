package net.officefloor.hq.app.batch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.history.AsOf;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * The store of settle batches, kept per sheet: a bulk settle that settled at least one claim (one going to PENDING
 * counts) is a batch, numbered 1, 2, ... on the sheet in the order they happen. Each claim it settled carries its
 * number until the claim is settled again one at a time.
 */
@Repository
public class SettleBatches {

    private final JdbcTemplate jdbc;

    public SettleBatches(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The number of the sheet's last batch; null when it has had none. */
    public Integer last(String sheetId) {
        return jdbc.queryForObject("SELECT MAX(batch_number) FROM " + AsOf.table("settle_batch") + " WHERE sheet_id = ?",
                Integer.class, sheetId);
    }

    /** The batch number of each of the sheet's claims settled by a batch, by claim id. */
    public Map<Long, Integer> byClaim(String sheetId) {
        Map<Long, Integer> batches = new HashMap<>();
        jdbc.query("SELECT claim_id, batch_number FROM " + AsOf.table("claim_batch") + " WHERE sheet_id = ?",
                rs -> {
                    batches.put(rs.getLong("claim_id"), rs.getInt("batch_number"));
                }, sheetId);
        return batches;
    }

    /** Makes the sheet's next batch, by {@code user}, of the claims (ids on the sheet); returns its number. */
    public int add(String sheetId, String user, List<Long> claimIds) {
        Integer last = jdbc.queryForObject("SELECT MAX(batch_number) FROM settle_batch WHERE sheet_id = ?", Integer.class,
                sheetId);
        int number = last == null ? 1 : last + 1;
        jdbc.update("INSERT INTO settle_batch (sheet_id, batch_number, by_user) VALUES (?, ?, ?)", sheetId, number, user);
        for (Long id : claimIds) {
            jdbc.update("DELETE FROM claim_batch WHERE claim_id = ?", id);
            jdbc.update("INSERT INTO claim_batch (claim_id, sheet_id, batch_number) VALUES (?, ?, ?)", id, sheetId, number);
        }
        return number;
    }

    /** Takes the claim out of its batch: it was settled again one at a time. */
    public void remove(long claimId) {
        jdbc.update("DELETE FROM claim_batch WHERE claim_id = ?", claimId);
    }
}

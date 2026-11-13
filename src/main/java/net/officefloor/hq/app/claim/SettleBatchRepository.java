package net.officefloor.hq.app.claim;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The sheet's settle batches: the bulk settles that settled at least one claim, numbered 1, 2, ... */
@Repository
public class SettleBatchRepository {

    private final JdbcTemplate jdbc;

    public SettleBatchRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The batch number of the bulk settle with the token, taking the sheet's next one the first time it is asked. */
    public int numberFor(String sheetId, String token) {
        jdbc.queryForList("SELECT id FROM sheet WHERE id = ? FOR UPDATE", String.class, sheetId);
        List<Integer> found = jdbc.queryForList("SELECT number FROM settle_batch WHERE sheet_id = ? AND token = ?",
                Integer.class, sheetId, token);
        if (!found.isEmpty()) {
            return found.get(0);
        }
        int next = jdbc.queryForObject("SELECT last_batch FROM FINAL TABLE "
                + "(UPDATE sheet SET last_batch = last_batch + 1 WHERE id = ?)", Integer.class, sheetId);
        jdbc.update("INSERT INTO settle_batch (sheet_id, token, number) VALUES (?, ?, ?)", sheetId, token, next);
        return next;
    }

    /** The sheet's last batch number, or null when it has had none. */
    public Integer last(String sheetId) {
        List<Integer> found = jdbc.queryForList("SELECT last_batch FROM sheet WHERE id = ?", Integer.class, sheetId);
        return found.isEmpty() || found.get(0) == 0 ? null : found.get(0);
    }

    /** Store the number of the batch that settled the claim (null: settled one at a time, or no longer settled). */
    public void saveBatch(String sheetId, long claimId, Integer batch) {
        jdbc.update("UPDATE claim SET batch = ? WHERE id = ? AND sheet_id = ?", batch, claimId, sheetId);
    }

    /** The number of the batch that settled the claim, or null. */
    public Integer batch(String sheetId, long claimId) {
        List<Integer> found = jdbc.queryForList("SELECT batch FROM claim WHERE sheet_id = ? AND id = ?",
                Integer.class, sheetId, claimId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The number of the batch that settled each of the sheet's claims settled in one, by claim id. */
    public Map<Long, Integer> batches(String sheetId) {
        Map<Long, Integer> batches = new HashMap<>();
        jdbc.query("SELECT id, batch FROM claim WHERE sheet_id = ? AND batch IS NOT NULL",
                (ResultSet rs) -> {
                    batches.put(rs.getLong("id"), rs.getInt("batch"));
                }, sheetId);
        return batches;
    }
}

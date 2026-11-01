package net.officefloor.hq.app.claim;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The bulk settles that settled at least one claim on a sheet: batches numbered 1, 2, ... in the order they happen. */
@Repository
public class SettleBatches {

    private final JdbcTemplate jdbc;

    public SettleBatches(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The number of the sheet's last batch; null when there has been none. */
    public Integer last(String sheet) {
        return jdbc.queryForObject("SELECT MAX(batch) FROM settle_batch WHERE sheet = ?", Integer.class, sheet);
    }

    /** Starts the sheet's next batch, by the user, and gives its number. */
    public int next(String sheet, String user) {
        Integer last = last(sheet);
        int batch = last == null ? 1 : last + 1;
        jdbc.update("INSERT INTO settle_batch (sheet, batch, settled_by) VALUES (?, ?, ?)", sheet, batch, user);
        return batch;
    }
}

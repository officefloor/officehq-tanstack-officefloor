package net.officefloor.hq.app.voidreason;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.history.History;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The void reasons per sheet. While a sheet has any, a claim is voided with one of them. */
@Repository
public class VoidReasonStore {

    private final JdbcTemplate jdbc;

    public VoidReasonStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's void reasons, in the order they were added. */
    public List<String> list(String sheetId) {
        return jdbc.queryForList("SELECT reason FROM void_reason WHERE sheet_id = ? ORDER BY id", String.class, sheetId);
    }

    /** The sheet's void reasons at the end of the date (see {@link History}), in the order they were added. */
    public List<String> listAsOf(String sheetId, LocalDate date) {
        return jdbc.queryForList("SELECT reason FROM " + History.asOf("void_reason", "id", date)
                + " WHERE sheet_id = ? ORDER BY id", String.class, sheetId);
    }

    public boolean has(String sheetId, String reason) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM void_reason WHERE sheet_id = ? AND reason = ?",
                Integer.class, sheetId, reason);
        return n != null && n > 0;
    }

    public void add(String sheetId, String reason) {
        jdbc.update("INSERT INTO void_reason (sheet_id, reason) VALUES (?, ?)", sheetId, reason);
    }
}

package net.officefloor.hq.app.voidreason;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The void reasons of each sheet, in the order added, each at most once on its sheet. */
@Repository
public class VoidReasonRepository {

    static final String TAKEN = "That void reason is already on this sheet's list";

    private final JdbcTemplate jdbc;

    public VoidReasonRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's void reasons, in the order added. */
    public List<String> list(String sheetId) {
        return jdbc.query("SELECT reason FROM void_reason WHERE sheet_id = ? ORDER BY id",
                (rs, n) -> rs.getString("reason"), sheetId);
    }

    /** Whether the reason is on the sheet's list. */
    public boolean taken(String sheetId, String reason) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM void_reason WHERE sheet_id = ? AND reason = ?",
                Integer.class, sheetId, reason);
        return n != null && n > 0;
    }

    public void insert(String sheetId, String reason) {
        jdbc.update("INSERT INTO void_reason (sheet_id, reason) VALUES (?, ?)", sheetId, reason);
    }
}

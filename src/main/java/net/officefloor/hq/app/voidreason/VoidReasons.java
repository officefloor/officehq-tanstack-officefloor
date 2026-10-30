package net.officefloor.hq.app.voidreason;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The void reason store: the reasons on each sheet's list, in the order added. */
@Repository
public class VoidReasons {

    private final JdbcTemplate jdbc;

    public VoidReasons(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's void reasons, in the order added. */
    public List<String> list(String sheetId) {
        return jdbc.queryForList("SELECT reason FROM void_reason WHERE sheet_id = ? ORDER BY seq", String.class, sheetId);
    }

    /** Whether the reason is on the sheet's list. */
    public boolean contains(String sheetId, String reason) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM void_reason WHERE sheet_id = ? AND reason = ?",
                Integer.class, sheetId, reason);
        return n != null && n > 0;
    }

    /** Adds the reason to the sheet's list. */
    public void add(String sheetId, String reason) {
        jdbc.update("INSERT INTO void_reason (sheet_id, reason) VALUES (?, ?)", sheetId, reason);
    }
}

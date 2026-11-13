package net.officefloor.hq.app.voidreason;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class VoidReasonRepository {

    private final JdbcTemplate jdbc;

    public VoidReasonRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's void reasons, in the order they were added. */
    public List<String> findAll(String sheetId) {
        return jdbc.queryForList("SELECT reason FROM void_reason WHERE sheet_id = ? ORDER BY position", String.class,
                sheetId);
    }

    public void add(String sheetId, String reason) {
        jdbc.update("INSERT INTO void_reason (sheet_id, reason, position) VALUES (?, ?, "
                + "(SELECT COALESCE(MAX(position), 0) + 1 FROM void_reason WHERE sheet_id = ?))", sheetId, reason,
                sheetId);
    }
}

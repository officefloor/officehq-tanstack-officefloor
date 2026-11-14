package net.officefloor.hq.app.columns;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ColumnsRepository {

    private final JdbcTemplate jdbc;

    public ColumnsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The columns the user switched off on the sheet; none unless chosen. */
    public List<String> hidden(String sheetId, String user) {
        return jdbc.queryForList(
                "SELECT column_name FROM user_column WHERE sheet_id = ? AND user_name = ? ORDER BY column_name",
                String.class, sheetId, user);
    }

    public void show(String sheetId, String user, String column, boolean shown) {
        if (shown) {
            jdbc.update("DELETE FROM user_column WHERE sheet_id = ? AND user_name = ? AND column_name = ?", sheetId,
                    user, column);
        } else {
            jdbc.update(
                    "MERGE INTO user_column (sheet_id, user_name, column_name) KEY (sheet_id, user_name, column_name) VALUES (?, ?, ?)",
                    sheetId, user, column);
        }
    }
}

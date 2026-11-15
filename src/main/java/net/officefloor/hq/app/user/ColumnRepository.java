package net.officefloor.hq.app.user;

import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The columns of the people list each user has switched off on a sheet: every other one is shown. */
@Repository
public class ColumnRepository {

    /** The columns a user may switch off. */
    public static final Set<String> COLUMNS = Set.of("claimed", "due-total", "excess", "statement-count",
            "stated-total", "statement-date", "overdue-count", "overdue-total", "patrons", "backing", "hold",
            "retired", "terms", "contact", "aliases");

    private final JdbcTemplate jdbc;

    public ColumnRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The columns the user has switched off on the sheet, by name. */
    public List<String> hiddenBy(String sheetId, String user) {
        return jdbc.queryForList(
                "SELECT column_name FROM user_hidden_column WHERE sheet_id = ? AND user_name = ? ORDER BY column_name",
                String.class, sheetId, user);
    }

    public void set(String sheetId, String user, String column, boolean shown) {
        if (shown) {
            jdbc.update("DELETE FROM user_hidden_column WHERE sheet_id = ? AND user_name = ? AND column_name = ?",
                    sheetId, user, column);
        } else {
            jdbc.update("MERGE INTO user_hidden_column (sheet_id, user_name, column_name) KEY (sheet_id, user_name, column_name) VALUES (?, ?, ?)",
                    sheetId, user, column);
        }
    }
}

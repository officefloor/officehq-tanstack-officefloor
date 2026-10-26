package net.officefloor.hq.app.session;

import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The people-list columns each user has switched off, per sheet: every column shown unless switched off. */
@Repository
public class ColumnsRepository {

    /** The columns of the people list a user may switch off. */
    public static final Set<String> COLUMNS = Set.of("claimed", "due-total", "excess", "statement-count", "stated-total",
            "statement-date", "overdue-count", "overdue-total", "patrons", "backing", "hold", "retired", "terms",
            "contact", "aliases");

    private final JdbcTemplate jdbc;

    public ColumnsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<String> hidden(String sheetId, String user) {
        return jdbc.queryForList("SELECT column_name FROM user_hidden_column WHERE sheet_id = ? AND user_name = ?"
                + " ORDER BY column_name", String.class, sheetId, user);
    }

    public void setShown(String sheetId, String user, String column, boolean shown) {
        if (shown) {
            jdbc.update("DELETE FROM user_hidden_column WHERE sheet_id = ? AND user_name = ? AND column_name = ?",
                    sheetId, user, column);
        } else {
            jdbc.update("MERGE INTO user_hidden_column (sheet_id, user_name, column_name) KEY (sheet_id, user_name,"
                    + " column_name) VALUES (?, ?, ?)", sheetId, user, column);
        }
    }
}

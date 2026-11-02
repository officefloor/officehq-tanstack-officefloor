package net.officefloor.hq.app.columns;

import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The people-list columns each user switched off per sheet; every column is shown unless switched off. It only
 *  changes which cells that user's screen shows; totals, exports and audit lines are the same whatever it is. */
@Repository
public class Columns {

    public static final Set<String> ALL = Set.of("claimed", "due-total", "excess", "statement-count", "stated-total",
            "statement-date", "overdue-count", "overdue-total", "patrons", "backing", "hold", "retired", "terms",
            "contact", "aliases");

    private final JdbcTemplate jdbc;

    public Columns(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The columns the user switched off on the sheet, in name order. */
    public List<String> hidden(String sheet, String user) {
        return jdbc.queryForList(
                "SELECT column_name FROM user_hidden_column WHERE sheet = ? AND user_name = ? ORDER BY column_name",
                String.class, sheet, user);
    }

    public void set(String sheet, String user, String column, boolean shown) {
        if (shown) {
            jdbc.update("DELETE FROM user_hidden_column WHERE sheet = ? AND user_name = ? AND column_name = ?", sheet,
                    user, column);
        } else {
            jdbc.update("MERGE INTO user_hidden_column (sheet, user_name, column_name) KEY (sheet, user_name, column_name)"
                    + " VALUES (?, ?, ?)", sheet, user, column);
        }
    }
}

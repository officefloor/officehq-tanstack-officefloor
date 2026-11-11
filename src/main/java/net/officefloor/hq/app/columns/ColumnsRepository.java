package net.officefloor.hq.app.columns;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The people-list columns each user has switched off, kept per user (not per sheet). */
@Repository
public class ColumnsRepository {

    private final JdbcTemplate jdbc;

    public ColumnsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The columns {@code user} has switched off, in key order; none for a user who never chose. */
    public List<String> hiddenOf(String user) {
        return jdbc.queryForList(
                "SELECT column_key FROM user_column_hidden WHERE user_name = ? ORDER BY column_key", String.class, user);
    }

    /** {@code user} switches {@code column} on ({@code shown}) or off. */
    public void save(String user, String column, boolean shown) {
        if (shown) {
            jdbc.update("DELETE FROM user_column_hidden WHERE user_name = ? AND column_key = ?", user, column);
        } else {
            jdbc.update("MERGE INTO user_column_hidden (user_name, column_key) KEY (user_name, column_key) VALUES (?, ?)",
                    user, column);
        }
    }
}

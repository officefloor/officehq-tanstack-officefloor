package net.officefloor.hq.app.columns;

import java.util.Arrays;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ColumnsRepository {

    /** The people-list columns a user may switch off. */
    public static final List<String> COLUMNS = List.of("claimed", "due-total", "excess", "statement-count",
            "stated-total", "statement-date", "overdue-count", "overdue-total", "patrons", "backing", "hold",
            "retired", "terms", "contact", "aliases");

    private final JdbcTemplate jdbc;

    public ColumnsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The columns the user switched off; none when they never chose. */
    public List<String> hidden(String user) {
        List<String> found = jdbc.queryForList("SELECT hidden FROM user_columns WHERE user_name = ?", String.class, user);
        if (found.isEmpty() || found.get(0).isEmpty()) {
            return List.of();
        }
        return Arrays.asList(found.get(0).split(","));
    }

    /** Keep the columns the user switched off. */
    public void setHidden(String user, List<String> hidden) {
        jdbc.update("MERGE INTO user_columns (user_name, hidden) KEY (user_name) VALUES (?, ?)", user,
                String.join(",", hidden));
    }
}

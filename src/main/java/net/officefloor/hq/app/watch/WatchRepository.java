package net.officefloor.hq.app.watch;

import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The stored watches, kept per sheet, in the order they were started. */
@Repository
public class WatchRepository {

    private final JdbcTemplate jdbc;

    public WatchRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Watch> list(String sheetId) {
        return jdbc.query("SELECT person_id, user_name FROM watch WHERE sheet_id = ? ORDER BY id",
                (rs, n) -> new Watch(rs.getLong("person_id"), rs.getString("user_name")), sheetId);
    }

    /** The users watching the person, in the order they started watching. */
    public List<String> watchers(String sheetId, long personId) {
        return jdbc.queryForList("SELECT user_name FROM watch WHERE sheet_id = ? AND person_id = ? ORDER BY id",
                String.class, sheetId, personId);
    }

    /** Starts the user watching the person; false when they already did (the unique key refuses a second). */
    public boolean add(String sheetId, long personId, String user) {
        try {
            jdbc.update("INSERT INTO watch (sheet_id, person_id, user_name) VALUES (?, ?, ?)", sheetId, personId, user);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /** Stops the user watching the person; false when they did not. */
    public boolean remove(String sheetId, long personId, String user) {
        return jdbc.update("DELETE FROM watch WHERE sheet_id = ? AND person_id = ? AND user_name = ?", sheetId, personId,
                user) > 0;
    }
}

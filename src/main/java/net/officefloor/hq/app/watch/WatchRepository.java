package net.officefloor.hq.app.watch;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Who watches whom, kept per sheet: each user's watched people, each person's watchers. */
@Repository
public class WatchRepository {

    private final JdbcTemplate jdbc;

    public WatchRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Whether {@code user} watches the person. */
    public boolean watches(String sheetId, long personId, String user) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person_watch WHERE sheet_id = ? AND person_id = ? AND user_name = ?",
                Integer.class, sheetId, personId, user);
        return n != null && n > 0;
    }

    /** {@code user} starts watching the person. */
    public void watch(String sheetId, long personId, String user) {
        jdbc.update("INSERT INTO person_watch (sheet_id, person_id, user_name) VALUES (?, ?, ?)",
                sheetId, personId, user);
    }

    /** {@code user} stops watching the person. */
    public void unwatch(String sheetId, long personId, String user) {
        jdbc.update("DELETE FROM person_watch WHERE sheet_id = ? AND person_id = ? AND user_name = ?",
                sheetId, personId, user);
    }

    /** The ids of the people {@code user} watches on the sheet. */
    public List<Long> watchedBy(String sheetId, String user) {
        return jdbc.queryForList("SELECT person_id FROM person_watch WHERE sheet_id = ? AND user_name = ? ORDER BY id",
                Long.class, sheetId, user);
    }

    /** The users watching the person, in the order they started watching. */
    public List<String> watchers(String sheetId, long personId) {
        return jdbc.queryForList("SELECT user_name FROM person_watch WHERE sheet_id = ? AND person_id = ? ORDER BY id",
                String.class, sheetId, personId);
    }
}

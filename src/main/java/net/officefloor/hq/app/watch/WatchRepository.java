package net.officefloor.hq.app.watch;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Who watches which person, per sheet, in the order they started watching. */
@Repository
public class WatchRepository {

    private final JdbcTemplate jdbc;

    public WatchRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The users watching the person, in the order they started watching. */
    public List<String> watchers(String sheetId, long personId) {
        return jdbc.queryForList("SELECT user_name FROM person_watch WHERE sheet_id = ? AND person_id = ? ORDER BY id",
                String.class, sheetId, personId);
    }

    /** The ids of the people on the sheet this user watches. */
    public List<Long> watchedBy(String sheetId, String user) {
        return jdbc.queryForList("SELECT person_id FROM person_watch WHERE sheet_id = ? AND user_name = ? ORDER BY person_id",
                Long.class, sheetId, user);
    }

    public boolean watching(String sheetId, long personId, String user) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person_watch WHERE sheet_id = ? AND person_id = ? AND user_name = ?",
                Integer.class, sheetId, personId, user);
        return n != null && n > 0;
    }

    public void watch(String sheetId, long personId, String user) {
        jdbc.update("INSERT INTO person_watch (sheet_id, person_id, user_name) VALUES (?, ?, ?)", sheetId, personId, user);
    }

    public void unwatch(String sheetId, long personId, String user) {
        jdbc.update("DELETE FROM person_watch WHERE sheet_id = ? AND person_id = ? AND user_name = ?", sheetId, personId,
                user);
    }

    /** Drop everyone's watch of the person. */
    public void drop(String sheetId, long personId) {
        jdbc.update("DELETE FROM person_watch WHERE sheet_id = ? AND person_id = ?", sheetId, personId);
    }
}

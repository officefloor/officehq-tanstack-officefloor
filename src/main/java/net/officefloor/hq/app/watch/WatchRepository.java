package net.officefloor.hq.app.watch;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WatchRepository {

    private final JdbcTemplate jdbc;

    public WatchRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every watch on the sheet, in the order they started. */
    public List<Watch> findAll(String sheetId) {
        return jdbc.query("SELECT sheet_id, person_id, user_name FROM person_watch WHERE sheet_id = ? ORDER BY id",
                (rs, n) -> new Watch(rs.getString("sheet_id"), rs.getLong("person_id"), rs.getString("user_name")),
                sheetId);
    }

    /** The users watching the person, in the order they started watching. */
    public List<String> watchers(String sheetId, long personId) {
        return jdbc.queryForList("SELECT user_name FROM person_watch WHERE sheet_id = ? AND person_id = ? ORDER BY id",
                String.class, sheetId, personId);
    }

    public boolean exists(String sheetId, long personId, String user) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person_watch WHERE sheet_id = ? AND person_id = ? AND user_name = ?",
                Integer.class, sheetId, personId, user);
        return n != null && n > 0;
    }

    public void watch(String sheetId, long personId, String user) {
        jdbc.update("INSERT INTO person_watch (sheet_id, person_id, user_name) VALUES (?, ?, ?)", sheetId, personId,
                user);
    }

    public void unwatch(String sheetId, long personId, String user) {
        jdbc.update("DELETE FROM person_watch WHERE sheet_id = ? AND person_id = ? AND user_name = ?", sheetId,
                personId, user);
    }
}

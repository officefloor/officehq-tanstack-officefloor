package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.hq.app.Audit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Who watches which person, per sheet; a user who stops and starts again goes to the back of the order. */
@Repository
public class WatchRepository {

    private final JdbcTemplate jdbc;

    public WatchRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The ids of the people the user watches on the sheet. */
    public List<Long> watched(String sheetId, String user) {
        return jdbc.queryForList("SELECT person_id FROM person_watch WHERE sheet_id = ? AND user_name = ? ORDER BY person_id",
                Long.class, sheetId, user);
    }

    public boolean watches(String sheetId, long personId, String user) {
        Integer known = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person_watch WHERE sheet_id = ? AND person_id = ? AND user_name = ?",
                Integer.class, sheetId, personId, user);
        return known != null && known > 0;
    }

    public void watch(String sheetId, long personId, String user) {
        jdbc.update("INSERT INTO person_watch (sheet_id, person_id, user_name) VALUES (?, ?, ?)", sheetId, personId, user);
    }

    public void unwatch(String sheetId, long personId, String user) {
        jdbc.update("DELETE FROM person_watch WHERE sheet_id = ? AND person_id = ? AND user_name = ?", sheetId, personId, user);
    }

    /** The users watching the person, in the order they started watching. */
    public List<String> watchers(String sheetId, long personId) {
        return jdbc.queryForList("SELECT user_name FROM person_watch WHERE sheet_id = ? AND person_id = ? ORDER BY id",
                String.class, sheetId, personId);
    }

    /** Tell each of the person's watchers that their claim numbered so was settled, approved or re-settled. */
    public void notify(String sheetId, long personId, String personName, String number, String by, Audit audit) {
        for (String user : watchers(sheetId, personId)) {
            audit.record("NOTIFY sheet=" + sheetId + " by=" + by + " user=" + user + " person=" + personName
                    + " number=" + (number == null ? "n/a" : number));
        }
    }
}

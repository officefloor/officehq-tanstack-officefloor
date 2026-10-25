package net.officefloor.hq.app.person;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Which users watch which people; a person's watchers are kept in the order they started watching. */
@Repository
public class WatchRepository {

    private final JdbcTemplate jdbc;

    public WatchRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The users watching the person, in the order they started watching. */
    public List<String> watchers(long personId) {
        return jdbc.queryForList("SELECT user_name FROM person_watch WHERE person_id = ? ORDER BY id", String.class,
                personId);
    }

    /** The ids of the people on the sheet the user watches. */
    public List<Long> watchedBy(String sheetId, String user) {
        return jdbc.queryForList("SELECT w.person_id FROM person_watch w JOIN person p ON p.id = w.person_id"
                + " WHERE p.sheet_id = ? AND w.user_name = ? ORDER BY w.person_id", Long.class, sheetId, user);
    }

    /** Start the user watching the person; false when they already were. */
    public boolean watch(long personId, String user) {
        if (isWatching(personId, user)) {
            return false;
        }
        jdbc.update("INSERT INTO person_watch (person_id, user_name) VALUES (?, ?)", personId, user);
        return true;
    }

    /** Stop the user watching the person; false when they were not. */
    public boolean unwatch(long personId, String user) {
        return jdbc.update("DELETE FROM person_watch WHERE person_id = ? AND user_name = ?", personId, user) > 0;
    }

    private boolean isWatching(long personId, String user) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM person_watch WHERE person_id = ? AND user_name = ?",
                Integer.class, personId, user);
        return n != null && n > 0;
    }
}

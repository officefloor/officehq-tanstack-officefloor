package net.officefloor.hq.app.person;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Who watches whom on a sheet, each user for themselves, by when they started watching. */
@Repository
public class WatchRepository {

    private final JdbcTemplate jdbc;

    public WatchRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The ids of the people the user watches on the sheet. */
    public List<Long> watchedBy(String sheetId, String user) {
        return jdbc.queryForList("SELECT person_id FROM person_watch WHERE sheet_id = ? AND watched_by = ? ORDER BY id",
                Long.class, sheetId, user);
    }

    /** The users watching the person, in the order they started watching. */
    public List<String> watchers(String sheetId, long personId) {
        return jdbc.queryForList("SELECT watched_by FROM person_watch WHERE sheet_id = ? AND person_id = ? ORDER BY id",
                String.class, sheetId, personId);
    }

    /** Start (true) or stop (false) the user watching the person; false when that is already so. */
    public boolean set(Person person, String user, boolean on) {
        if (on) {
            if (watchers(person.getSheetId(), person.getId()).contains(user)) {
                return false;
            }
            jdbc.update("INSERT INTO person_watch (sheet_id, person_id, watched_by) VALUES (?, ?, ?)",
                    person.getSheetId(), person.getId(), user);
            return true;
        }
        return jdbc.update("DELETE FROM person_watch WHERE sheet_id = ? AND person_id = ? AND watched_by = ?",
                person.getSheetId(), person.getId(), user) > 0;
    }
}

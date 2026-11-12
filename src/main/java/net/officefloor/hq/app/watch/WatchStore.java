package net.officefloor.hq.app.watch;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.history.History;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Which users watch which people, kept per sheet, each person's watchers in the order they started watching. */
@Repository
public class WatchStore {

    private final JdbcTemplate jdbc;

    public WatchStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The ids of the people the user watches on the sheet. */
    public List<Long> watchedBy(String sheetId, String user) {
        return jdbc.queryForList("SELECT person_id FROM watch WHERE sheet_id = ? AND username = ? ORDER BY person_id",
                Long.class, sheetId, user);
    }

    /** The ids of the people the user watched on the sheet at the end of the date (see {@link History}). */
    public List<Long> watchedByAsOf(String sheetId, String user, LocalDate date) {
        return jdbc.queryForList("SELECT person_id FROM " + History.asOf("watch", "id", date)
                + " WHERE sheet_id = ? AND username = ? ORDER BY person_id", Long.class, sheetId, user);
    }

    /** The users watching the person, in the order they started watching. */
    public List<String> watchers(String sheetId, long personId) {
        return jdbc.queryForList("SELECT username FROM watch WHERE sheet_id = ? AND person_id = ? ORDER BY id",
                String.class, sheetId, personId);
    }

    public boolean watches(String sheetId, long personId, String user) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM watch WHERE sheet_id = ? AND person_id = ? AND username = ?",
                Integer.class, sheetId, personId, user);
        return n != null && n > 0;
    }

    public void watch(String sheetId, long personId, String user) {
        jdbc.update("INSERT INTO watch (sheet_id, person_id, username) VALUES (?, ?, ?)", sheetId, personId, user);
    }

    public void unwatch(String sheetId, long personId, String user) {
        jdbc.update("DELETE FROM watch WHERE sheet_id = ? AND person_id = ? AND username = ?", sheetId, personId, user);
    }
}

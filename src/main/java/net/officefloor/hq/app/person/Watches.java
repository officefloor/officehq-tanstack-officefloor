package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.hq.app.Audit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Who watches whom, per sheet and per user, in the order they started watching. */
@Repository
public class Watches {

    private final JdbcTemplate jdbc;

    public Watches(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean isWatching(String sheetId, long personId, String user) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person_watch WHERE sheet_id = ? AND person_id = ? AND user_name = ?",
                Integer.class, sheetId, personId, user);
        return count != null && count > 0;
    }

    public void watch(String sheetId, long personId, String user) {
        jdbc.update("INSERT INTO person_watch (sheet_id, person_id, user_name) VALUES (?, ?, ?)", sheetId, personId, user);
        history(sheetId, personId, user, true);
    }

    public void unwatch(String sheetId, long personId, String user) {
        jdbc.update("DELETE FROM person_watch WHERE sheet_id = ? AND person_id = ? AND user_name = ?", sheetId, personId, user);
        history(sheetId, personId, user, false);
    }

    /** Records the start or stop of a watch in the watch history, dated the sheet's today. */
    private void history(String sheetId, long personId, String user, boolean watching) {
        jdbc.update("INSERT INTO watch_history (sheet_id, person_id, user_name, set_on, watching) VALUES (?, ?, ?, (SELECT today FROM sheet WHERE id = ?), ?)",
                sheetId, personId, user, sheetId, watching);
    }

    /** The ids of the people the user watches on the sheet. */
    public List<String> watchedBy(String sheetId, String user) {
        return jdbc.queryForList("SELECT person_id FROM person_watch WHERE sheet_id = ? AND user_name = ? ORDER BY seq",
                Long.class, sheetId, user).stream().map(String::valueOf).toList();
    }

    /** The users watching the person, in the order they started watching. */
    public List<String> watchers(String sheetId, long personId) {
        return jdbc.queryForList("SELECT user_name FROM person_watch WHERE sheet_id = ? AND person_id = ? ORDER BY seq",
                String.class, sheetId, personId);
    }

    /** Audits one NOTIFY line per user watching the person, for their claim numbered as given. */
    public void notifyWatchers(Person person, String by, String number, Audit audit) {
        for (String watcher : watchers(person.getSheetId(), person.getId())) {
            audit.recordOnCommit("NOTIFY sheet=" + person.getSheetId() + " by=" + by + " user=" + watcher
                    + " person=" + person.getName() + " number=" + number);
        }
    }
}

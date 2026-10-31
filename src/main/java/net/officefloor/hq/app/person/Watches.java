package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Who watches whom on each sheet, per user, in the order they started watching. */
@Repository
public class Watches {

    private final JdbcTemplate jdbc;

    public Watches(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The ids of the people on this sheet this user watches. */
    public List<Long> watchedBy(String sheetId, String user) {
        return jdbc.query("SELECT person_id FROM person_watch WHERE sheet_id = ? AND user_name = ? ORDER BY id",
                (rs, i) -> rs.getLong("person_id"), sheetId, user);
    }

    /** The users watching this person, in the order they started watching. */
    public List<String> watchers(String sheetId, long personId) {
        return jdbc.query("SELECT user_name FROM person_watch WHERE sheet_id = ? AND person_id = ? ORDER BY id",
                (rs, i) -> rs.getString("user_name"), sheetId, personId);
    }

    public boolean watches(String sheetId, long personId, String user) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM person_watch WHERE sheet_id = ? AND person_id = ? AND user_name = ?",
                Integer.class, sheetId, personId, user);
        return n != null && n > 0;
    }

    public void watch(String sheetId, long personId, String user) {
        jdbc.update("INSERT INTO person_watch (sheet_id, person_id, user_name) VALUES (?, ?, ?)", sheetId, personId, user);
    }

    public void unwatch(String sheetId, long personId, String user) {
        jdbc.update("DELETE FROM person_watch WHERE sheet_id = ? AND person_id = ? AND user_name = ?", sheetId, personId, user);
    }

    /** One NOTIFY line per user watching the claim's person, in the order they started watching. */
    public void notify(Claim claim, Person person, String by, Audit audit) {
        for (String watcher : watchers(claim.getSheetId(), person.getId())) {
            audit.record("NOTIFY sheet=" + claim.getSheetId() + " by=" + by + " user=" + watcher
                    + " person=" + person.getName() + " number=" + (claim.getNumber() != null ? claim.getNumber() : "n/a"));
        }
    }
}

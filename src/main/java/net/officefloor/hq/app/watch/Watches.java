package net.officefloor.hq.app.watch;

import net.officefloor.hq.app.history.AsOf;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The store of which signed-in users watch which people, kept per sheet through the people. */
@Repository
public class Watches {

    private final JdbcTemplate jdbc;

    public Watches(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every watch on the sheet, in the order they started. */
    public List<WatchResponse> list(String sheetId) {
        return jdbc.query("SELECT w.person_id, w.user_name FROM " + AsOf.table("person_watch", "w")
                + " JOIN " + AsOf.table("person", "p") + " ON p.id = w.person_id WHERE p.sheet_id = ? ORDER BY w.id",
                (rs, n) -> new WatchResponse(rs.getLong("person_id"), rs.getString("user_name")), sheetId);
    }

    /** The users watching the person, in the order they started watching. */
    public List<String> watchers(long personId) {
        return jdbc.queryForList("SELECT user_name FROM " + AsOf.table("person_watch") + " WHERE person_id = ? ORDER BY id", String.class,
                personId);
    }

    public boolean has(long personId, String user) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + AsOf.table("person_watch") + " WHERE person_id = ? AND user_name = ?",
                Integer.class, personId, user);
        return n != null && n > 0;
    }

    public void add(long personId, String user) {
        jdbc.update("INSERT INTO person_watch (person_id, user_name) VALUES (?, ?)", personId, user);
    }

    public void remove(long personId, String user) {
        jdbc.update("DELETE FROM person_watch WHERE person_id = ? AND user_name = ?", personId, user);
    }
}

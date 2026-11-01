package net.officefloor.hq.app.watch;

import java.util.List;
import net.officefloor.hq.app.Audit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The people each user watches on a sheet, kept in the order they started watching. */
@Repository
public class Watches {

    private final JdbcTemplate jdbc;
    private final Audit audit;

    public Watches(JdbcTemplate jdbc, Audit audit) {
        this.jdbc = jdbc;
        this.audit = audit;
    }

    /** The ids of the people the user watches on the sheet. */
    public List<Long> watchedBy(String sheet, String user) {
        return jdbc.queryForList("SELECT person_id FROM watch WHERE sheet = ? AND user_name = ? ORDER BY id",
                Long.class, sheet, user);
    }

    /** The users watching the person, in the order they started watching. */
    public List<String> watchers(String sheet, long person) {
        return jdbc.queryForList("SELECT user_name FROM watch WHERE sheet = ? AND person_id = ? ORDER BY id",
                String.class, sheet, person);
    }

    /** Starts the user watching the person; false when they already were. */
    public boolean watch(String sheet, long person, String user) {
        return jdbc.update("INSERT INTO watch (sheet, person_id, user_name) SELECT ?, ?, ? WHERE NOT EXISTS "
                + "(SELECT 1 FROM watch WHERE sheet = ? AND person_id = ? AND user_name = ?)", sheet, person, user,
                sheet, person, user) > 0;
    }

    /** Stops the user watching the person; false when they were not. */
    public boolean unwatch(String sheet, long person, String user) {
        return jdbc.update("DELETE FROM watch WHERE sheet = ? AND person_id = ? AND user_name = ?", sheet, person,
                user) > 0;
    }

    /** Writes one NOTIFY line per user watching the person, in the order they started watching. */
    public void notify(String sheet, String by, long person, String name, String number) {
        for (String watcher : watchers(sheet, person)) {
            audit.record("NOTIFY sheet=" + sheet + " by=" + by + " user=" + watcher + " person=" + name + " number="
                    + number);
        }
    }
}

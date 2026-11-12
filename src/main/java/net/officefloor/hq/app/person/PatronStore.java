package net.officefloor.hq.app.person;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Each person's patrons, kept per sheet as the ids of the people they named. */
@Repository
public class PatronStore {

    private final JdbcTemplate jdbc;

    public PatronStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The ids of each person's patrons on the sheet, keyed by the person's id. */
    public Map<Long, List<Long>> all(String sheetId) {
        Map<Long, List<Long>> found = new HashMap<>();
        jdbc.query("SELECT person_id, patron_id FROM patron WHERE sheet_id = ? ORDER BY patron_id",
                rs -> {
                    found.computeIfAbsent(rs.getLong("person_id"), k -> new ArrayList<>()).add(rs.getLong("patron_id"));
                }, sheetId);
        return found;
    }

    public List<Long> of(String sheetId, long personId) {
        return jdbc.queryForList("SELECT patron_id FROM patron WHERE sheet_id = ? AND person_id = ? ORDER BY patron_id",
                Long.class, sheetId, personId);
    }

    public void add(String sheetId, long personId, long patronId) {
        jdbc.update("INSERT INTO patron (sheet_id, person_id, patron_id) VALUES (?, ?, ?)", sheetId, personId, patronId);
    }
}

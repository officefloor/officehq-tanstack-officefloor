package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.history.History;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Each person's patrons, kept per sheet as the ids of the people they named. */
@Repository
public class PatronStore {

    private final JdbcTemplate jdbc;

    public PatronStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The ids of each person's patrons on the sheet, in the order they were named, keyed by the person's id. */
    public Map<Long, List<Long>> all(String sheetId) {
        return allFrom(sheetId, "patron");
    }

    /** The ids of each person's patrons on the sheet as they stood at the end of the date (see {@link History}). */
    public Map<Long, List<Long>> allAsOf(String sheetId, LocalDate date) {
        return allFrom(sheetId, History.asOf("patron", "sheet_id, person_id, patron_id", date));
    }

    private Map<Long, List<Long>> allFrom(String sheetId, String table) {
        Map<Long, List<Long>> found = new HashMap<>();
        jdbc.query("SELECT person_id, patron_id FROM " + table + " WHERE sheet_id = ? ORDER BY named",
                rs -> {
                    found.computeIfAbsent(rs.getLong("person_id"), k -> new ArrayList<>()).add(rs.getLong("patron_id"));
                }, sheetId);
        return found;
    }

    public List<Long> of(String sheetId, long personId) {
        return jdbc.queryForList("SELECT patron_id FROM patron WHERE sheet_id = ? AND person_id = ? ORDER BY named",
                Long.class, sheetId, personId);
    }

    public void add(String sheetId, long personId, long patronId) {
        jdbc.update("INSERT INTO patron (sheet_id, person_id, patron_id) VALUES (?, ?, ?)", sheetId, personId, patronId);
    }

    /** Gives {@code intoId} the patrons of {@code fromId} (not themselves, none twice), has anyone naming
     *  {@code fromId} name {@code intoId} instead (not themselves, none twice), and drops every patron of or by
     *  {@code fromId}. */
    public void merge(String sheetId, long fromId, long intoId) {
        List<Long> into = new ArrayList<>(of(sheetId, intoId));
        for (Long patron : of(sheetId, fromId)) {
            if (patron != intoId && !into.contains(patron)) {
                add(sheetId, intoId, patron);
                into.add(patron);
            }
        }
        for (Long person : jdbc.queryForList("SELECT person_id FROM patron WHERE sheet_id = ? AND patron_id = ?",
                Long.class, sheetId, fromId)) {
            if (person != intoId && person != fromId && !of(sheetId, person).contains(intoId)) {
                add(sheetId, person, intoId);
            }
        }
        jdbc.update("DELETE FROM patron WHERE sheet_id = ? AND (person_id = ? OR patron_id = ?)", sheetId, fromId, fromId);
    }

    /** Drops every patron of or by the person. */
    public void drop(String sheetId, long personId) {
        jdbc.update("DELETE FROM patron WHERE sheet_id = ? AND (person_id = ? OR patron_id = ?)", sheetId, personId, personId);
    }
}

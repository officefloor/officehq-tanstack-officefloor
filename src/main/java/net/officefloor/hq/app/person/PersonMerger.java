package net.officefloor.hq.app.person;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Stores a merge of one person into another on the sheet (see #merge). */
@Repository
public class PersonMerger {

    private final JdbcTemplate jdbc;

    public PersonMerger(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Merges {@code from} into {@code into}: from's claims move to into (numbers and dates kept, listed after every claim
     * on the sheet in the order they were added, each unbound from any statement figure); from's name and aliases become
     * into's aliases; from's patrons are added to into's (not into themselves, no repeats) and anyone naming from as
     * patron names into instead (no repeats; into never their own patron); from's pool memberships move to into (shares
     * added where into is already a member). From's statements, holds, watchers, contact and grades are dropped, and from
     * leaves the sheet.
     */
    public void merge(String sheetId, Person from, Person into) {
        long f = from.id();
        long t = into.id();
        List<Long> claims = jdbc.queryForList("SELECT id FROM claim WHERE sheet_id = ? AND person_id = ? ORDER BY listed, id",
                Long.class, sheetId, f);
        for (Long id : claims) {
            jdbc.update("UPDATE claim SET person_id = ?, stated = FALSE, stated_due = NULL, "
                    + "listed = (SELECT COALESCE(MAX(c.listed), 0) + 1 FROM claim c WHERE c.sheet_id = ?) WHERE id = ?",
                    t, sheetId, id);
        }

        jdbc.update("UPDATE person_alias SET person_id = ? WHERE sheet_id = ? AND person_id = ?", t, sheetId, f);

        for (Map<String, Object> p : jdbc.queryForList("SELECT patron_id, added_on FROM patron WHERE sheet_id = ? AND person_id = ?",
                sheetId, f)) {
            addPatron(sheetId, t, ((Number) p.get("patron_id")).longValue(), p.get("added_on"));
        }
        for (Map<String, Object> p : jdbc.queryForList("SELECT person_id, added_on FROM patron WHERE sheet_id = ? AND patron_id = ?",
                sheetId, f)) {
            addPatron(sheetId, ((Number) p.get("person_id")).longValue(), t, p.get("added_on"));
        }
        jdbc.update("DELETE FROM patron WHERE sheet_id = ? AND (person_id = ? OR patron_id = ?)", sheetId, f, f);

        for (Map<String, Object> m : jdbc.queryForList("SELECT id, pool_id, share FROM pool_member WHERE person_id = ? ORDER BY id", f)) {
            long poolId = ((Number) m.get("pool_id")).longValue();
            BigDecimal share = (BigDecimal) m.get("share");
            List<Long> theirs = jdbc.queryForList("SELECT id FROM pool_member WHERE pool_id = ? AND person_id = ?", Long.class,
                    poolId, t);
            if (theirs.isEmpty()) {
                jdbc.update("UPDATE pool_member SET person_id = ? WHERE id = ?", t, m.get("id"));
            } else {
                jdbc.update("UPDATE pool_member SET share = share + ? WHERE id = ?", share, theirs.get(0));
                jdbc.update("DELETE FROM pool_member WHERE id = ?", m.get("id"));
            }
        }

        for (String table : List.of("statement", "person_hold", "watch", "person_grade", "person_grade_log",
                "person_retire_log", "person_terms_log")) {
            jdbc.update("DELETE FROM " + table + " WHERE sheet_id = ? AND person_id = ?", sheetId, f);
        }
        jdbc.update("DELETE FROM person WHERE sheet_id = ? AND id = ?", sheetId, f);
        jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias) VALUES (?, ?, ?)", sheetId, t, from.name());
    }

    /** Names the patron for the person, unless it is themselves or they already have them. */
    private void addPatron(String sheetId, long personId, long patronId, Object addedOn) {
        if (personId == patronId) {
            return;
        }
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM patron WHERE sheet_id = ? AND person_id = ? AND patron_id = ?",
                Integer.class, sheetId, personId, patronId);
        if (n != null && n == 0) {
            jdbc.update("INSERT INTO patron (sheet_id, person_id, patron_id, added_on) VALUES (?, ?, ?, ?)", sheetId, personId,
                    patronId, addedOn);
        }
    }
}

package net.officefloor.hq.app.person;

import java.sql.Date;
import java.util.List;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.statement.StatementRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Stores a transfer of a person to another sheet (see #transfer). */
@Repository
public class PersonTransferrer {

    private final JdbcTemplate jdbc;
    private final SheetRepository sheets;
    private final PersonRepository people;
    private final StatementRepository statements;

    public PersonTransferrer(JdbcTemplate jdbc, SheetRepository sheets, PersonRepository people, StatementRepository statements) {
        this.jdbc = jdbc;
        this.sheets = sheets;
        this.people = people;
        this.statements = statements;
    }

    /**
     * Moves the person to the other sheet with their grades (and from-dates), terms, contact and aliases, added there on
     * its today (the sheet they left kept as their origin) at the end of its list; and their claims with their status, dates, notes, tags, frozen figures, logs, comments and reminders,
     * listed there as its newest claims in the order they were listed, each keeping the figure a statement bound it to,
     * without its reference, lock or settle batch; and their statements. Their holds, watchers, patron links (either way), pool memberships,
     * period totals and retirement log are dropped: they leave this sheet entirely. Returns the ids of the moved claims
     * that have a number, in the order they were opened (by the sheet's today when each was opened, then as added), for
     * the caller to number on the other sheet.
     */
    public List<Long> transfer(Person person, String to) {
        String from = person.sheetId();
        long id = person.id();
        List<Long> claims = jdbc.queryForList("SELECT id FROM claim WHERE sheet_id = ? AND person_id = ? ORDER BY listed, id",
                Long.class, from, id);
        List<Long> numbered = jdbc.queryForList("SELECT c.id FROM claim c WHERE c.sheet_id = ? AND c.person_id = ?"
                + " AND c.claim_number IS NOT NULL ORDER BY (SELECT MIN(l.log_date) FROM claim_log l WHERE l.sheet_id = c.sheet_id"
                + " AND l.claim_id = c.id AND l.status = 'OPEN') NULLS LAST, c.id", Long.class, from, id);
        for (Long c : claims) {
            for (String table : List.of("claim_log", "claim_reminder", "claim_comment", "claim_line", "claim_void_reason")) {
                jdbc.update("UPDATE " + table + " SET sheet_id = ? WHERE sheet_id = ? AND claim_id = ?", to, from, c);
            }
            jdbc.update("UPDATE claim_edit SET sheet_id = ?, ref = NULL WHERE sheet_id = ? AND claim_id = ?", to, from, c);
            jdbc.update("DELETE FROM claim_batch WHERE sheet_id = ? AND claim_id = ?", from, c);
            jdbc.update("UPDATE claim SET sheet_id = ?, ref = NULL, ref_key = NULL, locked_by = NULL, "
                    + "listed = (SELECT COALESCE(MAX(x.listed), 0) + 1 FROM claim x WHERE x.sheet_id = ?) WHERE id = ?",
                    to, to, c);
        }

        statements.moveAll(from, id, to, id);
        jdbc.update("DELETE FROM patron WHERE sheet_id = ? AND (person_id = ? OR patron_id = ?)", from, id, id);
        jdbc.update("DELETE FROM pool_member WHERE person_id = ?", id);
        jdbc.update("DELETE FROM crew_member WHERE person_id = ?", id);
        // the crews they lead stay, without a lead
        jdbc.update("UPDATE crew SET lead_id = NULL WHERE lead_id = ?", id);
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND person_id = ?", from, id);
        for (String table : List.of("person_hold", "watch", "person_retire_log")) {
            jdbc.update("DELETE FROM " + table + " WHERE sheet_id = ? AND person_id = ?", from, id);
        }
        for (String table : List.of("person_grade", "person_grade_log", "person_terms_log", "person_terms_from", "person_alias",
                "person_retirement")) {
            jdbc.update("UPDATE " + table + " SET sheet_id = ? WHERE sheet_id = ? AND person_id = ?", to, from, id);
        }
        jdbc.update("UPDATE person SET sheet_id = ?, added_on = ?, origin = ? WHERE sheet_id = ? AND id = ?", to,
                Date.valueOf(sheets.today(to)), from, from, id);
        people.toEnd(to, id);
        return numbered;
    }

    /**
     * Moves the person to the other sheet merged into {@code into} there, as a merge does (see PersonMerger#merge): their
     * claims go to into as transfer moves them (see #transfer), listed there after every claim of the sheet; their
     * statements go to into, before into's own (into's count becomes their own plus theirs; into's last statement stays
     * into's, or having none becomes theirs); their name
     * and aliases become into's aliases (one into already has as written is not repeated); their grades, terms, contact
     * and retirement are dropped, as is everything a transfer drops, and they leave this sheet entirely. Returns the ids
     * of the moved claims that have a number, in the order they were opened, for the caller to number on the other sheet.
     */
    public List<Long> transferInto(Person person, String to, Person into) {
        String from = person.sheetId();
        long id = person.id();
        long t = into.id();
        List<Long> claims = jdbc.queryForList("SELECT id FROM claim WHERE sheet_id = ? AND person_id = ? ORDER BY listed, id",
                Long.class, from, id);
        List<Long> numbered = jdbc.queryForList("SELECT c.id FROM claim c WHERE c.sheet_id = ? AND c.person_id = ?"
                + " AND c.claim_number IS NOT NULL ORDER BY (SELECT MIN(l.log_date) FROM claim_log l WHERE l.sheet_id = c.sheet_id"
                + " AND l.claim_id = c.id AND l.status = 'OPEN') NULLS LAST, c.id", Long.class, from, id);
        for (Long c : claims) {
            for (String table : List.of("claim_log", "claim_reminder", "claim_comment", "claim_line", "claim_void_reason")) {
                jdbc.update("UPDATE " + table + " SET sheet_id = ? WHERE sheet_id = ? AND claim_id = ?", to, from, c);
            }
            jdbc.update("UPDATE claim_edit SET sheet_id = ?, ref = NULL WHERE sheet_id = ? AND claim_id = ?", to, from, c);
            jdbc.update("DELETE FROM claim_batch WHERE sheet_id = ? AND claim_id = ?", from, c);
            jdbc.update("UPDATE claim SET sheet_id = ?, person_id = ?, ref = NULL, ref_key = NULL, "
                    + "locked_by = NULL, listed = (SELECT COALESCE(MAX(x.listed), 0) + 1 FROM claim x WHERE x.sheet_id = ?) WHERE id = ?",
                    to, t, to, c);
        }

        statements.moveAll(from, id, to, t);
        jdbc.update("DELETE FROM patron WHERE sheet_id = ? AND (person_id = ? OR patron_id = ?)", from, id, id);
        jdbc.update("DELETE FROM pool_member WHERE person_id = ?", id);
        jdbc.update("DELETE FROM crew_member WHERE person_id = ?", id);
        // the crews they lead stay, without a lead
        jdbc.update("UPDATE crew SET lead_id = NULL WHERE lead_id = ?", id);
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND person_id = ?", from, id);
        for (String table : List.of("person_hold", "watch", "person_retire_log", "person_grade", "person_grade_log",
                "person_terms_log", "person_terms_from", "person_retirement")) {
            jdbc.update("DELETE FROM " + table + " WHERE sheet_id = ? AND person_id = ?", from, id);
        }

        java.util.Set<String> held = new java.util.HashSet<>(into.aliases());
        held.add(into.name());
        for (java.util.Map<String, Object> a : jdbc.queryForList("SELECT id, alias FROM person_alias WHERE sheet_id = ? AND person_id = ? ORDER BY id",
                from, id)) {
            if (held.add((String) a.get("alias"))) {
                jdbc.update("UPDATE person_alias SET sheet_id = ?, person_id = ? WHERE id = ?", to, t, a.get("id"));
            } else {
                jdbc.update("DELETE FROM person_alias WHERE id = ?", a.get("id"));
            }
        }
        jdbc.update("DELETE FROM person WHERE sheet_id = ? AND id = ?", from, id);
        if (held.add(person.name())) {
            jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias, added_on) VALUES (?, ?, ?, ?)", to, t, person.name(),
                    Date.valueOf(sheets.today(to)));
        }
        return numbered;
    }
}

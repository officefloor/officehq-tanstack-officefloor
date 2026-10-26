package net.officefloor.hq.app.sheet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Copies a sheet to a sheet id never used: its today, settings, roles, rates, holidays, void reasons, templates, the
 * tags ever used on its claims, its people (grades, terms, contacts, aliases, holds, retirements, patrons), its pools,
 * its plans (with the dates they made claims for), its periods (all open, with no figures fixed) and its claims (status, frozen figures, dates, notes, logs). Its
 * statements are not copied (no claim is bound), nor its snapshots, watches, delegations, comments, editor locks or
 * settle batches.
 */
@Repository
public class SheetCloneRepository {

    private final JdbcTemplate jdbc;

    public SheetCloneRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Create the clone with everything but its claims; answers the clone's person id for each person's id. */
    public Map<Long, Long> copySheet(String from, String to) {
        jdbc.update("INSERT INTO sheet (id, today, last_claim_listed) SELECT ?, today, last_claim_listed FROM sheet"
                + " WHERE id = ?", to, from);
        jdbc.update("INSERT INTO sheet_setting (sheet_id, setting_key, setting_value) SELECT ?, setting_key,"
                + " setting_value FROM sheet_setting WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO sheet_setting_from (sheet_id, setting_key, from_date, setting_value) SELECT ?,"
                + " setting_key, from_date, setting_value FROM sheet_setting_from WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO sheet_role (sheet_id, user_name, role) SELECT ?, user_name, role FROM sheet_role"
                + " WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO rate (sheet_id, grade, rate, from_date, to_date) SELECT ?, grade, rate, from_date,"
                + " to_date FROM rate WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO holiday (sheet_id, holiday_date) SELECT ?, holiday_date FROM holiday"
                + " WHERE sheet_id = ? ORDER BY id", to, from);
        jdbc.update("INSERT INTO void_reason (sheet_id, reason) SELECT ?, reason FROM void_reason"
                + " WHERE sheet_id = ? ORDER BY id", to, from);
        jdbc.update("INSERT INTO claim_template (sheet_id, name, amount, note) SELECT ?, name, amount, note"
                + " FROM claim_template WHERE sheet_id = ? ORDER BY id", to, from);
        jdbc.update("INSERT INTO claim_tag (sheet_id, tag) SELECT ?, tag FROM claim_tag WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date) SELECT ?, name, from_date, to_date"
                + " FROM period WHERE sheet_id = ? ORDER BY id", to, from);

        Map<Long, Long> people = new HashMap<>();
        for (Long id : ids("SELECT id FROM person WHERE sheet_id = ? ORDER BY id", from)) {
            long copy = jdbc.queryForObject("SELECT id FROM FINAL TABLE (INSERT INTO person (sheet_id, name, version,"
                    + " retired_from, terms, contact, list_position, origin) SELECT ?, name, 0, retired_from, terms,"
                    + " contact, list_position, origin FROM person WHERE id = ?)", Long.class, to, id);
            people.put(id, copy);
            jdbc.update("INSERT INTO person_grade (person_id, grade, from_date) SELECT ?, grade, from_date"
                    + " FROM person_grade WHERE person_id = ?", copy, id);
            jdbc.update("INSERT INTO person_hold (person_id, from_date, to_date) SELECT ?, from_date, to_date"
                    + " FROM person_hold WHERE person_id = ? ORDER BY id", copy, id);
            jdbc.update("INSERT INTO person_retirement (person_id, from_date, to_date) SELECT ?, from_date, to_date"
                    + " FROM person_retirement WHERE person_id = ? ORDER BY id", copy, id);
            jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias) SELECT ?, ?, alias FROM person_alias"
                    + " WHERE person_id = ? ORDER BY id", to, copy, id);
        }
        for (Map<String, Object> patron : jdbc.queryForList("SELECT pp.person_id, pp.patron_id FROM person_patron pp"
                + " JOIN person q ON q.id = pp.person_id WHERE q.sheet_id = ? ORDER BY pp.named", from)) {
            Long person = people.get(((Number) patron.get("person_id")).longValue());
            Long named = people.get(((Number) patron.get("patron_id")).longValue());
            if (person != null && named != null) {
                jdbc.update("INSERT INTO person_patron (person_id, patron_id) VALUES (?, ?)", person, named);
            }
        }

        Map<Long, Long> pools = new HashMap<>();
        for (Long id : ids("SELECT id FROM pool WHERE sheet_id = ? ORDER BY id", from)) {
            pools.put(id, jdbc.queryForObject("SELECT id FROM FINAL TABLE (INSERT INTO pool (sheet_id, name)"
                    + " SELECT ?, name FROM pool WHERE id = ?)", Long.class, to, id));
        }
        for (Map<String, Object> member : jdbc.queryForList("SELECT pool_id, person_id, member_pool_id, share,"
                + " from_date, to_date FROM pool_member WHERE sheet_id = ? ORDER BY id", from)) {
            jdbc.update("INSERT INTO pool_member (sheet_id, pool_id, person_id, member_pool_id, share, from_date,"
                    + " to_date) VALUES (?, ?, ?, ?, ?, ?, ?)", to, pools.get(id(member.get("pool_id"))),
                    people.get(id(member.get("person_id"))), pools.get(id(member.get("member_pool_id"))),
                    member.get("share"), member.get("from_date"), member.get("to_date"));
        }
        for (Long id : ids("SELECT id FROM plan WHERE sheet_id = ? ORDER BY id", from)) {
            Long person = jdbc.queryForObject("SELECT person_id FROM plan WHERE id = ?", Long.class, id);
            long copy = jdbc.queryForObject("SELECT id FROM FINAL TABLE (INSERT INTO plan (sheet_id, person_id, amount,"
                    + " start_date, every_days, end_date) SELECT ?, ?, amount, start_date, every_days, end_date FROM plan"
                    + " WHERE id = ?)", Long.class, to, people.get(person), id);
            jdbc.update("INSERT INTO plan_claim (sheet_id, plan_id, plan_date) SELECT ?, ?, plan_date FROM plan_claim"
                    + " WHERE plan_id = ? ORDER BY id", to, copy, id);
        }
        return people;
    }

    /**
     * Copy the sheet's claims, each with its log, to the clone's people: a claim that had a number takes its new one
     * from {@code numbers} (by claim id), and a correction or split names its claim by the new number.
     */
    public void copyClaims(String from, String to, Map<Long, Long> people, Map<Long, String> numbers) {
        List<Map<String, Object>> claims = jdbc.queryForList(
                "SELECT id, person_id, claim_number, corrects, split_from FROM claim WHERE sheet_id = ? ORDER BY id", from);
        Map<String, String> renumbered = new HashMap<>();
        for (Map<String, Object> claim : claims) {
            if (claim.get("claim_number") != null) {
                renumbered.put((String) claim.get("claim_number"), numbers.get(id(claim.get("id"))));
            }
        }
        for (Map<String, Object> claim : claims) {
            long id = id(claim.get("id"));
            long copy = jdbc.queryForObject("SELECT id FROM FINAL TABLE (INSERT INTO claim (sheet_id, person_id,"
                    + " amount, claim_date, note, status, claim_number, settled_due, opened_by, stated, stated_due,"
                    + " corrects, listed, deferred_until, proofs, split_from, late_entry, reminded_on, due_from, terms,"
                    + " ref, tags, void_reason, edited_by, due_by, settle_batch)"
                    + " SELECT ?, ?, amount, claim_date, note, status, ?, settled_due, opened_by, FALSE, NULL,"
                    + " ?, listed, deferred_until, proofs, ?, late_entry, reminded_on, due_from, terms,"
                    + " ref, tags, void_reason, edited_by, due_by, NULL FROM claim WHERE id = ?)", Long.class,
                    to, people.get(id(claim.get("person_id"))), numbers.get(id), renumbered.get((String) claim.get("corrects")),
                    renumbered.get((String) claim.get("split_from")), id);
            jdbc.update("INSERT INTO claim_log (sheet_id, claim_id, status, by_user, logged_on, due) SELECT ?, ?,"
                    + " status, by_user, logged_on, due FROM claim_log WHERE claim_id = ? ORDER BY id", to, copy, id);
        }
    }

    private List<Long> ids(String sql, String sheetId) {
        return jdbc.queryForList(sql, Long.class, sheetId);
    }

    private static Long id(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }
}

package net.officefloor.hq.app.sheet;

import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.claim.Claim;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** Copies a whole sheet to a new sheet id, for a clone. */
@Repository
public class SheetClones {

    /** The tables kept per sheet (by sheet_id) whose rows show a sheet has been used. */
    private static final List<String> SHEET_TABLES = List.of("person", "claim", "setting", "setting_from", "rate",
            "sheet_role", "period", "holiday", "void_reason", "claim_template", "pool", "crew", "grade_retired", "grade");

    private final JdbcTemplate jdbc;

    public SheetClones(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Whether anything is stored for the sheet. */
    public boolean used(String sheetId) {
        for (String table : SHEET_TABLES) {
            Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE sheet_id = ?", Integer.class, sheetId);
            if (n != null && n > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Copy the sheet {@code from} to the empty sheet {@code to}, which takes {@code today}: its settings, roles, grades, rates,
     * holidays, void reasons, templates, people (grades, terms, contacts, tags, aliases, holds, retirements, patrons), pools, crews,
     * periods (all open) and claims (status, frozen figures, dates, notes, tags, references, logs; unnumbered and unbound by
     * any statement, listed in the order they were). Returns each copied claim's id here by its id on {@code from}, in the
     * order they were added to the clone.
     */
    public Map<Long, Long> copy(String from, String to, LocalDate today) {
        // The sheet's own row first, so every copied row's history is dated by the clone's today.
        jdbc.update("INSERT INTO sheet (id, today_date, archived) VALUES (?, ?, FALSE)", to, today);
        jdbc.update("MERGE INTO sheet_loaded (id) KEY (id) VALUES (?)", to);
        jdbc.update("INSERT INTO sheet_source (id, source_id) VALUES (?, ?)", to, from);

        jdbc.update("INSERT INTO setting (sheet_id, setting_key, setting_value) SELECT ?, setting_key, setting_value "
                + "FROM setting WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO setting_from (sheet_id, setting_key, from_date, setting_value) "
                + "SELECT ?, setting_key, from_date, setting_value FROM setting_from WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO sheet_role (sheet_id, user_name, role) SELECT ?, user_name, role FROM sheet_role "
                + "WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO rate (sheet_id, grade, rate, from_date, to_date, version) SELECT ?, grade, rate, from_date, to_date, "
                + "version FROM rate WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO rate_version (sheet_id, grade, from_date, version, rate, to_date) SELECT ?, grade, from_date, "
                + "version, rate, to_date FROM rate_version WHERE sheet_id = ? ORDER BY id", to, from);
        jdbc.update("INSERT INTO holiday (sheet_id, holiday_date) SELECT ?, holiday_date FROM holiday WHERE sheet_id = ?",
                to, from);
        jdbc.update("INSERT INTO void_reason (sheet_id, reason, listed) SELECT ?, reason, listed FROM void_reason "
                + "WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO grade (sheet_id, grade) SELECT ?, grade FROM grade WHERE sheet_id = ? ORDER BY seq", to, from);
        jdbc.update("INSERT INTO grade_retired (sheet_id, grade) SELECT ?, grade FROM grade_retired WHERE sheet_id = ?",
                to, from);
        jdbc.update("INSERT INTO claim_template (sheet_id, name, amount, note, listed) SELECT ?, name, amount, note, listed "
                + "FROM claim_template WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date, closed, closed_total, soft) "
                + "SELECT ?, name, from_date, to_date, FALSE, NULL, FALSE FROM period WHERE sheet_id = ?", to, from);

        // People in list order, each with their dated grades, aliases, holds and past retirements.
        Map<Long, Long> people = new LinkedHashMap<>();
        for (Long person : jdbc.queryForList("SELECT id FROM person WHERE sheet_id = ? ORDER BY list_position, id",
                Long.class, from)) {
            KeyHolder key = new GeneratedKeyHolder();
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO person (sheet_id, name, grade, retired_from, terms, contact, tags, list_position, "
                                + "origin_sheet_id, origin_name) SELECT ?, name, grade, retired_from, terms, contact, tags, "
                                + "list_position, origin_sheet_id, origin_name FROM person WHERE id = ?",
                        new String[] { "id" });
                ps.setString(1, to);
                ps.setLong(2, person);
                return ps;
            }, key);
            long copy = key.getKey().longValue();
            people.put(person, copy);
            jdbc.update("INSERT INTO person_grade (person_id, from_date, grade) SELECT ?, from_date, grade FROM person_grade "
                    + "WHERE person_id = ? ORDER BY from_date", copy, person);
            jdbc.update("INSERT INTO person_terms (person_id, from_date, terms) SELECT ?, from_date, terms FROM person_terms "
                    + "WHERE person_id = ? ORDER BY from_date", copy, person);
            jdbc.update("INSERT INTO person_alias (person_id, sheet_id, alias) SELECT ?, ?, alias FROM person_alias "
                    + "WHERE person_id = ? ORDER BY id", copy, to, person);
            jdbc.update("INSERT INTO person_hold (person_id, from_date, to_date) SELECT ?, from_date, to_date FROM person_hold "
                    + "WHERE person_id = ? ORDER BY id", copy, person);
            jdbc.update("INSERT INTO person_retirement (person_id, from_date, to_date) SELECT ?, from_date, to_date "
                    + "FROM person_retirement WHERE person_id = ? ORDER BY id", copy, person);
        }
        // Patrons, in the order they were named.
        for (Map<String, Object> link : jdbc.queryForList("SELECT pp.person_id, pp.patron_id FROM person_patron pp "
                + "JOIN person p ON p.id = pp.person_id WHERE p.sheet_id = ? ORDER BY pp.named", from)) {
            Long person = people.get(((Number) link.get("person_id")).longValue());
            Long patron = people.get(((Number) link.get("patron_id")).longValue());
            if (person != null && patron != null) {
                jdbc.update("INSERT INTO person_patron (person_id, patron_id) VALUES (?, ?)", person, patron);
            }
        }

        // Pools, their person members and the pools nested in them.
        jdbc.update("INSERT INTO pool (sheet_id, name, listed) SELECT ?, name, listed FROM pool WHERE sheet_id = ?", to, from);
        for (Map<String, Object> m : jdbc.queryForList("SELECT * FROM pool_member WHERE sheet_id = ? ORDER BY listed", from)) {
            Long person = people.get(((Number) m.get("person_id")).longValue());
            if (person != null) {
                jdbc.update("INSERT INTO pool_member (sheet_id, pool_name, person_id, share, listed, from_date, to_date) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)", to, m.get("pool_name"), person, m.get("share"), m.get("listed"),
                        m.get("from_date"), m.get("to_date"));
            }
        }
        jdbc.update("INSERT INTO pool_nested (sheet_id, pool_name, member_pool, share, listed, from_date, to_date) "
                + "SELECT ?, pool_name, member_pool, share, listed, from_date, to_date FROM pool_nested WHERE sheet_id = ?",
                to, from);

        // Crews whose lead came across, and their members who did.
        for (Map<String, Object> c : jdbc.queryForList("SELECT * FROM crew WHERE sheet_id = ? ORDER BY listed", from)) {
            Long lead = people.get(((Number) c.get("lead_id")).longValue());
            if (lead != null) {
                jdbc.update("INSERT INTO crew (sheet_id, name, lead_id, listed) VALUES (?, ?, ?, ?)", to, c.get("name"), lead,
                        c.get("listed"));
            }
        }
        for (Map<String, Object> m : jdbc.queryForList("SELECT * FROM crew_member WHERE sheet_id = ? ORDER BY listed", from)) {
            Long person = people.get(((Number) m.get("person_id")).longValue());
            if (person != null) {
                jdbc.update("INSERT INTO crew_member (sheet_id, crew_name, person_id, listed) SELECT ?, ?, ?, ? FROM crew "
                        + "WHERE sheet_id = ? AND name = ?", to, m.get("crew_name"), person, m.get("listed"), to, m.get("crew_name"));
            }
        }

        // Claims, person by person in list order, each person's in the order they are listed, with their logs and lines.
        Map<Long, Long> claims = new LinkedHashMap<>();
        for (Map.Entry<Long, Long> person : people.entrySet()) {
            for (Long claim : jdbc.queryForList("SELECT id FROM claim WHERE person_id = ? ORDER BY listed, id", Long.class,
                    person.getKey())) {
                KeyHolder key = new GeneratedKeyHolder();
                jdbc.update(con -> {
                    PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, status, settled_due, "
                                    + "opened_by, corrects, due_by, deferred_until, proofs, proofs_photo, split_from, late_entry, reminded_on, escalated, waived, held, "
                                    + "ref, ref_key, tags, void_reason, edited_by, due_terms, listed) "
                                    + "SELECT ?, ?, amount, claim_date, note, status, settled_due, opened_by, corrects, due_by, "
                                    + "deferred_until, proofs, proofs_photo, split_from, late_entry, reminded_on, escalated, waived, held, ref, ref_key, tags, "
                                    + "void_reason, edited_by, due_terms, listed FROM claim WHERE id = ?",
                            new String[] { "id" });
                    ps.setString(1, to);
                    ps.setLong(2, person.getValue());
                    ps.setLong(3, claim);
                    return ps;
                }, key);
                long copy = key.getKey().longValue();
                claims.put(claim, copy);
                jdbc.update("INSERT INTO claim_log (sheet_id, claim_id, status, by_user, logged_on, due) "
                        + "SELECT ?, ?, status, by_user, logged_on, due FROM claim_log WHERE claim_id = ? ORDER BY id",
                        to, copy, claim);
                jdbc.update("INSERT INTO claim_line (sheet_id, claim_id, label, qty, unit) "
                        + "SELECT ?, ?, label, qty, unit FROM claim_line WHERE claim_id = ? ORDER BY id", to, copy, claim);
            }
        }
        return claims;
    }

    /**
     * Give the copied claims their numbers here ({@code numbers}, by copied id), and have each correction and split name
     * the number the claim it names was given ({@code renumbered}, by the number it had on the sheet cloned).
     */
    public void number(Map<Long, Long> copied, Map<Long, Claim> originals, Map<Long, String> numbers,
            Map<String, String> renumbered) {
        Map<Long, Claim> byCopy = new HashMap<>();
        copied.forEach((original, copy) -> byCopy.put(copy, originals.get(original)));
        byCopy.forEach((copy, original) -> {
            String corrects = original.corrects() == null ? null : renumbered.getOrDefault(original.corrects(), original.corrects());
            String splitFrom = original.splitFrom() == null ? null : renumbered.getOrDefault(original.splitFrom(), original.splitFrom());
            if (numbers.containsKey(copy) || corrects != null || splitFrom != null) {
                jdbc.update("UPDATE claim SET claim_number = ?, corrects = ?, split_from = ? WHERE id = ?",
                        numbers.get(copy), corrects, splitFrom, copy);
            }
        });
    }
}

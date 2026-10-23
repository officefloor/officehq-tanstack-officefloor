package net.officefloor.hq.app.sheet;

import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.claim.ClaimRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Copies a sheet to an id never used, the clone recording the sheet it was cloned from. The clone gets the sheet's today and settings (dated ones, their log, the
 *  permissions and the sheet-wide ones, the pot and its log; never archived), roles, rates (and their log), holidays,
 *  void reasons,
 *  templates and tags used, its people (in their place in the list, with their grades, terms, contact, aliases, holds,
 *  retirements and patrons), its pools and their members, its periods (all open) and its claims (status, settled dues,
 *  due-bys, dates, notes, tags, references, comments, void reasons, logs and history; each numbered claim takes a new
 *  number by the clone's own numbering, person by person in list order and each person's claims in the order they came
 *  to them, corrections and splits following the numbers they name). Statements are not copied (no claim is bound to
 *  one); nor are watches, delegations, settle batches or editor locks. */
@Repository
public class SheetCloner {

    private final JdbcTemplate jdbc;
    private final ClaimRepository claims;

    public SheetCloner(JdbcTemplate jdbc, ClaimRepository claims) {
        this.jdbc = jdbc;
        this.claims = claims;
    }

    public void copy(String from, String to, String user) {
        jdbc.update("INSERT INTO sheet (id, created_by, today, policy_date, period_numbering_from, grace_days, entry_date, "
                + "pot, parent, fiscal_start_month, alert_threshold, escalate_after, review_sla, proof_kinds, default_grade, score_mid, score_high, score_log, max_open, cloned_from) SELECT ?, ?, today, policy_date, period_numbering_from, grace_days, entry_date, pot, parent, fiscal_start_month, alert_threshold, escalate_after, review_sla, proof_kinds, default_grade, score_mid, score_high, score_log, max_open, id "
                + "FROM sheet WHERE id = ?", to, user, from);
        copyRows("rate", "grade, pct, from_date, to_date, version, after_claim", null, from, to);
        copyRows("rate_log", "grade, pct, from_date, to_date, made_on, version, after_claim", "id", from, to);
        copyRows("sheet_role", "user_name, role", "user_name", from, to);
        copyRows("sheet_permission", "action, roles", "action", from, to);
        copyRows("setting_value", "setting_key, from_date, val", "setting_key, from_date", from, to);
        copyRows("setting_log", "setting_key, from_date, val, made_on", "id", from, to);
        copyRows("pot_log", "val, made_on", "id", from, to);
        copyRows("sheet_holiday", "holiday_date, added_by", "id", from, to);
        copyRows("sheet_void_reason", "reason, added_by", "id", from, to);
        copyRows("claim_template", "name, amount, note, saved_by", "name", from, to);
        copyRows("claim_tag", "tag", "tag", from, to);
        jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date, added_on) SELECT ?, name, from_date, to_date, "
                + "added_on FROM period WHERE sheet_id = ? ORDER BY from_date, name", to, from);
        Map<Long, Long> people = copyPeople(from, to);
        copyPools(from, to, people);
        markForwardRates(to, copyClaims(from, to, people));
    }

    /** Re-mark the clone's forward-only rates by the copies of the claims: each reaches the copies of the claims added
     *  after it (the highest copy of a claim it did not reach; 0 when it reached every claim). */
    private void markForwardRates(String to, Map<Long, Long> ids) {
        for (String table : List.of("rate", "rate_log")) {
            for (Long after : jdbc.queryForList("SELECT DISTINCT after_claim FROM " + table
                    + " WHERE sheet_id = ? AND after_claim IS NOT NULL", Long.class, to)) {
                long marked = ids.entrySet().stream().filter(e -> e.getKey() <= after).mapToLong(Map.Entry::getValue)
                        .max().orElse(0);
                jdbc.update("UPDATE " + table + " SET after_claim = ? WHERE sheet_id = ? AND after_claim = ?", -marked,
                        to, after);
            }
            jdbc.update("UPDATE " + table + " SET after_claim = -after_claim WHERE sheet_id = ? AND after_claim < 0", to);
        }
    }

    /** Copy the sheet's rows of a table, the given columns as they are, to the clone. */
    private void copyRows(String table, String columns, String order, String from, String to) {
        jdbc.update("INSERT INTO " + table + " (sheet_id, " + columns + ") SELECT ?, " + columns + " FROM " + table
                + " WHERE sheet_id = ?" + (order != null ? " ORDER BY " + order : ""), to, from);
    }

    /** Copy the rows of a table belonging to one person to their copy. */
    private void copyPersonRows(String table, String columns, String order, String from, String to, long personId,
            long copyId) {
        jdbc.update("INSERT INTO " + table + " (sheet_id, person_id, " + columns + ") SELECT ?, ?, " + columns + " FROM "
                + table + " WHERE sheet_id = ? AND person_id = ?" + (order != null ? " ORDER BY " + order : ""), to,
                copyId, from, personId);
    }

    private long insert(String sql, Object... args) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, new String[] { "id" });
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }

    /** Copy every person, with what is kept per person; their ids on the clone by their ids here. */
    private Map<Long, Long> copyPeople(String from, String to) {
        Map<Long, Long> ids = new HashMap<>();
        for (Map<String, Object> p : jdbc.queryForList("SELECT * FROM person WHERE sheet_id = ? ORDER BY id", from)) {
            long id = ((Number) p.get("id")).longValue();
            long copy = insert("INSERT INTO person (sheet_id, name, terms, contact, added_on, list_position, origin, "
                    + "retired_from) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", to, p.get("name"), p.get("terms"), p.get("contact"),
                    p.get("added_on"), p.get("list_position"), p.get("origin"), p.get("retired_from"));
            ids.put(id, copy);
            copyPersonRows("person_grade", "grade, from_date", null, from, to, id, copy);
            copyPersonRows("person_grade_log", "grade, from_date, made_on", "id", from, to, id, copy);
            copyPersonRows("person_terms", "from_date, terms_days", null, from, to, id, copy);
            copyPersonRows("person_hold", "from_date, to_date, made_on", "id", from, to, id, copy);
            copyPersonRows("person_retire_log", "from_date, made_on", "id", from, to, id, copy);
            copyPersonRows("person_retirement", "from_date, to_date, made_on", "id", from, to, id, copy);
            copyPersonRows("person_alias", "alias, made_on", "id", from, to, id, copy);
        }
        for (Map<String, Object> link : jdbc.queryForList(
                "SELECT person_id, patron_id, made_on FROM person_patron WHERE sheet_id = ? ORDER BY named", from)) {
            Long person = ids.get(((Number) link.get("person_id")).longValue());
            Long patron = ids.get(((Number) link.get("patron_id")).longValue());
            if (person != null && patron != null) {
                jdbc.update("INSERT INTO person_patron (sheet_id, person_id, patron_id, made_on) VALUES (?, ?, ?, ?)", to,
                        person, patron, link.get("made_on"));
            }
        }
        return ids;
    }

    /** Copy every pool, then their members (people and pools), in the order they were added. */
    private void copyPools(String from, String to, Map<Long, Long> people) {
        Map<Long, Long> pools = new HashMap<>();
        for (Map<String, Object> p : jdbc.queryForList("SELECT * FROM pool WHERE sheet_id = ? ORDER BY id", from)) {
            pools.put(((Number) p.get("id")).longValue(), insert(
                    "INSERT INTO pool (sheet_id, name, added_by, added_on) VALUES (?, ?, ?, ?)", to, p.get("name"),
                    p.get("added_by"), p.get("added_on")));
        }
        for (Map<String, Object> m : jdbc.queryForList("SELECT * FROM pool_member WHERE sheet_id = ? ORDER BY id", from)) {
            Long pool = pools.get(((Number) m.get("pool_id")).longValue());
            Long person = m.get("person_id") == null ? null : people.get(((Number) m.get("person_id")).longValue());
            Long memberPool = m.get("member_pool_id") == null ? null
                    : pools.get(((Number) m.get("member_pool_id")).longValue());
            if (pool == null || (person == null && memberPool == null)) {
                continue;
            }
            jdbc.update("INSERT INTO pool_member (sheet_id, pool_id, person_id, member_pool_id, share, added_by, added_on, "
                    + "from_date, to_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", to, pool, person, memberPool,
                    m.get("share"), m.get("added_by"), m.get("added_on"), m.get("from_date"), m.get("to_date"));
        }
    }

    /** Copy every claim (newest still newest), numbering afresh each that had a number, with its comments, void reason,
     *  log and history; the copy of each claim by its id. */
    private Map<Long, Long> copyClaims(String from, String to, Map<Long, Long> people) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM claim WHERE sheet_id = ? ORDER BY listed", from);
        Map<Long, Long> positions = new HashMap<>();
        long place = 0;
        for (Long id : jdbc.queryForList("SELECT id FROM person WHERE sheet_id = ? ORDER BY list_position, id",
                Long.class, from)) {
            positions.put(id, place++);
        }
        List<Map<String, Object>> numbered = new ArrayList<>(rows.stream()
                .filter(c -> c.get("claim_number") != null && people.containsKey(personOf(c))).toList());
        numbered.sort(Comparator.comparingLong((Map<String, Object> c) -> positions.get(personOf(c))));
        Map<String, String> numbers = new HashMap<>();
        Map<Long, String> numberOf = new HashMap<>();
        for (Map<String, Object> c : numbered) {
            LocalDate dueBy = claims.find(from, idOf(c)).dueBy();
            String number = claims.nextNumber(to, dueBy);
            numbers.putIfAbsent((String) c.get("claim_number"), number);
            numberOf.put(idOf(c), number);
        }
        Map<Long, Long> ids = new HashMap<>();
        for (Map<String, Object> c : rows) {
            Long person = people.get(personOf(c));
            if (person == null) {
                continue;
            }
            long copy = insert("INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, status, claim_number, "
                    + "settled_due, opened_by, corrects, due_from, terms_days, deferred_until, proofs, split_from, "
                    + "late_entry, reminded_on, ref, tags, edited_by, due_by, escalated, review_on, waived, proof_photos, held) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", to, person, c.get("amount"),
                    c.get("claim_date"), c.get("note"), c.get("status"), numberOf.get(idOf(c)), c.get("settled_due"),
                    c.get("opened_by"), renumbered(numbers, c.get("corrects")), c.get("due_from"), c.get("terms_days"),
                    c.get("deferred_until"), c.get("proofs"), renumbered(numbers, c.get("split_from")),
                    c.get("late_entry"), c.get("reminded_on"), c.get("ref"), c.get("tags"), c.get("edited_by"),
                    c.get("due_by"), c.get("escalated"), c.get("review_on"), c.get("waived"), c.get("proof_photos"), c.get("held"));
            ids.put(idOf(c), copy);
        }
        for (var e : ids.entrySet()) {
            long id = e.getKey();
            long copy = e.getValue();
            jdbc.update("INSERT INTO claim_log (sheet_id, claim_id, status, by_user, on_date, due) SELECT ?, ?, status, "
                    + "by_user, on_date, due FROM claim_log WHERE sheet_id = ? AND claim_id = ? ORDER BY id", to, copy,
                    from, id);
            jdbc.update("INSERT INTO claim_comment (sheet_id, claim_id, by_user, on_date, text) SELECT ?, ?, by_user, "
                    + "on_date, text FROM claim_comment WHERE sheet_id = ? AND claim_id = ? ORDER BY id", to, copy, from,
                    id);
            jdbc.update("INSERT INTO claim_line (sheet_id, claim_id, label, qty, unit, on_date) SELECT ?, ?, label, qty, "
                    + "unit, on_date FROM claim_line WHERE sheet_id = ? AND claim_id = ? ORDER BY id", to, copy, from, id);
            jdbc.update("INSERT INTO claim_void_reason (sheet_id, claim_id, reason) SELECT ?, ?, reason FROM "
                    + "claim_void_reason WHERE sheet_id = ? AND claim_id = ?", to, copy, from, id);
        }
        copyHistory(from, to, people, ids, numbers, numberOf);
        return ids;
    }

    /** Copy each claim's history as the clone's own: its person, number and the numbers it names as on the clone, and
     *  bound to no statement; then keep each claim as it stands on the clone now. */
    private void copyHistory(String from, String to, Map<Long, Long> people, Map<Long, Long> ids,
            Map<String, String> numbers, Map<Long, String> numberOf) {
        for (Map<String, Object> h : jdbc.queryForList("SELECT * FROM claim_history WHERE sheet_id = ? ORDER BY id", from)) {
            Long claim = ids.get(((Number) h.get("claim_id")).longValue());
            Long person = people.get(((Number) h.get("person_id")).longValue());
            if (claim == null || person == null) {
                continue;
            }
            String number = h.get("claim_number") == null ? null
                    : numbers.getOrDefault((String) h.get("claim_number"),
                            numberOf.get(((Number) h.get("claim_id")).longValue()));
            jdbc.update("INSERT INTO claim_history (sheet_id, claim_id, person_id, amount, claim_date, note, status, "
                    + "claim_number, settled_due, opened_by, stated, stated_due, corrects, due_from, terms_days, "
                    + "deferred_until, proofs, split_from, late_entry, reminded_on, ref, tags, due_by, made_on, waived, proof_photos, held) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE, NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", to, claim,
                    person, h.get("amount"), h.get("claim_date"), h.get("note"), h.get("status"), number,
                    h.get("settled_due"), h.get("opened_by"), renumbered(numbers, h.get("corrects")), h.get("due_from"),
                    h.get("terms_days"), h.get("deferred_until"), h.get("proofs"), renumbered(numbers, h.get("split_from")),
                    h.get("late_entry"), h.get("reminded_on"), h.get("ref"), h.get("tags"), h.get("due_by"),
                    h.get("made_on"), h.get("waived"), h.get("proof_photos"), h.get("held"));
        }
        jdbc.update("INSERT INTO claim_history (sheet_id, claim_id, person_id, amount, claim_date, note, status, "
                + "claim_number, settled_due, opened_by, stated, stated_due, corrects, due_from, terms_days, deferred_until, "
                + "proofs, split_from, late_entry, reminded_on, ref, tags, due_by, made_on, waived, proof_photos, held) SELECT c.sheet_id, c.id, "
                + "c.person_id, c.amount, c.claim_date, c.note, c.status, c.claim_number, c.settled_due, c.opened_by, "
                + "c.stated, c.stated_due, c.corrects, c.due_from, c.terms_days, c.deferred_until, c.proofs, c.split_from, "
                + "c.late_entry, c.reminded_on, c.ref, c.tags, c.due_by, s.today, c.waived, c.proof_photos, c.held FROM claim c JOIN sheet s ON s.id = "
                + "c.sheet_id WHERE c.sheet_id = ? ORDER BY c.listed", to);
    }

    /** The number as on the clone (null stays null; one no claim had stays as it is). */
    private static String renumbered(Map<String, String> numbers, Object number) {
        return number == null ? null : numbers.getOrDefault((String) number, (String) number);
    }

    private static long idOf(Map<String, Object> claim) {
        return ((Number) claim.get("id")).longValue();
    }

    private static long personOf(Map<String, Object> claim) {
        return ((Number) claim.get("person_id")).longValue();
    }
}

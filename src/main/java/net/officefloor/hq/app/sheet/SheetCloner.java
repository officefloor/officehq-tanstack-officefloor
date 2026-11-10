package net.officefloor.hq.app.sheet;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.history.HistoryRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Copies a sheet to a sheet id never used. */
@Repository
public class SheetCloner {

    private final JdbcTemplate jdbc;
    private final ClaimRepository claims;
    private final HistoryRepository history;

    public SheetCloner(JdbcTemplate jdbc, ClaimRepository claims, HistoryRepository history) {
        this.jdbc = jdbc;
        this.claims = claims;
        this.history = history;
    }

    /** Make sheet {@code to} (made by {@code by}) a copy of sheet {@code from}: its today, settings, roles, rates,
     *  holidays, retired grades, void reasons, tags and templates; its people in list order with their grades, terms, contacts,
     *  aliases, holds, retirements and patrons; its pools and their members; its periods, all open; and its claims,
     *  each person's in the order they are listed, with their status, settled figures, dates, notes, tags, status logs
     *  and comments. Statements are not copied (no claim is bound to one), nor locks, settle batches, watches or
     *  delegations. The clone remembers the sheet it was cloned from, whose rates its grades fall back
     *  to (see {@link net.officefloor.hq.app.settings.SettingsRepository#rateAncestors}). Each claim that had a number takes the clone's next one, in the order the claims were opened (by
     *  the sheet's today when each was opened, then by the order they were added to the clone); a number one of them
     *  corrects or was split from follows it. The clone's history starts with all of this on its today. */
    public void cloneTo(String from, String to, String by) {
        jdbc.update("INSERT INTO sheet (id, created_by, today, policy_date, period_numbering_from, grace_days, entry_date, pot, fiscal_start_month, escalate_after, review_sla, pending_sla, proof_kinds, default_grade, score_mid, score_high, score_log, max_open)"
                + " SELECT ?, ?, today, policy_date, period_numbering_from, grace_days, entry_date, pot, fiscal_start_month, escalate_after, review_sla, pending_sla, proof_kinds, default_grade, score_mid, score_high, score_log, max_open FROM sheet WHERE id = ?",
                to, by, from);
        jdbc.update("UPDATE sheet SET cloned_from = ? WHERE id = ?", from, to);
        jdbc.update("INSERT INTO setting_value (sheet_id, setting_key, from_date, setting) SELECT ?, setting_key, from_date,"
                + " setting FROM setting_value WHERE sheet_id = ?", to, from);
        jdbc.query("SELECT setting_key, from_date, setting FROM setting_value WHERE sheet_id = ? ORDER BY setting_key, from_date",
                rs -> {
                    history.setting(to, rs.getString("setting_key"), rs.getDate("from_date").toLocalDate(), rs.getString("setting"));
                }, to);
        Date policy = jdbc.queryForObject("SELECT policy_date FROM sheet WHERE id = ?", Date.class, to);
        if (policy != null) history.policy(to, policy.toLocalDate());
        jdbc.update("INSERT INTO sheet_role (sheet_id, user_name, role) SELECT ?, user_name, role FROM sheet_role"
                + " WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO sheet_permission (sheet_id, action, roles) SELECT ?, action, roles FROM sheet_permission"
                + " WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO rate (sheet_id, grade, pct, from_date, to_date, version, after_seq) SELECT ?, grade, pct, from_date,"
                + " to_date, version, after_seq FROM rate WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO rate_version (sheet_id, grade, from_date, version, pct, to_date, after_seq) SELECT ?, grade, from_date,"
                + " version, pct, to_date, after_seq FROM rate_version WHERE sheet_id = ?", to, from);
        jdbc.query("SELECT grade, pct, from_date, to_date, after_seq FROM rate WHERE sheet_id = ? ORDER BY grade, from_date NULLS FIRST",
                rs -> {
                    history.rate(to, rs.getString("grade"), rs.getBigDecimal("pct"), local(rs.getDate("from_date")),
                            local(rs.getDate("to_date")), rs.getObject("after_seq", Long.class));
                }, to);
        jdbc.update("INSERT INTO holiday (sheet_id, holiday_date) SELECT ?, holiday_date FROM holiday WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO sheet_grade (sheet_id, grade) SELECT ?, grade FROM sheet_grade WHERE sheet_id = ? ORDER BY id",
                to, from);
        jdbc.update("INSERT INTO retired_grade (sheet_id, grade) SELECT ?, grade FROM retired_grade WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO void_reason (sheet_id, position, reason) SELECT ?, position, reason FROM void_reason"
                + " WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO sheet_tag (sheet_id, tag) SELECT ?, tag FROM sheet_tag WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO claim_template (sheet_id, name, amount, note) SELECT ?, name, amount, note FROM claim_template"
                + " WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date, claim_cap) SELECT ?, name, from_date, to_date, claim_cap FROM period"
                + " WHERE sheet_id = ? ORDER BY id", to, from);

        Map<Long, Long> people = new LinkedHashMap<>();
        for (Long id : jdbc.queryForList("SELECT id FROM person WHERE sheet_id = ? ORDER BY list_position, id", Long.class, from)) {
            people.put(id, copyPerson(from, to, id));
        }
        for (Map.Entry<Long, Long> p : people.entrySet()) {
            jdbc.query("SELECT patron_id FROM patron WHERE sheet_id = ? AND person_id = ? ORDER BY seq", rs -> {
                Long patron = people.get(rs.getLong("patron_id"));
                if (patron != null) {
                    jdbc.update("INSERT INTO patron (sheet_id, person_id, patron_id) VALUES (?, ?, ?)", to, p.getValue(), patron);
                }
            }, from, p.getKey());
        }
        jdbc.update("INSERT INTO pool (sheet_id, name, added) SELECT ?, name, added FROM pool WHERE sheet_id = ?", to, from);
        jdbc.query("SELECT pool_name, person_id, member_pool, share, from_date, to_date FROM pool_member WHERE sheet_id = ?"
                + " ORDER BY id", rs -> {
                    long person = rs.getLong("person_id");
                    Long member = rs.wasNull() ? null : people.get(person);
                    jdbc.update("INSERT INTO pool_member (sheet_id, pool_name, person_id, member_pool, share, from_date, to_date)"
                            + " VALUES (?, ?, ?, ?, ?, ?, ?)", to, rs.getString("pool_name"), member, rs.getString("member_pool"),
                            rs.getBigDecimal("share"), rs.getDate("from_date"), rs.getDate("to_date"));
                }, from);

        Map<Long, Long> copied = new LinkedHashMap<>();
        for (Map.Entry<Long, Long> p : people.entrySet()) {
            for (Long id : jdbc.queryForList("SELECT id FROM claim WHERE sheet_id = ? AND person_id = ? ORDER BY listed, id",
                    Long.class, from, p.getKey())) {
                copied.put(id, copyClaim(from, to, id, p.getValue()));
            }
        }
        number(from, to, copied);
    }

    /** Copy one person (not their statements or watchers); the copy's id. */
    private long copyPerson(String from, String to, long id) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement("INSERT INTO person (sheet_id, name, grade, terms, contact, added_on,"
                    + " list_position, origin_sheet, retired_from, tags) SELECT ?, name, grade, terms, contact, added_on,"
                    + " list_position, origin_sheet, retired_from, tags FROM person WHERE id = ? AND sheet_id = ?",
                    new String[] { "id" });
            ps.setString(1, to);
            ps.setLong(2, id);
            ps.setString(3, from);
            return ps;
        }, keys);
        long made = keys.getKey().longValue();
        jdbc.query("SELECT grade, from_date FROM person_grade WHERE sheet_id = ? AND person_id = ? ORDER BY from_date NULLS FIRST",
                rs -> {
                    LocalDate fromDate = local(rs.getDate("from_date"));
                    jdbc.update("INSERT INTO person_grade (sheet_id, person_id, grade, from_date) VALUES (?, ?, ?, ?)",
                            to, made, rs.getString("grade"), rs.getDate("from_date"));
                    history.grade(to, made, rs.getString("grade"), fromDate);
                }, from, id);
        jdbc.update("INSERT INTO person_terms (sheet_id, person_id, terms, from_date) SELECT ?, ?, terms, from_date"
                + " FROM person_terms WHERE sheet_id = ? AND person_id = ? ORDER BY from_date", to, made, from, id);
        jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias) SELECT ?, ?, alias FROM person_alias"
                + " WHERE sheet_id = ? AND person_id = ? ORDER BY id", to, made, from, id);
        jdbc.update("INSERT INTO person_hold (sheet_id, person_id, from_date, to_date, held_on) SELECT ?, ?, from_date,"
                + " to_date, held_on FROM person_hold WHERE sheet_id = ? AND person_id = ? ORDER BY id", to, made, from, id);
        jdbc.update("INSERT INTO person_retirement (sheet_id, person_id, from_date, to_date) SELECT ?, ?, from_date, to_date"
                + " FROM person_retirement WHERE sheet_id = ? AND person_id = ? ORDER BY id", to, made, from, id);
        Date retired = jdbc.queryForObject("SELECT retired_from FROM person WHERE id = ?", Date.class, made);
        if (retired != null) history.retire(to, made, retired.toLocalDate());
        return made;
    }

    /** Copy one claim to a person on the clone, with its status log, comments and lines, bound to no statement, unlocked,
     *  in no settle batch and (for now) with no number; the copy's id. */
    private long copyClaim(String from, String to, long id, long personId) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement("INSERT INTO claim (sheet_id, person_id, amount, claim_date, note,"
                    + " status, settled_due, opened_by, corrects, due_by, deferred_until, proofs, split_from, late_entry,"
                    + " reminded, escalated, ref, tags, void_reason, edited_by, added_seq, review_day, waived, proofs_photo, held, review_assigned, pending_day) SELECT ?, ?, amount, claim_date, note, status, settled_due,"
                    + " opened_by, corrects, due_by, deferred_until, proofs, split_from, late_entry, reminded, escalated, ref, tags,"
                    + " void_reason, edited_by, added_seq, review_day, waived, proofs_photo, held, review_assigned, pending_day FROM claim WHERE id = ? AND sheet_id = ?", new String[] { "id" });
            ps.setString(1, to);
            ps.setLong(2, personId);
            ps.setLong(3, id);
            ps.setString(4, from);
            return ps;
        }, keys);
        long made = keys.getKey().longValue();
        jdbc.update("INSERT INTO claim_log (sheet_id, claim_id, status, by_user, on_date, due) SELECT ?, ?, status,"
                + " by_user, on_date, due FROM claim_log WHERE sheet_id = ? AND claim_id = ? ORDER BY id", to, made, from, id);
        jdbc.update("INSERT INTO claim_comment (sheet_id, claim_id, by_user, on_date, text) SELECT ?, ?, by_user,"
                + " on_date, text FROM claim_comment WHERE sheet_id = ? AND claim_id = ? ORDER BY id", to, made, from, id);
        jdbc.update("INSERT INTO claim_line (sheet_id, claim_id, label, qty, unit) SELECT ?, ?, label, qty, unit"
                + " FROM claim_line WHERE sheet_id = ? AND claim_id = ? ORDER BY id", to, made, from, id);
        return made;
    }

    /** Number afresh, by the clone's own numbering, the copies of the claims that had a number, then point the
     *  corrects and split-from numbers at the new ones. */
    private void number(String from, String to, Map<Long, Long> copied) {
        Map<Long, String> was = new LinkedHashMap<>();
        jdbc.query("SELECT id, claim_no FROM claim WHERE sheet_id = ? AND claim_no IS NOT NULL", rs -> {
            was.put(rs.getLong("id"), rs.getString("claim_no"));
        }, from);
        Map<Long, String> wasOfCopy = new LinkedHashMap<>();
        for (Map.Entry<Long, Long> e : copied.entrySet()) {
            if (was.containsKey(e.getKey())) wasOfCopy.put(e.getValue(), was.get(e.getKey()));
        }
        List<Object[]> numbered = new ArrayList<>();
        jdbc.query("SELECT c.id, c.due_by FROM claim c WHERE c.sheet_id = ? ORDER BY (SELECT MIN(l.on_date) FROM claim_log l"
                + " WHERE l.sheet_id = c.sheet_id AND l.claim_id = c.id AND l.status = 'OPEN') NULLS LAST, c.id", rs -> {
                    if (wasOfCopy.containsKey(rs.getLong("id"))) {
                        numbered.add(new Object[] { rs.getLong("id"), rs.getDate("due_by").toLocalDate() });
                    }
                }, to);
        boolean perPeriod = claims.numbersPerPeriod(to);
        Map<String, String> numbers = new LinkedHashMap<>();
        for (Object[] c : numbered) {
            long made = (Long) c[0];
            numbers.put(wasOfCopy.get(made), perPeriod ? claims.numberInPeriod(to, made, (LocalDate) c[1]) : claims.number(to, made, (LocalDate) c[1]));
        }
        for (Long made : copied.values()) {
            Map<String, Object> links = jdbc.queryForMap("SELECT corrects, split_from FROM claim WHERE id = ?", made);
            String corrects = (String) links.get("corrects");
            String splitFrom = (String) links.get("split_from");
            jdbc.update("UPDATE claim SET corrects = ?, split_from = ? WHERE id = ?",
                    corrects == null ? null : numbers.getOrDefault(corrects, corrects),
                    splitFrom == null ? null : numbers.getOrDefault(splitFrom, splitFrom), made);
            history.claim(to, made);
        }
    }

    private static LocalDate local(Date d) {
        return d == null ? null : d.toLocalDate();
    }
}

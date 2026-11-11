package net.officefloor.hq.app.sheet;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Copies a sheet to a new id: everything but its claims (see {@code ClaimRepository#copy}). What the
 * copy is given is kept in its histories as given on its today.
 */
@Repository
public class SheetCloneRepository {

    private final JdbcTemplate jdbc;

    public SheetCloneRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Create sheet {@code to} as a copy of sheet {@code from}: its today and settings (dated and undated,
     * its permissions and parent; it names this sheet as the one it was cloned from), roles, rates, holidays, void reasons and templates; its people (not those
     * merged away) in list order, with their grades, terms, contacts, tags, aliases, holds, retirements and
     * patrons, but no statements; its pools and their members; its periods, all open. It is not archived,
     * and its claim numbers, settle batches and period numbers start again. Returns the copy's person id
     * for each person copied, by their id here.
     */
    public Map<Long, Long> copy(String from, String to) {
        jdbc.update("INSERT INTO sheet (id, today, clerk_limit, due_floor, terms, late_pct, policy_date,"
                + " period_numbering_from, cap, grace_days, entry_date, pot, parent_id, fiscal_start_month, alert_threshold, escalate_after, review_sla, score_mid, score_high, score_log, max_open, pending_sla, cloned_from)"
                + " SELECT ?, today, clerk_limit, due_floor, terms, late_pct, policy_date, period_numbering_from, cap,"
                + " grace_days, entry_date, pot, parent_id, fiscal_start_month, alert_threshold, escalate_after, review_sla, score_mid, score_high, score_log, max_open, pending_sla, id FROM sheet WHERE id = ?", to, from);
        java.sql.Date today = jdbc.queryForObject("SELECT today FROM sheet WHERE id = ?", java.sql.Date.class, to);

        jdbc.update("INSERT INTO setting_value (sheet_id, setting_key, from_date, setting_value)"
                + " SELECT ?, setting_key, from_date, setting_value FROM setting_value WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO setting_history (sheet_id, setting_key, from_date, setting_value, set_on)"
                + " SELECT sheet_id, setting_key, from_date, setting_value, ?"
                + " FROM setting_value WHERE sheet_id = ? ORDER BY setting_key, from_date", today, to);
        jdbc.update("INSERT INTO setting_history (sheet_id, setting_key, from_date, setting_value, set_on)"
                + " SELECT id, 'policy-date', NULL, COALESCE(CAST(policy_date AS VARCHAR), ''), today FROM sheet WHERE id = ?", to);
        jdbc.update("INSERT INTO setting_history (sheet_id, setting_key, from_date, setting_value, set_on)"
                + " SELECT id, 'pot', NULL, COALESCE(CAST(pot AS VARCHAR), ''), today FROM sheet WHERE id = ?", to);
        jdbc.update("INSERT INTO setting_perm (sheet_id, action, roles) SELECT ?, action, roles FROM setting_perm"
                + " WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO grade_terms (sheet_id, grade, terms) SELECT ?, grade, terms FROM grade_terms"
                + " WHERE sheet_id = ?", to, from);

        jdbc.update("INSERT INTO sheet_role (sheet_id, user_name, role) SELECT ?, user_name, role FROM sheet_role"
                + " WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO role_history (sheet_id, user_name, role, set_on)"
                + " SELECT sheet_id, user_name, role, ? FROM sheet_role WHERE sheet_id = ? ORDER BY user_name", today, to);
        jdbc.update("INSERT INTO rate (sheet_id, grade, pct, from_date, to_date, version, added_after)"
                + " SELECT ?, grade, pct, from_date, to_date, version, added_after FROM rate WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO rate_version (sheet_id, grade, from_date, version, pct, to_date, added_after)"
                + " SELECT ?, grade, from_date, version, pct, to_date, added_after FROM rate_version WHERE sheet_id = ?"
                + " ORDER BY id", to, from);
        jdbc.update("INSERT INTO rate_history (sheet_id, grade, pct, from_date, to_date, set_on, added_after)"
                + " SELECT sheet_id, grade, pct, from_date, to_date, ?, added_after"
                + " FROM rate WHERE sheet_id = ? ORDER BY grade, from_date NULLS FIRST", today, to);
        jdbc.update("INSERT INTO sheet_holiday (sheet_id, holiday_date) SELECT ?, holiday_date FROM sheet_holiday"
                + " WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO grade_retired (sheet_id, grade) SELECT ?, grade FROM grade_retired"
                + " WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO sheet_void_reason (sheet_id, reason) SELECT ?, reason FROM sheet_void_reason"
                + " WHERE sheet_id = ? ORDER BY id", to, from);
        jdbc.update("INSERT INTO claim_template (sheet_id, name, amount, note) SELECT ?, name, amount, note"
                + " FROM claim_template WHERE sheet_id = ? ORDER BY id", to, from);
        jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date, cap, added_on)"
                + " SELECT ?, name, from_date, to_date, cap, added_on FROM period WHERE sheet_id = ?", to, from);

        Map<Long, Long> people = new HashMap<>();
        List<Long> ids = jdbc.queryForList("SELECT id FROM person WHERE sheet_id = ? AND merged_into IS NULL"
                + " ORDER BY list_pos, id", Long.class, from);
        long pos = 0;
        for (long id : ids) {
            long at = ++pos;
            GeneratedKeyHolder keys = new GeneratedKeyHolder();
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement("INSERT INTO person (sheet_id, name, grade, terms, contact, tags,"
                        + " retired_from, added_on, origin_sheet, list_pos)"
                        + " SELECT ?, name, grade, terms, contact, tags, retired_from, added_on, origin_sheet, ?"
                        + " FROM person WHERE id = ? AND sheet_id = ?", Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, to);
                ps.setLong(2, at);
                ps.setLong(3, id);
                ps.setString(4, from);
                return ps;
            }, keys);
            people.put(id, keys.getKey().longValue());
        }
        for (long id : ids) {
            long copy = people.get(id);
            jdbc.update("INSERT INTO person_grade (sheet_id, person_id, from_date, grade)"
                    + " SELECT ?, ?, from_date, grade FROM person_grade WHERE person_id = ?", to, copy, id);
            jdbc.update("INSERT INTO person_grade_history (sheet_id, person_id, from_date, grade, set_on)"
                    + " SELECT sheet_id, id, NULL, grade, ? FROM person WHERE id = ?", today, copy);
            jdbc.update("INSERT INTO person_grade_history (sheet_id, person_id, from_date, grade, set_on)"
                    + " SELECT sheet_id, person_id, from_date, grade, ?"
                    + " FROM person_grade WHERE person_id = ? ORDER BY from_date", today, copy);
            jdbc.update("INSERT INTO person_terms (sheet_id, person_id, from_date, terms)"
                    + " SELECT ?, ?, from_date, terms FROM person_terms WHERE person_id = ?", to, copy, id);
            jdbc.update("INSERT INTO person_terms_history (sheet_id, person_id, terms, set_on)"
                    + " SELECT sheet_id, id, terms, ? FROM person WHERE id = ?", today, copy);
            jdbc.update("INSERT INTO person_terms_history (sheet_id, person_id, from_date, terms, set_on)"
                    + " SELECT sheet_id, person_id, from_date, terms, ?"
                    + " FROM person_terms WHERE person_id = ? ORDER BY from_date", today, copy);
            jdbc.update("INSERT INTO person_retirement (sheet_id, person_id, from_date, set_on)"
                    + " SELECT sheet_id, id, retired_from, ? FROM person WHERE id = ? AND retired_from IS NOT NULL",
                    today, copy);
            jdbc.update("INSERT INTO person_past_retirement (sheet_id, person_id, from_date, to_date)"
                    + " SELECT ?, ?, from_date, to_date FROM person_past_retirement WHERE person_id = ? ORDER BY id",
                    to, copy, id);
            jdbc.update("INSERT INTO person_snapshot (sheet_id, person_id, snapshot_day, due_total)"
                    + " SELECT ?, ?, snapshot_day, due_total FROM person_snapshot WHERE person_id = ? ORDER BY id",
                    to, copy, id);
            jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias, added_on)"
                    + " SELECT ?, ?, alias, added_on FROM person_alias WHERE person_id = ? ORDER BY id", to, copy, id);
            jdbc.update("INSERT INTO person_hold (sheet_id, person_id, from_date, to_date, made_on)"
                    + " SELECT ?, ?, from_date, to_date, made_on FROM person_hold WHERE person_id = ? ORDER BY id",
                    to, copy, id);
        }
        // Patrons in the order each person named them.
        jdbc.query("SELECT person_id, patron_id, added_on FROM person_patron WHERE sheet_id = ?"
                + " ORDER BY person_id, named_seq NULLS FIRST, patron_id", (rs) -> {
                    Long person = people.get(rs.getLong("person_id"));
                    Long patron = people.get(rs.getLong("patron_id"));
                    if (person == null || patron == null) return;
                    jdbc.update("INSERT INTO person_patron (sheet_id, person_id, patron_id, added_on, named_seq)"
                            + " VALUES (?, ?, ?, ?, NEXT VALUE FOR person_patron_seq)", to, person, patron,
                            rs.getDate("added_on"));
                }, from);

        Map<Long, Long> pools = new HashMap<>();
        jdbc.query("SELECT id, name, added_on FROM pool WHERE sheet_id = ? ORDER BY id", (rs) -> {
            long id = rs.getLong("id");
            String name = rs.getString("name");
            java.sql.Date addedOn = rs.getDate("added_on");
            GeneratedKeyHolder keys = new GeneratedKeyHolder();
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement("INSERT INTO pool (sheet_id, name, added_on) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, to);
                ps.setString(2, name);
                ps.setDate(3, addedOn);
                return ps;
            }, keys);
            pools.put(id, keys.getKey().longValue());
        }, from);
        jdbc.query("SELECT m.pool_id, m.person_id, m.member_pool_id, m.share, m.from_date, m.to_date, m.added_on FROM pool_member m"
                + " JOIN pool p ON p.id = m.pool_id WHERE p.sheet_id = ? ORDER BY m.id", (rs) -> {
                    Long pool = pools.get(rs.getLong("pool_id"));
                    Long person = rs.getObject("person_id") == null ? null : people.get(rs.getLong("person_id"));
                    Long member = rs.getObject("member_pool_id") == null ? null : pools.get(rs.getLong("member_pool_id"));
                    if (pool == null || (person == null && member == null)) return;
                    jdbc.update("INSERT INTO pool_member (pool_id, person_id, member_pool_id, share, from_date, to_date, added_on)"
                            + " VALUES (?, ?, ?, ?, ?, ?, ?)", pool, person, member, rs.getBigDecimal("share"),
                            rs.getDate("from_date"), rs.getDate("to_date"), rs.getDate("added_on"));
                }, from);
        return people;
    }
}

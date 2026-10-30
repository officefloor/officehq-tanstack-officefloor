package net.officefloor.hq.app.sheet;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Copies a sheet's set-up and people to a sheet never used: its today, settings (but not its parent), roles,
 * delegations, permissions, rates, holidays, void reasons, templates, tags, periods (all open), pools and people
 * (grades, terms, contacts, aliases, holds, retirements, patrons), with the history the as-of view reads. Statements,
 * watches and fixed period figures are not copied. The claims are copied by {@link CloneSheet}.
 */
@Repository
public class SheetCloner {

    private final JdbcTemplate jdbc;

    public SheetCloner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Copies the sheet to the other; returns each person's id on the clone by their id on the sheet, in list order. */
    public Map<Long, Long> copy(String from, String to) {
        jdbc.update("INSERT INTO sheet (id, today, policy_date, period_numbering_from, grace_days, entry_date, pot, last_settle_batch)"
                + " SELECT ?, today, policy_date, period_numbering_from, grace_days, entry_date, pot, last_settle_batch"
                + " FROM sheet WHERE id = ?", to, from);
        copyRows("sheet_setting", "setting_key, from_date, setting_value", "", from, to);
        copyRows("setting_history", "set_on, setting_key, from_date, setting_value", " ORDER BY seq", from, to);
        copyRows("policy_history", "set_on, policy_date", " ORDER BY seq", from, to);
        copyRows("plain_setting_history", "set_on, setting_key, setting_value",
                " AND setting_key <> 'parent' ORDER BY seq", from, to);
        copyRows("sheet_role", "user_name, role", "", from, to);
        copyRows("sheet_permission", "action, roles", "", from, to);
        copyRows("sheet_delegation", "user_name, from_date, to_date, delegated_by, set_on", " ORDER BY seq", from, to);
        copyRows("rate", "grade, rate, from_date, to_date", "", from, to);
        copyRows("rate_history", "set_on, grade, rate, from_date, to_date", " ORDER BY seq", from, to);
        copyRows("holiday", "holiday_date", "", from, to);
        copyRows("void_reason", "reason", " ORDER BY seq", from, to);
        copyRows("claim_template", "name, amount, note", "", from, to);
        copyRows("sheet_tag", "tag", "", from, to);
        copyRows("period", "name, from_date, to_date, added_on", "", from, to);
        copyRows("pool", "name", " ORDER BY seq", from, to);

        Map<Long, Long> ids = new LinkedHashMap<>();
        for (Long id : jdbc.queryForList("SELECT id FROM person WHERE sheet_id = ? ORDER BY place, id", Long.class, from)) {
            ids.put(id, copyPerson(from, id, to));
        }
        for (Map.Entry<Long, Long> person : ids.entrySet()) {
            String[] personCols = {
                "person_grade:from_date, grade",
                "grade_history:set_on, from_date, grade",
                "person_history:set_on, name, terms, contact",
                "retire_history:set_on, retired_from",
                "person_hold:from_date, to_date, set_on",
                "person_retirement:from_date, to_date, set_on",
                "person_snapshot:due_total, set_on",
                "person_alias:alias, set_on",
            };
            for (String table : personCols) {
                String[] parts = table.split(":");
                jdbc.update("INSERT INTO " + parts[0] + " (sheet_id, person_id, " + parts[1] + ") SELECT ?, ?, " + parts[1]
                        + " FROM " + parts[0] + " WHERE sheet_id = ? AND person_id = ? ORDER BY seq",
                        to, person.getValue(), from, person.getKey());
            }
            jdbc.query("SELECT patron_id, set_on FROM person_patron WHERE sheet_id = ? AND person_id = ? ORDER BY seq", rs -> {
                Long patron = ids.get(rs.getLong(1));
                if (patron != null) {
                    jdbc.update("INSERT INTO person_patron (sheet_id, person_id, patron_id, set_on) VALUES (?, ?, ?, ?)",
                            to, person.getValue(), patron, rs.getDate(2));
                }
            }, from, person.getKey());
        }
        jdbc.query("SELECT pool_name, person_id, member_pool, share, from_date, to_date FROM pool_member WHERE sheet_id = ? ORDER BY seq",
                rs -> {
                    long personId = rs.getLong(2);
                    Long person = rs.wasNull() ? null : ids.get(personId);
                    String memberPool = rs.getString(3);
                    if (person == null && memberPool == null) {
                        return;
                    }
                    jdbc.update("INSERT INTO pool_member (sheet_id, pool_name, person_id, member_pool, share, from_date, to_date)"
                            + " VALUES (?, ?, ?, ?, ?, ?, ?)", to, rs.getString(1), person, memberPool, rs.getBigDecimal(4),
                            rs.getDate(5), rs.getDate(6));
                }, from);
        return ids;
    }

    /** Copies the sheet's rows of the table (the columns after sheet_id), the tail filtering and ordering them. */
    private void copyRows(String table, String columns, String tail, String from, String to) {
        jdbc.update("INSERT INTO " + table + " (sheet_id, " + columns + ") SELECT ?, " + columns + " FROM " + table
                + " WHERE sheet_id = ?" + tail, to, from);
    }

    /** Copies the person (no statement taken), placed after everyone on the clone; returns their id there. */
    private long copyPerson(String from, long id, String to) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade, retired_from, added_on, terms, contact, tags, origin)"
                            + " SELECT ?, name, grade, retired_from, added_on, terms, contact, tags, origin FROM person"
                            + " WHERE sheet_id = ? AND id = ?",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, to);
            ps.setString(2, from);
            ps.setLong(3, id);
            return ps;
        }, keys);
        long copied = keys.getKey().longValue();
        jdbc.update("UPDATE person SET place = id WHERE id = ?", copied);
        return copied;
    }
}

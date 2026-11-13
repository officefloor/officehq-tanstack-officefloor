package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.sql.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.Rounding;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SettingRepository {

    /** The floor a sheet starts with. */
    public static final BigDecimal DEFAULT_FLOOR = new BigDecimal("1.00");

    /** The terms a sheet starts with, in days. */
    public static final int DEFAULT_TERMS = 14;

    /** The late percent a sheet starts with. */
    public static final BigDecimal DEFAULT_LATE_PCT = new BigDecimal("0.00");

    private final JdbcTemplate jdbc;

    public SettingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's settings: the dated ones in force on its today (and every value recorded), its policy date, its period numbering from date and its roles, by user name. */
    public Settings find(String sheetId) {
        List<UserRole> roles = jdbc.query("SELECT sheet_id, user_name, role FROM user_role WHERE sheet_id = ?"
                + " ORDER BY user_name", (rs, n) -> new UserRole(rs.getString("sheet_id"), rs.getString("user_name"),
                        Role.valueOf(rs.getString("role"))), sheetId);
        List<Date> found = jdbc.query("SELECT today FROM sheet WHERE id = ?", (rs, n) -> rs.getDate("today"), sheetId);
        LocalDate today = found.isEmpty() || found.get(0) == null ? LocalDate.now() : found.get(0).toLocalDate();
        DatedSettings dated = dated(sheetId);
        return new Settings(sheetId, dated.clerkLimitOn(today), dated.floorOn(today), dated.termsOn(today),
                dated.gradeTermsOn(today), dated.latePctOn(today), policyDate(sheetId), periodNumberingFrom(sheetId),
                dated.capOn(today), dated.proofThresholdOn(today), roles, dated, permissions(sheetId));
    }

    /** The sheet's dated settings: every value recorded, by key and from-date. */
    public DatedSettings dated(String sheetId) {
        Map<String, NavigableMap<LocalDate, String>> values = new LinkedHashMap<>();
        jdbc.query("SELECT setting_key, from_date, setting_value FROM setting_from WHERE sheet_id = ?"
                + " ORDER BY setting_key, from_date", (rs) -> {
                    values.computeIfAbsent(rs.getString("setting_key"), (k) -> new TreeMap<>())
                            .put(rs.getDate("from_date").toLocalDate(), rs.getString("setting_value"));
                }, sheetId);
        return new DatedSettings(values);
    }

    /** Record the key's value from the date (null: blank), replacing any value already recorded from that date. */
    public void saveDated(String sheetId, String key, LocalDate from, String value) {
        jdbc.update("DELETE FROM setting_from WHERE sheet_id = ? AND setting_key = ? AND from_date = ?",
                sheetId, key, Date.valueOf(from));
        jdbc.update("INSERT INTO setting_from (sheet_id, setting_key, from_date, setting_value) VALUES (?, ?, ?, ?)",
                sheetId, key, Date.valueOf(from), value);
    }

    /** The sheet's rounding policy date as set (null: blank, the sheet's today). */
    public LocalDate policyDate(String sheetId) {
        List<Date> date = jdbc.query("SELECT policy_date FROM sheet WHERE id = ?",
                (rs, n) -> rs.getDate("policy_date"), sheetId);
        return date.isEmpty() || date.get(0) == null ? null : date.get(0).toLocalDate();
    }

    /** How the sheet rounds the given claims' live figures on the given today: by its policy date (blank: today). */
    public Rounding rounding(String sheetId, LocalDate today, List<Claim> claims) {
        LocalDate policyDate = policyDate(sheetId);
        return Rounding.of(policyDate == null ? today : policyDate, claims);
    }

    /** Set the sheet's rounding policy date (null: blank, the sheet's today). */
    public void savePolicyDate(String sheetId, LocalDate policyDate) {
        jdbc.update("UPDATE sheet SET policy_date = ? WHERE id = ?", policyDate == null ? null : Date.valueOf(policyDate), sheetId);
    }

    /** The sheet's period numbering from date (null: blank, off). */
    public LocalDate periodNumberingFrom(String sheetId) {
        List<Date> date = jdbc.query("SELECT period_numbering_from FROM sheet WHERE id = ?",
                (rs, n) -> rs.getDate("period_numbering_from"), sheetId);
        return date.isEmpty() || date.get(0) == null ? null : date.get(0).toLocalDate();
    }

    /** Set the sheet's period numbering from date (null: blank, off). */
    public void savePeriodNumberingFrom(String sheetId, LocalDate from) {
        jdbc.update("UPDATE sheet SET period_numbering_from = ? WHERE id = ?", from == null ? null : Date.valueOf(from), sheetId);
    }

    /** The sheet's permission matrix: the roles set for each action, as shown (an action with none keeps the standing rule). */
    public Permissions permissions(String sheetId) {
        Map<String, String> set = new LinkedHashMap<>();
        jdbc.query("SELECT action, roles FROM permission WHERE sheet_id = ?", (rs) -> {
            set.put(rs.getString("action"), rs.getString("roles"));
        }, sheetId);
        return new Permissions(set);
    }

    /** Set the roles allowed the action on the sheet ("": blank, back to the standing rule). */
    public void savePermission(String sheetId, String action, String roles) {
        jdbc.update("DELETE FROM permission WHERE sheet_id = ? AND action = ?", sheetId, action);
        if (!roles.isEmpty()) {
            jdbc.update("INSERT INTO permission (sheet_id, action, roles) VALUES (?, ?, ?)", sheetId, action, roles);
        }
    }

    /** Give the user name its role on the sheet, replacing any it had. */
    public void saveRole(UserRole role) {
        int updated = jdbc.update("UPDATE user_role SET role = ? WHERE sheet_id = ? AND user_name = ?",
                role.role().name(), role.sheetId(), role.user());
        if (updated == 0) {
            jdbc.update("INSERT INTO user_role (sheet_id, user_name, role) VALUES (?, ?, ?)",
                    role.sheetId(), role.user(), role.role().name());
        }
    }
}

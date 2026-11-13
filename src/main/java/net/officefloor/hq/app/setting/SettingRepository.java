package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.sql.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.LateCharge;
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

    /** The sheet's settings: its clerk limit, its floor, its terms and its roles, by user name. */
    public Settings find(String sheetId) {
        List<BigDecimal> limit = jdbc.query("SELECT clerk_limit FROM sheet WHERE id = ?",
                (rs, n) -> rs.getBigDecimal("clerk_limit"), sheetId);
        List<UserRole> roles = jdbc.query("SELECT sheet_id, user_name, role FROM user_role WHERE sheet_id = ?"
                + " ORDER BY user_name", (rs, n) -> new UserRole(rs.getString("sheet_id"), rs.getString("user_name"),
                        Role.valueOf(rs.getString("role"))), sheetId);
        BigDecimal clerkLimit = limit.isEmpty() || limit.get(0) == null ? null : limit.get(0).setScale(2);
        return new Settings(sheetId, clerkLimit, floor(sheetId), terms(sheetId), gradeTerms(sheetId), latePct(sheetId), policyDate(sheetId), periodNumberingFrom(sheetId), cap(sheetId), roles);
    }

    /** The sheet's floor: a claim's live due is at least it (1.00 for a sheet not yet stored). */
    public BigDecimal floor(String sheetId) {
        List<BigDecimal> floor = jdbc.query("SELECT due_floor FROM sheet WHERE id = ?",
                (rs, n) -> rs.getBigDecimal("due_floor"), sheetId);
        return floor.isEmpty() || floor.get(0) == null ? DEFAULT_FLOOR : floor.get(0).setScale(2);
    }

    /** The sheet's terms in days: a claim is due by its date plus them (14 for a sheet not yet stored). */
    public int terms(String sheetId) {
        List<Integer> terms = jdbc.query("SELECT terms FROM sheet WHERE id = ?", (rs, n) -> rs.getInt("terms"), sheetId);
        return terms.isEmpty() ? DEFAULT_TERMS : terms.get(0);
    }

    /** The terms set for each grade that has its own, by grade. */
    public Map<String, Integer> gradeTerms(String sheetId) {
        Map<String, Integer> terms = new LinkedHashMap<>();
        jdbc.query("SELECT grade, terms FROM grade_terms WHERE sheet_id = ? ORDER BY grade",
                (rs) -> { terms.put(rs.getString("grade"), rs.getInt("terms")); }, sheetId);
        return terms;
    }

    /** The terms in days for a claim of the grade: the grade's own, else the sheet's. */
    public int termsFor(String sheetId, String grade) {
        List<Integer> terms = jdbc.query("SELECT terms FROM grade_terms WHERE sheet_id = ? AND grade = ?",
                (rs, n) -> rs.getInt("terms"), sheetId, grade);
        return terms.isEmpty() ? terms(sheetId) : terms.get(0);
    }

    /** Set the grade's own terms (null: none, the grade uses the sheet's terms). */
    public void saveGradeTerms(String sheetId, String grade, Integer terms) {
        jdbc.update("DELETE FROM grade_terms WHERE sheet_id = ? AND grade = ?", sheetId, grade);
        if (terms != null) {
            jdbc.update("INSERT INTO grade_terms (sheet_id, grade, terms) VALUES (?, ?, ?)", sheetId, grade, terms);
        }
    }

    /** The sheet's late percent: an overdue claim gains it of its figure every full 30 days (0.00 for a sheet not yet stored). */
    public BigDecimal latePct(String sheetId) {
        List<BigDecimal> pct = jdbc.query("SELECT late_pct FROM sheet WHERE id = ?",
                (rs, n) -> rs.getBigDecimal("late_pct"), sheetId);
        return pct.isEmpty() || pct.get(0) == null ? DEFAULT_LATE_PCT : pct.get(0).setScale(2);
    }

    /** What the sheet charges an overdue claim on the given today: its late percent. */
    public LateCharge lateCharge(String sheetId, LocalDate today) {
        return new LateCharge(latePct(sheetId), today);
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

    /** Set the sheet's late percent. */
    public void saveLatePct(String sheetId, BigDecimal latePct) {
        jdbc.update("UPDATE sheet SET late_pct = ? WHERE id = ?", latePct, sheetId);
    }

    /** Set the sheet's terms. */
    public void saveTerms(String sheetId, int terms) {
        jdbc.update("UPDATE sheet SET terms = ? WHERE id = ?", terms, sheetId);
    }

    /** Set the sheet's floor. */
    public void saveFloor(String sheetId, BigDecimal floor) {
        jdbc.update("UPDATE sheet SET due_floor = ? WHERE id = ?", floor, sheetId);
    }

    /** The sheet's cap: a person's due-total is at most it (null: no cap). */
    public BigDecimal cap(String sheetId) {
        List<BigDecimal> cap = jdbc.query("SELECT due_cap FROM sheet WHERE id = ?",
                (rs, n) -> rs.getBigDecimal("due_cap"), sheetId);
        return cap.isEmpty() || cap.get(0) == null ? null : cap.get(0).setScale(2);
    }

    /** Set the sheet's cap (null: no cap). */
    public void saveCap(String sheetId, BigDecimal cap) {
        jdbc.update("UPDATE sheet SET due_cap = ? WHERE id = ?", cap, sheetId);
    }

    /** Set the sheet's clerk limit (null: no limit). */
    public void saveClerkLimit(String sheetId, BigDecimal clerkLimit) {
        jdbc.update("UPDATE sheet SET clerk_limit = ? WHERE id = ?", clerkLimit, sheetId);
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

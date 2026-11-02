package net.officefloor.hq.app.settings;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SettingsRepository {
    public static final String CLERK = "clerk";
    public static final String STEWARD = "steward";

    private final JdbcTemplate jdbc;

    public SettingsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean sheetExists(String sheetId) {
        Integer known = jdbc.queryForObject("SELECT COUNT(*) FROM sheet WHERE id = ?", Integer.class, sheetId);
        return known != null && known > 0;
    }

    /** The sheet's settings and roles, roles by user name. */
    public Settings load(String sheetId) {
        List<Role> roles = jdbc.query("SELECT user_name, role FROM sheet_role WHERE sheet_id = ? ORDER BY user_name",
                (rs, i) -> new Role(rs.getString("user_name"), rs.getString("role")), sheetId);
        BigDecimal limit = clerkLimit(sheetId);
        LocalDate policy = policyDate(sheetId);
        LocalDate numbering = periodNumberingFrom(sheetId);
        return new Settings(limit == null ? null : money(limit), money(floor(sheetId)), terms(sheetId),
                money(latePct(sheetId)), roles, policy == null ? null : policy.toString(),
                numbering == null ? null : numbering.toString(), gradeTerms(sheetId));
    }

    /** The sheet's clerk limit; null when it has none. */
    public BigDecimal clerkLimit(String sheetId) {
        List<BigDecimal> found = jdbc.query("SELECT clerk_limit FROM sheet WHERE id = ?",
                (rs, i) -> rs.getBigDecimal("clerk_limit"), sheetId);
        return found.isEmpty() ? null : found.get(0);
    }

    public void setClerkLimit(String sheetId, BigDecimal limit) {
        jdbc.update("UPDATE sheet SET clerk_limit = ? WHERE id = ?", limit, sheetId);
    }

    /** The sheet's floor: the least a live due can be; 1.00 unless set otherwise. */
    public BigDecimal floor(String sheetId) {
        List<BigDecimal> found = jdbc.query("SELECT floor FROM sheet WHERE id = ?",
                (rs, i) -> rs.getBigDecimal("floor"), sheetId);
        return found.isEmpty() || found.get(0) == null ? BigDecimal.ONE : found.get(0);
    }

    public void setFloor(String sheetId, BigDecimal floor) {
        jdbc.update("UPDATE sheet SET floor = ? WHERE id = ?", floor, sheetId);
    }

    /** The sheet's terms: the days after a claim's date that it is due by; 14 unless set otherwise. */
    public int terms(String sheetId) {
        List<Integer> found = jdbc.query("SELECT terms FROM sheet WHERE id = ?",
                (rs, i) -> rs.getInt("terms"), sheetId);
        return found.isEmpty() ? 14 : found.get(0);
    }

    public void setTerms(String sheetId, int terms) {
        jdbc.update("UPDATE sheet SET terms = ? WHERE id = ?", terms, sheetId);
    }

    /** Each grade's own terms on the sheet, by grade, in grade order. */
    public Map<String, Integer> gradeTerms(String sheetId) {
        Map<String, Integer> terms = new LinkedHashMap<>();
        jdbc.query("SELECT grade, terms FROM grade_terms WHERE sheet_id = ? ORDER BY grade",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> terms.put(rs.getString("grade"), rs.getInt("terms")),
                sheetId);
        return terms;
    }

    /** Set a grade's own terms; null removes them, the grade then using the sheet's terms. */
    public void setGradeTerms(String sheetId, String grade, Integer terms) {
        if (terms == null) {
            jdbc.update("DELETE FROM grade_terms WHERE sheet_id = ? AND grade = ?", sheetId, grade);
        } else {
            jdbc.update("MERGE INTO grade_terms (sheet_id, grade, terms) KEY (sheet_id, grade) VALUES (?, ?, ?)",
                    sheetId, grade, terms);
        }
    }

    /** The sheet's late percent: what an overdue claim's due gains, of its figure, for every full 30 days
     *  it is overdue; 0.00 unless set otherwise. */
    public BigDecimal latePct(String sheetId) {
        List<BigDecimal> found = jdbc.query("SELECT late_pct FROM sheet WHERE id = ?",
                (rs, i) -> rs.getBigDecimal("late_pct"), sheetId);
        return found.isEmpty() || found.get(0) == null ? BigDecimal.ZERO : found.get(0);
    }

    public void setLatePct(String sheetId, BigDecimal pct) {
        jdbc.update("UPDATE sheet SET late_pct = ? WHERE id = ?", pct, sheetId);
    }

    /** The sheet's policy date; null when blank (the sheet's today is then the policy date). */
    public LocalDate policyDate(String sheetId) {
        List<Date> found = jdbc.query("SELECT policy_date FROM sheet WHERE id = ?",
                (rs, i) -> rs.getDate("policy_date"), sheetId);
        return found.isEmpty() || found.get(0) == null ? null : found.get(0).toLocalDate();
    }

    public void setPolicyDate(String sheetId, LocalDate policyDate) {
        jdbc.update("UPDATE sheet SET policy_date = ? WHERE id = ?", policyDate == null ? null : Date.valueOf(policyDate),
                sheetId);
    }

    /** The sheet's period numbering from-date; null when blank (period numbering is off). */
    public LocalDate periodNumberingFrom(String sheetId) {
        List<Date> found = jdbc.query("SELECT period_numbering_from FROM sheet WHERE id = ?",
                (rs, i) -> rs.getDate("period_numbering_from"), sheetId);
        return found.isEmpty() || found.get(0) == null ? null : found.get(0).toLocalDate();
    }

    public void setPeriodNumberingFrom(String sheetId, LocalDate from) {
        jdbc.update("UPDATE sheet SET period_numbering_from = ? WHERE id = ?", from == null ? null : Date.valueOf(from),
                sheetId);
    }

    /** The role a user name holds on the sheet: clerk unless set otherwise. */
    public String role(String sheetId, String user) {
        List<String> found = jdbc.query("SELECT role FROM sheet_role WHERE sheet_id = ? AND user_name = ?",
                (rs, i) -> rs.getString("role"), sheetId, user);
        return found.isEmpty() ? CLERK : found.get(0);
    }

    public void setRole(String sheetId, String user, String role) {
        jdbc.update("MERGE INTO sheet_role (sheet_id, user_name, role) KEY (sheet_id, user_name) VALUES (?, ?, ?)",
                sheetId, user, role);
    }

    /** A limit as shown: two decimals, a dot, no grouping. */
    public static String money(BigDecimal v) {
        return v.setScale(2).toPlainString();
    }
}

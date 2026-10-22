package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The sheet's settings, kept on the sheet's row. */
@Repository
public class SettingRepository {

    /** The floor of a sheet that has none stored yet. */
    public static final BigDecimal DEFAULT_FLOOR = new BigDecimal("1.00");

    /** The terms of a sheet that has none stored yet. */
    public static final int DEFAULT_TERMS = 14;

    /** The late percent of a sheet that has none stored yet. */
    public static final BigDecimal DEFAULT_LATE_PCT = new BigDecimal("0.00");

    private final JdbcTemplate jdbc;

    public SettingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's settings as they are now. */
    public Settings of(String sheetId) {
        return new Settings(sheetId, clerkLimit(sheetId), floor(sheetId), terms(sheetId), latePct(sheetId),
                policyDate(sheetId), periodNumberingFrom(sheetId), gradeTerms(sheetId));
    }

    /** The sheet's clerk limit: two decimals, or null when there is none. */
    public BigDecimal clerkLimit(String sheetId) {
        return jdbc.query("SELECT clerk_limit FROM sheet WHERE id = ?",
                rs -> rs.next() ? rs.getBigDecimal(1) : null, sheetId);
    }

    public void setClerkLimit(String sheetId, BigDecimal limit) {
        jdbc.update("UPDATE sheet SET clerk_limit = ? WHERE id = ?", limit, sheetId);
    }

    /** The sheet's floor: two decimals; a claim's live due is at least it. */
    public BigDecimal floor(String sheetId) {
        BigDecimal floor = jdbc.query("SELECT due_floor FROM sheet WHERE id = ?",
                rs -> rs.next() ? rs.getBigDecimal(1) : null, sheetId);
        return floor != null ? floor.setScale(2) : DEFAULT_FLOOR;
    }

    /** The sheet's terms in days: a claim's due-by is its date plus them. */
    public int terms(String sheetId) {
        Integer terms = jdbc.query("SELECT terms_days FROM sheet WHERE id = ?",
                rs -> rs.next() ? rs.getInt(1) : null, sheetId);
        return terms != null ? terms : DEFAULT_TERMS;
    }

    public void setTerms(String sheetId, int terms) {
        jdbc.update("UPDATE sheet SET terms_days = ? WHERE id = ?", terms, sheetId);
    }

    /** The grades' own terms in days, by grade (A, B, C order); a grade without its own uses the sheet's terms. */
    public Map<String, Integer> gradeTerms(String sheetId) {
        Map<String, Integer> terms = new LinkedHashMap<>();
        jdbc.query("SELECT grade, terms_days FROM grade_terms WHERE sheet_id = ? ORDER BY grade",
                rs -> {
                    terms.put(rs.getString(1), rs.getInt(2));
                }, sheetId);
        return terms;
    }

    /** The terms in days a claim of the grade gets: the grade's own, else the sheet's. */
    public int termsFor(String sheetId, String grade) {
        Integer terms = jdbc.query("SELECT terms_days FROM grade_terms WHERE sheet_id = ? AND grade = ?",
                rs -> rs.next() ? rs.getInt(1) : null, sheetId, grade);
        return terms != null ? terms : terms(sheetId);
    }

    /** Give the grade terms of its own (null: none, it uses the sheet's terms). */
    public void setGradeTerms(String sheetId, String grade, Integer terms) {
        jdbc.update("DELETE FROM grade_terms WHERE sheet_id = ? AND grade = ?", sheetId, grade);
        if (terms != null) {
            jdbc.update("INSERT INTO grade_terms (sheet_id, grade, terms_days) VALUES (?, ?, ?)", sheetId, grade, terms);
        }
    }

    /** The sheet's late percent: two decimals; an overdue claim gains it, of its figure, every full 30 days. */
    public BigDecimal latePct(String sheetId) {
        BigDecimal pct = jdbc.query("SELECT late_pct FROM sheet WHERE id = ?",
                rs -> rs.next() ? rs.getBigDecimal(1) : null, sheetId);
        return pct != null ? pct.setScale(2) : DEFAULT_LATE_PCT;
    }

    public void setLatePct(String sheetId, BigDecimal pct) {
        jdbc.update("UPDATE sheet SET late_pct = ? WHERE id = ?", pct, sheetId);
    }

    /** The sheet's policy date: null when blank (the sheet's today is the policy date then). */
    public LocalDate policyDate(String sheetId) {
        return jdbc.query("SELECT policy_date FROM sheet WHERE id = ?",
                rs -> rs.next() ? rs.getObject(1, LocalDate.class) : null, sheetId);
    }

    public void setPolicyDate(String sheetId, LocalDate date) {
        jdbc.update("UPDATE sheet SET policy_date = ? WHERE id = ?", date, sheetId);
    }

    /** The sheet's period numbering from: null when blank (off). */
    public LocalDate periodNumberingFrom(String sheetId) {
        return jdbc.query("SELECT period_numbering_from FROM sheet WHERE id = ?",
                rs -> rs.next() ? rs.getObject(1, LocalDate.class) : null, sheetId);
    }

    public void setPeriodNumberingFrom(String sheetId, LocalDate date) {
        jdbc.update("UPDATE sheet SET period_numbering_from = ? WHERE id = ?", date, sheetId);
    }

    public void setFloor(String sheetId, BigDecimal floor) {
        jdbc.update("UPDATE sheet SET due_floor = ? WHERE id = ?", floor, sheetId);
    }
}

package net.officefloor.hq.app.setting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The settings store: kept on the sheet's row. */
@Repository
public class SettingsRepository {

    private final JdbcTemplate jdbc;

    public SettingsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Settings find(String sheetId) {
        Map<String, Integer> gradeTerms = new HashMap<>();
        jdbc.query("SELECT grade, terms FROM grade_terms WHERE sheet_id = ?",
                (org.springframework.jdbc.core.RowCallbackHandler) (rs) -> gradeTerms.put(rs.getString("grade"), rs.getInt("terms")), sheetId);
        List<Settings> found = jdbc.query("SELECT clerk_limit, due_floor, terms, late_pct, policy_date, period_numbering_from, cap FROM sheet WHERE id = ?",
                (rs, n) -> new Settings(sheetId, rs.getBigDecimal("clerk_limit"), rs.getBigDecimal("due_floor"),
                        rs.getInt("terms"), rs.getBigDecimal("late_pct"),
                        rs.getDate("policy_date") == null ? null : rs.getDate("policy_date").toLocalDate(),
                        rs.getDate("period_numbering_from") == null ? null : rs.getDate("period_numbering_from").toLocalDate(), gradeTerms,
                        rs.getBigDecimal("cap")), sheetId);
        return found.isEmpty() ? new Settings(sheetId, null, Settings.DEFAULT_FLOOR, Settings.DEFAULT_TERMS, Settings.DEFAULT_LATE_PCT, null, null, gradeTerms, null) : found.get(0);
    }

    public void save(Settings settings) {
        jdbc.update("UPDATE sheet SET clerk_limit = ?, due_floor = ?, terms = ?, late_pct = ?, policy_date = ?, period_numbering_from = ?, cap = ? WHERE id = ?",
                settings.clerkLimit(), settings.floor(), settings.terms(), settings.latePct(),
                settings.policyDate() == null ? null : java.sql.Date.valueOf(settings.policyDate()),
                settings.periodNumberingFrom() == null ? null : java.sql.Date.valueOf(settings.periodNumberingFrom()), settings.cap(), settings.sheetId());
        jdbc.update("DELETE FROM grade_terms WHERE sheet_id = ?", settings.sheetId());
        settings.gradeTerms().forEach((grade, terms) ->
                jdbc.update("INSERT INTO grade_terms (sheet_id, grade, terms) VALUES (?, ?, ?)", settings.sheetId(), grade, terms));
    }
}

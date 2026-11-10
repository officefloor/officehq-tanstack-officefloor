package net.officefloor.hq.app.setting;

import java.util.List;
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
        List<Settings> found = jdbc.query("SELECT clerk_limit, due_floor, terms, late_pct, policy_date FROM sheet WHERE id = ?",
                (rs, n) -> new Settings(sheetId, rs.getBigDecimal("clerk_limit"), rs.getBigDecimal("due_floor"),
                        rs.getInt("terms"), rs.getBigDecimal("late_pct"),
                        rs.getDate("policy_date") == null ? null : rs.getDate("policy_date").toLocalDate()), sheetId);
        return found.isEmpty() ? new Settings(sheetId, null, Settings.DEFAULT_FLOOR, Settings.DEFAULT_TERMS, Settings.DEFAULT_LATE_PCT, null) : found.get(0);
    }

    public void save(Settings settings) {
        jdbc.update("UPDATE sheet SET clerk_limit = ?, due_floor = ?, terms = ?, late_pct = ?, policy_date = ? WHERE id = ?",
                settings.clerkLimit(), settings.floor(), settings.terms(), settings.latePct(),
                settings.policyDate() == null ? null : java.sql.Date.valueOf(settings.policyDate()), settings.sheetId());
    }
}

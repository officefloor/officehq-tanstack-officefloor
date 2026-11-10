package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
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
        List<BigDecimal> found = jdbc.query("SELECT clerk_limit FROM sheet WHERE id = ?",
                (rs, n) -> rs.getBigDecimal("clerk_limit"), sheetId);
        return new Settings(sheetId, found.isEmpty() ? null : found.get(0));
    }

    public void save(Settings settings) {
        jdbc.update("UPDATE sheet SET clerk_limit = ? WHERE id = ?", settings.clerkLimit(), settings.sheetId());
    }
}

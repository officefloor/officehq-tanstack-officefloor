package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The sheet's settings, kept on the sheet's row. */
@Repository
public class SettingRepository {

    private final JdbcTemplate jdbc;

    public SettingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's settings as they are now. */
    public Settings of(String sheetId) {
        return new Settings(sheetId, clerkLimit(sheetId));
    }

    /** The sheet's clerk limit: two decimals, or null when there is none. */
    public BigDecimal clerkLimit(String sheetId) {
        return jdbc.query("SELECT clerk_limit FROM sheet WHERE id = ?",
                rs -> rs.next() ? rs.getBigDecimal(1) : null, sheetId);
    }

    public void setClerkLimit(String sheetId, BigDecimal limit) {
        jdbc.update("UPDATE sheet SET clerk_limit = ? WHERE id = ?", limit, sheetId);
    }
}

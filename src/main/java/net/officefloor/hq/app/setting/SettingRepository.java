package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The sheet's settings, kept on the sheet's row. */
@Repository
public class SettingRepository {

    /** The floor of a sheet that has none stored yet. */
    public static final BigDecimal DEFAULT_FLOOR = new BigDecimal("1.00");

    /** The terms of a sheet that has none stored yet. */
    public static final int DEFAULT_TERMS = 14;

    private final JdbcTemplate jdbc;

    public SettingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's settings as they are now. */
    public Settings of(String sheetId) {
        return new Settings(sheetId, clerkLimit(sheetId), floor(sheetId), terms(sheetId));
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

    public void setFloor(String sheetId, BigDecimal floor) {
        jdbc.update("UPDATE sheet SET due_floor = ? WHERE id = ?", floor, sheetId);
    }
}

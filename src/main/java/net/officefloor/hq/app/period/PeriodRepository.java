package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PeriodRepository {

    private static final RowMapper<Period> ROW = (rs, i) -> new Period(rs.getLong("id"), rs.getString("name"),
            rs.getDate("from_date").toLocalDate().toString(), rs.getDate("to_date").toLocalDate().toString(),
            rs.getBoolean("closed"), rs.getBigDecimal("closed_total") == null ? null
                    : rs.getBigDecimal("closed_total").setScale(2).toPlainString());

    private static final String COLUMNS = "id, name, from_date, to_date, closed, closed_total";

    private final JdbcTemplate jdbc;

    public PeriodRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Lock the sheet's row until the transaction commits, so two periods added at once are checked
     *  one after the other; false when there is no such sheet. */
    public boolean lockSheet(String sheetId) {
        return !jdbc.queryForList("SELECT id FROM sheet WHERE id = ? FOR UPDATE", String.class, sheetId).isEmpty();
    }

    /** The sheet's periods, by from-date. */
    public List<Period> list(String sheetId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM period WHERE sheet_id = ? ORDER BY from_date",
                ROW, sheetId);
    }

    /** The sheet's period of this name (exactly); null when there is none. */
    public Period find(String sheetId, String name) {
        List<Period> found = jdbc.query("SELECT " + COLUMNS + " FROM period WHERE sheet_id = ? AND name = ?", ROW,
                sheetId, name);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The name of the sheet's closed period whose dates contain a date; null when none does. */
    public String closedOn(String sheetId, LocalDate on) {
        List<String> found = jdbc.queryForList("SELECT name FROM period WHERE sheet_id = ? AND closed"
                + " AND from_date <= ? AND to_date >= ?", String.class, sheetId, Date.valueOf(on), Date.valueOf(on));
        return found.isEmpty() ? null : found.get(0);
    }

    /** Close a period, fixing its due-total. */
    public void close(String sheetId, long periodId, BigDecimal total) {
        jdbc.update("UPDATE period SET closed = TRUE, closed_total = ? WHERE id = ? AND sheet_id = ?", total,
                periodId, sheetId);
    }

    /** Whether the sheet has a period of this name, without regard to case. */
    public boolean named(String sheetId, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM period WHERE sheet_id = ? AND name_key = LOWER(?)",
                Integer.class, sheetId, name);
        return n != null && n > 0;
    }

    /** Whether any period of the sheet shares a day with from..to (inclusive). */
    public boolean overlaps(String sheetId, LocalDate from, LocalDate to) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM period WHERE sheet_id = ?"
                + " AND from_date <= ? AND to_date >= ?", Integer.class, sheetId, Date.valueOf(to), Date.valueOf(from));
        return n != null && n > 0;
    }

    public Period add(String sheetId, String name, LocalDate from, LocalDate to) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO period (sheet_id, name, from_date, to_date) VALUES (?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setString(2, name);
            ps.setDate(3, Date.valueOf(from));
            ps.setDate(4, Date.valueOf(to));
            return ps;
        }, keys);
        return new Period(keys.getKey().longValue(), name, from.toString(), to.toString(), false, null);
    }
}

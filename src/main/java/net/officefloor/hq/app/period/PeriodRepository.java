package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The stored periods of each sheet. */
@Repository
public class PeriodRepository {

    private static final String COLUMNS = "id, sheet_id, name, from_date, to_date, closed, closed_total";

    private final JdbcTemplate jdbc;

    public PeriodRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's periods, by from-date. */
    public List<Period> list(String sheetId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM period WHERE sheet_id = ? ORDER BY from_date, id",
                PeriodRepository::map, sheetId);
    }

    /** Whether the name is already a period's on the sheet, case and outer spaces ignored. */
    public boolean nameTaken(String sheetId, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM period WHERE sheet_id = ? AND LOWER(name) = LOWER(?)",
                Integer.class, sheetId, name.trim());
        return n != null && n > 0;
    }

    /** Whether any period of the sheet shares a day with the dates from..to, both inclusive. */
    public boolean overlaps(String sheetId, LocalDate from, LocalDate to) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM period WHERE sheet_id = ? AND from_date <= ? AND to_date >= ?",
                Integer.class, sheetId, Date.valueOf(to), Date.valueOf(from));
        return n != null && n > 0;
    }

    /** A period of the sheet by id, or null when it has none. */
    public Period find(String sheetId, long id) {
        List<Period> found = jdbc.query("SELECT " + COLUMNS + " FROM period WHERE sheet_id = ? AND id = ?",
                PeriodRepository::map, sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The period of the sheet whose dates contain the date, or null when none does. */
    public Period containing(String sheetId, LocalDate date) {
        List<Period> found = jdbc.query("SELECT " + COLUMNS + " FROM period WHERE sheet_id = ? AND from_date <= ? AND to_date >= ?",
                PeriodRepository::map, sheetId, Date.valueOf(date), Date.valueOf(date));
        return found.isEmpty() ? null : found.get(0);
    }

    /** Whether a closed period of the sheet contains the date. */
    public boolean closedOn(String sheetId, LocalDate date) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM period WHERE sheet_id = ? AND closed = TRUE "
                + "AND from_date <= ? AND to_date >= ?", Integer.class, sheetId, Date.valueOf(date), Date.valueOf(date));
        return n != null && n > 0;
    }

    /** Closes the period with its fixed due-total, but only while it is still open; false when it was not. */
    public boolean close(Period period, BigDecimal total) {
        return jdbc.update("UPDATE period SET closed = TRUE, closed_total = ? WHERE sheet_id = ? AND id = ? AND closed = FALSE",
                total, period.sheetId(), period.id()) == 1;
    }

    /** Fixes each person's due-total for the period, as it is closed. */
    public void fixPersonTotals(Period period, Map<Long, BigDecimal> totals) {
        for (Map.Entry<Long, BigDecimal> e : totals.entrySet()) {
            jdbc.update("INSERT INTO period_person_total (sheet_id, period_id, person_id, total) VALUES (?, ?, ?, ?)",
                    period.sheetId(), period.id(), e.getKey(), e.getValue());
        }
    }

    /** The fixed due-total of each person for the closed period, by person id. */
    public Map<Long, BigDecimal> personTotals(Period period) {
        Map<Long, BigDecimal> totals = new LinkedHashMap<>();
        jdbc.query("SELECT person_id, total FROM period_person_total WHERE sheet_id = ? AND period_id = ? ORDER BY person_id",
                (ResultSet rs) -> {
                    totals.put(rs.getLong("person_id"), rs.getBigDecimal("total").setScale(2));
                }, period.sheetId(), period.id());
        return totals;
    }

    /** Reopens the period, dropping its fixed due-totals, but only while it is closed; false when it was not. */
    public boolean reopen(Period period) {
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND period_id = ?", period.sheetId(), period.id());
        return jdbc.update("UPDATE period SET closed = FALSE, closed_total = NULL WHERE sheet_id = ? AND id = ? AND closed = TRUE",
                period.sheetId(), period.id()) == 1;
    }

    /** Stores a new period and returns it with its id. */
    public Period add(String sheetId, String name, LocalDate from, LocalDate to) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update((con) -> {
            var ps = con.prepareStatement("INSERT INTO period (sheet_id, name, from_date, to_date) VALUES (?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setString(2, name);
            ps.setDate(3, Date.valueOf(from));
            ps.setDate(4, Date.valueOf(to));
            return ps;
        }, keys);
        return new Period(keys.getKey().longValue(), sheetId, name, from, to, false, null);
    }

    private static Period map(ResultSet rs, int n) throws SQLException {
        BigDecimal total = rs.getBigDecimal("closed_total");
        return new Period(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name"),
                rs.getDate("from_date").toLocalDate(), rs.getDate("to_date").toLocalDate(), rs.getBoolean("closed"), total == null ? null : total.setScale(2));
    }
}

package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Periods by sheet, earliest first. */
@Repository
public class PeriodRepository {

    private static final RowMapper<Period> ROW = (rs, i) -> new Period(rs.getString("sheet_id"),
            rs.getString("name"), rs.getObject("from_date", LocalDate.class), rs.getObject("to_date", LocalDate.class),
            rs.getBoolean("closed"), rs.getBigDecimal("closed_total"));

    private final JdbcTemplate jdbc;

    public PeriodRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Period> list(String sheetId) {
        return jdbc.query("SELECT * FROM period WHERE sheet_id = ? ORDER BY from_date", ROW, sheetId);
    }

    /** Lock the sheet's row until the transaction ends, so concurrent period adds are checked one after the other. */
    public void lockSheet(String sheetId) {
        jdbc.query("SELECT id FROM sheet WHERE id = ? FOR UPDATE", rs -> null, sheetId);
    }

    public Period insert(String sheetId, String name, LocalDate from, LocalDate to) {
        jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date) VALUES (?, ?, ?, ?)",
                sheetId, name, Date.valueOf(from), Date.valueOf(to));
        return new Period(sheetId, name, from, to, false, null);
    }

    /** The period on the sheet with this name (without regard to case or surrounding spaces), or null. */
    public Period find(String sheetId, String name) {
        return jdbc.query("SELECT * FROM period WHERE sheet_id = ? AND name_key = LOWER(TRIM(?))", ROW, sheetId, name)
                .stream().findFirst().orElse(null);
    }

    /** The closed period on the sheet whose dates contain the date, or null when none does. */
    public Period closedOn(String sheetId, LocalDate date) {
        return list(sheetId).stream().filter(p -> p.closed() && p.contains(date)).findFirst().orElse(null);
    }

    /** Close the period, fixing its due-total and each person's (by person id): false when it was already closed,
     *  and nothing is written. */
    public boolean close(Period period, BigDecimal total, Map<Long, BigDecimal> personTotals) {
        if (jdbc.update("UPDATE period SET closed = TRUE, closed_total = ? WHERE sheet_id = ? AND name = ? AND NOT closed",
                total, period.sheetId(), period.name()) != 1) {
            return false;
        }
        personTotals.forEach((personId, personTotal) -> jdbc.update(
                "INSERT INTO period_person_total (sheet_id, period_name, person_id, total) VALUES (?, ?, ?, ?)",
                period.sheetId(), period.name(), personId, personTotal));
        return true;
    }

    /** The people's due-totals fixed in the sheet's closed periods: by period name, then by person id. */
    public Map<String, Map<Long, BigDecimal>> personTotals(String sheetId) {
        Map<String, Map<Long, BigDecimal>> totals = new HashMap<>();
        jdbc.query("SELECT period_name, person_id, total FROM period_person_total WHERE sheet_id = ?", rs -> {
            totals.computeIfAbsent(rs.getString("period_name"), k -> new HashMap<>())
                    .put(rs.getLong("person_id"), rs.getBigDecimal("total"));
        }, sheetId);
        return totals;
    }

    /** Reopen the period, dropping its fixed due-totals: false when it was not closed, and nothing is written. */
    public boolean reopen(Period period) {
        if (jdbc.update("UPDATE period SET closed = FALSE, closed_total = NULL WHERE sheet_id = ? AND name = ? AND closed",
                period.sheetId(), period.name()) != 1) {
            return false;
        }
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND period_name = ?", period.sheetId(), period.name());
        return true;
    }
}

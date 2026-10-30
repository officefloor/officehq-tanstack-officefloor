package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The period store: named, non-overlapping date ranges (both ends inclusive), kept per sheet. */
@Repository
public class Periods {

    private static final RowMapper<PeriodResponse> ROW = (rs, i) -> {
        BigDecimal total = rs.getBigDecimal("closed_total");
        return new PeriodResponse(rs.getString("name"), rs.getDate("from_date").toLocalDate().toString(),
                rs.getDate("to_date").toLocalDate().toString(), rs.getBoolean("closed"),
                total == null ? "" : total.setScale(2).toPlainString(), new LinkedHashMap<>());
    };

    private final JdbcTemplate jdbc;

    public Periods(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's periods, by from-date. */
    public List<PeriodResponse> list(String sheetId) {
        List<PeriodResponse> periods = jdbc.query("SELECT * FROM period WHERE sheet_id = ? ORDER BY from_date", ROW, sheetId);
        Map<String, PeriodResponse> byName = new HashMap<>();
        periods.forEach(p -> byName.put(p.name(), p));
        jdbc.query("SELECT period_name, person_id, total FROM period_person_total WHERE sheet_id = ? ORDER BY person_id",
                (RowCallbackHandler) rs -> {
                    PeriodResponse period = byName.get(rs.getString("period_name"));
                    if (period != null) {
                        period.closedPersonTotals().put(String.valueOf(rs.getLong("person_id")),
                                rs.getBigDecimal("total").setScale(2).toPlainString());
                    }
                }, sheetId);
        return periods;
    }

    /** The named period, its row locked until the transaction ends; null when none. */
    public PeriodResponse lock(String sheetId, String name) {
        List<PeriodResponse> found = jdbc.query("SELECT * FROM period WHERE sheet_id = ? AND name = ? FOR UPDATE",
                ROW, sheetId, name);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Close the named period, fixing its due-total and each person's due-total for it. */
    public void close(String sheetId, String name, BigDecimal total, Map<Long, BigDecimal> personTotals) {
        jdbc.update("UPDATE period SET closed = TRUE, closed_total = ? WHERE sheet_id = ? AND name = ?",
                total, sheetId, name);
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND period_name = ?", sheetId, name);
        personTotals.forEach((personId, personTotal) -> jdbc.update(
                "INSERT INTO period_person_total (sheet_id, period_name, person_id, total) VALUES (?, ?, ?, ?)",
                sheetId, name, personId, personTotal));
    }

    /** Reopen the named period, dropping its fixed due-total. */
    public void reopen(String sheetId, String name) {
        jdbc.update("UPDATE period SET closed = FALSE, closed_total = NULL WHERE sheet_id = ? AND name = ?",
                sheetId, name);
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND period_name = ?", sheetId, name);
    }

    /** Whether a date falls inside a closed period of the sheet. */
    public boolean isClosed(String sheetId, LocalDate date) {
        Integer closed = jdbc.queryForObject(
                "SELECT COUNT(*) FROM period WHERE sheet_id = ? AND closed AND from_date <= ? AND to_date >= ?",
                Integer.class, sheetId, Date.valueOf(date), Date.valueOf(date));
        return closed != null && closed > 0;
    }

    /** The name of the sheet's period whose dates contain the date; null when none does. */
    public String nameOn(String sheetId, LocalDate date) {
        List<String> found = jdbc.query("SELECT name FROM period WHERE sheet_id = ? AND from_date <= ? AND to_date >= ?",
                (rs, i) -> rs.getString(1), sheetId, Date.valueOf(date), Date.valueOf(date));
        return found.isEmpty() ? null : found.get(0);
    }

    /** Add a period, refusing a name already on the sheet or dates overlapping another period. */
    public void add(String sheetId, ValidPeriod period) throws InvalidPeriodException {
        // Lock the sheet's row so two periods added at once cannot both pass the overlap check.
        jdbc.query("SELECT id FROM sheet WHERE id = ? FOR UPDATE", (rs, i) -> rs.getString(1), sheetId);
        Integer named = jdbc.queryForObject("SELECT COUNT(*) FROM period WHERE sheet_id = ? AND name = ?",
                Integer.class, sheetId, period.name());
        if (named != null && named > 0) {
            throw new InvalidPeriodException("A period named " + period.name() + " is already on the sheet");
        }
        Integer overlapping = jdbc.queryForObject(
                "SELECT COUNT(*) FROM period WHERE sheet_id = ? AND from_date <= ? AND to_date >= ?",
                Integer.class, sheetId, Date.valueOf(period.to()), Date.valueOf(period.from()));
        if (overlapping != null && overlapping > 0) {
            throw new InvalidPeriodException("The period's dates overlap another period");
        }
        try {
            jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date) VALUES (?, ?, ?, ?)",
                    sheetId, period.name(), Date.valueOf(period.from()), Date.valueOf(period.to()));
        } catch (DuplicateKeyException raced) {
            throw new InvalidPeriodException("A period named " + period.name() + " is already on the sheet");
        }
    }
}

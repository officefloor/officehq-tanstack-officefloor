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
import org.springframework.stereotype.Repository;

/** The periods of each sheet. */
@Repository
public class Periods {

    private static final String COLUMNS = "SELECT sheet_id, name, from_date, to_date, closed, closed_total, soft FROM period";

    private final JdbcTemplate jdbc;

    public Periods(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every period on the sheet, by from-date. */
    public List<Period> onSheet(String sheetId) {
        return jdbc.query(COLUMNS + " WHERE sheet_id = ? ORDER BY from_date", Periods::period, sheetId);
    }

    /** The period of this name on this sheet, or null when there is none. Locks its row for the
     *  rest of the transaction. */
    public Period find(String sheetId, String name) {
        return jdbc.query(COLUMNS + " WHERE sheet_id = ? AND name = ? FOR UPDATE", Periods::period, sheetId, name)
                .stream().findFirst().orElse(null);
    }

    /** The (not soft-) closed period of the sheet whose dates contain the date, or null when there is none. */
    public Period closedOn(String sheetId, LocalDate date) {
        return jdbc.query(COLUMNS + " WHERE sheet_id = ? AND closed AND NOT soft AND from_date <= ? AND to_date >= ?",
                Periods::period, sheetId, Date.valueOf(date), Date.valueOf(date)).stream().findFirst().orElse(null);
    }

    /** The soft-closed period of the sheet whose dates contain the date, or null when there is none. */
    public Period softClosedOn(String sheetId, LocalDate date) {
        return jdbc.query(COLUMNS + " WHERE sheet_id = ? AND closed AND soft AND from_date <= ? AND to_date >= ?",
                Periods::period, sheetId, Date.valueOf(date), Date.valueOf(date)).stream().findFirst().orElse(null);
    }

    public void add(Period period) {
        jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date) VALUES (?, ?, ?, ?)",
                period.sheetId(), period.name(), Date.valueOf(period.from()), Date.valueOf(period.to()));
    }

    /** Close (or, when `soft`, soft-close) the period, fixing its due-total and each person's (by
     *  person id) afresh. */
    public void close(Period period, BigDecimal total, Map<Long, BigDecimal> personTotals, boolean soft) {
        jdbc.update("UPDATE period SET closed = TRUE, soft = ?, closed_total = ? WHERE sheet_id = ? AND name = ?",
                soft, total, period.sheetId(), period.name());
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND period_name = ?",
                period.sheetId(), period.name());
        personTotals.forEach((personId, personTotal) -> jdbc.update(
                "INSERT INTO period_person_total (sheet_id, period_name, person_id, total) VALUES (?, ?, ?, ?)",
                period.sheetId(), period.name(), personId, personTotal));
    }

    /** Each person's due-total fixed when the period was closed, by person id; empty while it is open. */
    public Map<Long, BigDecimal> personTotals(Period period) {
        Map<Long, BigDecimal> totals = new LinkedHashMap<>();
        jdbc.query("SELECT person_id, total FROM period_person_total WHERE sheet_id = ? AND period_name = ?"
                + " ORDER BY person_id", rs -> {
                    totals.put(rs.getLong("person_id"), rs.getBigDecimal("total"));
                }, period.sheetId(), period.name());
        return totals;
    }

    /** Reopen the period: its due-total, and each person's, is live again. */
    public void reopen(Period period) {
        jdbc.update("UPDATE period SET closed = FALSE, soft = FALSE, closed_total = NULL WHERE sheet_id = ? AND name = ?",
                period.sheetId(), period.name());
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND period_name = ?",
                period.sheetId(), period.name());
    }

    private static Period period(ResultSet rs, int i) throws SQLException {
        return new Period(rs.getString("sheet_id"), rs.getString("name"), rs.getDate("from_date").toLocalDate(),
                rs.getDate("to_date").toLocalDate(), rs.getBoolean("closed"), rs.getBigDecimal("closed_total"),
                rs.getBoolean("soft"));
    }
}

package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The store of periods, kept per sheet. */
@Repository
public class Periods {

    private static final RowMapper<Period> ROW = (rs, n) -> new Period(rs.getString("sheet_id"),
            rs.getString("name"), rs.getDate("from_date").toLocalDate(), rs.getDate("to_date").toLocalDate(),
            rs.getBoolean("closed"), rs.getBigDecimal("closed_total"), Map.of());

    private final JdbcTemplate jdbc;

    public Periods(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's periods, by from-date, each closed one with its people's fixed figures. */
    public List<Period> list(String sheetId) {
        Map<String, Map<Long, BigDecimal>> totals = new HashMap<>();
        jdbc.query("SELECT * FROM period_person_total WHERE sheet_id = ? ORDER BY person_id", rs -> {
            totals.computeIfAbsent(rs.getString("period_name"), k -> new LinkedHashMap<>())
                    .put(rs.getLong("person_id"), rs.getBigDecimal("total"));
        }, sheetId);
        return jdbc.query("SELECT * FROM period WHERE sheet_id = ? ORDER BY from_date", ROW, sheetId).stream()
                .map(p -> p.withPersonTotals(totals.getOrDefault(p.name(), Map.of()))).toList();
    }

    /** The sheet's period of that name, or null when there is none. */
    public Period find(String sheetId, String name) {
        return jdbc.query("SELECT * FROM period WHERE sheet_id = ? AND name = ?", ROW, sheetId, name).stream()
                .findFirst().orElse(null);
    }

    /** The sheet's period containing the date, or null when none does. */
    public Period on(String sheetId, LocalDate date) {
        return jdbc.query("SELECT * FROM period WHERE sheet_id = ? AND from_date <= ? AND ? <= to_date",
                ROW, sheetId, Date.valueOf(date), Date.valueOf(date)).stream().findFirst().orElse(null);
    }

    /** The sheet's closed period containing the date, or null when none does. */
    public Period closedOn(String sheetId, LocalDate date) {
        return jdbc.query("SELECT * FROM period WHERE sheet_id = ? AND closed AND from_date <= ? AND ? <= to_date",
                ROW, sheetId, Date.valueOf(date), Date.valueOf(date)).stream().findFirst().orElse(null);
    }

    public void add(Period period) {
        jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date) VALUES (?, ?, ?, ?)",
                period.sheetId(), period.name(), Date.valueOf(period.from()), Date.valueOf(period.to()));
    }

    /** Closes the period, fixing its total and its people's figures; false when it was already closed (by someone else). */
    public boolean close(Period closed) {
        if (jdbc.update("UPDATE period SET closed = TRUE, closed_total = ? WHERE sheet_id = ? AND name = ? AND NOT closed",
                closed.closedTotal(), closed.sheetId(), closed.name()) != 1) {
            return false;
        }
        closed.personTotals().forEach((personId, total) -> jdbc.update(
                "INSERT INTO period_person_total (sheet_id, period_name, person_id, total) VALUES (?, ?, ?, ?)",
                closed.sheetId(), closed.name(), personId, total));
        return true;
    }

    /** Reopens the period, dropping its fixed total; false when it was not closed (someone else reopened it). */
    public boolean reopen(Period period) {
        if (jdbc.update("UPDATE period SET closed = FALSE, closed_total = NULL WHERE sheet_id = ? AND name = ? AND closed",
                period.sheetId(), period.name()) != 1) {
            return false;
        }
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND period_name = ?", period.sheetId(),
                period.name());
        return true;
    }
}

package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The periods kept per sheet. */
@Repository
public class Periods {

    private static final RowMapper<Period> ROW = (rs, i) -> new Period(rs.getString("name"),
            rs.getDate("from_date").toLocalDate().toString(), rs.getDate("to_date").toLocalDate().toString(),
            rs.getBoolean("closed"),
            rs.getBigDecimal("closed_total") == null ? null : rs.getBigDecimal("closed_total").toPlainString(),
            Map.of(), rs.getBoolean("soft"), (Integer) rs.getObject("claim_cap"), Map.of(), Map.of());

    private static final String COLUMNS = "SELECT name, from_date, to_date, closed, closed_total, soft, claim_cap FROM period";

    private final JdbcTemplate jdbc;

    public Periods(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every period on the sheet, by from-date. */
    public List<Period> list(String sheet) {
        return jdbc.query(COLUMNS + " WHERE sheet = ? ORDER BY from_date", ROW, sheet).stream()
                .map((p) -> withOpenings(sheet, withPeople(sheet, p))).toList();
    }

    /** The period with its people's opening balances. */
    private Period withOpenings(String sheet, Period period) {
        Map<Long, String> openings = new LinkedHashMap<>();
        jdbc.query("SELECT person, amount FROM period_opening WHERE sheet = ? AND period = ? ORDER BY person",
                (rs) -> {
                    openings.put(rs.getLong("person"), rs.getBigDecimal("amount").toPlainString());
                }, sheet, period.name());
        return new Period(period.name(), period.from(), period.to(), period.closed(), period.closedTotal(),
                period.closedPeople(), period.soft(), period.cap(), period.closedCarried(), openings);
    }

    /** Each person's opening balance for the period (by person id); none for a person with no opening. */
    public Map<Long, BigDecimal> openings(String sheet, String name) {
        Map<Long, BigDecimal> openings = new LinkedHashMap<>();
        jdbc.query("SELECT person, amount FROM period_opening WHERE sheet = ? AND period = ?",
                (rs) -> {
                    openings.put(rs.getLong("person"), rs.getBigDecimal("amount"));
                }, sheet, name);
        return openings;
    }

    /** Adds the amount to the person's opening balance for the period. */
    public void addOpening(String sheet, String name, long person, BigDecimal amount) {
        if (jdbc.update("UPDATE period_opening SET amount = amount + ? WHERE sheet = ? AND period = ? AND person = ?",
                amount, sheet, name, person) == 0) {
            jdbc.update("INSERT INTO period_opening (sheet, period, person, amount) VALUES (?, ?, ?, ?)", sheet, name,
                    person, amount);
        }
    }

    /** The period with its people's due-totals and carried figures fixed when it was closed. */
    private Period withPeople(String sheet, Period period) {
        if (!period.fixed()) {
            return period;
        }
        Map<Long, String> people = new LinkedHashMap<>();
        Map<Long, String> carried = new LinkedHashMap<>();
        jdbc.query("SELECT person, total, carried FROM period_person_total WHERE sheet = ? AND period = ? ORDER BY person",
                (rs) -> {
                    people.put(rs.getLong("person"), rs.getBigDecimal("total").toPlainString());
                    carried.put(rs.getLong("person"), rs.getBigDecimal("carried").toPlainString());
                }, sheet, period.name());
        return new Period(period.name(), period.from(), period.to(), period.closed(), period.closedTotal(), people,
                period.soft(), period.cap(), carried, period.openings());
    }

    /** The sheet's period of the name, or null when there is none. */
    public Period find(String sheet, String name) {
        List<Period> found = jdbc.query(COLUMNS + " WHERE sheet = ? AND name = ?", ROW, sheet, name);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Whether the date falls inside a closed period of the sheet. */
    public boolean closedOn(String sheet, LocalDate date) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM period WHERE sheet = ? AND closed AND from_date <= ? AND to_date >= ?",
                Integer.class, sheet, Date.valueOf(date), Date.valueOf(date));
        return n != null && n > 0;
    }

    /** Whether the date falls inside a soft-closed period of the sheet. */
    public boolean softClosedOn(String sheet, LocalDate date) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM period WHERE sheet = ? AND soft AND from_date <= ? AND to_date >= ?",
                Integer.class, sheet, Date.valueOf(date), Date.valueOf(date));
        return n != null && n > 0;
    }

    /** The name of the sheet's period the date falls inside, or null when it is in none. */
    public String nameOn(String sheet, LocalDate date) {
        List<String> found = jdbc.queryForList("SELECT name FROM period WHERE sheet = ? AND from_date <= ? AND to_date >= ?",
                String.class, sheet, Date.valueOf(date), Date.valueOf(date));
        return found.isEmpty() ? null : found.get(0);
    }

    /** The sheet's period the date falls inside, or null when it is in none. */
    public Period on(String sheet, LocalDate date) {
        List<Period> found = jdbc.query(COLUMNS + " WHERE sheet = ? AND from_date <= ? AND to_date >= ?", ROW, sheet,
                Date.valueOf(date), Date.valueOf(date));
        return found.isEmpty() ? null : found.get(0);
    }

    /** Whether the sheet has a period of the name. */
    public boolean named(String sheet, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM period WHERE sheet = ? AND name = ?", Integer.class,
                sheet, name);
        return n != null && n > 0;
    }

    /** Whether any period on the sheet shares a date with the inclusive range. */
    public boolean overlaps(String sheet, PeriodInput input) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM period WHERE sheet = ? AND from_date <= ? AND to_date >= ?", Integer.class,
                sheet, Date.valueOf(input.to()), Date.valueOf(input.from()));
        return n != null && n > 0;
    }

    public void add(String sheet, PeriodInput input) {
        jdbc.update("INSERT INTO period (sheet, name, from_date, to_date, claim_cap) VALUES (?, ?, ?, ?, ?)", sheet,
                input.name(), Date.valueOf(input.from()), Date.valueOf(input.to()), input.cap());
    }

    /**
     * Closes the period (or soft-closes it) with its due-total and each person's due-total and carried figure (by id)
     * fixed afresh; false when it was
     * already closed, or for a soft close already soft-closed (or is gone). Closing a soft-closed period is allowed.
     */
    public boolean close(String sheet, String name, BigDecimal total, Map<Long, BigDecimal> people,
            Map<Long, BigDecimal> carried, boolean soft) {
        String sql = soft
                ? "UPDATE period SET soft = TRUE, closed_total = ? WHERE sheet = ? AND name = ? AND NOT closed AND NOT soft"
                : "UPDATE period SET closed = TRUE, soft = FALSE, closed_total = ? WHERE sheet = ? AND name = ? AND NOT closed";
        if (jdbc.update(sql, total, sheet, name) != 1) {
            return false;
        }
        jdbc.update("DELETE FROM period_person_total WHERE sheet = ? AND period = ?", sheet, name);
        people.forEach((person, due) -> jdbc.update(
                "INSERT INTO period_person_total (sheet, period, person, total, carried) VALUES (?, ?, ?, ?, ?)", sheet,
                name, person, due, carried.getOrDefault(person, BigDecimal.ZERO.setScale(2))));
        return true;
    }

    /** Reopens the closed or soft-closed period, dropping its fixed due-totals and carried figures; false when it was neither (or is gone). */
    public boolean reopen(String sheet, String name) {
        if (jdbc.update("UPDATE period SET closed = FALSE, soft = FALSE, closed_total = NULL WHERE sheet = ? AND name = ?"
                + " AND (closed OR soft)",
                sheet, name) != 1) {
            return false;
        }
        jdbc.update("DELETE FROM period_person_total WHERE sheet = ? AND period = ?", sheet, name);
        return true;
    }
}

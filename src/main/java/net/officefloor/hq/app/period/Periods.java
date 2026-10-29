package net.officefloor.hq.app.period;

import net.officefloor.hq.app.history.AsOf;
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
            rs.getBoolean("closed"), rs.getBigDecimal("closed_total"), Map.of(), Map.of(),
            rs.getBoolean("soft"), rs.getObject("claim_cap", Integer.class),
            rs.getBoolean("locked"));

    private final JdbcTemplate jdbc;

    public Periods(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's periods, by from-date, each closed one with its people's fixed figures and carried figures. */
    public List<Period> list(String sheetId) {
        Map<String, Map<Long, BigDecimal>> totals = new HashMap<>();
        Map<String, Map<Long, BigDecimal>> carried = new HashMap<>();
        jdbc.query("SELECT * FROM " + AsOf.table("period_person_total") + " WHERE sheet_id = ? ORDER BY person_id", rs -> {
            totals.computeIfAbsent(rs.getString("period_name"), k -> new LinkedHashMap<>())
                    .put(rs.getLong("person_id"), rs.getBigDecimal("total"));
            // A row fixed before carried figures were fixed has none.
            if (rs.getBigDecimal("carried") != null) {
                carried.computeIfAbsent(rs.getString("period_name"), k -> new LinkedHashMap<>())
                        .put(rs.getLong("person_id"), rs.getBigDecimal("carried"));
            }
        }, sheetId);
        return jdbc.query("SELECT * FROM " + AsOf.table("period") + " WHERE sheet_id = ? ORDER BY from_date", ROW, sheetId).stream()
                .map(p -> p.withPersonTotals(totals.getOrDefault(p.name(), Map.of()), carried.getOrDefault(p.name(), Map.of())))
                .toList();
    }

    /** Each person's opening balance for each of the sheet's periods, by period name, then by person id. */
    public Map<String, Map<Long, BigDecimal>> openings(String sheetId) {
        Map<String, Map<Long, BigDecimal>> openings = new HashMap<>();
        jdbc.query("SELECT * FROM " + AsOf.table("period_person_opening") + " WHERE sheet_id = ? ORDER BY person_id", rs -> {
            openings.computeIfAbsent(rs.getString("period_name"), k -> new LinkedHashMap<>())
                    .put(rs.getLong("person_id"), rs.getBigDecimal("amount"));
        }, sheetId);
        return openings;
    }

    /** Adds the amount to the person's opening balance for the period (from none, 0.00). */
    public void addOpening(String sheetId, String periodName, long personId, BigDecimal amount) {
        if (jdbc.update("UPDATE period_person_opening SET amount = amount + ? WHERE sheet_id = ? AND period_name = ? AND person_id = ?",
                amount, sheetId, periodName, personId) == 0) {
            jdbc.update("INSERT INTO period_person_opening (sheet_id, period_name, person_id, amount) VALUES (?, ?, ?, ?)",
                    sheetId, periodName, personId, amount);
        }
    }

    /** The sheet's period of that name, or null when there is none. */
    public Period find(String sheetId, String name) {
        return jdbc.query("SELECT * FROM " + AsOf.table("period") + " WHERE sheet_id = ? AND name = ?", ROW, sheetId, name).stream()
                .findFirst().orElse(null);
    }

    /** The sheet's period containing the date, or null when none does. */
    public Period on(String sheetId, LocalDate date) {
        return jdbc.query("SELECT * FROM " + AsOf.table("period") + " WHERE sheet_id = ? AND from_date <= ? AND ? <= to_date",
                ROW, sheetId, Date.valueOf(date), Date.valueOf(date)).stream().findFirst().orElse(null);
    }

    /** The sheet's closed period containing the date, or null when none does. */
    public Period closedOn(String sheetId, LocalDate date) {
        return jdbc.query("SELECT * FROM " + AsOf.table("period") + " WHERE sheet_id = ? AND closed AND from_date <= ? AND ? <= to_date",
                ROW, sheetId, Date.valueOf(date), Date.valueOf(date)).stream().findFirst().orElse(null);
    }

    public void add(Period period) {
        jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date, claim_cap) VALUES (?, ?, ?, ?, ?)",
                period.sheetId(), period.name(), Date.valueOf(period.from()), Date.valueOf(period.to()), period.cap());
    }

    /**
     * Closes the period (open or soft-closed), fixing its total and its people's figures afresh; false when it was
     * already closed (by someone else).
     */
    public boolean close(Period closed) {
        if (jdbc.update("UPDATE period SET closed = TRUE, soft = FALSE, closed_total = ? WHERE sheet_id = ? AND name = ? AND NOT closed",
                closed.closedTotal(), closed.sheetId(), closed.name()) != 1) {
            return false;
        }
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND period_name = ?", closed.sheetId(),
                closed.name());
        insertPersonTotals(closed);
        return true;
    }

    /** Soft-closes the open period, fixing its total and its people's figures; false when it was not open (any more). */
    public boolean softClose(Period soft) {
        if (jdbc.update("UPDATE period SET soft = TRUE, closed_total = ? WHERE sheet_id = ? AND name = ? AND NOT closed AND NOT soft",
                soft.closedTotal(), soft.sheetId(), soft.name()) != 1) {
            return false;
        }
        insertPersonTotals(soft);
        return true;
    }

    private void insertPersonTotals(Period closed) {
        closed.personTotals().forEach((personId, total) -> jdbc.update(
                "INSERT INTO period_person_total (sheet_id, period_name, person_id, total, carried) VALUES (?, ?, ?, ?, ?)",
                closed.sheetId(), closed.name(), personId, total,
                closed.personCarried().getOrDefault(personId, BigDecimal.ZERO.setScale(2))));
    }

    /** Reopens the (soft-)closed period, dropping its fixed total and its people's fixed figures; false when it was not closed (someone else reopened it). */
    public boolean reopen(Period period) {
        if (jdbc.update("UPDATE period SET closed = FALSE, soft = FALSE, closed_total = NULL WHERE sheet_id = ? AND name = ? AND (closed OR soft) AND NOT locked",
                period.sheetId(), period.name()) != 1) {
            return false;
        }
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND period_name = ?", period.sheetId(),
                period.name());
        return true;
    }

    /** Locks or unlocks the period; false when it already was so (someone else changed it). */
    public boolean lock(Period period, boolean locked) {
        return jdbc.update("UPDATE period SET locked = ? WHERE sheet_id = ? AND name = ? AND locked = ?", locked,
                period.sheetId(), period.name(), !locked) == 1;
    }
}

package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.sheet.SheetClock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The plans of the people on a sheet, and the plan dates each has made a claim for. */
@Repository
public class Plans {

    private static RowMapper<Plan> row(LocalDate today) {
        return (rs, i) -> {
            LocalDate start = rs.getDate("start_date").toLocalDate();
            LocalDate end = rs.getDate("end_date").toLocalDate();
            int every = rs.getInt("every_days");
            Integer day = (Integer) rs.getObject("month_day");
            List<String> next = Plan.dates(start, every, day, today, end, Plan.NEXT).stream()
                    .map(LocalDate::toString).toList();
            return new Plan(rs.getLong("id"), rs.getLong("person_id"),
                    rs.getBigDecimal("amount").setScale(2).toPlainString(), start.toString(), every, end.toString(),
                    rs.getInt("generated"), day, next);
        };
    }

    private final JdbcTemplate jdbc;

    private final SheetClock clock;

    public Plans(JdbcTemplate jdbc, SheetClock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** Every plan on the sheet, in the order added, with its next dates after the sheet's today. */
    public List<Plan> list(String sheet) {
        return jdbc.query("SELECT p.id, p.person_id, p.amount, p.start_date, p.every_days, p.end_date, p.month_day,"
                + " (SELECT COUNT(*) FROM plan_claim c WHERE c.plan_id = p.id) AS generated"
                + " FROM plan p WHERE p.sheet = ? ORDER BY p.id", row(clock.today(sheet)), sheet);
    }

    /** Adds a plan running every {@code every} days, or, when {@code day} is not null, monthly on that day. */
    public void add(String sheet, long person, BigDecimal amount, LocalDate start, int every, Integer day, LocalDate end) {
        jdbc.update("INSERT INTO plan (sheet, person_id, amount, start_date, every_days, month_day, end_date)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?)", sheet, person, amount, Date.valueOf(start), every, day, Date.valueOf(end));
    }

    /** Whether the plan has already made a claim for this plan date. */
    public boolean made(long plan, LocalDate day) {
        Integer found = jdbc.queryForObject("SELECT COUNT(*) FROM plan_claim WHERE plan_id = ? AND plan_day = ?",
                Integer.class, plan, Date.valueOf(day));
        return found != null && found > 0;
    }

    public void recordMade(long plan, LocalDate day, long claim) {
        jdbc.update("INSERT INTO plan_claim (plan_id, plan_day, claim_id) VALUES (?, ?, ?)", plan, Date.valueOf(day), claim);
    }
}

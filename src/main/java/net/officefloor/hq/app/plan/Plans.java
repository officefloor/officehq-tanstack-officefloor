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

    private RowMapper<Plan> row(LocalDate today) {
        return (rs, i) -> {
            List<Plan.Pause> pauses = pauses(rs.getLong("id"));
            LocalDate start = rs.getDate("start_date").toLocalDate();
            LocalDate end = rs.getDate("end_date").toLocalDate();
            int every = rs.getInt("every_days");
            Integer day = (Integer) rs.getObject("month_day");
            List<String> next = Plan.dates(start, every, day, pauses, today, end, Plan.NEXT).stream()
                    .map(LocalDate::toString).toList();
            return new Plan(rs.getLong("id"), rs.getLong("person_id"),
                    rs.getBigDecimal("amount").setScale(2).toPlainString(), start.toString(), every, end.toString(),
                    rs.getInt("generated"), day, next,
                    pauses.stream().anyMatch(p -> p.until() == null), changes(rs.getLong("id")), rs.getString("target_sheet"));
        };
    }

    private final JdbcTemplate jdbc;

    private final SheetClock clock;

    public Plans(JdbcTemplate jdbc, SheetClock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    private static final String SELECT = "SELECT p.id, p.person_id, p.amount, p.start_date, p.every_days, p.end_date,"
            + " p.month_day, p.target_sheet, (SELECT COUNT(*) FROM plan_claim c WHERE c.plan_id = p.id) AS generated FROM plan p";

    /** Every plan on the sheet, in the order added, with its next dates after the sheet's today. */
    public List<Plan> list(String sheet) {
        return jdbc.query(SELECT + " WHERE p.sheet = ? ORDER BY p.id", row(clock.today(sheet)), sheet);
    }

    /** The sheet's plan with this id, or null. */
    public Plan find(String sheet, long id) {
        return jdbc.query(SELECT + " WHERE p.sheet = ? AND p.id = ?", row(clock.today(sheet)), sheet, id).stream()
                .findFirst().orElse(null);
    }

    /** The plan's dates up to {@code last}, leaving out those its pauses skip. */
    public List<LocalDate> dates(Plan plan, LocalDate last) {
        return Plan.dates(LocalDate.parse(plan.start()), plan.every(), plan.day(), pauses(plan.id()), null, last,
                Integer.MAX_VALUE);
    }

    private List<Plan.Pause> pauses(long plan) {
        return jdbc.query("SELECT paused_on, resumed_on FROM plan_pause WHERE plan_id = ? ORDER BY id",
                (rs, i) -> new Plan.Pause(rs.getDate("paused_on").toLocalDate(),
                        rs.getDate("resumed_on") == null ? null : rs.getDate("resumed_on").toLocalDate()), plan);
    }

    private List<String> changes(long plan) {
        return jdbc.query("SELECT changed_on, amount FROM plan_change WHERE plan_id = ? ORDER BY id",
                (rs, i) -> rs.getDate("changed_on").toLocalDate() + " amount="
                        + rs.getBigDecimal("amount").setScale(2).toPlainString(), plan);
    }

    /** Changes the plan's amount on {@code day}, for the claims it generates from then on. */
    public void changeAmount(long plan, LocalDate day, BigDecimal amount) {
        jdbc.update("UPDATE plan SET amount = ? WHERE id = ?", amount, plan);
        jdbc.update("INSERT INTO plan_change (plan_id, changed_on, amount) VALUES (?, ?, ?)", plan, Date.valueOf(day),
                amount);
    }

    /** Pauses the plan from {@code day}: false, changing nothing, when it is already paused. */
    public boolean pause(long plan, LocalDate day) {
        return jdbc.update("INSERT INTO plan_pause (plan_id, paused_on) SELECT ?, ? FROM DUAL WHERE NOT EXISTS"
                + " (SELECT 1 FROM plan_pause WHERE plan_id = ? AND resumed_on IS NULL)", plan, Date.valueOf(day), plan) > 0;
    }

    /** Resumes the paused plan from {@code day}: false, changing nothing, when it is not paused. */
    public boolean resume(long plan, LocalDate day) {
        return jdbc.update("UPDATE plan_pause SET resumed_on = ? WHERE plan_id = ? AND resumed_on IS NULL",
                Date.valueOf(day), plan) > 0;
    }

    /** Adds a plan running every {@code every} days, or, when {@code day} is not null, monthly on that day. */
    /** {@code target}, when not null, is the descendant sheet it generates its claims on. */
    public void add(String sheet, long person, BigDecimal amount, LocalDate start, int every, Integer day, LocalDate end,
            String target) {
        jdbc.update("INSERT INTO plan (sheet, person_id, amount, start_date, every_days, month_day, end_date, target_sheet)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)", sheet, person, amount, Date.valueOf(start), every, day,
                Date.valueOf(end), target);
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

    /** The claims the plan generated, by plan date. */
    public List<Long> claimsMade(long plan) {
        return jdbc.queryForList("SELECT claim_id FROM plan_claim WHERE plan_id = ? ORDER BY plan_day", Long.class, plan);
    }
}

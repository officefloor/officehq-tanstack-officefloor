package net.officefloor.hq.app.plan;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PlanRepository {

    private static final RowMapper<Plan> ROW = (rs, n) -> new Plan(rs.getLong("id"), rs.getString("sheet_id"),
            rs.getLong("person_id"), rs.getBigDecimal("amount"), rs.getDate("start_date").toLocalDate(),
            rs.getObject("every_days", Integer.class), rs.getObject("month_day", Integer.class),
            rs.getDate("end_date").toLocalDate(), rs.getInt("generated"), List.of(), List.of(),
            rs.getString("target_sheet_id"));

    private final JdbcTemplate jdbc;

    public PlanRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's plans, in the order they were added, each with how many claims it has made. */
    public List<Plan> findAll(String sheetId) {
        return withPauses(jdbc.query("SELECT p.*, (SELECT COUNT(*) FROM person_plan_date d WHERE d.plan_id = p.id) AS generated"
                + " FROM person_plan p WHERE p.sheet_id = ? ORDER BY p.id", ROW, sheetId));
    }

    /** The sheet's plan, or null when it has none of that id. */
    public Plan find(String sheetId, long id) {
        List<Plan> found = withPauses(jdbc.query("SELECT p.*, (SELECT COUNT(*) FROM person_plan_date d"
                + " WHERE d.plan_id = p.id) AS generated FROM person_plan p WHERE p.sheet_id = ? AND p.id = ?", ROW,
                sheetId, id));
        return found.isEmpty() ? null : found.get(0);
    }

    /** Pauses the plan from the day. */
    public void pause(Plan plan, LocalDate on) {
        jdbc.update("INSERT INTO person_plan_pause (sheet_id, plan_id, paused_on) VALUES (?, ?, ?)", plan.sheetId(),
                plan.id(), Date.valueOf(on));
    }

    /** Resumes the plan's open pause on the day. */
    public void resume(Plan plan, LocalDate on) {
        jdbc.update("UPDATE person_plan_pause SET resumed_on = ? WHERE plan_id = ? AND resumed_on IS NULL",
                Date.valueOf(on), plan.id());
    }

    /** Changes the plan's amount on the day, keeping the change in its history. */
    public void changeAmount(Plan plan, LocalDate on, java.math.BigDecimal amount) {
        jdbc.update("UPDATE person_plan SET amount = ? WHERE id = ?", amount, plan.id());
        jdbc.update("INSERT INTO person_plan_change (sheet_id, plan_id, changed_on, amount) VALUES (?, ?, ?, ?)",
                plan.sheetId(), plan.id(), Date.valueOf(on), amount);
    }

    private List<Plan> withPauses(List<Plan> plans) {
        return plans.stream().map(p -> new Plan(p.id(), p.sheetId(), p.personId(), p.amount(), p.start(), p.every(),
                p.day(), p.end(), p.generated(), jdbc.query("SELECT paused_on, resumed_on FROM person_plan_pause"
                        + " WHERE plan_id = ? ORDER BY id", (rs, n) -> new PlanPause(rs.getDate("paused_on").toLocalDate(),
                                rs.getDate("resumed_on") == null ? null : rs.getDate("resumed_on").toLocalDate()),
                        p.id()), jdbc.query("SELECT changed_on, amount FROM person_plan_change WHERE plan_id = ?"
                                + " ORDER BY id", (rs, n) -> new PlanChange(rs.getDate("changed_on").toLocalDate(),
                                        rs.getBigDecimal("amount")), p.id()), p.targetSheet())).toList();
    }

    /** Saves a new plan; it with its id. */
    public Plan save(Plan plan) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement p = c.prepareStatement("INSERT INTO person_plan (sheet_id, person_id, amount, start_date,"
                    + " every_days, month_day, end_date, target_sheet_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", new String[] { "id" });
            p.setString(1, plan.sheetId());
            p.setLong(2, plan.personId());
            p.setBigDecimal(3, plan.amount());
            p.setDate(4, Date.valueOf(plan.start()));
            p.setObject(5, plan.every(), Types.INTEGER);
            p.setObject(6, plan.day(), Types.INTEGER);
            p.setDate(7, Date.valueOf(plan.end()));
            p.setString(8, plan.targetSheet());
            return p;
        }, keys);
        long id = keys.getKey().longValue();
        return new Plan(id, plan.sheetId(), plan.personId(), plan.amount(), plan.start(), plan.every(), plan.day(),
                plan.end(), 0, List.of(), List.of(), plan.targetSheet());
    }

    /** The dates the plan has made a claim for. */
    public List<LocalDate> datesMade(long planId) {
        return jdbc.query("SELECT plan_date FROM person_plan_date WHERE plan_id = ?",
                (rs, n) -> rs.getDate("plan_date").toLocalDate(), planId);
    }

    /** Records that the plan has made its claim for the date. */
    public void markMade(Plan plan, LocalDate date) {
        jdbc.update("INSERT INTO person_plan_date (sheet_id, plan_id, plan_date) VALUES (?, ?, ?)", plan.sheetId(),
                plan.id(), Date.valueOf(date));
    }
}

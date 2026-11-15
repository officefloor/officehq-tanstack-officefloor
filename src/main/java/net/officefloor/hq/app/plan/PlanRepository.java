package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PlanRepository {

    private static final String SELECT = "SELECT p.id, p.sheet_id, p.person_id, p.amount, p.start_date, p.every_days, p.month_day, "
            + "p.end_date, p.added_by, p.target_sheet, (SELECT COUNT(*) FROM plan_claim c WHERE c.plan_id = p.id) AS made FROM plan p ";

    private static final RowMapper<Plan> ROW = (rs, n) -> new Plan(rs.getLong("id"), rs.getString("sheet_id"),
            rs.getLong("person_id"), rs.getBigDecimal("amount").setScale(2), rs.getDate("start_date").toLocalDate(),
            rs.getObject("every_days", Integer.class), rs.getObject("month_day", Integer.class),
            rs.getDate("end_date").toLocalDate(), rs.getString("added_by"), rs.getInt("made"), List.of(), List.of(),
            rs.getString("target_sheet"));

    private final JdbcTemplate jdbc;

    public PlanRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's plans, in the order they were added. */
    public List<Plan> list(String sheetId) {
        return jdbc.query(SELECT + "WHERE p.sheet_id = ? ORDER BY p.id", ROW, sheetId).stream()
                .map(this::withHistory).toList();
    }

    public Plan find(long id) {
        List<Plan> found = jdbc.query(SELECT + "WHERE p.id = ?", ROW, id);
        return found.isEmpty() ? null : withHistory(found.get(0));
    }

    private Plan withHistory(Plan plan) {
        return withPauses(plan).withChanges(jdbc.query(
                "SELECT changed_on, amount FROM plan_change WHERE plan_id = ? ORDER BY id",
                (rs, n) -> new PlanChange(rs.getDate("changed_on").toLocalDate(), rs.getBigDecimal("amount").setScale(2)),
                plan.id()));
    }

    private Plan withPauses(Plan plan) {
        return plan.withPauses(jdbc.query("SELECT paused_on, resumed_on FROM plan_pause WHERE plan_id = ? ORDER BY id",
                (rs, n) -> new PlanPause(rs.getDate("paused_on").toLocalDate(),
                        rs.getDate("resumed_on") == null ? null : rs.getDate("resumed_on").toLocalDate()),
                plan.id()));
    }

    /** Pauses the plan from that day. */
    public void pause(long planId, LocalDate day) {
        jdbc.update("INSERT INTO plan_pause (plan_id, paused_on) VALUES (?, ?)", planId, Date.valueOf(day));
    }

    /** Resumes the plan's open pause on that day. */
    public void resume(long planId, LocalDate day) {
        jdbc.update("UPDATE plan_pause SET resumed_on = ? WHERE plan_id = ? AND resumed_on IS NULL",
                Date.valueOf(day), planId);
    }

    /** Changes the plan's amount from that day, keeping the change. */
    public void changeAmount(long planId, LocalDate day, BigDecimal amount) {
        jdbc.update("UPDATE plan SET amount = ? WHERE id = ?", amount, planId);
        jdbc.update("INSERT INTO plan_change (plan_id, changed_on, amount) VALUES (?, ?, ?)",
                planId, Date.valueOf(day), amount);
    }

    /** Adds the plan to the person. */
    public Plan add(String sheetId, long personId, ValidPlan plan, String by) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO plan (sheet_id, person_id, amount, start_date, every_days, month_day, end_date, added_by, target_sheet) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setLong(2, personId);
            ps.setBigDecimal(3, plan.amount());
            ps.setDate(4, Date.valueOf(plan.start()));
            ps.setObject(5, plan.every(), Types.INTEGER);
            ps.setObject(6, plan.day(), Types.INTEGER);
            ps.setDate(7, Date.valueOf(plan.end()));
            ps.setString(8, by);
            ps.setString(9, plan.sheet());
            return ps;
        }, key);
        return find(key.getKey().longValue());
    }

    /** The plan dates the plan has made a claim for. */
    public Set<LocalDate> made(long planId) {
        return new HashSet<>(jdbc.query("SELECT claim_date FROM plan_claim WHERE plan_id = ?",
                (rs, n) -> rs.getDate("claim_date").toLocalDate(), planId));
    }

    /** Keeps that the plan made the claim for that plan date. */
    public void recordMade(long planId, LocalDate date, long claimId) {
        jdbc.update("INSERT INTO plan_claim (plan_id, claim_date, claim_id) VALUES (?, ?, ?)",
                planId, Date.valueOf(date), claimId);
    }

}

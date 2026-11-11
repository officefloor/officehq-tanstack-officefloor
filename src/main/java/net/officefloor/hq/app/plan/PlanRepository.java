package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The plans store, kept per sheet, with the dates each plan has made its claim for. */
@Repository
public class PlanRepository {

    private static final String COLUMNS = "p.id, p.sheet_id, p.person_id, p.amount, p.start_date, p.every_days, p.day_of_month,"
            + " p.end_date, p.target_sheet_id, (SELECT COUNT(*) FROM plan_claim c WHERE c.plan_id = p.id) AS generated";

    private static final RowMapper<Plan> ROW = (rs, n) -> new Plan(rs.getLong("id"), rs.getString("sheet_id"),
            rs.getLong("person_id"), rs.getBigDecimal("amount").setScale(2),
            rs.getDate("start_date").toLocalDate(), rs.getObject("every_days", Integer.class),
            rs.getObject("day_of_month", Integer.class), rs.getDate("end_date").toLocalDate(),
            rs.getInt("generated"), List.of(), List.of(), rs.getString("target_sheet_id"));

    private final JdbcTemplate jdbc;

    public PlanRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's plans, in the order they were added. */
    public List<Plan> findAll(String sheetId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM plan p WHERE p.sheet_id = ? ORDER BY p.id", ROW, sheetId)
                .stream().map(this::withPauses).toList();
    }

    /** The plan on the sheet, or null. */
    public Plan find(String sheetId, long id) {
        List<Plan> found = jdbc.query("SELECT " + COLUMNS + " FROM plan p WHERE p.sheet_id = ? AND p.id = ?", ROW,
                sheetId, id);
        return found.isEmpty() ? null : withPauses(found.get(0));
    }

    private Plan withPauses(Plan p) {
        List<PlanPause> pauses = jdbc.query("SELECT paused_on, resumed_on FROM plan_pause WHERE plan_id = ? ORDER BY id",
                (rs, n) -> new PlanPause(rs.getDate("paused_on").toLocalDate(),
                        rs.getDate("resumed_on") == null ? null : rs.getDate("resumed_on").toLocalDate()),
                p.id());
        List<PlanChange> changes = jdbc.query("SELECT changed_on, amount FROM plan_change WHERE plan_id = ? ORDER BY id",
                (rs, n) -> new PlanChange(rs.getDate("changed_on").toLocalDate(), rs.getBigDecimal("amount").setScale(2)),
                p.id());
        return new Plan(p.id(), p.sheetId(), p.personId(), p.amount(), p.start(), p.every(), p.day(), p.end(),
                p.generated(), pauses, changes, p.target());
    }

    /** Pauses the plan from that day. */
    public void pause(long planId, LocalDate on) {
        jdbc.update("INSERT INTO plan_pause (plan_id, paused_on) VALUES (?, ?)", planId, Date.valueOf(on));
    }

    /** Resumes the plan's open pause on that day. */
    public void resume(long planId, LocalDate on) {
        jdbc.update("UPDATE plan_pause SET resumed_on = ? WHERE plan_id = ? AND resumed_on IS NULL", Date.valueOf(on),
                planId);
    }

    /** Changes the plan's amount on that day, recording the change. */
    public void changeAmount(long planId, LocalDate on, BigDecimal amount) {
        jdbc.update("UPDATE plan SET amount = ? WHERE id = ?", amount, planId);
        jdbc.update("INSERT INTO plan_change (plan_id, changed_on, amount) VALUES (?, ?, ?)", planId, Date.valueOf(on),
                amount);
    }

    /** Inserts a new plan, returning it with its id. */
    public Plan save(Plan plan) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO plan (sheet_id, person_id, amount, start_date, every_days, day_of_month, end_date, target_sheet_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, plan.sheetId());
            ps.setLong(2, plan.personId());
            ps.setBigDecimal(3, plan.amount());
            ps.setDate(4, Date.valueOf(plan.start()));
            ps.setObject(5, plan.every(), Types.INTEGER);
            ps.setObject(6, plan.day(), Types.INTEGER);
            ps.setDate(7, Date.valueOf(plan.end()));
            ps.setString(8, plan.target());
            return ps;
        }, keys);
        long id = ((Number) keys.getKeys().get("ID")).longValue();
        return new Plan(id, plan.sheetId(), plan.personId(), plan.amount(), plan.start(), plan.every(), plan.day(),
                plan.end(), 0, List.of(), List.of(), plan.target());
    }

    /** The dates the plan has made its claim for. */
    public Set<LocalDate> made(long planId) {
        return new HashSet<>(jdbc.query("SELECT claim_date FROM plan_claim WHERE plan_id = ?",
                (rs, n) -> rs.getDate("claim_date").toLocalDate(), planId));
    }

    /** The ids of the claims the plan made, in the order it made them. */
    public List<Long> madeClaims(long planId) {
        return jdbc.query("SELECT claim_id FROM plan_claim WHERE plan_id = ? ORDER BY claim_id",
                (rs, n) -> rs.getLong("claim_id"), planId);
    }

    /** Records that the plan made this claim for that date. */
    public void recordMade(long planId, LocalDate date, long claimId) {
        jdbc.update("INSERT INTO plan_claim (plan_id, claim_date, claim_id) VALUES (?, ?, ?)", planId,
                Date.valueOf(date), claimId);
    }
}

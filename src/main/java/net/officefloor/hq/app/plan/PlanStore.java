package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The plans per sheet, in the order they were added, the plan dates each has made a claim for, their pauses and their amount changes. */
@Repository
public class PlanStore {

    private static final String SELECT = "SELECT p.*, (SELECT COUNT(*) FROM plan_made m WHERE m.plan_id = p.id) AS generated"
            + " FROM plan p";

    private static final RowMapper<Plan> ROW = (rs, n) -> new Plan(rs.getLong("id"), rs.getString("sheet_id"),
            rs.getLong("person_id"), rs.getBigDecimal("amount").setScale(2), rs.getObject("start_date", LocalDate.class),
            rs.getObject("every_days", Integer.class), rs.getObject("month_day", Integer.class),
            rs.getObject("end_date", LocalDate.class), rs.getInt("generated"), List.of(), List.of());

    private final JdbcTemplate jdbc;

    public PlanStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's plans, in the order they were added. */
    public List<Plan> list(String sheetId) {
        return jdbc.query(SELECT + " WHERE p.sheet_id = ? ORDER BY p.id", ROW, sheetId).stream().map(this::withDetail).toList();
    }

    public Plan find(long id) {
        List<Plan> found = jdbc.query(SELECT + " WHERE p.id = ?", ROW, id);
        return found.isEmpty() ? null : withDetail(found.get(0));
    }

    private Plan withDetail(Plan p) {
        List<PlanPause> pauses = jdbc.query("SELECT paused_on, resumed_on FROM plan_pause WHERE plan_id = ? ORDER BY paused_on",
                (rs, n) -> new PlanPause(rs.getObject("paused_on", LocalDate.class), rs.getObject("resumed_on", LocalDate.class)),
                p.id());
        List<PlanChange> changes = jdbc.query("SELECT changed_on, amount FROM plan_change WHERE plan_id = ? ORDER BY id",
                (rs, n) -> new PlanChange(rs.getObject("changed_on", LocalDate.class), rs.getBigDecimal("amount").setScale(2)),
                p.id());
        return new Plan(p.id(), p.sheetId(), p.personId(), p.amount(), p.start(), p.every(), p.day(), p.end(),
                p.generated(), pauses, changes);
    }

    /** Changes the plan's amount from the date on, keeping the change. */
    public void changeAmount(long planId, LocalDate date, BigDecimal amount) {
        jdbc.update("UPDATE plan SET amount = ? WHERE id = ?", amount, planId);
        jdbc.update("INSERT INTO plan_change (plan_id, changed_on, amount) VALUES (?, ?, ?)", planId, date, amount);
    }

    /** Pauses the plan from the date. */
    public void pause(long planId, LocalDate date) {
        jdbc.update("INSERT INTO plan_pause (plan_id, paused_on) VALUES (?, ?)", planId, date);
    }

    /** Resumes the plan's pause on the date. */
    public void resume(long planId, LocalDate date) {
        jdbc.update("UPDATE plan_pause SET resumed_on = ? WHERE plan_id = ? AND resumed_on IS NULL", date, planId);
    }

    public Plan add(String sheetId, long personId, PlanInput input) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(c -> {
            var ps = c.prepareStatement("INSERT INTO plan (sheet_id, person_id, amount, start_date, every_days, month_day, end_date)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?)", new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setLong(2, personId);
            ps.setBigDecimal(3, input.amount());
            ps.setObject(4, input.start());
            ps.setObject(5, input.every());
            ps.setObject(6, input.day());
            ps.setObject(7, input.end());
            return ps;
        }, keys);
        return find(keys.getKey().longValue());
    }

    /** Whether the plan has made a claim for the plan date. */
    public boolean made(long planId, LocalDate date) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM plan_made WHERE plan_id = ? AND made_on = ?",
                Integer.class, planId, date);
        return n != null && n > 0;
    }

    public void markMade(long planId, LocalDate date) {
        jdbc.update("INSERT INTO plan_made (plan_id, made_on) VALUES (?, ?)", planId, date);
    }
}

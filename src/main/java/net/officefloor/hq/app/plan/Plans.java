package net.officefloor.hq.app.plan;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The plan store: each sheet's plans in the order they were added, and the plan dates each has made a claim for. */
@Repository
public class Plans {

    private static final String SELECT = "SELECT p.id, p.sheet_id, p.person_id, p.amount, p.start_date, p.every_days, p.month_day, p.end_date,"
            + " (SELECT COUNT(*) FROM plan_claim c WHERE c.plan_id = p.id) AS generated FROM plan p";

    private static final RowMapper<Plan> ROW = (rs, n) -> new Plan(rs.getLong("id"), rs.getString("sheet_id"), rs.getLong("person_id"),
            rs.getBigDecimal("amount").setScale(2), rs.getDate("start_date").toLocalDate(), rs.getObject("every_days", Integer.class),
            rs.getObject("month_day", Integer.class), rs.getDate("end_date").toLocalDate(), rs.getInt("generated"), List.of());

    private final JdbcTemplate jdbc;

    public Plans(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's plans, in the order they were added. */
    public List<Plan> list(String sheetId) {
        return jdbc.query(SELECT + " WHERE p.sheet_id = ? ORDER BY p.id", ROW, sheetId).stream().map(this::withPauses).toList();
    }

    public Plan find(long id) {
        return jdbc.query(SELECT + " WHERE p.id = ?", ROW, id).stream().findFirst().map(this::withPauses).orElse(null);
    }

    private Plan withPauses(Plan plan) {
        return plan.withPauses(jdbc.query("SELECT paused_on, resumed_on FROM plan_pause WHERE plan_id = ? ORDER BY id",
                (rs, n) -> new Plan.Pause(rs.getDate("paused_on").toLocalDate(),
                        rs.getDate("resumed_on") == null ? null : rs.getDate("resumed_on").toLocalDate()), plan.id()));
    }

    /** Pauses the plan from the day. */
    public void pause(long planId, LocalDate on) {
        jdbc.update("INSERT INTO plan_pause (plan_id, paused_on) VALUES (?, ?)", planId, Date.valueOf(on));
    }

    /** Resumes the plan's open pause on the day. */
    public void resume(long planId, LocalDate on) {
        jdbc.update("UPDATE plan_pause SET resumed_on = ? WHERE plan_id = ? AND resumed_on IS NULL", Date.valueOf(on), planId);
    }

    public Plan add(String sheetId, long personId, java.math.BigDecimal amount, LocalDate start, Integer every, Integer day, LocalDate end) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO plan (sheet_id, person_id, amount, start_date, every_days, month_day, end_date) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, sheetId);
            ps.setLong(2, personId);
            ps.setBigDecimal(3, amount);
            ps.setDate(4, Date.valueOf(start));
            ps.setObject(5, every, Types.INTEGER);
            ps.setObject(6, day, Types.INTEGER);
            ps.setDate(7, Date.valueOf(end));
            return ps;
        }, keys);
        return find(keys.getKey().longValue());
    }

    /** Whether the plan has made its claim for the date. */
    public boolean made(long planId, LocalDate date) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM plan_claim WHERE plan_id = ? AND claim_date = ?", Integer.class,
                planId, Date.valueOf(date));
        return n != null && n > 0;
    }

    /** Records that the plan made the claim for the date. */
    public void recordMade(long planId, LocalDate date, long claimId) {
        jdbc.update("INSERT INTO plan_claim (plan_id, claim_date, claim_id) VALUES (?, ?, ?)", planId, Date.valueOf(date), claimId);
    }
}

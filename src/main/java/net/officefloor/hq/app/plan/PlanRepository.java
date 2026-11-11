package net.officefloor.hq.app.plan;

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
            + " p.end_date, (SELECT COUNT(*) FROM plan_claim c WHERE c.plan_id = p.id) AS generated";

    private static final RowMapper<Plan> ROW = (rs, n) -> new Plan(rs.getLong("id"), rs.getString("sheet_id"),
            rs.getLong("person_id"), rs.getBigDecimal("amount").setScale(2),
            rs.getDate("start_date").toLocalDate(), rs.getObject("every_days", Integer.class),
            rs.getObject("day_of_month", Integer.class), rs.getDate("end_date").toLocalDate(),
            rs.getInt("generated"));

    private final JdbcTemplate jdbc;

    public PlanRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's plans, in the order they were added. */
    public List<Plan> findAll(String sheetId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM plan p WHERE p.sheet_id = ? ORDER BY p.id", ROW, sheetId);
    }

    /** Inserts a new plan, returning it with its id. */
    public Plan save(Plan plan) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO plan (sheet_id, person_id, amount, start_date, every_days, day_of_month, end_date) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, plan.sheetId());
            ps.setLong(2, plan.personId());
            ps.setBigDecimal(3, plan.amount());
            ps.setDate(4, Date.valueOf(plan.start()));
            ps.setObject(5, plan.every(), Types.INTEGER);
            ps.setObject(6, plan.day(), Types.INTEGER);
            ps.setDate(7, Date.valueOf(plan.end()));
            return ps;
        }, keys);
        long id = ((Number) keys.getKeys().get("ID")).longValue();
        return new Plan(id, plan.sheetId(), plan.personId(), plan.amount(), plan.start(), plan.every(), plan.day(),
                plan.end(), 0);
    }

    /** The dates the plan has made its claim for. */
    public Set<LocalDate> made(long planId) {
        return new HashSet<>(jdbc.query("SELECT claim_date FROM plan_claim WHERE plan_id = ?",
                (rs, n) -> rs.getDate("claim_date").toLocalDate(), planId));
    }

    /** Records that the plan made this claim for that date. */
    public void recordMade(long planId, LocalDate date, long claimId) {
        jdbc.update("INSERT INTO plan_claim (plan_id, claim_date, claim_id) VALUES (?, ?, ?)", planId,
                Date.valueOf(date), claimId);
    }
}

package net.officefloor.hq.app.plan;

import java.sql.Date;
import java.sql.PreparedStatement;
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
            rs.getInt("every_days"), rs.getDate("end_date").toLocalDate(), rs.getInt("generated"));

    private final JdbcTemplate jdbc;

    public PlanRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's plans, in the order they were added, each with how many claims it has made. */
    public List<Plan> findAll(String sheetId) {
        return jdbc.query("SELECT p.*, (SELECT COUNT(*) FROM person_plan_date d WHERE d.plan_id = p.id) AS generated"
                + " FROM person_plan p WHERE p.sheet_id = ? ORDER BY p.id", ROW, sheetId);
    }

    /** Saves a new plan; it with its id. */
    public Plan save(Plan plan) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement p = c.prepareStatement("INSERT INTO person_plan (sheet_id, person_id, amount, start_date,"
                    + " every_days, end_date) VALUES (?, ?, ?, ?, ?, ?)", new String[] { "id" });
            p.setString(1, plan.sheetId());
            p.setLong(2, plan.personId());
            p.setBigDecimal(3, plan.amount());
            p.setDate(4, Date.valueOf(plan.start()));
            p.setInt(5, plan.every());
            p.setDate(6, Date.valueOf(plan.end()));
            return p;
        }, keys);
        long id = keys.getKey().longValue();
        return new Plan(id, plan.sheetId(), plan.personId(), plan.amount(), plan.start(), plan.every(), plan.end(), 0);
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

package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The plans per sheet, in the order they were added, and the plan dates each has made a claim for. */
@Repository
public class PlanStore {

    private static final String SELECT = "SELECT p.*, (SELECT COUNT(*) FROM plan_made m WHERE m.plan_id = p.id) AS generated"
            + " FROM plan p";

    private static final RowMapper<Plan> ROW = (rs, n) -> new Plan(rs.getLong("id"), rs.getString("sheet_id"),
            rs.getLong("person_id"), rs.getBigDecimal("amount").setScale(2), rs.getObject("start_date", LocalDate.class),
            rs.getInt("every_days"), rs.getObject("end_date", LocalDate.class), rs.getInt("generated"));

    private final JdbcTemplate jdbc;

    public PlanStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's plans, in the order they were added. */
    public List<Plan> list(String sheetId) {
        return jdbc.query(SELECT + " WHERE p.sheet_id = ? ORDER BY p.id", ROW, sheetId);
    }

    public Plan find(long id) {
        List<Plan> found = jdbc.query(SELECT + " WHERE p.id = ?", ROW, id);
        return found.isEmpty() ? null : found.get(0);
    }

    public Plan add(String sheetId, long personId, PlanInput input) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(c -> {
            var ps = c.prepareStatement("INSERT INTO plan (sheet_id, person_id, amount, start_date, every_days, end_date)"
                    + " VALUES (?, ?, ?, ?, ?, ?)", new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setLong(2, personId);
            ps.setBigDecimal(3, input.amount());
            ps.setObject(4, input.start());
            ps.setInt(5, input.every());
            ps.setObject(6, input.end());
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

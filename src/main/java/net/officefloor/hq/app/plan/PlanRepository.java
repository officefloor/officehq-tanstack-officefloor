package net.officefloor.hq.app.plan;

import java.sql.Date;
import java.sql.PreparedStatement;
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

    private static final String SELECT = "SELECT p.id, p.sheet_id, p.person_id, p.amount, p.start_date, p.every_days, "
            + "p.end_date, p.added_by, (SELECT COUNT(*) FROM plan_claim c WHERE c.plan_id = p.id) AS made FROM plan p ";

    private static final RowMapper<Plan> ROW = (rs, n) -> new Plan(rs.getLong("id"), rs.getString("sheet_id"),
            rs.getLong("person_id"), rs.getBigDecimal("amount").setScale(2), rs.getDate("start_date").toLocalDate(),
            rs.getInt("every_days"), rs.getDate("end_date").toLocalDate(), rs.getString("added_by"), rs.getInt("made"));

    private final JdbcTemplate jdbc;

    public PlanRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's plans, in the order they were added. */
    public List<Plan> list(String sheetId) {
        return jdbc.query(SELECT + "WHERE p.sheet_id = ? ORDER BY p.id", ROW, sheetId);
    }

    public Plan find(long id) {
        List<Plan> found = jdbc.query(SELECT + "WHERE p.id = ?", ROW, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Adds the plan to the person. */
    public Plan add(String sheetId, long personId, ValidPlan plan, String by) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO plan (sheet_id, person_id, amount, start_date, every_days, end_date, added_by) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setLong(2, personId);
            ps.setBigDecimal(3, plan.amount());
            ps.setDate(4, Date.valueOf(plan.start()));
            ps.setInt(5, plan.every());
            ps.setDate(6, Date.valueOf(plan.end()));
            ps.setString(7, by);
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

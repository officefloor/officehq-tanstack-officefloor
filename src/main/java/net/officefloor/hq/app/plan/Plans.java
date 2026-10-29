package net.officefloor.hq.app.plan;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** The store of the sheet's plans and of the dates each has made its claim for. */
@Repository
public class Plans {

    private final JdbcTemplate jdbc;

    public Plans(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's plans, in the order they were added. */
    public List<Plan> list(String sheetId) {
        return jdbc.query("SELECT id, sheet_id, person_id, amount, start_date, every_days, end_date FROM plan "
                + "WHERE sheet_id = ? ORDER BY id",
                (rs, n) -> new Plan(rs.getLong("id"), rs.getString("sheet_id"), rs.getLong("person_id"),
                        rs.getBigDecimal("amount").setScale(2), rs.getDate("start_date").toLocalDate(),
                        rs.getInt("every_days"), rs.getDate("end_date").toLocalDate()),
                sheetId);
    }

    /** Adds the plan: the stored plan with its id. */
    public Plan add(Plan plan) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement("INSERT INTO plan (sheet_id, person_id, amount, start_date, every_days, end_date) "
                    + "VALUES (?, ?, ?, ?, ?, ?)", new String[] { "id" });
            ps.setString(1, plan.sheetId());
            ps.setLong(2, plan.personId());
            ps.setBigDecimal(3, plan.amount());
            ps.setDate(4, Date.valueOf(plan.start()));
            ps.setInt(5, plan.every());
            ps.setDate(6, Date.valueOf(plan.end()));
            return ps;
        }, key);
        return new Plan(key.getKey().longValue(), plan.sheetId(), plan.personId(), plan.amount(), plan.start(),
                plan.every(), plan.end());
    }

    /** The dates the plan has made its claim for. */
    public List<LocalDate> made(long planId) {
        return jdbc.query("SELECT on_date FROM plan_claim WHERE plan_id = ? ORDER BY on_date",
                (rs, n) -> rs.getDate("on_date").toLocalDate(), planId);
    }

    /** How many claims each plan of the sheet has made, by plan id (absent for none). */
    public Map<Long, Integer> madeCounts(String sheetId) {
        return jdbc.query("SELECT pc.plan_id, COUNT(*) AS n FROM plan_claim pc JOIN plan p ON p.id = pc.plan_id "
                + "WHERE p.sheet_id = ? GROUP BY pc.plan_id",
                (rs, n) -> Map.entry(rs.getLong("plan_id"), rs.getInt("n")), sheetId)
                .stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /** Records that the plan made the claim for the date. */
    public void markMade(long planId, LocalDate date, long claimId) {
        jdbc.update("INSERT INTO plan_claim (plan_id, on_date, claim_id) VALUES (?, ?, ?)", planId, Date.valueOf(date), claimId);
    }
}

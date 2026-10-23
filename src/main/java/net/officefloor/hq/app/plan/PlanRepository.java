package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.person.Person;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** People's plans by sheet, and the plan dates each has made a claim for. */
@Repository
public class PlanRepository {

    private static final RowMapper<Plan> ROW = (rs, i) -> new Plan(rs.getLong("id"), rs.getString("sheet_id"),
            rs.getLong("person_id"), rs.getBigDecimal("amount"), rs.getObject("start_date", LocalDate.class),
            rs.getObject("every_days", Integer.class), rs.getObject("day_of_month", Integer.class),
            rs.getObject("end_date", LocalDate.class), rs.getInt("generated"));

    private final JdbcTemplate jdbc;

    public PlanRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every plan on the sheet (its people's), in the order they were added, each with how many claims it has made. */
    public List<Plan> list(String sheetId) {
        return jdbc.query("SELECT pl.id, p.sheet_id, pl.person_id, pl.amount, pl.start_date, pl.every_days, pl.day_of_month, "
                + "pl.end_date, "
                + "(SELECT COUNT(*) FROM plan_claim c WHERE c.plan_id = pl.id) AS generated "
                + "FROM plan pl JOIN person p ON p.id = pl.person_id WHERE p.sheet_id = ? ORDER BY pl.id", ROW, sheetId);
    }

    public Plan insert(Person person, PlanInput input) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement("INSERT INTO plan (person_id, amount, start_date, every_days, day_of_month, end_date, "
                    + "added_by) VALUES (?, ?, ?, ?, ?, ?, ?)", new String[] { "id" });
            ps.setLong(1, person.id());
            ps.setBigDecimal(2, input.amount());
            ps.setObject(3, input.start());
            ps.setObject(4, input.every(), java.sql.Types.INTEGER);
            ps.setObject(5, input.day(), java.sql.Types.INTEGER);
            ps.setObject(6, input.end());
            ps.setString(7, input.user());
            return ps;
        }, keys);
        return new Plan(keys.getKey().longValue(), person.sheetId(), person.id(), input.amount(), input.start(),
                input.every(), input.day(), input.end(), 0);
    }

    /** Whether the plan has made a claim for this plan date. */
    public boolean made(long planId, LocalDate date) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM plan_claim WHERE plan_id = ? AND on_date = ?",
                Integer.class, planId, date);
        return n != null && n > 0;
    }

    /** Keep that the plan has made this claim for this plan date. */
    public void made(long planId, LocalDate date, long claimId) {
        jdbc.update("INSERT INTO plan_claim (plan_id, on_date, claim_id) VALUES (?, ?, ?)", planId, date, claimId);
    }
}

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
            rs.getObject("end_date", LocalDate.class), rs.getInt("generated"), List.of());

    private final JdbcTemplate jdbc;

    public PlanRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every plan on the sheet (its people's), in the order they were added, each with how many claims it has made. */
    public List<Plan> list(String sheetId) {
        return jdbc.query("SELECT pl.id, p.sheet_id, pl.person_id, pl.amount, pl.start_date, pl.every_days, pl.day_of_month, "
                + "pl.end_date, "
                + "(SELECT COUNT(*) FROM plan_claim c WHERE c.plan_id = pl.id) AS generated "
                + "FROM plan pl JOIN person p ON p.id = pl.person_id WHERE p.sheet_id = ? ORDER BY pl.id", ROW, sheetId)
                .stream().map(this::withPauses).toList();
    }

    /** The plan on the sheet with this id, with its pauses, or null when there is none. */
    public Plan find(String sheetId, long planId) {
        return list(sheetId).stream().filter(p -> p.id() == planId).findFirst().orElse(null);
    }

    private Plan withPauses(Plan p) {
        List<PlanPause> pauses = jdbc.query("SELECT paused_on, resumed_on FROM plan_pause WHERE plan_id = ? ORDER BY id",
                (rs, i) -> new PlanPause(rs.getObject("paused_on", LocalDate.class),
                        rs.getObject("resumed_on", LocalDate.class)), p.id());
        return new Plan(p.id(), p.sheetId(), p.personId(), p.amount(), p.start(), p.every(), p.day(), p.end(),
                p.generated(), pauses);
    }

    /** Pause the plan from this day: false when it is already paused (nothing written). */
    public boolean pause(long planId, LocalDate on, String user) {
        jdbc.queryForList("SELECT id FROM plan WHERE id = ? FOR UPDATE", planId);
        return jdbc.update("INSERT INTO plan_pause (plan_id, paused_on, paused_by) SELECT ?, ?, ? WHERE NOT EXISTS "
                + "(SELECT 1 FROM plan_pause WHERE plan_id = ? AND resumed_on IS NULL)", planId, on, user, planId) > 0;
    }

    /** Resume the paused plan from this day (not before the day it was paused): false when it is not paused. */
    public boolean resume(long planId, LocalDate on, String user) {
        jdbc.queryForList("SELECT id FROM plan WHERE id = ? FOR UPDATE", planId);
        return jdbc.update("UPDATE plan_pause SET resumed_on = GREATEST(paused_on, ?), resumed_by = ? "
                + "WHERE plan_id = ? AND resumed_on IS NULL", on, user, planId) > 0;
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
                input.every(), input.day(), input.end(), 0, List.of());
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

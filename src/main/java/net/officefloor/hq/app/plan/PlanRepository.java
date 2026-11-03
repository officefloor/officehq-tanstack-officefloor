package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PlanRepository {

    private final JdbcTemplate jdbc;

    public PlanRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Lock the sheet's row so one generate runs at a time; false when there is no such sheet. */
    public boolean lockSheet(String sheetId) {
        return !jdbc.queryForList("SELECT id FROM sheet WHERE id = ? FOR UPDATE", String.class, sheetId).isEmpty();
    }

    /** The person's name, or null when they are not on the sheet. */
    public String personName(String sheetId, long personId) {
        List<String> names = jdbc.queryForList("SELECT name FROM person WHERE sheet_id = ? AND id = ?",
                String.class, sheetId, personId);
        return names.isEmpty() ? null : names.get(0);
    }

    public Plan add(String sheetId, long personId, PlanInput input) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO plan (sheet_id, person_id, amount, start_date, every_days, end_date) VALUES (?, ?, ?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setLong(2, personId);
            ps.setBigDecimal(3, input.amount());
            ps.setDate(4, Date.valueOf(input.start()));
            ps.setInt(5, input.every());
            ps.setDate(6, Date.valueOf(input.end()));
            return ps;
        }, keys);
        return new Plan(keys.getKey().longValue(), personId, money(input.amount()), input.start().toString(),
                input.every(), input.end().toString(), 0);
    }

    /** The sheet's plans, in the order they were added, each with how many claims it has made. */
    public List<Plan> list(String sheetId) {
        return jdbc.query("SELECT p.id, p.person_id, p.amount, p.start_date, p.every_days, p.end_date,"
                + " (SELECT COUNT(*) FROM plan_claim c WHERE c.plan_id = p.id) AS generated"
                + " FROM plan p WHERE p.sheet_id = ? ORDER BY p.id",
                (rs, i) -> new Plan(rs.getLong("id"), rs.getLong("person_id"), money(rs.getBigDecimal("amount")),
                        rs.getDate("start_date").toLocalDate().toString(), rs.getInt("every_days"),
                        rs.getDate("end_date").toLocalDate().toString(), rs.getInt("generated")),
                sheetId);
    }

    /** The dates a plan has already made a claim for. */
    public Set<LocalDate> madeOn(long planId) {
        return new HashSet<>(jdbc.query("SELECT on_date FROM plan_claim WHERE plan_id = ?",
                (rs, i) -> rs.getDate("on_date").toLocalDate(), planId));
    }

    /** Keep that a plan made a claim for a date. */
    public void made(String sheetId, long planId, LocalDate on, long claimId) {
        jdbc.update("INSERT INTO plan_claim (sheet_id, plan_id, on_date, claim_id) VALUES (?, ?, ?, ?)",
                sheetId, planId, Date.valueOf(on), claimId);
    }

    static String money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_EVEN).toPlainString();
    }
}

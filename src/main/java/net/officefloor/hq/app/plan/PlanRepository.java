package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The plans of each sheet, in the order added, and the plan dates each has made a claim for. */
@Repository
public class PlanRepository {

    private static final String SELECT = "SELECT p.id, p.sheet_id, p.person_id, p.amount, p.start_date, p.every_days,"
            + " p.end_date, (SELECT COUNT(*) FROM plan_claim c WHERE c.plan_id = p.id) AS generated FROM plan p";

    private final JdbcTemplate jdbc;

    public PlanRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's plans, in the order they were added. */
    public List<Plan> list(String sheetId) {
        return jdbc.query(SELECT + " WHERE p.sheet_id = ? ORDER BY p.id", PlanRepository::map, sheetId);
    }

    public Plan insert(String sheetId, long personId, BigDecimal amount, LocalDate start, int every, LocalDate end) {
        long id = jdbc.queryForObject("SELECT id FROM FINAL TABLE (INSERT INTO plan (sheet_id, person_id, amount,"
                + " start_date, every_days, end_date) VALUES (?, ?, ?, ?, ?, ?))", Long.class, sheetId, personId, amount,
                Date.valueOf(start), every, Date.valueOf(end));
        return new Plan(id, sheetId, personId, amount, start, every, end, 0);
    }

    /** The plan dates the plan has made a claim for. */
    public Set<LocalDate> made(long planId) {
        return new HashSet<>(jdbc.query("SELECT plan_date FROM plan_claim WHERE plan_id = ?",
                (rs, n) -> rs.getObject(1, LocalDate.class), planId));
    }

    /** Note that the plan has made its claim for the date. */
    public void markMade(Plan plan, LocalDate date) {
        jdbc.update("INSERT INTO plan_claim (sheet_id, plan_id, plan_date) VALUES (?, ?, ?)", plan.sheetId(), plan.id(),
                Date.valueOf(date));
    }

    private static Plan map(ResultSet rs, int n) throws SQLException {
        return new Plan(rs.getLong("id"), rs.getString("sheet_id"), rs.getLong("person_id"),
                rs.getBigDecimal("amount").setScale(2), rs.getObject("start_date", LocalDate.class),
                rs.getInt("every_days"), rs.getObject("end_date", LocalDate.class), rs.getInt("generated"));
    }
}

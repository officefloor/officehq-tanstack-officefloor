package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The plans of the people on a sheet, and the plan dates each has made a claim for. */
@Repository
public class Plans {

    private static final RowMapper<Plan> ROW = (rs, i) -> new Plan(rs.getLong("id"), rs.getLong("person_id"),
            rs.getBigDecimal("amount").setScale(2).toPlainString(), rs.getDate("start_date").toLocalDate().toString(),
            rs.getInt("every_days"), rs.getDate("end_date").toLocalDate().toString(), rs.getInt("generated"));

    private final JdbcTemplate jdbc;

    public Plans(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every plan on the sheet, in the order added. */
    public List<Plan> list(String sheet) {
        return jdbc.query("SELECT p.id, p.person_id, p.amount, p.start_date, p.every_days, p.end_date,"
                + " (SELECT COUNT(*) FROM plan_claim c WHERE c.plan_id = p.id) AS generated"
                + " FROM plan p WHERE p.sheet = ? ORDER BY p.id", ROW, sheet);
    }

    public void add(String sheet, long person, BigDecimal amount, LocalDate start, int every, LocalDate end) {
        jdbc.update("INSERT INTO plan (sheet, person_id, amount, start_date, every_days, end_date) VALUES (?, ?, ?, ?, ?, ?)",
                sheet, person, amount, Date.valueOf(start), every, Date.valueOf(end));
    }

    /** Whether the plan has already made a claim for this plan date. */
    public boolean made(long plan, LocalDate day) {
        Integer found = jdbc.queryForObject("SELECT COUNT(*) FROM plan_claim WHERE plan_id = ? AND plan_day = ?",
                Integer.class, plan, Date.valueOf(day));
        return found != null && found > 0;
    }

    public void recordMade(long plan, LocalDate day, long claim) {
        jdbc.update("INSERT INTO plan_claim (plan_id, plan_day, claim_id) VALUES (?, ?, ?)", plan, Date.valueOf(day), claim);
    }
}

package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The plans of each sheet, in the order added, and the plan dates each has made its claim for. */
@Repository
public class Plans {

    private final JdbcTemplate jdbc;

    public Plans(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every plan on the sheet, in the order added, with how many claims it has made so far. */
    public List<Plan> onSheet(String sheetId) {
        return jdbc.query("SELECT p.id, p.sheet_id, p.person_id, p.amount, p.start_date, p.every_days, p.month_day, p.end_date, "
                + "(SELECT COUNT(*) FROM plan_claim c WHERE c.plan_id = p.id) "
                + "FROM plan p WHERE p.sheet_id = ? ORDER BY p.id",
                (rs, i) -> new Plan(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getBigDecimal(4).setScale(2),
                        rs.getDate(5).toLocalDate(), rs.getObject(6, Integer.class), rs.getObject(7, Integer.class),
                        rs.getDate(8).toLocalDate(), rs.getInt(9)),
                sheetId);
    }

    /** Adds the plan; answers its id. */
    public long add(String sheetId, long personId, BigDecimal amount, LocalDate start, Integer every, Integer day,
            LocalDate end) {
        jdbc.update("INSERT INTO plan (sheet_id, person_id, amount, start_date, every_days, month_day, end_date) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)", sheetId, personId, amount, Date.valueOf(start), every, day,
                Date.valueOf(end));
        return jdbc.queryForObject("SELECT MAX(id) FROM plan WHERE sheet_id = ? AND person_id = ?", Long.class,
                sheetId, personId);
    }

    /** Whether the plan has made its claim for that plan date. */
    public boolean made(long planId, LocalDate date) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM plan_claim WHERE plan_id = ? AND on_date = ?",
                Integer.class, planId, Date.valueOf(date));
        return n != null && n > 0;
    }

    /** Notes the plan has made its claim for that plan date. */
    public void markMade(String sheetId, long planId, LocalDate date) {
        jdbc.update("INSERT INTO plan_claim (sheet_id, plan_id, on_date) VALUES (?, ?, ?)", sheetId, planId,
                Date.valueOf(date));
    }
}

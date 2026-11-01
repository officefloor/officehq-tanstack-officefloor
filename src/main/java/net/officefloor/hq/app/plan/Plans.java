package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The plans of each sheet, in the order added, the plan dates each has made its claim for, its pauses
 *  and its changes of amount. */
@Repository
public class Plans {

    private final JdbcTemplate jdbc;

    public Plans(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every plan on the sheet, in the order added, with how many claims it has made so far and its pauses. */
    public List<Plan> onSheet(String sheetId) {
        Map<Long, List<PlanChange>> changes = new HashMap<>();
        jdbc.query("SELECT plan_id, changed_on, amount FROM plan_change WHERE sheet_id = ? ORDER BY id",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> changes
                        .computeIfAbsent(rs.getLong(1), k -> new ArrayList<>())
                        .add(new PlanChange(rs.getDate(2).toLocalDate(), rs.getBigDecimal(3).setScale(2))),
                sheetId);
        Map<Long, List<PlanPause>> pauses = new HashMap<>();
        jdbc.query("SELECT plan_id, paused_on, resumed_on FROM plan_pause WHERE sheet_id = ? ORDER BY id",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> pauses
                        .computeIfAbsent(rs.getLong(1), k -> new ArrayList<>())
                        .add(new PlanPause(rs.getDate(2).toLocalDate(),
                                rs.getDate(3) == null ? null : rs.getDate(3).toLocalDate())),
                sheetId);
        return jdbc.query("SELECT p.id, p.sheet_id, p.person_id, p.amount, p.start_date, p.every_days, p.month_day, p.end_date, p.target_sheet, "
                + "(SELECT COUNT(*) FROM plan_claim c WHERE c.plan_id = p.id) "
                + "FROM plan p WHERE p.sheet_id = ? ORDER BY p.id",
                (rs, i) -> new Plan(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getBigDecimal(4).setScale(2),
                        rs.getDate(5).toLocalDate(), rs.getObject(6, Integer.class), rs.getObject(7, Integer.class),
                        rs.getDate(8).toLocalDate(), rs.getInt(10),
                        pauses.getOrDefault(rs.getLong(1), List.of()),
                        changes.getOrDefault(rs.getLong(1), List.of()), rs.getString(9)),
                sheetId);
    }

    /** Adds the plan; answers its id. */
    public long add(String sheetId, long personId, BigDecimal amount, LocalDate start, Integer every, Integer day,
            LocalDate end, String targetSheet) {
        jdbc.update("INSERT INTO plan (sheet_id, person_id, amount, start_date, every_days, month_day, end_date, target_sheet) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)", sheetId, personId, amount, Date.valueOf(start), every, day,
                Date.valueOf(end), targetSheet);
        return jdbc.queryForObject("SELECT MAX(id) FROM plan WHERE sheet_id = ? AND person_id = ?", Long.class,
                sheetId, personId);
    }

    /** Whether the plan has made its claim for that plan date. */
    public boolean made(long planId, LocalDate date) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM plan_claim WHERE plan_id = ? AND on_date = ?",
                Integer.class, planId, Date.valueOf(date));
        return n != null && n > 0;
    }

    /** Notes the plan has made that claim for that plan date. */
    public void markMade(String sheetId, long planId, LocalDate date, long claimId) {
        jdbc.update("INSERT INTO plan_claim (sheet_id, plan_id, on_date, claim_id) VALUES (?, ?, ?, ?)", sheetId,
                planId, Date.valueOf(date), claimId);
    }

    /** The ids of the claims the plan has made, in plan date order (none for a plan date made before
     *  they were kept). */
    public List<Long> madeClaims(long planId) {
        return jdbc.queryForList("SELECT claim_id FROM plan_claim WHERE plan_id = ? AND claim_id IS NOT NULL "
                + "ORDER BY on_date", Long.class, planId);
    }

    /** The plan on the sheet with that id, or null when there is none. */
    public Plan find(String sheetId, long planId) {
        return onSheet(sheetId).stream().filter(p -> p.id() == planId).findFirst().orElse(null);
    }

    /** Opens a pause of the plan from that day. */
    public void pause(String sheetId, long planId, LocalDate on) {
        jdbc.update("INSERT INTO plan_pause (sheet_id, plan_id, paused_on) VALUES (?, ?, ?)", sheetId, planId,
                Date.valueOf(on));
    }

    /** Changes the plan's amount on that day, from then on, and notes the change. */
    public void changeAmount(String sheetId, long planId, LocalDate on, BigDecimal amount) {
        jdbc.update("UPDATE plan SET amount = ? WHERE id = ?", amount, planId);
        jdbc.update("INSERT INTO plan_change (sheet_id, plan_id, changed_on, amount) VALUES (?, ?, ?, ?)", sheetId,
                planId, Date.valueOf(on), amount);
    }

    /** Ends the plan's open pause on that day. */
    public void resume(long planId, LocalDate on) {
        jdbc.update("UPDATE plan_pause SET resumed_on = ? WHERE plan_id = ? AND resumed_on IS NULL", Date.valueOf(on),
                planId);
    }
}

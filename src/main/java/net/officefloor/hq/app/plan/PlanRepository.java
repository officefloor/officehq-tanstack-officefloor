package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.officefloor.hq.app.BadRequestException;
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

    /** Whether a sheet is a descendant of another: its parent, or that one's parent and so on, is that sheet. */
    public boolean descendant(String sheetId, String of) {
        Set<String> seen = new HashSet<>(List.of(sheetId));
        for (String at = parentOf(sheetId); at != null && seen.add(at); at = parentOf(at)) {
            if (at.equals(of)) {
                return true;
            }
        }
        return false;
    }

    private String parentOf(String sheetId) {
        List<String> found = jdbc.queryForList("SELECT parent_id FROM sheet WHERE id = ?", String.class, sheetId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The id of the person on the sheet with that name, or null when there is none. */
    public Long personNamed(String sheetId, String name) {
        List<Long> ids = jdbc.queryForList("SELECT id FROM person WHERE sheet_id = ? AND name = ? ORDER BY id",
                Long.class, sheetId, name);
        return ids.isEmpty() ? null : ids.get(0);
    }

    public Plan add(String sheetId, long personId, PlanInput input) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO plan (sheet_id, person_id, amount, start_date, every_days, day_of_month, end_date, target_sheet_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setLong(2, personId);
            ps.setBigDecimal(3, input.amount());
            ps.setDate(4, Date.valueOf(input.start()));
            ps.setObject(5, input.every(), Types.INTEGER);
            ps.setObject(6, input.day(), Types.INTEGER);
            ps.setDate(7, Date.valueOf(input.end()));
            ps.setString(8, input.sheet());
            return ps;
        }, keys);
        return new Plan(keys.getKey().longValue(), personId, money(input.amount()), input.start().toString(),
                input.every(), input.day(), input.end().toString(), input.sheet(), 0, false, List.of(), List.of());
    }

    /** The sheet's plans, in the order they were added, each with how many claims it has made, its pauses and its amount changes. */
    public List<Plan> list(String sheetId) {
        Map<Long, List<PlanPause>> pauses = new HashMap<>();
        jdbc.query("SELECT plan_id, paused_on, resumed_on FROM plan_pause WHERE sheet_id = ? ORDER BY id",
                rs -> {
                    Date to = rs.getDate("resumed_on");
                    pauses.computeIfAbsent(rs.getLong("plan_id"), k -> new ArrayList<>()).add(new PlanPause(
                            rs.getDate("paused_on").toLocalDate().toString(), to == null ? null : to.toLocalDate().toString()));
                }, sheetId);
        Map<Long, List<PlanChange>> changes = new HashMap<>();
        jdbc.query("SELECT plan_id, changed_on, amount FROM plan_change WHERE sheet_id = ? ORDER BY id",
                rs -> {
                    changes.computeIfAbsent(rs.getLong("plan_id"), k -> new ArrayList<>()).add(new PlanChange(
                            rs.getDate("changed_on").toLocalDate().toString(), money(rs.getBigDecimal("amount"))));
                }, sheetId);
        return jdbc.query("SELECT p.id, p.person_id, p.amount, p.start_date, p.every_days, p.day_of_month, p.end_date, p.target_sheet_id,"
                + " (SELECT COUNT(*) FROM plan_claim c WHERE c.plan_id = p.id) AS generated"
                + " FROM plan p WHERE p.sheet_id = ? ORDER BY p.id",
                (rs, i) -> {
                    List<PlanPause> mine = pauses.getOrDefault(rs.getLong("id"), List.of());
                    return new Plan(rs.getLong("id"), rs.getLong("person_id"), money(rs.getBigDecimal("amount")),
                            rs.getDate("start_date").toLocalDate().toString(), rs.getObject("every_days", Integer.class),
                            rs.getObject("day_of_month", Integer.class),
                            rs.getDate("end_date").toLocalDate().toString(), rs.getString("target_sheet_id"), rs.getInt("generated"),
                            mine.stream().anyMatch(p -> p.to() == null), mine,
                            changes.getOrDefault(rs.getLong("id"), List.of()));
                },
                sheetId);
    }

    /** The sheet's plan with that id, its row locked so one pause or resume of it runs at a time; refused when there
     *  is no such plan on the sheet. */
    public Plan lockedPlan(String sheetId, String planId) throws BadRequestException {
        long pid;
        try {
            pid = Long.parseLong(planId == null ? "" : planId.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("no such plan");
        }
        if (jdbc.queryForList("SELECT id FROM plan WHERE sheet_id = ? AND id = ? FOR UPDATE", Long.class, sheetId, pid)
                .isEmpty()) {
            throw new BadRequestException("no such plan");
        }
        return list(sheetId).stream().filter(p -> p.id() == pid).findFirst()
                .orElseThrow(() -> new BadRequestException("no such plan"));
    }

    /** Open a pause of the plan from a day. */
    public void pause(String sheetId, long planId, LocalDate from) {
        jdbc.update("INSERT INTO plan_pause (sheet_id, plan_id, paused_on) VALUES (?, ?, ?)", sheetId, planId,
                Date.valueOf(from));
    }

    /** Close the plan's open pause on a day. */
    public void resume(long planId, LocalDate on) {
        jdbc.update("UPDATE plan_pause SET resumed_on = ? WHERE plan_id = ? AND resumed_on IS NULL", Date.valueOf(on),
                planId);
    }

    /** Change the plan's amount from a day, keeping the change; answers the new amount (two decimals). */
    public String changeAmount(String sheetId, long planId, LocalDate on, BigDecimal amount) {
        jdbc.update("UPDATE plan SET amount = ? WHERE sheet_id = ? AND id = ?", amount, sheetId, planId);
        jdbc.update("INSERT INTO plan_change (sheet_id, plan_id, changed_on, amount) VALUES (?, ?, ?, ?)", sheetId,
                planId, Date.valueOf(on), amount);
        return money(amount);
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

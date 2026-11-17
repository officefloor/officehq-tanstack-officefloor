package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** The stored plans, kept per sheet, and the dates each has made a claim for. */
@Repository
public class PlanRepository {

    private final JdbcTemplate jdbc;
    private final SheetRepository sheets;

    public PlanRepository(JdbcTemplate jdbc, SheetRepository sheets) {
        this.jdbc = jdbc;
        this.sheets = sheets;
    }

    /** The sheet's plans in the order added, each with how many claims it has made; as they stood at the end of the
     *  date when one is given (only those added, and claims made, while the sheet's today was on or before it). */
    public List<Plan> list(String sheetId, LocalDate asOf) {
        Date until = asOf == null ? null : Date.valueOf(asOf);
        Map<Long, List<PlanPause>> pauses = pauses(sheetId, asOf);
        Map<Long, List<PlanChange>> changes = changes(sheetId, asOf);
        return jdbc.query("SELECT p.id, p.person_id, p.amount, p.start_date, p.every_days, p.month_day, p.end_date, p.target_sheet,"
                + " (SELECT COUNT(*) FROM plan_claim c WHERE c.plan_id = p.id"
                + " AND (CAST(? AS DATE) IS NULL OR c.made_on IS NULL OR c.made_on <= ?)) AS generated"
                + " FROM plan p WHERE p.sheet_id = ? AND (CAST(? AS DATE) IS NULL OR p.added_on IS NULL OR p.added_on <= ?)"
                + " ORDER BY p.id",
                (rs, n) -> {
                    List<PlanChange> made = changes.getOrDefault(rs.getLong("id"), List.of());
                    BigDecimal amount = made.isEmpty() ? rs.getBigDecimal("amount").setScale(2)
                            : made.get(made.size() - 1).amount();
                    return new Plan(rs.getLong("id"), rs.getLong("person_id"), amount,
                            rs.getObject("start_date", LocalDate.class), rs.getObject("every_days", Integer.class), rs.getObject("month_day", Integer.class),
                            rs.getObject("end_date", LocalDate.class), rs.getInt("generated"),
                            pauses.getOrDefault(rs.getLong("id"), List.of()), made, rs.getString("target_sheet"));
                },
                until, until, sheetId, until, until);
    }

    /** The sheet's plan of the id, as it is now; null when there is none. */
    public Plan find(String sheetId, long planId) {
        return list(sheetId, null).stream().filter((p) -> p.id() == planId).findFirst().orElse(null);
    }

    /** Pauses the plan from the sheet's today. */
    public void pause(String sheetId, long planId) {
        jdbc.update("INSERT INTO plan_pause (plan_id, paused_on) VALUES (?, ?)", planId,
                Date.valueOf(sheets.today(sheetId)));
    }

    /** Resumes the plan's open pause from the sheet's today. */
    public void resume(String sheetId, long planId) {
        jdbc.update("UPDATE plan_pause SET resumed_on = ? WHERE plan_id = ? AND resumed_on IS NULL",
                Date.valueOf(sheets.today(sheetId)), planId);
    }

    /** Changes the plan's amount from the sheet's today; the change as stored. */
    public PlanChange change(String sheetId, long planId, BigDecimal amount) {
        LocalDate today = sheets.today(sheetId);
        jdbc.update("INSERT INTO plan_change (plan_id, changed_on, amount) VALUES (?, ?, ?)", planId, Date.valueOf(today),
                amount);
        return new PlanChange(today, amount.setScale(2));
    }

    /** The changes of amount of the sheet's plans by plan, oldest first; as of the date when one is given (only those
     *  made while the sheet's today was on or before it). */
    private Map<Long, List<PlanChange>> changes(String sheetId, LocalDate asOf) {
        Map<Long, List<PlanChange>> byPlan = new HashMap<>();
        jdbc.query("SELECT z.plan_id, z.changed_on, z.amount FROM plan_change z JOIN plan p ON p.id = z.plan_id"
                + " WHERE p.sheet_id = ? ORDER BY z.id", (rs) -> {
                    LocalDate on = rs.getObject("changed_on", LocalDate.class);
                    if (asOf != null && on.isAfter(asOf)) {
                        return;
                    }
                    byPlan.computeIfAbsent(rs.getLong("plan_id"), (k) -> new ArrayList<>())
                            .add(new PlanChange(on, rs.getBigDecimal("amount").setScale(2)));
                }, sheetId);
        return byPlan;
    }

    /** The pauses of the sheet's plans by plan, in the order made; as of the date when one is given (only those
     *  made while the sheet's today was on or before it, a resume after it not yet made). */
    private Map<Long, List<PlanPause>> pauses(String sheetId, LocalDate asOf) {
        Map<Long, List<PlanPause>> byPlan = new HashMap<>();
        jdbc.query("SELECT z.plan_id, z.paused_on, z.resumed_on FROM plan_pause z JOIN plan p ON p.id = z.plan_id"
                + " WHERE p.sheet_id = ? ORDER BY z.id", (rs) -> {
                    LocalDate from = rs.getObject("paused_on", LocalDate.class);
                    LocalDate to = rs.getObject("resumed_on", LocalDate.class);
                    if (asOf != null && from.isAfter(asOf)) {
                        return;
                    }
                    if (asOf != null && to != null && to.isAfter(asOf)) {
                        to = null;
                    }
                    byPlan.computeIfAbsent(rs.getLong("plan_id"), (k) -> new ArrayList<>()).add(new PlanPause(from, to));
                }, sheetId);
        return byPlan;
    }

    /** Stores the plan for the person; the plan as stored. */
    public Plan add(String sheetId, long personId, PlanValues values) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update((con) -> {
            var ps = con.prepareStatement("INSERT INTO plan (sheet_id, person_id, amount, start_date, every_days, month_day, end_date,"
                    + " added_on, target_sheet) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setLong(2, personId);
            ps.setBigDecimal(3, values.amount());
            ps.setDate(4, Date.valueOf(values.start()));
            ps.setObject(5, values.every(), Types.INTEGER);
            ps.setObject(6, values.day(), Types.INTEGER);
            ps.setDate(7, Date.valueOf(values.end()));
            ps.setDate(8, Date.valueOf(sheets.today(sheetId)));
            ps.setString(9, values.sheet());
            return ps;
        }, key);
        return new Plan(key.getKey().longValue(), personId, values.amount(), values.start(), values.every(), values.day(),
                values.end(), 0, List.of(), List.of(), values.sheet());
    }

    /** Whether the plan has made a claim for the date. */
    public boolean made(long planId, LocalDate date) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM plan_claim WHERE plan_id = ? AND plan_date = ?", Integer.class,
                planId, Date.valueOf(date));
        return n != null && n > 0;
    }

    /** Records that the plan made the claim for the date. */
    public void recordMade(String sheetId, long planId, LocalDate date, long claimId) {
        jdbc.update("INSERT INTO plan_claim (plan_id, plan_date, claim_id, made_on) VALUES (?, ?, ?, ?)", planId,
                Date.valueOf(date), claimId, Date.valueOf(sheets.today(sheetId)));
    }
}

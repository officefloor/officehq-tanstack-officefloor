package net.officefloor.hq.app.plan;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
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
        return jdbc.query("SELECT p.id, p.person_id, p.amount, p.start_date, p.every_days, p.end_date,"
                + " (SELECT COUNT(*) FROM plan_claim c WHERE c.plan_id = p.id"
                + " AND (CAST(? AS DATE) IS NULL OR c.made_on IS NULL OR c.made_on <= ?)) AS generated"
                + " FROM plan p WHERE p.sheet_id = ? AND (CAST(? AS DATE) IS NULL OR p.added_on IS NULL OR p.added_on <= ?)"
                + " ORDER BY p.id",
                (rs, n) -> new Plan(rs.getLong("id"), rs.getLong("person_id"), rs.getBigDecimal("amount").setScale(2),
                        rs.getObject("start_date", LocalDate.class), rs.getInt("every_days"),
                        rs.getObject("end_date", LocalDate.class), rs.getInt("generated")),
                until, until, sheetId, until, until);
    }

    /** Stores the plan for the person; the plan as stored. */
    public Plan add(String sheetId, long personId, PlanValues values) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update((con) -> {
            var ps = con.prepareStatement("INSERT INTO plan (sheet_id, person_id, amount, start_date, every_days, end_date, added_on)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?)", new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setLong(2, personId);
            ps.setBigDecimal(3, values.amount());
            ps.setDate(4, Date.valueOf(values.start()));
            ps.setInt(5, values.every());
            ps.setDate(6, Date.valueOf(values.end()));
            ps.setDate(7, Date.valueOf(sheets.today(sheetId)));
            return ps;
        }, key);
        return new Plan(key.getKey().longValue(), personId, values.amount(), values.start(), values.every(), values.end(), 0);
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

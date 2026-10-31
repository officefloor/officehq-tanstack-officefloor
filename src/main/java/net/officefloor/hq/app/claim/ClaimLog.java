package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.sheet.Sheets;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Each claim's status log, oldest first: one line per status it has had, as
 *  "<STATUS> by <user> on <date> due=<due>", the date the sheet's today when the change was made and
 *  the due the claim's due as it was worked out then (n/a when it had none). */
@Repository
public class ClaimLog {

    private final JdbcTemplate jdbc;
    private final Sheets sheets;
    private final People people;
    private final Figures figures;

    public ClaimLog(JdbcTemplate jdbc, Sheets sheets, People people, Figures figures) {
        this.jdbc = jdbc;
        this.sheets = sheets;
        this.people = people;
        this.figures = figures;
    }

    /** Write the claim's (saved) status now to its log, by this user. */
    public void write(Claim claim, String user) {
        String grade = people.find(claim.getSheetId(), claim.getPersonId()).gradeOn(claim.getDate());
        BigDecimal due = figures.due(claim, grade);
        jdbc.update("INSERT INTO claim_log (sheet_id, claim_id, status, by_user, on_date, due) VALUES (?, ?, ?, ?, ?, ?)",
                claim.getSheetId(), claim.getId(), claim.getStatus().name(), user,
                Date.valueOf(sheets.today(claim.getSheetId())), due);
    }

    /** The date of the claim's last SETTLED log line, null when it has none. */
    public LocalDate settledOn(Claim claim) {
        List<Date> dates = jdbc.queryForList("SELECT on_date FROM claim_log WHERE sheet_id = ? AND claim_id = ? AND status = ? ORDER BY id DESC",
                Date.class, claim.getSheetId(), claim.getId(), ClaimStatus.SETTLED.name());
        return dates.isEmpty() ? null : dates.get(0).toLocalDate();
    }

    /** The log lines of one claim. */
    public List<String> of(Claim claim) {
        return jdbc.query("SELECT status, by_user, on_date, due FROM claim_log WHERE sheet_id = ? AND claim_id = ? ORDER BY id",
                (rs, i) -> line(rs.getString("status"), rs.getString("by_user"), rs.getDate("on_date"), rs.getBigDecimal("due")),
                claim.getSheetId(), claim.getId());
    }

    /** The log lines of every claim on the sheet, by claim id. */
    public Map<Long, List<String>> onSheet(String sheetId) {
        Map<Long, List<String>> lines = new HashMap<>();
        jdbc.query("SELECT claim_id, status, by_user, on_date, due FROM claim_log WHERE sheet_id = ? ORDER BY id",
                rs -> {
                    lines.computeIfAbsent(rs.getLong("claim_id"), k -> new ArrayList<>())
                            .add(line(rs.getString("status"), rs.getString("by_user"), rs.getDate("on_date"), rs.getBigDecimal("due")));
                }, sheetId);
        return lines;
    }

    /** The status each claim on the sheet had at the end of `date`, by claim id: its last log line
     *  dated on or before it, null when the claim was added after it. A claim with no lines (kept
     *  before the log began) is not in the map. */
    public Map<Long, ClaimStatus> statusesAt(String sheetId, LocalDate date) {
        Map<Long, ClaimStatus> statuses = new HashMap<>();
        jdbc.query("SELECT claim_id, status, on_date FROM claim_log WHERE sheet_id = ? ORDER BY id", rs -> {
            long id = rs.getLong("claim_id");
            if (!rs.getDate("on_date").toLocalDate().isAfter(date)) {
                statuses.put(id, ClaimStatus.valueOf(rs.getString("status")));
            } else {
                statuses.putIfAbsent(id, null);
            }
        }, sheetId);
        return statuses;
    }

    private static String line(String status, String user, Date date, BigDecimal due) {
        return status + " by " + user + " on " + date.toLocalDate() + " due=" + (due != null ? due.toPlainString() : "n/a");
    }
}

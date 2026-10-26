package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Each claim's status log: one line per status it has had, oldest first. */
@Repository
public class ClaimLogRepository {

    private final JdbcTemplate jdbc;

    public ClaimLogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Log the claim's status now, by the user on the date, at the due (null: n/a). */
    public void add(Claim claim, String by, LocalDate on, BigDecimal due) {
        jdbc.update("INSERT INTO claim_log (sheet_id, claim_id, status, by_user, logged_on, due) VALUES (?, ?, ?, ?, ?, ?)",
                claim.getSheetId(), claim.getId(), claim.getStatus().name(), by, java.sql.Date.valueOf(on), due);
    }

    /** The date of the claim's first log line in the status, or null when it never had it. */
    public LocalDate firstOn(String sheetId, long claimId, ClaimStatus status) {
        List<LocalDate> on = jdbc.query("SELECT logged_on FROM claim_log WHERE sheet_id = ? AND claim_id = ? AND status = ?"
                + " ORDER BY id LIMIT 1", (rs, i) -> rs.getObject("logged_on", LocalDate.class),
                sheetId, claimId, status.name());
        return on.isEmpty() ? null : on.get(0);
    }

    /** The date of the claim's last log line in the status, or null when it never had it. */
    public LocalDate lastOn(String sheetId, long claimId, ClaimStatus status) {
        List<LocalDate> on = jdbc.query("SELECT logged_on FROM claim_log WHERE sheet_id = ? AND claim_id = ? AND status = ?"
                + " ORDER BY id DESC LIMIT 1", (rs, i) -> rs.getObject("logged_on", LocalDate.class),
                sheetId, claimId, status.name());
        return on.isEmpty() ? null : on.get(0);
    }

    /** The date of each claim's last log line in the status, by claim id; a claim that never had it is absent. */
    public Map<Long, LocalDate> lastOnEach(String sheetId, ClaimStatus status) {
        Map<Long, LocalDate> on = new HashMap<>();
        jdbc.query("SELECT claim_id, logged_on FROM claim_log WHERE sheet_id = ? AND status = ? ORDER BY id",
                rs -> {
                    on.put(rs.getLong("claim_id"), rs.getObject("logged_on", LocalDate.class));
                }, sheetId, status.name());
        return on;
    }

    /**
     * Each claim's status at the end of the day, by claim id, as its log records it: its last line (in log order)
     * logged on or before the day. A claim with no such line (added after it) is absent.
     */
    public Map<Long, ClaimStatus> statusesAsOf(String sheetId, LocalDate day) {
        Map<Long, ClaimStatus> statuses = new HashMap<>();
        jdbc.query("SELECT claim_id, status FROM claim_log WHERE sheet_id = ? AND logged_on <= ? ORDER BY id",
                rs -> {
                    statuses.put(rs.getLong("claim_id"), ClaimStatus.valueOf(rs.getString("status")));
                }, sheetId, java.sql.Date.valueOf(day));
        return statuses;
    }

    /** The sheet's log lines by claim id, each claim's oldest first, as {@code <STATUS> by <user> on <date> due=<due>}. */
    public Map<Long, List<String>> lines(String sheetId) {
        Map<Long, List<String>> lines = new HashMap<>();
        jdbc.query("SELECT claim_id, status, by_user, logged_on, due FROM claim_log WHERE sheet_id = ? ORDER BY id", rs -> {
            BigDecimal due = rs.getBigDecimal("due");
            lines.computeIfAbsent(rs.getLong("claim_id"), id -> new ArrayList<>())
                    .add(rs.getString("status") + " by " + rs.getString("by_user") + " on "
                            + rs.getObject("logged_on", LocalDate.class) + " due="
                            + (due == null ? "n/a" : due.setScale(2).toPlainString()));
        }, sheetId);
        return lines;
    }
}

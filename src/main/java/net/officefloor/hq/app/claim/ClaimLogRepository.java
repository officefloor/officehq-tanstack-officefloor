package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The claims' status logs, kept per sheet: one line per status a claim has had, oldest first. */
@Repository
public class ClaimLogRepository {

    private final JdbcTemplate jdbc;

    public ClaimLogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Log the claim's current status, made by {@code user} on {@code today} at {@code due} (null: n/a). */
    public void record(Claim claim, String user, LocalDate today, BigDecimal due) {
        jdbc.update("INSERT INTO claim_log (sheet_id, claim_id, status, by_user, on_date, due) VALUES (?, ?, ?, ?, ?, ?)",
                claim.getSheetId(), claim.getId(), claim.getStatus().name(), user, Date.valueOf(today),
                due == null ? "n/a" : due.toPlainString());
    }

    /** The sheet's logs by claim id, each as the screen shows it: {@code <STATUS> by <user> on <date> due=<due>}. */
    public Map<Long, List<String>> findAll(String sheetId) {
        Map<Long, List<String>> logs = new HashMap<>();
        jdbc.query("SELECT claim_id, status, by_user, on_date, due FROM claim_log WHERE sheet_id = ? ORDER BY id",
                (rs) -> {
                    logs.computeIfAbsent(rs.getLong("claim_id"), (k) -> new ArrayList<>())
                            .add(rs.getString("status") + " by " + rs.getString("by_user") + " on "
                                    + rs.getDate("on_date").toLocalDate() + " due=" + rs.getString("due"));
                }, sheetId);
        return logs;
    }

    /** The sheet's logs by claim id as they stood at the end of {@code day}: the lines dated on or before it. */
    public Map<Long, List<String>> findAllOn(String sheetId, LocalDate day) {
        Map<Long, List<String>> logs = new HashMap<>();
        jdbc.query("SELECT claim_id, status, by_user, on_date, due FROM claim_log WHERE sheet_id = ? AND on_date <= ? ORDER BY id",
                (rs) -> {
                    logs.computeIfAbsent(rs.getLong("claim_id"), (k) -> new ArrayList<>())
                            .add(rs.getString("status") + " by " + rs.getString("by_user") + " on "
                                    + rs.getDate("on_date").toLocalDate() + " due=" + rs.getString("due"));
                }, sheetId, Date.valueOf(day));
        return logs;
    }

    /**
     * Each claim's status at the end of {@code day}, by claim id: its last log line dated on or
     * before it. A claim with no such line (added after) is absent.
     */
    public Map<Long, ClaimStatus> statusesOn(String sheetId, LocalDate day) {
        Map<Long, ClaimStatus> statuses = new HashMap<>();
        jdbc.query("SELECT claim_id, status FROM claim_log WHERE sheet_id = ? AND on_date <= ? ORDER BY id",
                (rs) -> {
                    statuses.put(rs.getLong("claim_id"), ClaimStatus.valueOf(rs.getString("status")));
                }, sheetId, Date.valueOf(day));
        return statuses;
    }

    /** A claim's status at the end of a day, with its due as logged then (null: n/a). */
    public record Line(ClaimStatus status, BigDecimal due) {
    }

    /**
     * Each claim's last log line dated on or before {@code day}, by claim id. A claim with no such
     * line (added after) is absent.
     */
    public Map<Long, Line> linesOn(String sheetId, LocalDate day) {
        Map<Long, Line> lines = new HashMap<>();
        jdbc.query("SELECT claim_id, status, due FROM claim_log WHERE sheet_id = ? AND on_date <= ? ORDER BY id",
                (rs) -> {
                    String due = rs.getString("due");
                    lines.put(rs.getLong("claim_id"), new Line(ClaimStatus.valueOf(rs.getString("status")),
                            "n/a".equals(due) ? null : new BigDecimal(due)));
                }, sheetId, Date.valueOf(day));
        return lines;
    }

    /**
     * The date the claim was settled: its first SETTLED log line's date (a re-settle does not move
     * it); null when it was never settled.
     */
    public LocalDate settledOn(Claim claim) {
        List<Date> found = jdbc.queryForList("SELECT on_date FROM claim_log WHERE sheet_id = ? AND claim_id = ?"
                + " AND status = 'SETTLED' ORDER BY id", Date.class, claim.getSheetId(), claim.getId());
        return found.isEmpty() ? null : found.get(0).toLocalDate();
    }

    /** The date the claim was last deleted: its last DELETED log line's date; null when it was never deleted. */
    public LocalDate deletedOn(Claim claim) {
        List<Date> found = jdbc.queryForList("SELECT on_date FROM claim_log WHERE sheet_id = ? AND claim_id = ?"
                + " AND status = 'DELETED' ORDER BY id DESC", Date.class, claim.getSheetId(), claim.getId());
        return found.isEmpty() ? null : found.get(0).toLocalDate();
    }
}

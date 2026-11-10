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
}

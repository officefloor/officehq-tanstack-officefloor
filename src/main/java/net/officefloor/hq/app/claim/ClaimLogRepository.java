package net.officefloor.hq.app.claim;

import java.sql.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The claims' status logs, each oldest first. */
@Repository
public class ClaimLogRepository {

    private final JdbcTemplate jdbc;

    public ClaimLogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void append(String sheetId, ClaimLogLine line) {
        jdbc.update("INSERT INTO claim_log (sheet_id, claim_id, status, by_user, log_date, due) VALUES (?, ?, ?, ?, ?, ?)",
                sheetId, line.claimId(), line.status().name(), line.by(), Date.valueOf(line.date()), line.due());
    }

    /** The sheet's status logs by claim id, each oldest first. */
    public Map<Long, List<ClaimLogLine>> findAll(String sheetId) {
        Map<Long, List<ClaimLogLine>> logs = new LinkedHashMap<>();
        jdbc.query("SELECT claim_id, status, by_user, log_date, due FROM claim_log WHERE sheet_id = ? ORDER BY id",
                (rs) -> {
                    ClaimLogLine line = new ClaimLogLine(rs.getLong("claim_id"), ClaimStatus.valueOf(rs.getString("status")),
                            rs.getString("by_user"), rs.getDate("log_date").toLocalDate(), rs.getString("due"));
                    logs.computeIfAbsent(line.claimId(), (k) -> new ArrayList<>()).add(line);
                }, sheetId);
        return logs;
    }

    /** The claim's status log, oldest first. */
    public List<ClaimLogLine> find(String sheetId, long claimId) {
        return findAll(sheetId).getOrDefault(claimId, List.of());
    }
}

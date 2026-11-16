package net.officefloor.hq.app.claim;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The stored status logs of the claims, kept per sheet, each claim's lines oldest first. */
@Repository
public class ClaimLogRepository {

    private final JdbcTemplate jdbc;

    public ClaimLogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void add(String sheetId, ClaimLogLine line) {
        jdbc.update("INSERT INTO claim_log (sheet_id, claim_id, status, by_user, log_date, due) VALUES (?, ?, ?, ?, ?, ?)",
                sheetId, line.claimId(), line.status().name(), line.by(), Date.valueOf(line.date()), line.due());
    }

    /** The claim's lines, oldest first. */
    public List<ClaimLogLine> of(String sheetId, long claimId) {
        return jdbc.query("SELECT claim_id, status, by_user, log_date, due FROM claim_log WHERE sheet_id = ? AND claim_id = ? ORDER BY id",
                ClaimLogRepository::map, sheetId, claimId);
    }

    /** Every claim's lines on the sheet, by claim id, each oldest first. */
    public Map<Long, List<ClaimLogLine>> bySheet(String sheetId) {
        return jdbc.query("SELECT claim_id, status, by_user, log_date, due FROM claim_log WHERE sheet_id = ? ORDER BY id",
                ClaimLogRepository::map, sheetId).stream()
                .collect(Collectors.groupingBy(ClaimLogLine::claimId, LinkedHashMap::new, Collectors.toList()));
    }

    private static ClaimLogLine map(ResultSet rs, int n) throws SQLException {
        return new ClaimLogLine(rs.getLong("claim_id"), ClaimStatus.valueOf(rs.getString("status")),
                rs.getString("by_user"), rs.getObject("log_date", LocalDate.class), rs.getString("due"));
    }
}

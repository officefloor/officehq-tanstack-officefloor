package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Each claim's comments, oldest first. */
@Repository
public class ClaimCommentRepository {

    private final JdbcTemplate jdbc;

    public ClaimCommentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Add the comment to the claim, by the user on the date. */
    public void add(Claim claim, String by, LocalDate on, String text) {
        jdbc.update("INSERT INTO claim_comment (sheet_id, claim_id, by_user, written_on, text) VALUES (?, ?, ?, ?, ?)",
                claim.getSheetId(), claim.getId(), by, java.sql.Date.valueOf(on), text);
    }

    /** The sheet's comments by claim id, each claim's oldest first, as {@code <user> on <date>: <text>}. */
    public Map<Long, List<String>> lines(String sheetId) {
        Map<Long, List<String>> lines = new HashMap<>();
        jdbc.query("SELECT claim_id, by_user, written_on, text FROM claim_comment WHERE sheet_id = ? ORDER BY id", rs -> {
            lines.computeIfAbsent(rs.getLong("claim_id"), id -> new ArrayList<>())
                    .add(rs.getString("by_user") + " on " + rs.getObject("written_on", LocalDate.class) + ": "
                            + rs.getString("text"));
        }, sheetId);
        return lines;
    }
}

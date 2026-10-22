package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Each claim's comments, oldest first, as <user> on <date>: <text>, the date being the sheet's today when it was
 *  written. */
@Repository
public class ClaimCommentRepository {

    private final JdbcTemplate jdbc;

    public ClaimCommentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Keep a comment on the claim, written by the user on the day given (null: the sheet had no today). */
    public void add(Claim claim, String user, LocalDate on, String text) {
        jdbc.update("INSERT INTO claim_comment (sheet_id, claim_id, by_user, on_date, text) VALUES (?, ?, ?, ?, ?)",
                claim.sheetId(), claim.id(), user, on, text);
    }

    /** The comment lines of every claim on the sheet, by claim id, oldest first. */
    public Map<Long, List<String>> lines(String sheetId) {
        return lines(sheetId, null);
    }

    /** The comment lines of every claim on the sheet as they stood at the end of the day (null: now), by claim id,
     *  oldest first: those written while the sheet's today was on or before it (or while it had none). */
    public Map<Long, List<String>> lines(String sheetId, LocalDate end) {
        Map<Long, List<String>> lines = new HashMap<>();
        jdbc.query("SELECT * FROM claim_comment WHERE sheet_id = ? ORDER BY id", rs -> {
            LocalDate on = rs.getObject("on_date", LocalDate.class);
            if (end != null && on != null && on.isAfter(end)) {
                return;
            }
            lines.computeIfAbsent(rs.getLong("claim_id"), k -> new ArrayList<>())
                    .add(rs.getString("by_user") + " on " + (on != null ? on.toString() : "") + ": "
                            + rs.getString("text"));
        }, sheetId);
        return lines;
    }
}

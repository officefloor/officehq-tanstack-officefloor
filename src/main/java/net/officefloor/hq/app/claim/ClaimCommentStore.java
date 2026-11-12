package net.officefloor.hq.app.claim;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Each claim's comments, oldest first: a line per comment, {@code <user> on <sheet's today then>: <text>}. */
@Repository
public class ClaimCommentStore {

    private final JdbcTemplate jdbc;

    public ClaimCommentStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Keeps a comment on the claim, by the user, written on the date. */
    public void add(Claim claim, String user, LocalDate on, String text) {
        jdbc.update("INSERT INTO claim_comment (sheet_id, claim_id, by_user, written_on, text) VALUES (?, ?, ?, ?, ?)",
                claim.getSheetId(), claim.getId(), user, Date.valueOf(on), text);
    }

    /** Every claim's comment lines on the sheet written on or before the date (all of them when it is null),
     *  by claim id, oldest first. */
    public Map<Long, List<String>> lines(String sheetId, LocalDate until) {
        Map<Long, List<String>> lines = new HashMap<>();
        jdbc.query("SELECT * FROM claim_comment WHERE sheet_id = ? AND (CAST(? AS DATE) IS NULL OR written_on <= ?) ORDER BY id", rs -> {
            lines.computeIfAbsent(rs.getLong("claim_id"), id -> new ArrayList<>())
                    .add(rs.getString("by_user") + " on " + rs.getObject("written_on", LocalDate.class) + ": "
                            + rs.getString("text"));
        }, sheetId, until == null ? null : Date.valueOf(until), until == null ? null : Date.valueOf(until));
        return lines;
    }

    /** The claim's comment lines, oldest first. */
    public List<String> lines(Claim claim) {
        return lines(claim.getSheetId(), null).getOrDefault(claim.getId(), List.of());
    }
}

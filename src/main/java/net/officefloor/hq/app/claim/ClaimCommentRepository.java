package net.officefloor.hq.app.claim;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The claims' comments, kept per sheet, oldest first. */
@Repository
public class ClaimCommentRepository {

    private final JdbcTemplate jdbc;

    public ClaimCommentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Keep a comment on the claim, written by {@code user} on {@code today}. */
    public void add(Claim claim, String user, LocalDate today, String text) {
        jdbc.update("INSERT INTO claim_comment (sheet_id, claim_id, by_user, on_date, text) VALUES (?, ?, ?, ?, ?)",
                claim.getSheetId(), claim.getId(), user, Date.valueOf(today), text);
    }

    /**
     * The sheet's comments by claim id, each as the screen shows it: {@code <user> on <date>: <text>};
     * as of {@code day} (null: now), only those written on or before it.
     */
    public Map<Long, List<String>> findAll(String sheetId, LocalDate day) {
        Map<Long, List<String>> comments = new HashMap<>();
        String sql = "SELECT claim_id, by_user, on_date, text FROM claim_comment WHERE sheet_id = ?"
                + (day == null ? "" : " AND on_date <= ?") + " ORDER BY id";
        Object[] args = day == null ? new Object[] { sheetId } : new Object[] { sheetId, Date.valueOf(day) };
        jdbc.query(sql, (rs) -> {
            comments.computeIfAbsent(rs.getLong("claim_id"), (k) -> new ArrayList<>())
                    .add(rs.getString("by_user") + " on " + rs.getDate("on_date").toLocalDate() + ": "
                            + rs.getString("text"));
        }, args);
        return comments;
    }
}

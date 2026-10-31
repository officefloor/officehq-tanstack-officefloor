package net.officefloor.hq.app.claim;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.sheet.Sheets;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Each claim's comments, oldest first, as "<user> on <date>: <text>", the date the sheet's today
 *  when the comment was written. */
@Repository
public class ClaimComments {

    private final JdbcTemplate jdbc;
    private final Sheets sheets;

    public ClaimComments(JdbcTemplate jdbc, Sheets sheets) {
        this.jdbc = jdbc;
        this.sheets = sheets;
    }

    /** Keep the comment on the claim, dated the sheet's today. */
    public void write(Claim claim, ClaimComment comment) {
        jdbc.update("INSERT INTO claim_comment (sheet_id, claim_id, by_user, on_date, comment_text) VALUES (?, ?, ?, ?, ?)",
                claim.getSheetId(), claim.getId(), comment.user(), Date.valueOf(sheets.today(claim.getSheetId())),
                comment.text());
    }

    /** The comment lines of one claim. */
    public List<String> of(Claim claim) {
        return jdbc.query("SELECT by_user, on_date, comment_text FROM claim_comment WHERE sheet_id = ? AND claim_id = ? ORDER BY id",
                (rs, i) -> line(rs.getString("by_user"), rs.getDate("on_date"), rs.getString("comment_text")),
                claim.getSheetId(), claim.getId());
    }

    /** The comment lines of every claim on the sheet, by claim id. */
    public Map<Long, List<String>> onSheet(String sheetId) {
        Map<Long, List<String>> lines = new HashMap<>();
        jdbc.query("SELECT claim_id, by_user, on_date, comment_text FROM claim_comment WHERE sheet_id = ? ORDER BY id",
                rs -> {
                    lines.computeIfAbsent(rs.getLong("claim_id"), k -> new ArrayList<>())
                            .add(line(rs.getString("by_user"), rs.getDate("on_date"), rs.getString("comment_text")));
                }, sheetId);
        return lines;
    }

    private static String line(String user, Date date, String text) {
        return user + " on " + date.toLocalDate() + ": " + text;
    }
}

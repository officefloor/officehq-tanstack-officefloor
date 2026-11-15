package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Each claim's comments, oldest first, shown as {@code <user> on <date>: <text>}. */
@Repository
public class ClaimCommentRepository {

    private final JdbcTemplate jdbc;

    public ClaimCommentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void add(Claim claim, ClaimComment comment) {
        jdbc.update("INSERT INTO claim_comment (sheet_id, claim_id, by_user, on_date, text) VALUES (?, ?, ?, ?, ?)",
                claim.getSheetId(), claim.getId(), comment.by(), LocalDate.parse(comment.date()), comment.text());
    }

    /** Give each claim its comment lines. */
    public List<Claim> withComments(String sheetId, List<Claim> claims) {
        Map<Long, List<String>> lines = new HashMap<>();
        jdbc.query("SELECT claim_id, by_user, on_date, text FROM claim_comment WHERE sheet_id = ? ORDER BY id",
                (rs) -> {
                    lines.computeIfAbsent(rs.getLong("claim_id"), (k) -> new ArrayList<>())
                            .add(new ClaimComment(rs.getString("by_user"),
                                    rs.getObject("on_date", LocalDate.class).toString(), rs.getString("text")).line());
                }, sheetId);
        for (Claim c : claims) c.setComments(lines.getOrDefault(c.getId(), List.of()));
        return claims;
    }
}

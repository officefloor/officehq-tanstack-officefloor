package net.officefloor.hq.app.claim;

import java.sql.Date;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The claims' comments, each oldest first. */
@Repository
public class ClaimCommentRepository {

    private final JdbcTemplate jdbc;

    public ClaimCommentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void append(String sheetId, ClaimComment comment) {
        jdbc.update("INSERT INTO claim_comment (sheet_id, claim_id, by_user, comment_date, text) VALUES (?, ?, ?, ?, ?)",
                sheetId, comment.claimId(), comment.by(), Date.valueOf(comment.date()), comment.text());
    }

    /** The sheet's comments by claim id, each oldest first. */
    public Map<Long, List<ClaimComment>> findAll(String sheetId) {
        Map<Long, List<ClaimComment>> comments = new LinkedHashMap<>();
        jdbc.query("SELECT claim_id, by_user, comment_date, text FROM claim_comment WHERE sheet_id = ? ORDER BY id",
                (rs) -> {
                    ClaimComment comment = new ClaimComment(rs.getLong("claim_id"), rs.getString("by_user"),
                            rs.getDate("comment_date").toLocalDate(), rs.getString("text"));
                    comments.computeIfAbsent(comment.claimId(), (k) -> new ArrayList<>()).add(comment);
                }, sheetId);
        return comments;
    }

    /** The claim's comments, oldest first. */
    public List<ClaimComment> find(String sheetId, long claimId) {
        return findAll(sheetId).getOrDefault(claimId, List.of());
    }
}

package net.officefloor.hq.app.voidreason;

import java.util.List;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.history.AsOf;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The store of the sheet's void reasons, each at most once per sheet without regard to case, and of the reason each claim was voided with. */
@Repository
public class VoidReasons {

    private final JdbcTemplate jdbc;

    public VoidReasons(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's void reasons, in the order they were added. */
    public List<String> list(String sheetId) {
        return jdbc.queryForList("SELECT reason FROM " + AsOf.table("void_reason") + " WHERE sheet_id = ? ORDER BY listed",
                String.class, sheetId);
    }

    /** The sheet's void reasons, in the order they were added, each with how many VOID claims were voided with it. */
    public List<VoidReasonResponse> counts(String sheetId) {
        return jdbc.query("SELECT r.reason, (SELECT COUNT(*) FROM " + AsOf.table("claim", "c")
                + " WHERE c.sheet_id = r.sheet_id AND c.status = ? AND c.void_reason = r.reason) AS voided FROM "
                + AsOf.table("void_reason", "r") + " WHERE r.sheet_id = ? ORDER BY r.listed",
                (rs, n) -> new VoidReasonResponse(rs.getString("reason"), rs.getInt("voided")), ClaimStatus.VOID.name(),
                sheetId);
    }

    public boolean has(String sheetId, String reason) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + AsOf.table("void_reason")
                + " WHERE sheet_id = ? AND LOWER(reason) = LOWER(?)", Integer.class, sheetId, reason);
        return n != null && n > 0;
    }

    public void add(String sheetId, String reason) {
        jdbc.update("INSERT INTO void_reason (sheet_id, reason, listed) VALUES (?, ?, "
                + "(SELECT COALESCE(MAX(listed), 0) + 1 FROM void_reason WHERE sheet_id = ?))", sheetId, reason, sheetId);
    }

    /** Keeps the reason the claim was voided with. */
    public void voided(long claimId, String reason) {
        jdbc.update("UPDATE claim SET void_reason = ? WHERE id = ?", reason, claimId);
    }
}

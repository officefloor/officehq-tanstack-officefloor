package net.officefloor.hq.app.voidreason;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The void reasons on each sheet, and the reason each claim was voided with. */
@Repository
public class VoidReasons {

    private final JdbcTemplate jdbc;

    public VoidReasons(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every void reason on the sheet, in the order added, with how many VOID claims were voided with it. */
    public List<VoidReason> list(String sheet) {
        return jdbc.query("SELECT r.reason, (SELECT COUNT(*) FROM claim_void v JOIN claim c ON c.sheet = v.sheet"
                + " AND c.id = v.claim WHERE v.sheet = r.sheet AND v.reason = r.reason AND c.status = 'VOID') AS voided"
                + " FROM void_reason r WHERE r.sheet = ? ORDER BY r.position",
                (rs, i) -> new VoidReason(rs.getString("reason"), rs.getInt("voided")), sheet);
    }

    /** Whether the sheet has any void reason. */
    public boolean any(String sheet) {
        Integer found = jdbc.queryForObject("SELECT COUNT(*) FROM void_reason WHERE sheet = ?", Integer.class, sheet);
        return found != null && found > 0;
    }

    /** Whether the reason is already on the sheet's list. */
    public boolean has(String sheet, String reason) {
        Integer found = jdbc.queryForObject("SELECT COUNT(*) FROM void_reason WHERE sheet = ? AND reason = ?",
                Integer.class, sheet, reason);
        return found != null && found > 0;
    }

    public void add(String sheet, String reason) {
        jdbc.update("INSERT INTO void_reason (sheet, reason, position) VALUES (?, ?,"
                + " (SELECT COALESCE(MAX(p.position), 0) + 1 FROM void_reason p WHERE p.sheet = ?))", sheet, reason, sheet);
    }

    /** Records the reason the claim was voided with. */
    public void voided(String sheet, long claim, String reason) {
        jdbc.update("DELETE FROM claim_void WHERE sheet = ? AND claim = ?", sheet, claim);
        jdbc.update("INSERT INTO claim_void (sheet, claim, reason) VALUES (?, ?, ?)", sheet, claim, reason);
    }
}

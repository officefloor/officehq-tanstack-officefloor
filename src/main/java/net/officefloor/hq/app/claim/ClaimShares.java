package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.history.AsOf;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** The store of claims' shares (see V94). */
@Service
public class ClaimShares {

    private final JdbcTemplate jdbc;

    public ClaimShares(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Adds a share to the claim, after its others. */
    public void add(Claim claim, ClaimShare share) {
        jdbc.update("INSERT INTO claim_share (sheet_id, claim_id, person_id, share) VALUES (?, ?, ?, ?)",
                claim.sheetId(), claim.id(), share.personId(), share.share());
    }

    /** The claim's shares, in the order added. */
    public List<ClaimShare> of(Claim claim) {
        return jdbc.query("SELECT * FROM " + AsOf.table("claim_share") + " WHERE claim_id = ? ORDER BY id",
                (rs, n) -> new ClaimShare(rs.getLong("person_id"), rs.getBigDecimal("share").setScale(2)), claim.id());
    }

    /** The shares of the sheet's claims, by claim id, each in the order added. */
    public Map<Long, List<ClaimShare>> of(String sheetId) {
        Map<Long, List<ClaimShare>> shares = new HashMap<>();
        jdbc.query("SELECT * FROM " + AsOf.table("claim_share") + " WHERE sheet_id = ? ORDER BY id", rs -> {
            shares.computeIfAbsent(rs.getLong("claim_id"), k -> new ArrayList<>())
                    .add(new ClaimShare(rs.getLong("person_id"), rs.getBigDecimal("share").setScale(2)));
        }, sheetId);
        return shares;
    }

    /** The shares added up (0.00 for none). */
    public static BigDecimal total(List<ClaimShare> shares) {
        return shares.stream().map(ClaimShare::share).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }
}

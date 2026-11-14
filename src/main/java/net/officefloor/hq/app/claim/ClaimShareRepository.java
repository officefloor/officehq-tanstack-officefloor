package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The claims' shares, each in the order given. */
@Repository
public class ClaimShareRepository {

    private final JdbcTemplate jdbc;

    public ClaimShareRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void append(String sheetId, ClaimShare share) {
        jdbc.update("INSERT INTO claim_share (sheet_id, claim_id, person_id, share) VALUES (?, ?, ?, ?)", sheetId,
                share.claimId(), share.personId(), share.share());
    }

    /** The sheet's shares by claim id, each in the order given. */
    public Map<Long, List<ClaimShare>> findAll(String sheetId) {
        Map<Long, List<ClaimShare>> shares = new LinkedHashMap<>();
        jdbc.query("SELECT claim_id, person_id, share FROM claim_share WHERE sheet_id = ? ORDER BY id", (rs) -> {
            ClaimShare share = map(rs.getLong("claim_id"), rs.getLong("person_id"), rs.getBigDecimal("share"));
            shares.computeIfAbsent(share.claimId(), (k) -> new ArrayList<>()).add(share);
        }, sheetId);
        return shares;
    }

    /** The claim's shares, in the order given. */
    public List<ClaimShare> find(String sheetId, long claimId) {
        return jdbc.query("SELECT claim_id, person_id, share FROM claim_share WHERE sheet_id = ? AND claim_id = ?"
                + " ORDER BY id", (rs, n) -> map(rs.getLong("claim_id"), rs.getLong("person_id"),
                        rs.getBigDecimal("share")), sheetId, claimId);
    }

    private static ClaimShare map(long claimId, long personId, BigDecimal share) {
        return new ClaimShare(claimId, personId, share.setScale(2));
    }
}

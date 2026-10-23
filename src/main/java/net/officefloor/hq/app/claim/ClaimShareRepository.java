package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Each claim's shares, in the order added: another person on the sheet holding a part of the claim's due, as a
 *  percent with two decimals. */
@Repository
public class ClaimShareRepository {

    /** A share of a claim: the person holding it and their percent (two decimals). */
    public record Share(long personId, String share) {
        public BigDecimal pct() {
            return new BigDecimal(share);
        }
    }

    private final JdbcTemplate jdbc;

    public ClaimShareRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Keep a share of the claim, added on the day given (null: the sheet had no today). */
    public void add(Claim claim, long personId, BigDecimal share, LocalDate on) {
        jdbc.update("INSERT INTO claim_share (sheet_id, claim_id, person_id, share, on_date) VALUES (?, ?, ?, ?, ?)",
                claim.sheetId(), claim.id(), personId, share, on);
    }

    /** The claim's shares, in the order added. */
    public List<Share> of(Claim claim) {
        return jdbc.query("SELECT person_id, share FROM claim_share WHERE sheet_id = ? AND claim_id = ? ORDER BY id",
                (rs, i) -> new Share(rs.getLong("person_id"), rs.getBigDecimal("share").setScale(2).toPlainString()),
                claim.sheetId(), claim.id());
    }

    /** The shares of every claim on the sheet, by claim id, in the order added. */
    public Map<Long, List<Share>> shares(String sheetId) {
        return shares(sheetId, null);
    }

    /** The shares of every claim on the sheet as they stood at the end of the day (null: now), by claim id, in the
     *  order added: those added while the sheet's today was on or before it (or while it had none). */
    public Map<Long, List<Share>> shares(String sheetId, LocalDate end) {
        Map<Long, List<Share>> shares = new HashMap<>();
        jdbc.query("SELECT * FROM claim_share WHERE sheet_id = ? ORDER BY id", rs -> {
            LocalDate on = rs.getObject("on_date", LocalDate.class);
            if (end != null && on != null && on.isAfter(end)) {
                return;
            }
            shares.computeIfAbsent(rs.getLong("claim_id"), k -> new ArrayList<>())
                    .add(new Share(rs.getLong("person_id"), rs.getBigDecimal("share").setScale(2).toPlainString()));
        }, sheetId);
        return shares;
    }
}

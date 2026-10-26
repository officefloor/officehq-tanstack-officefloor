package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Each claim's lines, in the order added. */
@Repository
public class ClaimLineRepository {

    private final JdbcTemplate jdbc;

    public ClaimLineRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Add the line to the claim. */
    public void add(Claim claim, ClaimLine line) {
        jdbc.update("INSERT INTO claim_line (sheet_id, claim_id, label, qty, unit) VALUES (?, ?, ?, ?, ?)",
                claim.getSheetId(), claim.getId(), line.label(), line.qty(), line.unit());
    }

    /** The claim's lines, in the order added. */
    public List<ClaimLine> of(long claimId) {
        return jdbc.query("SELECT label, qty, unit FROM claim_line WHERE claim_id = ? ORDER BY id",
                (rs, i) -> new ClaimLine(rs.getString("label"), rs.getInt("qty"), rs.getBigDecimal("unit").setScale(2)),
                claimId);
    }

    /** The claim's amount from its lines: the quantities times the unit amounts added up; null when it has none. */
    public BigDecimal total(long claimId) {
        List<ClaimLine> lines = of(claimId);
        return lines.isEmpty() ? null : lines.stream().map(ClaimLine::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** The sheet's lines by claim id, each claim's in the order added, as {@code <label> <qty> x <unit>}. */
    public Map<Long, List<String>> lines(String sheetId) {
        Map<Long, List<String>> lines = new HashMap<>();
        jdbc.query("SELECT claim_id, label, qty, unit FROM claim_line WHERE sheet_id = ? ORDER BY id", rs -> {
            lines.computeIfAbsent(rs.getLong("claim_id"), id -> new ArrayList<>())
                    .add(rs.getString("label") + " " + rs.getInt("qty") + " x "
                            + rs.getBigDecimal("unit").setScale(2).toPlainString());
        }, sheetId);
        return lines;
    }
}

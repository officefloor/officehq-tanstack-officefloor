package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Each claim's lines, in the order they were added, shown as {@code <label> <qty> x <unit>}. */
@Repository
public class ClaimLineRepository {

    private final JdbcTemplate jdbc;

    public ClaimLineRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void add(Claim claim, ClaimLine line) {
        jdbc.update("INSERT INTO claim_line (sheet_id, claim_id, label, qty, unit) VALUES (?, ?, ?, ?, ?)",
                claim.getSheetId(), claim.getId(), line.label(), line.qty(), line.unit());
    }

    /** The claim's lines, in the order they were added. */
    public List<ClaimLine> of(Claim claim) {
        return jdbc.query("SELECT label, qty, unit FROM claim_line WHERE sheet_id = ? AND claim_id = ? ORDER BY id",
                (rs, n) -> new ClaimLine(rs.getString("label"), rs.getInt("qty"), rs.getBigDecimal("unit").setScale(2)),
                claim.getSheetId(), claim.getId());
    }

    /** The claim's amount by its lines: the quantities times the unit amounts added up; null when it has none. */
    public BigDecimal total(Claim claim) {
        List<ClaimLine> lines = of(claim);
        if (lines.isEmpty()) return null;
        return lines.stream().map(ClaimLine::total).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
    }

    /** Give each claim its lines. */
    public List<Claim> withLines(String sheetId, List<Claim> claims) {
        Map<Long, List<String>> lines = new HashMap<>();
        jdbc.query("SELECT claim_id, label, qty, unit FROM claim_line WHERE sheet_id = ? ORDER BY id",
                (rs) -> {
                    lines.computeIfAbsent(rs.getLong("claim_id"), (k) -> new ArrayList<>())
                            .add(new ClaimLine(rs.getString("label"), rs.getInt("qty"),
                                    rs.getBigDecimal("unit").setScale(2)).line());
                }, sheetId);
        for (Claim c : claims) c.setLines(lines.getOrDefault(c.getId(), List.of()));
        return claims;
    }
}

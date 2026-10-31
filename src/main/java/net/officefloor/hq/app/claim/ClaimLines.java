package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Each claim's lines, in the order added, as "<label> <qty> x <unit>". A claim with lines has their
 *  quantities times their unit amounts, added up, as its amount. */
@Repository
public class ClaimLines {

    private final JdbcTemplate jdbc;

    public ClaimLines(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Keep the line on the claim. */
    public void write(Claim claim, ClaimLine line) {
        jdbc.update("INSERT INTO claim_line (sheet_id, claim_id, label, qty, unit) VALUES (?, ?, ?, ?, ?)",
                claim.getSheetId(), claim.getId(), line.label(), line.qty(), line.unit());
    }

    /** Whether the claim has lines. */
    public boolean any(Claim claim) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM claim_line WHERE sheet_id = ? AND claim_id = ?",
                Integer.class, claim.getSheetId(), claim.getId());
        return n != null && n > 0;
    }

    /** The claim's lines added up (zero when it has none). */
    public BigDecimal total(Claim claim) {
        BigDecimal total = jdbc.queryForObject("SELECT SUM(qty * unit) FROM claim_line WHERE sheet_id = ? AND claim_id = ?",
                BigDecimal.class, claim.getSheetId(), claim.getId());
        return total == null ? BigDecimal.ZERO.setScale(2) : total.setScale(2);
    }

    /** The lines of one claim. */
    public List<String> of(Claim claim) {
        return jdbc.query("SELECT label, qty, unit FROM claim_line WHERE sheet_id = ? AND claim_id = ? ORDER BY id",
                (rs, i) -> line(rs.getString("label"), rs.getInt("qty"), rs.getBigDecimal("unit")),
                claim.getSheetId(), claim.getId());
    }

    /** The lines of every claim on the sheet, by claim id. */
    public Map<Long, List<String>> onSheet(String sheetId) {
        Map<Long, List<String>> lines = new HashMap<>();
        jdbc.query("SELECT claim_id, label, qty, unit FROM claim_line WHERE sheet_id = ? ORDER BY id",
                rs -> {
                    lines.computeIfAbsent(rs.getLong("claim_id"), k -> new ArrayList<>())
                            .add(line(rs.getString("label"), rs.getInt("qty"), rs.getBigDecimal("unit")));
                }, sheetId);
        return lines;
    }

    private static String line(String label, int qty, BigDecimal unit) {
        return label + " " + qty + " x " + unit.setScale(2).toPlainString();
    }
}

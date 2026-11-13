package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The claims' lines, each in the order added. */
@Repository
public class ClaimLineRepository {

    private final JdbcTemplate jdbc;

    public ClaimLineRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void append(String sheetId, ClaimLine line) {
        jdbc.update("INSERT INTO claim_line (sheet_id, claim_id, label, qty, unit) VALUES (?, ?, ?, ?, ?)",
                sheetId, line.claimId(), line.label(), line.qty(), line.unit());
    }

    /** The sheet's lines by claim id, each in the order added. */
    public Map<Long, List<ClaimLine>> findAll(String sheetId) {
        Map<Long, List<ClaimLine>> lines = new LinkedHashMap<>();
        jdbc.query("SELECT claim_id, label, qty, unit FROM claim_line WHERE sheet_id = ? ORDER BY id", (rs) -> {
            ClaimLine line = new ClaimLine(rs.getLong("claim_id"), rs.getString("label"), rs.getInt("qty"),
                    rs.getBigDecimal("unit").setScale(2));
            lines.computeIfAbsent(line.claimId(), (k) -> new ArrayList<>()).add(line);
        }, sheetId);
        return lines;
    }

    /** The claim's lines, in the order added. */
    public List<ClaimLine> find(String sheetId, long claimId) {
        List<ClaimLine> lines = new ArrayList<>();
        jdbc.query("SELECT claim_id, label, qty, unit FROM claim_line WHERE sheet_id = ? AND claim_id = ? ORDER BY id",
                (rs) -> {
                    lines.add(new ClaimLine(rs.getLong("claim_id"), rs.getString("label"), rs.getInt("qty"),
                            rs.getBigDecimal("unit").setScale(2)));
                }, sheetId, claimId);
        return lines;
    }

    /** The lines' quantities times their unit amounts, added up. */
    public static BigDecimal total(List<ClaimLine> lines) {
        return lines.stream().map(ClaimLine::total).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }
}

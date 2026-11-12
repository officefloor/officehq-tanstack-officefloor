package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Each claim's lines, in the order added: a line per line, {@code <label> <qty> x <unit>}. */
@Repository
public class ClaimLineStore {

    private final JdbcTemplate jdbc;

    public ClaimLineStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Keeps a line on the claim, added on the date. */
    public void add(Claim claim, LineInput line, LocalDate on) {
        jdbc.update("INSERT INTO claim_line (sheet_id, claim_id, label, qty, unit, added_on) VALUES (?, ?, ?, ?, ?, ?)",
                claim.getSheetId(), claim.getId(), line.label(), line.qty(), line.unit(), Date.valueOf(on));
    }

    /** The claim's amount by its lines: the quantities times the unit amounts, added up. */
    public BigDecimal total(Claim claim) {
        BigDecimal total = jdbc.queryForObject("SELECT COALESCE(SUM(qty * unit), 0) FROM claim_line WHERE sheet_id = ? AND claim_id = ?",
                BigDecimal.class, claim.getSheetId(), claim.getId());
        return (total == null ? BigDecimal.ZERO : total).setScale(2);
    }

    /** Whether the claim has any line. */
    public boolean has(Claim claim) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM claim_line WHERE sheet_id = ? AND claim_id = ?",
                Integer.class, claim.getSheetId(), claim.getId());
        return n != null && n > 0;
    }

    /** Every claim's lines on the sheet added on or before the date (all of them when it is null), by claim id,
     *  in the order added. */
    public Map<Long, List<String>> lines(String sheetId, LocalDate until) {
        Map<Long, List<String>> lines = new HashMap<>();
        jdbc.query("SELECT * FROM claim_line WHERE sheet_id = ? AND (CAST(? AS DATE) IS NULL OR added_on <= ?) ORDER BY id", rs -> {
            lines.computeIfAbsent(rs.getLong("claim_id"), id -> new ArrayList<>())
                    .add(rs.getString("label") + " " + rs.getInt("qty") + " x "
                            + rs.getBigDecimal("unit").setScale(2).toPlainString());
        }, sheetId, until == null ? null : Date.valueOf(until), until == null ? null : Date.valueOf(until));
        return lines;
    }

    /** The claim's lines, in the order added. */
    public List<String> lines(Claim claim) {
        return lines(claim.getSheetId(), null).getOrDefault(claim.getId(), List.of());
    }
}

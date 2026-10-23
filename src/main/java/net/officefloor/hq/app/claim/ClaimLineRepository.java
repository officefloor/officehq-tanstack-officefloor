package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Each claim's lines, in the order added, as <label> <qty> x <unit>; a claim with lines has their quantities times
 *  their unit amounts, added up, as its amount. */
@Repository
public class ClaimLineRepository {

    private final JdbcTemplate jdbc;

    public ClaimLineRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Keep a line on the claim, added on the day given (null: the sheet had no today). */
    public void add(Claim claim, String label, int qty, BigDecimal unit, LocalDate on) {
        jdbc.update("INSERT INTO claim_line (sheet_id, claim_id, label, qty, unit, on_date) VALUES (?, ?, ?, ?, ?, ?)",
                claim.sheetId(), claim.id(), label, qty, unit, on);
    }

    /** The claim's lines' quantities times unit amounts, added up (two decimals); null when it has no lines. */
    public BigDecimal total(Claim claim) {
        return jdbc.query("SELECT COUNT(*), SUM(qty * unit) FROM claim_line WHERE sheet_id = ? AND claim_id = ?",
                rs -> rs.next() && rs.getInt(1) > 0 ? rs.getBigDecimal(2).setScale(2) : null,
                claim.sheetId(), claim.id());
    }

    /** The lines of every claim on the sheet, by claim id, in the order added. */
    public Map<Long, List<String>> lines(String sheetId) {
        return lines(sheetId, null);
    }

    /** The lines of every claim on the sheet as they stood at the end of the day (null: now), by claim id, in the
     *  order added: those added while the sheet's today was on or before it (or while it had none). */
    public Map<Long, List<String>> lines(String sheetId, LocalDate end) {
        Map<Long, List<String>> lines = new HashMap<>();
        jdbc.query("SELECT * FROM claim_line WHERE sheet_id = ? ORDER BY id", rs -> {
            LocalDate on = rs.getObject("on_date", LocalDate.class);
            if (end != null && on != null && on.isAfter(end)) {
                return;
            }
            lines.computeIfAbsent(rs.getLong("claim_id"), k -> new ArrayList<>())
                    .add(rs.getString("label") + " " + rs.getInt("qty") + " x "
                            + rs.getBigDecimal("unit").setScale(2).toPlainString());
        }, sheetId);
        return lines;
    }
}

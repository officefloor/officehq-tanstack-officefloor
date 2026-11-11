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

/** The claims' lines, kept per sheet, oldest first. */
@Repository
public class ClaimLineRepository {

    private final JdbcTemplate jdbc;

    public ClaimLineRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Keep a line on the claim, added on {@code today}. */
    public void add(Claim claim, LocalDate today, ClaimLine line) {
        jdbc.update("INSERT INTO claim_line (sheet_id, claim_id, label, qty, unit, on_date) VALUES (?, ?, ?, ?, ?, ?)",
                claim.getSheetId(), claim.getId(), line.label(), line.qty(), line.unit(), Date.valueOf(today));
    }

    /** The claim's lines' total: their quantities times their unit amounts added up; null when it has none. */
    public BigDecimal total(Claim claim) {
        return jdbc.queryForObject("SELECT SUM(qty * unit) FROM claim_line WHERE sheet_id = ? AND claim_id = ?",
                BigDecimal.class, claim.getSheetId(), claim.getId());
    }

    /**
     * The sheet's lines by claim id, each as the screen shows it: {@code <label> <qty> x <unit>};
     * as of {@code day} (null: now), only those added on or before it.
     */
    public Map<Long, List<String>> findAll(String sheetId, LocalDate day) {
        Map<Long, List<String>> lines = new HashMap<>();
        String sql = "SELECT claim_id, label, qty, unit FROM claim_line WHERE sheet_id = ?"
                + (day == null ? "" : " AND on_date <= ?") + " ORDER BY id";
        Object[] args = day == null ? new Object[] { sheetId } : new Object[] { sheetId, Date.valueOf(day) };
        jdbc.query(sql, (rs) -> {
            lines.computeIfAbsent(rs.getLong("claim_id"), (k) -> new ArrayList<>())
                    .add(rs.getString("label") + " " + rs.getInt("qty") + " x "
                            + rs.getBigDecimal("unit").setScale(2).toPlainString());
        }, args);
        return lines;
    }
}

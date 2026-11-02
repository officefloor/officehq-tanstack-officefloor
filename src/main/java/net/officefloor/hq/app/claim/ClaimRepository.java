package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ClaimRepository {

    private static final String COLUMNS = "id, person_id, amount, claim_date, note, status, claim_no, settled_due";

    private static final RowMapper<Claim> ROW = (rs, i) -> new Claim(rs.getLong("id"), rs.getLong("person_id"),
            money(rs.getBigDecimal("amount")), rs.getDate("claim_date").toLocalDate().toString(),
            rs.getString("note"), rs.getString("status"),
            rs.getObject("claim_no", Long.class), dueOf(rs.getBigDecimal("settled_due")));

    private final JdbcTemplate jdbc;

    public ClaimRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean personExists(String sheetId, long personId) {
        Integer known = jdbc.queryForObject("SELECT COUNT(*) FROM person WHERE sheet_id = ? AND id = ?",
                Integer.class, sheetId, personId);
        return known != null && known > 0;
    }

    /** The sheet's claims, newest first. */
    public List<Claim> list(String sheetId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? ORDER BY id DESC", ROW, sheetId);
    }

    /** A claim on the sheet; null when the sheet has no such claim. */
    public Claim find(String sheetId, long claimId) {
        List<Claim> found = jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? AND id = ?", ROW,
                sheetId, claimId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Move a claim to a status, only when it is now in one of {@code from}; false when it is not. */
    public boolean changeStatus(String sheetId, long claimId, List<String> from, String to) {
        String in = String.join(", ", from.stream().map(s -> "?").toList());
        List<Object> args = new ArrayList<>(List.of(to, claimId, sheetId));
        args.addAll(from);
        return jdbc.update("UPDATE claim SET status = ? WHERE id = ? AND sheet_id = ? AND status IN (" + in + ")",
                args.toArray()) > 0;
    }

    /** Give an opened claim the sheet's next number. The sheet's row stays locked until the
     *  transaction commits, so two claims opened at once get consecutive numbers. */
    public long number(String sheetId, long claimId) {
        jdbc.update("UPDATE sheet SET claim_number = claim_number + 1 WHERE id = ?", sheetId);
        Long next = jdbc.queryForObject("SELECT claim_number FROM sheet WHERE id = ?", Long.class, sheetId);
        jdbc.update("UPDATE claim SET claim_no = ? WHERE id = ? AND sheet_id = ?", next, claimId, sheetId);
        return next;
    }

    /** Clear a claim's number (it is never given to another claim). */
    public void clearNumber(String sheetId, long claimId) {
        jdbc.update("UPDATE claim SET claim_no = NULL WHERE id = ? AND sheet_id = ?", claimId, sheetId);
    }

    /** A claim's due now: its amount at the rate for its person's grade, rounded half up to two decimals;
     *  null when the grade has no rate. */
    public BigDecimal due(String sheetId, Claim claim) {
        List<BigDecimal> pct = jdbc.queryForList("SELECT r.pct FROM rate r JOIN person p ON p.sheet_id = r.sheet_id"
                + " AND p.grade = r.grade WHERE p.sheet_id = ? AND p.id = ?", BigDecimal.class, sheetId,
                claim.personId());
        return pct.isEmpty() ? null
                : new BigDecimal(claim.amount()).multiply(pct.get(0)).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
    }

    /** Fix a settled claim's due. */
    public void settleDue(String sheetId, long claimId, BigDecimal due) {
        jdbc.update("UPDATE claim SET settled_due = ? WHERE id = ? AND sheet_id = ?", due, claimId, sheetId);
    }

    /** The name of a person on the sheet. */
    public String personName(String sheetId, long personId) {
        return jdbc.queryForObject("SELECT name FROM person WHERE sheet_id = ? AND id = ?",
                String.class, sheetId, personId);
    }

    public Claim add(String sheetId, long personId, BigDecimal amount, LocalDate date, String note) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note) VALUES (?, ?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setLong(2, personId);
            ps.setBigDecimal(3, amount);
            ps.setDate(4, Date.valueOf(date));
            ps.setString(5, note);
            return ps;
        }, keys);
        return new Claim(keys.getKey().longValue(), personId, money(amount), date.toString(), note, "DRAFT", null, null);
    }

    /** Change a claim's amount, date and note; false when the sheet has no such claim. */
    public boolean update(String sheetId, long claimId, BigDecimal amount, LocalDate date, String note) {
        return jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ? WHERE id = ? AND sheet_id = ?",
                amount, Date.valueOf(date), note, claimId, sheetId) > 0;
    }

    /** An amount as shown: two decimals, a dot, no grouping. */
    static String money(BigDecimal amount) {
        return amount.setScale(2).toPlainString();
    }

    /** A fixed due as shown, or null when there is none. */
    static String dueOf(BigDecimal due) {
        return due == null ? null : money(due);
    }
}

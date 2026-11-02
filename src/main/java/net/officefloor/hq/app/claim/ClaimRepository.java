package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ClaimRepository {

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
        return jdbc.query(
                "SELECT id, person_id, amount, claim_date, note FROM claim WHERE sheet_id = ? ORDER BY id DESC",
                (rs, i) -> new Claim(rs.getLong("id"), rs.getLong("person_id"),
                        money(rs.getBigDecimal("amount")), rs.getDate("claim_date").toLocalDate().toString(),
                        rs.getString("note")),
                sheetId);
    }

    /** The person (on the sheet) a claim belongs to; null when the sheet has no such claim. */
    public Long personOf(String sheetId, long claimId) {
        List<Long> found = jdbc.queryForList("SELECT person_id FROM claim WHERE sheet_id = ? AND id = ?",
                Long.class, sheetId, claimId);
        return found.isEmpty() ? null : found.get(0);
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
        return new Claim(keys.getKey().longValue(), personId, money(amount), date.toString(), note);
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
}

package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** The store of claims, kept per sheet. */
@Repository
public class Claims {

    private static final RowMapper<Claim> ROW = (rs, n) -> new Claim(rs.getLong("id"), rs.getString("sheet_id"),
            rs.getLong("person_id"), rs.getBigDecimal("amount"), rs.getObject("claim_date", LocalDate.class),
            rs.getString("note"), ClaimStatus.valueOf(rs.getString("status")),
            rs.getObject("claim_number", Long.class), rs.getBigDecimal("settled_due"));

    private final JdbcTemplate jdbc;

    public Claims(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's claims, newest first. */
    public List<Claim> list(String sheetId) {
        return jdbc.query("SELECT * FROM claim WHERE sheet_id = ? ORDER BY id DESC", ROW, sheetId);
    }

    public Claim find(String sheetId, long id) {
        List<Claim> found = jdbc.query("SELECT * FROM claim WHERE sheet_id = ? AND id = ?", ROW, sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    public Claim add(String sheetId, long personId, BigDecimal amount, LocalDate date, String note) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note) VALUES (?, ?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setLong(2, personId);
            ps.setBigDecimal(3, amount);
            ps.setDate(4, Date.valueOf(date));
            ps.setString(5, note);
            return ps;
        }, key);
        return new Claim(key.getKey().longValue(), sheetId, personId, amount, date, note, ClaimStatus.DRAFT, null, null);
    }

    public void save(Claim claim) {
        jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ? WHERE id = ?", claim.amount(),
                Date.valueOf(claim.date()), claim.note(), claim.id());
    }

    /**
     * Moves the claim to {@code next} if it still has its loaded status, and returns it as changed; null when
     * it has since changed. Opening gives it the sheet's next number; voiding clears its number. Settling
     * stores {@code due} as its fixed due (null keeps it live).
     */
    public Claim changeStatus(Claim claim, ClaimStatus next, BigDecimal due) {
        if (jdbc.update("UPDATE claim SET status = ? WHERE id = ? AND status = ?", next.name(), claim.id(),
                claim.status().name()) != 1) {
            return null;
        }
        Long number = switch (next) {
            case OPEN -> nextNumber(claim.sheetId());
            case VOID -> null;
            default -> claim.number();
        };
        BigDecimal settledDue = next == ClaimStatus.SETTLED ? due : claim.settledDue();
        jdbc.update("UPDATE claim SET claim_number = ?, settled_due = ? WHERE id = ?", number, settledDue,
                claim.id());
        return claim.withStatus(next, number, settledDue);
    }

    /**
     * The sheet's next claim number. The counter row is locked by the increment until the transaction
     * commits, so two claims opened at once get different numbers.
     */
    private long nextNumber(String sheetId) {
        try {
            jdbc.update("INSERT INTO claim_number_seq (sheet_id, last_number) SELECT ?, 0 "
                    + "WHERE NOT EXISTS (SELECT 1 FROM claim_number_seq WHERE sheet_id = ?)", sheetId, sheetId);
        } catch (DuplicateKeyException e) {
            // Another user started the sheet's counter at the same moment.
        }
        jdbc.update("UPDATE claim_number_seq SET last_number = last_number + 1 WHERE sheet_id = ?", sheetId);
        return jdbc.queryForObject("SELECT last_number FROM claim_number_seq WHERE sheet_id = ?", Long.class,
                sheetId);
    }
}

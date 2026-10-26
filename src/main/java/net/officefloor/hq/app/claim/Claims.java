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
            rs.getString("claim_number"), rs.getBigDecimal("settled_due"),
            rs.getString("opened_by"), rs.getBoolean("stated"), rs.getBigDecimal("stated_due"),
            rs.getString("corrects"));

    /** The next place in a sheet's newest-first list (the sheet's id its one parameter). */
    private static final String NEXT_LISTED = "(SELECT COALESCE(MAX(listed), 0) + 1 FROM claim WHERE sheet_id = ?)";

    private final JdbcTemplate jdbc;

    public Claims(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's claims, newest first (a reassigned claim as new as when it was moved). */
    public List<Claim> list(String sheetId) {
        return jdbc.query("SELECT * FROM claim WHERE sheet_id = ? ORDER BY listed DESC, id DESC", ROW, sheetId);
    }

    public Claim find(String sheetId, long id) {
        List<Claim> found = jdbc.query("SELECT * FROM claim WHERE sheet_id = ? AND id = ?", ROW, sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The person's claim with the number, or null when they have none. */
    public Claim findByNumber(String sheetId, long personId, String number) {
        List<Claim> found = jdbc.query("SELECT * FROM claim WHERE sheet_id = ? AND person_id = ? AND claim_number = ?",
                ROW, sheetId, personId, number);
        return found.isEmpty() ? null : found.get(0);
    }

    public Claim add(String sheetId, long personId, BigDecimal amount, LocalDate date, String note, String corrects) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, corrects, listed) VALUES (?, ?, ?, ?, ?, ?, "
                            + NEXT_LISTED + ")",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setLong(2, personId);
            ps.setBigDecimal(3, amount);
            ps.setDate(4, Date.valueOf(date));
            ps.setString(5, note);
            ps.setObject(6, corrects);
            ps.setString(7, sheetId);
            return ps;
        }, key);
        return new Claim(key.getKey().longValue(), sheetId, personId, amount, date, note, ClaimStatus.DRAFT, null, null, null,
                false, null, corrects);
    }

    public void save(Claim claim) {
        jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ?, corrects = ? WHERE id = ?", claim.amount(),
                Date.valueOf(claim.date()), claim.note(), claim.corrects(), claim.id());
    }

    /**
     * Moves the claim to another person if it still has its loaded person and status, and returns it as moved; null
     * when it has since changed. It keeps its number and date, is unbound from any statement (live again until its
     * new person's next statement) and lists as the newest claim on the sheet.
     */
    public Claim reassign(Claim claim, long personId) {
        if (jdbc.update("UPDATE claim SET person_id = ?, stated = FALSE, stated_due = NULL, listed = " + NEXT_LISTED
                + " WHERE id = ? AND person_id = ? AND status = ?", personId, claim.sheetId(), claim.id(),
                claim.personId(), claim.status().name()) != 1) {
            return null;
        }
        return claim.withPerson(personId);
    }

    /**
     * Moves the claim to {@code next} if it still has its loaded status, and returns it as changed; null when
     * it has since changed. Opening gives it the next number of {@code scope} (null: the sheet-wide sequence; a
     * period's name, or "" for no period, while the sheet numbers by period), which it keeps
     * (voided or rejected back to OPEN too: a number is never reused). Settling
     * stores {@code due} as its fixed due (null keeps it live). Opening records {@code user} as its opener.
     */
    public Claim changeStatus(Claim claim, ClaimStatus next, BigDecimal due, String user, String scope) {
        if (jdbc.update("UPDATE claim SET status = ? WHERE id = ? AND status = ?", next.name(), claim.id(),
                claim.status().name()) != 1) {
            return null;
        }
        String number = switch (next) {
            case OPEN -> claim.status() != ClaimStatus.DRAFT ? claim.number() : scope == null ? Long.toString(nextNumber(claim.sheetId()))
                    : (scope.isEmpty() ? "X" : scope) + "-" + nextNumber(claim.sheetId(), scope);
            default -> claim.number();
        };
        BigDecimal settledDue = next == ClaimStatus.SETTLED ? due : claim.settledDue();
        String openedBy = next == ClaimStatus.OPEN && claim.status() == ClaimStatus.DRAFT ? user : claim.openedBy();
        jdbc.update("UPDATE claim SET claim_number = ?, settled_due = ?, opened_by = ? WHERE id = ?", number,
                settledDue, openedBy, claim.id());
        return claim.withStatus(next, number, settledDue, openedBy);
    }

    /**
     * Binds the person's claims to the figures of a statement: each claim in {@code figures} is stated at
     * its figure (null: n/a), and every other claim of theirs is left unbound.
     */
    public void bind(String sheetId, long personId, java.util.Map<Long, BigDecimal> figures) {
        jdbc.update("UPDATE claim SET stated = FALSE, stated_due = NULL WHERE sheet_id = ? AND person_id = ?",
                sheetId, personId);
        figures.forEach((id, figure) -> jdbc.update(
                "UPDATE claim SET stated = TRUE, stated_due = ? WHERE id = ?", figure, id));
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

    /** The next number of the sheet's sequence for the period ("" for no period); locked like {@link #nextNumber(String)}. */
    private long nextNumber(String sheetId, String scope) {
        try {
            jdbc.update("INSERT INTO claim_period_number_seq (sheet_id, scope, last_number) SELECT ?, ?, 0 "
                    + "WHERE NOT EXISTS (SELECT 1 FROM claim_period_number_seq WHERE sheet_id = ? AND scope = ?)",
                    sheetId, scope, sheetId, scope);
        } catch (DuplicateKeyException e) {
            // Another user started the period's counter at the same moment.
        }
        jdbc.update("UPDATE claim_period_number_seq SET last_number = last_number + 1 WHERE sheet_id = ? AND scope = ?",
                sheetId, scope);
        return jdbc.queryForObject("SELECT last_number FROM claim_period_number_seq WHERE sheet_id = ? AND scope = ?",
                Long.class, sheetId, scope);
    }
}

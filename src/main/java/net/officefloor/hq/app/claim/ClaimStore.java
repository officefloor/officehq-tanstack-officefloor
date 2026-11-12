package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The claims kept per sheet, newest first: by when each was added, or moved to its person. */
@Repository
public class ClaimStore {

    private static final RowMapper<Claim> ROW = (rs, i) -> {
        Claim claim = new Claim(rs.getLong("id"), rs.getString("sheet_id"),
            rs.getLong("person_id"), rs.getBigDecimal("amount"), rs.getObject("claim_date", LocalDate.class),
            rs.getString("note"), ClaimStatus.valueOf(rs.getString("status")),
            rs.getString("number"), rs.getBigDecimal("settled_due"),
            rs.getString("opened_by"), rs.getBoolean("stated"), rs.getBigDecimal("stated_due"),
            rs.getString("corrects"), rs.getObject("due_by", LocalDate.class));
        claim.setStatedNa(rs.getBoolean("stated_na"));
        return claim;
    };

    private final JdbcTemplate jdbc;

    public ClaimStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Claim> list(String sheetId) {
        return jdbc.query("SELECT * FROM claim WHERE sheet_id = ? ORDER BY listed DESC", ROW, sheetId);
    }

    public Claim find(String sheetId, long id) {
        List<Claim> found = jdbc.query("SELECT * FROM claim WHERE sheet_id = ? AND id = ?", ROW, sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The person's claim with the number, or null when they have none. */
    public Claim findByNumber(String sheetId, long personId, String number) {
        List<Claim> found = jdbc.query("SELECT * FROM claim WHERE sheet_id = ? AND person_id = ? AND number = ?", ROW,
                sheetId, personId, number);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Adds a draft claim due by {@code dueBy}, which stays as it is whatever later changes. */
    public Claim add(String sheetId, long personId, BigDecimal amount, LocalDate date, String note, String corrects,
            LocalDate dueBy) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, corrects, due_by) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    new String[] { "ID" });
            ps.setString(1, sheetId);
            ps.setLong(2, personId);
            ps.setBigDecimal(3, amount);
            ps.setDate(4, Date.valueOf(date));
            ps.setString(5, note);
            ps.setString(6, corrects);
            ps.setDate(7, Date.valueOf(dueBy));
            return ps;
        }, keys);
        return new Claim(keys.getKey().longValue(), sheetId, personId, amount, date, note, ClaimStatus.DRAFT, null, null, null, false, null,
                corrects, dueBy);
    }

    public void save(Claim claim) {
        jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ?, status = ?, number = ?, settled_due = ?, opened_by = ? "
                + "WHERE sheet_id = ? AND id = ?",
                claim.getAmount(), Date.valueOf(claim.getDate()), claim.getNote(), claim.getStatus().name(),
                claim.getNumber(), claim.getSettledDue(), claim.getOpenedBy(), claim.getSheetId(), claim.getId());
    }

    /** Stores a draft's amount, date, note and the claim it corrects; false when the stored claim is no longer a draft. */
    public boolean edit(Claim claim) {
        return jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ?, corrects = ? WHERE sheet_id = ? AND id = ? AND status = ?",
                claim.getAmount(), Date.valueOf(claim.getDate()), claim.getNote(), claim.getCorrects(), claim.getSheetId(), claim.getId(),
                ClaimStatus.DRAFT.name()) == 1;
    }

    /** The person's claims, newest first, their rows locked until the transaction ends. */
    public List<Claim> listForUpdate(String sheetId, long personId) {
        return jdbc.query("SELECT * FROM claim WHERE sheet_id = ? AND person_id = ? ORDER BY listed DESC FOR UPDATE",
                ROW, sheetId, personId);
    }

    /** Binds the claim to the figure (null for n/a), or unbinds it when not {@code stated}. */
    public void bind(Claim claim) {
        jdbc.update("UPDATE claim SET stated = ?, stated_due = ?, stated_na = ? WHERE sheet_id = ? AND id = ?",
                claim.isStated(), claim.getStatedDue(), claim.isStatedNa(), claim.getSheetId(), claim.getId());
    }

    /** Moves the stored DRAFT, OPEN or PENDING claim to the person, unbound from any statement figure and
     *  listed as their newest claim; false when it is no longer one of those (another user moved it on). */
    public boolean reassign(Claim claim, long personId) {
        boolean moved = jdbc.update("UPDATE claim SET person_id = ?, stated = FALSE, stated_due = NULL, stated_na = FALSE, "
                + "listed = NEXT VALUE FOR claim_listed_seq WHERE sheet_id = ? AND id = ? AND status IN (?, ?, ?)",
                personId, claim.getSheetId(), claim.getId(), ClaimStatus.DRAFT.name(), ClaimStatus.OPEN.name(),
                ClaimStatus.PENDING.name()) == 1;
        if (moved) {
            claim.setPersonId(personId);
            claim.setStated(false);
            claim.setStatedDue(null);
            claim.setStatedNa(false);
        }
        return moved;
    }

    /** Moves the stored claim from {@code from} to {@code to}, locking its row until the transaction
     *  ends; false when it is no longer {@code from} (another user moved it first). */
    public boolean move(Claim claim, ClaimStatus from, ClaimStatus to) {
        return jdbc.update("UPDATE claim SET status = ? WHERE sheet_id = ? AND id = ? AND status = ?", to.name(),
                claim.getSheetId(), claim.getId(), from.name()) == 1;
    }

    /** Takes the next claim number on the sheet: 1, then 2, and so on. The sheet's counter row stays
     *  locked until the transaction ends, so a number is never given twice, and is not used up if
     *  the transaction rolls back. */
    public long nextNumber(String sheetId) {
        try {
            jdbc.update("INSERT INTO claim_number (sheet_id, last_number) SELECT ?, 0 FROM DUAL "
                    + "WHERE NOT EXISTS (SELECT 1 FROM claim_number WHERE sheet_id = ?)", sheetId, sheetId);
        } catch (DataAccessException e) {
            // another transaction added the sheet's counter first
        }
        jdbc.update("UPDATE claim_number SET last_number = last_number + 1 WHERE sheet_id = ?", sheetId);
        return jdbc.queryForObject("SELECT last_number FROM claim_number WHERE sheet_id = ?", Long.class, sheetId);
    }

    /** Takes the next number in one of the sheet's per-period sequences: 1, then 2, and so on, locked
     *  as {@link #nextNumber(String)} is. */
    public long nextNumber(String sheetId, String sequence) {
        try {
            jdbc.update("INSERT INTO claim_period_number (sheet_id, sequence, last_number) SELECT ?, ?, 0 FROM DUAL "
                    + "WHERE NOT EXISTS (SELECT 1 FROM claim_period_number WHERE sheet_id = ? AND sequence = ?)",
                    sheetId, sequence, sheetId, sequence);
        } catch (DataAccessException e) {
            // another transaction added the sequence's counter first
        }
        jdbc.update("UPDATE claim_period_number SET last_number = last_number + 1 WHERE sheet_id = ? AND sequence = ?",
                sheetId, sequence);
        return jdbc.queryForObject("SELECT last_number FROM claim_period_number WHERE sheet_id = ? AND sequence = ?",
                Long.class, sheetId, sequence);
    }
}

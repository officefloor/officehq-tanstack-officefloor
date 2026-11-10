package net.officefloor.hq.app.claim;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The claims store, kept per sheet. */
@Repository
public class ClaimRepository {

    private static final String COLUMNS = "id, sheet_id, person_id, amount, claim_date, note, status, claim_number, settled_due, opened_by, stated, stated_due, corrects, due_by, deferred_until, proofs, split_from, late_entry,"
            + " (SELECT c.claim_date FROM claim c WHERE c.sheet_id = claim.sheet_id AND c.person_id = claim.person_id"
            + " AND c.claim_number = claim.corrects) AS corrected_date,"
            + " CASE WHEN locked_on = (SELECT s.today FROM sheet s WHERE s.id = claim.sheet_id) THEN locked_by END AS locked_by_now";

    private static final RowMapper<Claim> ROW = (rs, n) -> {
        Claim claim = new Claim(rs.getLong("id"),
            rs.getString("sheet_id"), rs.getLong("person_id"), rs.getBigDecimal("amount"),
            rs.getDate("claim_date").toLocalDate(), rs.getString("note"),
            ClaimStatus.valueOf(rs.getString("status")),
            rs.getString("claim_number"), rs.getBigDecimal("settled_due"),
            rs.getString("opened_by"));
        claim.bind(rs.getBoolean("stated"), rs.getBigDecimal("stated_due"));
        claim.setCorrects(rs.getString("corrects"));
        claim.setDueBy(rs.getDate("due_by").toLocalDate());
        java.sql.Date deferred = rs.getDate("deferred_until");
        claim.setDeferredUntil(deferred == null ? null : deferred.toLocalDate());
        claim.setProofs(rs.getInt("proofs"));
        claim.setSplitFrom(rs.getString("split_from"));
        claim.setLateEntry(rs.getBoolean("late_entry"));
        claim.setLockedBy(rs.getString("locked_by_now"));
        java.sql.Date corrected = rs.getDate("corrected_date");
        claim.setCorrectedDate(corrected == null ? null : corrected.toLocalDate());
        return claim;
    };

    private final JdbcTemplate jdbc;

    public ClaimRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's claims, newest first (a moved claim listed as made when it was moved). */
    public List<Claim> findAll(String sheetId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? ORDER BY listed DESC, id DESC",
                ROW, sheetId);
    }

    /** The person's claims on the sheet, newest first. */
    public List<Claim> findByPerson(String sheetId, long personId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? AND person_id = ? ORDER BY listed DESC, id DESC",
                ROW, sheetId, personId);
    }

    /** The claim with this id on the sheet, or null when there is none. */
    public Claim find(String sheetId, long id) {
        List<Claim> found = jdbc.query(
                "SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? AND id = ?", ROW, sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * Mark the claim OPEN in the store when it is still DRAFT there, holding its row until the
     * request commits so two opens of the same claim cannot both go through; false when another
     * request changed it first.
     */
    public boolean markOpened(Claim claim) {
        return jdbc.update("UPDATE claim SET status = 'OPEN' WHERE id = ? AND sheet_id = ? AND status = 'DRAFT'",
                claim.getId(), claim.getSheetId()) == 1;
    }

    /**
     * Lock the claim for {@code user}'s editor on {@code today}, in one statement so two users
     * opening it at once cannot both hold it; false when another user holds it on that today (a
     * lock taken on an earlier today has lapsed).
     */
    public boolean lock(Claim claim, String user, LocalDate today) {
        return jdbc.update("UPDATE claim SET locked_by = ?, locked_on = ? WHERE id = ? AND sheet_id = ?"
                + " AND (locked_by IS NULL OR locked_by = ? OR locked_on IS NULL OR locked_on <> ?)",
                user, Date.valueOf(today), claim.getId(), claim.getSheetId(), user, Date.valueOf(today)) == 1;
    }

    /** Release the locks {@code user} holds on the same person's other claims. */
    public void unlockOthers(Claim claim, String user) {
        jdbc.update("UPDATE claim SET locked_by = NULL, locked_on = NULL WHERE sheet_id = ? AND person_id = ?"
                + " AND locked_by = ? AND id <> ?", claim.getSheetId(), claim.getPersonId(), user, claim.getId());
    }

    /** Release the claim's lock: only when {@code user} holds it, or whoever does when null. */
    public void unlock(Claim claim, String user) {
        if (user == null) {
            jdbc.update("UPDATE claim SET locked_by = NULL, locked_on = NULL WHERE id = ? AND sheet_id = ?",
                    claim.getId(), claim.getSheetId());
        } else {
            jdbc.update("UPDATE claim SET locked_by = NULL, locked_on = NULL WHERE id = ? AND sheet_id = ? AND locked_by = ?",
                    claim.getId(), claim.getSheetId(), user);
        }
    }

    /** Whether a user other than {@code user} holds the claim's lock on {@code today}. */
    public boolean lockedByOther(Claim claim, String user, LocalDate today) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM claim WHERE id = ? AND sheet_id = ?"
                + " AND locked_by IS NOT NULL AND locked_by <> ? AND locked_on = ?",
                Integer.class, claim.getId(), claim.getSheetId(), user, Date.valueOf(today));
        return n != null && n > 0;
    }

    /**
     * The sheet's next claim number, taken in one statement so two claims opened at the same
     * moment never share a number; it is never given out again.
     */
    public int nextNumber(String sheetId) {
        return jdbc.queryForObject("SELECT last_claim_number FROM FINAL TABLE"
                + " (UPDATE sheet SET last_claim_number = last_claim_number + 1 WHERE id = ?)",
                Integer.class, sheetId);
    }

    /**
     * The next number in one period's sequence on the sheet ({@code sequence} '' for claims in no
     * period), counting from 1; taken in one statement so two claims opened at the same moment
     * never share a number, and never given out again.
     */
    public int nextNumberIn(String sheetId, String sequence) {
        jdbc.update("MERGE INTO period_number p USING (VALUES (CAST(? AS VARCHAR(200)), CAST(? AS VARCHAR(200)))) v (sheet_id, seq_name)"
                + " ON p.sheet_id = v.sheet_id AND p.seq_name = v.seq_name"
                + " WHEN NOT MATCHED THEN INSERT (sheet_id, seq_name, last_number) VALUES (v.sheet_id, v.seq_name, 0)",
                sheetId, sequence);
        return jdbc.queryForObject("SELECT last_number FROM FINAL TABLE"
                + " (UPDATE period_number SET last_number = last_number + 1 WHERE sheet_id = ? AND seq_name = ?)",
                Integer.class, sheetId, sequence);
    }

    /** The sheet's next place in its claims list, taken in one statement; never given out again. */
    private long nextListed(String sheetId) {
        return jdbc.queryForObject("SELECT last_claim_listed FROM FINAL TABLE"
                + " (UPDATE sheet SET last_claim_listed = last_claim_listed + 1 WHERE id = ?)",
                Long.class, sheetId);
    }

    /** Insert a new claim (assigning its id) or update an existing one. */
    public void save(Claim claim) {
        if (claim.getId() != null) {
            if (claim.isRelisted()) {
                jdbc.update("UPDATE claim SET person_id = ?, listed = ? WHERE id = ? AND sheet_id = ?",
                        claim.getPersonId(), nextListed(claim.getSheetId()), claim.getId(), claim.getSheetId());
            }
            jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ?, status = ?, claim_number = ?, settled_due = ?, opened_by = ?, stated = ?, stated_due = ?, corrects = ?, deferred_until = ?, proofs = ? WHERE id = ? AND sheet_id = ?",
                    claim.getAmount(), Date.valueOf(claim.getDate()), claim.getNote(),
                    claim.getStatus().name(), claim.getNumber(), claim.getSettledDue(), claim.getOpenedBy(),
                    claim.isStated(), claim.getStatedDue(), claim.getCorrects(),
                    claim.getDeferredUntil() == null ? null : Date.valueOf(claim.getDeferredUntil()), claim.getProofs(), claim.getId(),
                    claim.getSheetId());
            return;
        }
        long listed = nextListed(claim.getSheetId());
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, status, corrects, listed, due_by, split_from, claim_number, opened_by, late_entry) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, claim.getSheetId());
            ps.setLong(2, claim.getPersonId());
            ps.setBigDecimal(3, claim.getAmount());
            ps.setDate(4, Date.valueOf(claim.getDate()));
            ps.setString(5, claim.getNote());
            ps.setString(6, claim.getStatus().name());
            ps.setString(7, claim.getCorrects());
            ps.setLong(8, listed);
            ps.setDate(9, Date.valueOf(claim.getDueBy()));
            ps.setString(10, claim.getSplitFrom());
            ps.setString(11, claim.getNumber());
            ps.setString(12, claim.getOpenedBy());
            ps.setBoolean(13, claim.isLateEntry());
            return ps;
        }, keys);
        claim.setId(keys.getKey().longValue());
    }
}

package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The claims on each sheet; ids are assigned in the order claims are added, and claims are listed newest first (a moved claim as its new person's newest). */
@Repository
public class ClaimRepository {

    // A correction rounds by the date of the claim it corrects (a claim of the same person, by number).
    private static final String COLUMNS = "id, sheet_id, person_id, amount, claim_date, note, status, claim_number, settled_due, opened_by, stated, stated_due, corrects,"
            + " (SELECT o.claim_date FROM claim o WHERE o.sheet_id = claim.sheet_id AND o.person_id = claim.person_id"
            + " AND o.claim_number = claim.corrects) AS corrected_date";

    private static final RowMapper<Claim> ROW = (rs, n) -> {
        Claim claim = new Claim(rs.getLong("id"), rs.getString("sheet_id"),
                rs.getLong("person_id"), rs.getBigDecimal("amount"), rs.getObject("claim_date", LocalDate.class),
                rs.getString("note"), ClaimStatus.valueOf(rs.getString("status")),
                rs.getString("claim_number"), rs.getBigDecimal("settled_due"),
                rs.getString("opened_by"), rs.getBoolean("stated"), rs.getBigDecimal("stated_due"),
                rs.getString("corrects"));
        LocalDate corrected = rs.getObject("corrected_date", LocalDate.class);
        if (corrected != null) {
            claim.setRoundOn(corrected);
        }
        return claim;
    };

    private final JdbcTemplate jdbc;

    public ClaimRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The person's claims, newest first. */
    public List<Claim> listOf(String sheetId, long personId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? AND person_id = ? ORDER BY listed DESC", ROW,
                sheetId, personId);
    }

    /** Bind the claim to a figure (null: n/a), or unbind it when not {@code stated}. */
    public void bind(long id, boolean stated, BigDecimal statedDue) {
        jdbc.update("UPDATE claim SET stated = ?, stated_due = ? WHERE id = ?", stated, statedDue, id);
    }

    /** The sheet's claims, newest first. */
    public List<Claim> list(String sheetId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? ORDER BY listed DESC", ROW, sheetId);
    }

    /** Whether the person has a SETTLED claim with this number on the sheet. */
    public boolean settledNumbered(String sheetId, long personId, String number) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM claim WHERE sheet_id = ? AND person_id = ? AND claim_number = ? AND status = ?",
                Integer.class, sheetId, personId, number, ClaimStatus.SETTLED.name()) > 0;
    }

    public Claim find(String sheetId, long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? AND id = ?", ROW, sheetId, id)
                .stream().findFirst().orElse(null);
    }

    public Claim insert(String sheetId, long personId, BigDecimal amount, LocalDate date, String note, String corrects) {
        long listed = nextListed(sheetId);
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, corrects, listed) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setLong(2, personId);
            ps.setBigDecimal(3, amount);
            ps.setDate(4, Date.valueOf(date));
            ps.setString(5, note);
            ps.setString(6, corrects);
            ps.setLong(7, listed);
            return ps;
        }, keys);
        return new Claim(keys.getKey().longValue(), sheetId, personId, amount, date, note, ClaimStatus.DRAFT, null, null, null, false, null, corrects);
    }

    public void update(Claim claim) {
        jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ?, corrects = ? WHERE id = ?", claim.getAmount(),
                Date.valueOf(claim.getDate()), claim.getNote(), claim.getCorrects(), claim.getId());
    }

    /**
     * Move the claim from {@code from} to its status, number, settled due and opener. Whether it was still {@code from}:
     * the row is locked by the first of two users changing it, so the other one finds it moved.
     */
    public boolean updateStatus(Claim claim, ClaimStatus from) {
        return jdbc.update("UPDATE claim SET status = ?, claim_number = ?, settled_due = ?, opened_by = ? WHERE id = ? AND status = ?",
                claim.getStatus().name(), claim.getNumber(), claim.getSettledDue(), claim.getOpenedBy(), claim.getId(), from.name()) == 1;
    }

    /**
     * Move the claim to its person, unbound from any statement figure and listed as that person's newest claim. Whether
     * it was still in its status: the first of two users changing it locks the row, so the other one finds it moved.
     */
    public boolean reassign(Claim claim, long personId) {
        boolean moved = jdbc.update("UPDATE claim SET person_id = ?, stated = FALSE, stated_due = NULL, listed = ?"
                + " WHERE id = ? AND status = ?", personId, nextListed(claim.getSheetId()), claim.getId(),
                claim.getStatus().name()) == 1;
        if (moved) {
            claim.moveTo(personId);
        }
        return moved;
    }

    /** Take the sheet's next listing key: a claim added or moved is listed before every claim listed earlier. */
    private long nextListed(String sheetId) {
        return jdbc.queryForObject("SELECT last_claim_listed FROM FINAL TABLE "
                + "(UPDATE sheet SET last_claim_listed = last_claim_listed + 1 WHERE id = ?)", Long.class, sheetId);
    }

    /** Take the sheet's next claim number, in one statement so two users never get the same one. */
    public int nextNumber(String sheetId) {
        return jdbc.queryForObject("SELECT last_claim_number FROM FINAL TABLE "
                + "(UPDATE sheet SET last_claim_number = last_claim_number + 1 WHERE id = ?)", Integer.class, sheetId);
    }

    /** Take the next number of one of the sheet's per-period sequences (a period's id, or X for no period), from 1. */
    public int nextNumber(String sheetId, String sequence) {
        try {
            jdbc.update("INSERT INTO claim_sequence (sheet_id, seq_key) SELECT ?, ? FROM DUAL WHERE NOT EXISTS"
                    + " (SELECT 1 FROM claim_sequence WHERE sheet_id = ? AND seq_key = ?)", sheetId, sequence, sheetId, sequence);
        } catch (org.springframework.dao.DuplicateKeyException started) {
            // another user started it first
        }
        return jdbc.queryForObject("SELECT last_number FROM FINAL TABLE (UPDATE claim_sequence SET last_number = last_number + 1"
                + " WHERE sheet_id = ? AND seq_key = ?)", Integer.class, sheetId, sequence);
    }
}

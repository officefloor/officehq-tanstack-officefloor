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

/** The claims on each sheet; ids are assigned in the order claims are added. */
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
                rs.getObject("claim_number", Integer.class), rs.getBigDecimal("settled_due"),
                rs.getString("opened_by"), rs.getBoolean("stated"), rs.getBigDecimal("stated_due"),
                rs.getObject("corrects", Integer.class));
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
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? AND person_id = ? ORDER BY id DESC", ROW,
                sheetId, personId);
    }

    /** Bind the claim to a figure (null: n/a), or unbind it when not {@code stated}. */
    public void bind(long id, boolean stated, BigDecimal statedDue) {
        jdbc.update("UPDATE claim SET stated = ?, stated_due = ? WHERE id = ?", stated, statedDue, id);
    }

    /** The sheet's claims, newest first. */
    public List<Claim> list(String sheetId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? ORDER BY id DESC", ROW, sheetId);
    }

    /** Whether the person has a SETTLED claim with this number on the sheet. */
    public boolean settledNumbered(String sheetId, long personId, int number) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM claim WHERE sheet_id = ? AND person_id = ? AND claim_number = ? AND status = ?",
                Integer.class, sheetId, personId, number, ClaimStatus.SETTLED.name()) > 0;
    }

    public Claim find(String sheetId, long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? AND id = ?", ROW, sheetId, id)
                .stream().findFirst().orElse(null);
    }

    public Claim insert(String sheetId, long personId, BigDecimal amount, LocalDate date, String note, Integer corrects) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, corrects) VALUES (?, ?, ?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setLong(2, personId);
            ps.setBigDecimal(3, amount);
            ps.setDate(4, Date.valueOf(date));
            ps.setString(5, note);
            ps.setObject(6, corrects, java.sql.Types.INTEGER);
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

    /** Take the sheet's next claim number, in one statement so two users never get the same one. */
    public int nextNumber(String sheetId) {
        return jdbc.queryForObject("SELECT last_claim_number FROM FINAL TABLE "
                + "(UPDATE sheet SET last_claim_number = last_claim_number + 1 WHERE id = ?)", Integer.class, sheetId);
    }
}

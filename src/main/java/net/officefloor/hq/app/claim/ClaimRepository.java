package net.officefloor.hq.app.claim;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The claims store, kept per sheet. */
@Repository
public class ClaimRepository {

    private static final String COLUMNS = "id, sheet_id, person_id, amount, claim_date, note, status, claim_number, settled_due, opened_by, stated, stated_due, corrects, due_by,"
            + " (SELECT c.claim_date FROM claim c WHERE c.sheet_id = claim.sheet_id AND c.person_id = claim.person_id"
            + " AND c.claim_number = claim.corrects) AS corrected_date";

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
            jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ?, status = ?, claim_number = ?, settled_due = ?, opened_by = ?, stated = ?, stated_due = ?, corrects = ? WHERE id = ? AND sheet_id = ?",
                    claim.getAmount(), Date.valueOf(claim.getDate()), claim.getNote(),
                    claim.getStatus().name(), claim.getNumber(), claim.getSettledDue(), claim.getOpenedBy(),
                    claim.isStated(), claim.getStatedDue(), claim.getCorrects(), claim.getId(),
                    claim.getSheetId());
            return;
        }
        long listed = nextListed(claim.getSheetId());
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, status, corrects, listed, due_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
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
            return ps;
        }, keys);
        claim.setId(keys.getKey().longValue());
    }
}

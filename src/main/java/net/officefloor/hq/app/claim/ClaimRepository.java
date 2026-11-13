package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ClaimRepository {

    private static final String COLUMNS = "id, sheet_id, person_id, amount, claim_date, note, status, claim_number, due, opened_by, stated, stated_due, corrects, due_by, deferred_until, proofs, split_from";

    private final JdbcTemplate jdbc;

    public ClaimRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's claims, newest first. */
    public List<Claim> findAll(String sheetId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? ORDER BY listed DESC, id DESC",
                ClaimRepository::map, sheetId);
    }

    /** The claim with this id on this sheet, or null. */
    public Claim find(String sheetId, long id) {
        List<Claim> found = jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? AND id = ?",
                ClaimRepository::map, sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Insert a new claim (null id) or update an existing one; the claim as stored. */
    public Claim save(Claim claim) {
        if (claim.id() != null) {
            jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ?, status = ?, claim_number = ?, due = ?, opened_by = ?, corrects = ? WHERE id = ? AND sheet_id = ?",
                    claim.amount(), Date.valueOf(claim.date()), claim.note(), claim.status().name(), claim.number(),
                    claim.due(), claim.openedBy(), claim.corrects(), claim.id(), claim.sheetId());
            return claim;
        }
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, status, corrects, due_by, claim_number, opened_by, split_from) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, claim.sheetId());
            ps.setLong(2, claim.personId());
            ps.setBigDecimal(3, claim.amount());
            ps.setDate(4, Date.valueOf(claim.date()));
            ps.setString(5, claim.note());
            ps.setString(6, claim.status().name());
            ps.setString(7, claim.corrects());
            ps.setDate(8, Date.valueOf(claim.dueBy()));
            ps.setString(9, claim.number());
            ps.setString(10, claim.openedBy());
            ps.setString(11, claim.splitFrom());
            return ps;
        }, keys);
        return new Claim(keys.getKey().longValue(), claim.sheetId(), claim.personId(), claim.amount(), claim.date(),
                claim.note(), claim.status(), claim.number(), claim.due(), claim.openedBy(), false, null,
                claim.corrects(), claim.dueBy(), null, 0, claim.splitFrom());
    }

    /** The person's claims, newest first. */
    public List<Claim> findForPerson(String sheetId, long personId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? AND person_id = ? ORDER BY listed DESC, id DESC",
                ClaimRepository::map, sheetId, personId);
    }

    /**
     * Move the claim to another person: it loses any figure a statement bound it to (live again) and is listed as
     * their newest claim; the claim as stored.
     */
    public Claim reassign(Claim claim, long personId) {
        jdbc.update("UPDATE claim SET person_id = ?, stated = FALSE, stated_due = NULL,"
                + " listed = NEXT VALUE FOR claim_listed_seq WHERE id = ? AND sheet_id = ?",
                personId, claim.id(), claim.sheetId());
        return find(claim.sheetId(), claim.id());
    }

    /** Bind the claim to a statement's figure (null: n/a), or unbind it (live again) when not stated. */
    public void bindStatement(Claim claim, boolean stated, BigDecimal statedDue) {
        jdbc.update("UPDATE claim SET stated = ?, stated_due = ? WHERE id = ? AND sheet_id = ?",
                stated, stated ? statedDue : null, claim.id(), claim.sheetId());
    }

    /** Defer the claim until its {@code deferredUntil}. */
    public void defer(Claim claim) {
        jdbc.update("UPDATE claim SET deferred_until = ? WHERE id = ? AND sheet_id = ?",
                Date.valueOf(claim.deferredUntil()), claim.id(), claim.sheetId());
    }

    /** Store the claim's {@code proofs} count. */
    public void saveProofs(Claim claim) {
        jdbc.update("UPDATE claim SET proofs = ? WHERE id = ? AND sheet_id = ?", claim.proofs(), claim.id(),
                claim.sheetId());
    }

    private static Claim map(ResultSet rs, int n) throws SQLException {
        return new Claim(rs.getLong("id"), rs.getString("sheet_id"), rs.getLong("person_id"),
                rs.getBigDecimal("amount").setScale(2), rs.getDate("claim_date").toLocalDate(), rs.getString("note"),
                ClaimStatus.valueOf(rs.getString("status")), rs.getString("claim_number"),
                rs.getBigDecimal("due"), rs.getString("opened_by"), rs.getBoolean("stated"), rs.getBigDecimal("stated_due"),
                rs.getString("corrects"), rs.getDate("due_by").toLocalDate(),
                rs.getDate("deferred_until") == null ? null : rs.getDate("deferred_until").toLocalDate(),
                rs.getInt("proofs"), rs.getString("split_from"));
    }
}

package net.officefloor.hq.app.claim;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The claims of each sheet, newest first: by when each was added, or last reassigned to another person. */
@Repository
public class Claims {

    private static final String COLUMNS = "SELECT id, sheet_id, person_id, amount, claim_date, note, status, number, due, opened_by, stated, stated_due, corrects, due_by, deferred_until, proofs, split_from, locked_by, late_entry, reminded_on, "
            + "(SELECT today FROM sheet WHERE sheet.id = claim.sheet_id) AS sheet_today FROM claim";

    private final JdbcTemplate jdbc;

    public Claims(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Claim> onSheet(String sheetId) {
        return jdbc.query(COLUMNS + " WHERE sheet_id = ? ORDER BY listed DESC", Claims::claim, sheetId);
    }

    /** The claim with this id on this sheet, or null when there is none. Locks the claim's row for
     *  the rest of the transaction, so two changes to one claim at once happen one after the other. */
    public Claim find(String sheetId, long id) {
        return jdbc.query(COLUMNS + " WHERE sheet_id = ? AND id = ? FOR UPDATE", Claims::claim, sheetId, id)
                .stream().findFirst().orElse(null);
    }

    /** The person's claims, newest first, each locked for the rest of the transaction. */
    public List<Claim> ofPerson(String sheetId, long personId) {
        return jdbc.query(COLUMNS + " WHERE sheet_id = ? AND person_id = ? ORDER BY listed DESC FOR UPDATE",
                Claims::claim, sheetId, personId);
    }

    /** Unbind every claim of the person a statement binds: each shows its live figure again. */
    public void unbind(String sheetId, long personId) {
        jdbc.update("UPDATE claim SET stated = FALSE, stated_due = NULL WHERE sheet_id = ? AND person_id = ? AND stated",
                sheetId, personId);
    }

    /** Whether the person has a SETTLED claim with this number: one a correction may correct. */
    public boolean settled(String sheetId, long personId, String number) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM claim WHERE sheet_id = ? AND person_id = ? AND number = ? AND status = 'SETTLED'",
                Integer.class, sheetId, personId, number);
        return n != null && n > 0;
    }

    /** The date of the person's claim with this number, or null when there is none. */
    public LocalDate dateOf(String sheetId, long personId, String number) {
        return jdbc.query("SELECT claim_date FROM claim WHERE sheet_id = ? AND person_id = ? AND number = ?",
                (rs, i) -> rs.getDate("claim_date").toLocalDate(), sheetId, personId, number)
                .stream().findFirst().orElse(null);
    }

    /** Move the claim to its (new) person: unbound from any statement, and listed as that person's newest claim. */
    public void reassign(Claim claim) {
        jdbc.update("UPDATE claim SET person_id = ?, stated = FALSE, stated_due = NULL, listed = NEXT VALUE FOR claim_listed WHERE id = ?",
                claim.getPersonId(), claim.getId());
    }

    /** Release every editor's lock on the sheet's claims: the sheet's today has changed. */
    public void unlockAll(String sheetId) {
        jdbc.update("UPDATE claim SET locked_by = NULL WHERE sheet_id = ? AND locked_by IS NOT NULL", sheetId);
    }

    /** Release the user's locks on the person's other claims: the person's one claim form edits one claim at a time. */
    public void unlockOthers(String sheetId, long personId, String user, long keptId) {
        jdbc.update("UPDATE claim SET locked_by = NULL WHERE sheet_id = ? AND person_id = ? AND locked_by = ? AND id <> ?",
                sheetId, personId, user, keptId);
    }

    /** Record the day the claim was last reminded. */
    public void remind(Claim claim) {
        jdbc.update("UPDATE claim SET reminded_on = ? WHERE id = ?", Date.valueOf(claim.getRemindedOn()), claim.getId());
    }

    /** Insert a new claim (assigning its id) or update an existing one. */
    public void save(Claim claim) {
        if (claim.getId() != null) {
            jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ?, status = ?, number = ?, due = ?, opened_by = ?, stated = ?, stated_due = ?, corrects = ?, deferred_until = ?, proofs = ?, locked_by = ? WHERE id = ?",
                    claim.getAmount(), Date.valueOf(claim.getDate()), claim.getNote(), claim.getStatus().name(),
                    claim.getNumber(), claim.getDue(), claim.getOpenedBy(), claim.isStated(), claim.getStatedDue(), claim.getCorrects(),
                    claim.getDeferredUntil() != null ? Date.valueOf(claim.getDeferredUntil()) : null, claim.getProofs(), claim.getLockedBy(), claim.getId());
            return;
        }
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, status, corrects, due_by, number, opened_by, split_from, late_entry) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, claim.getSheetId());
            ps.setLong(2, claim.getPersonId());
            ps.setBigDecimal(3, claim.getAmount());
            ps.setDate(4, Date.valueOf(claim.getDate()));
            ps.setString(5, claim.getNote());
            ps.setString(6, claim.getStatus().name());
            ps.setString(7, claim.getCorrects());
            ps.setDate(8, Date.valueOf(claim.getDueBy()));
            ps.setString(9, claim.getNumber());
            ps.setString(10, claim.getOpenedBy());
            ps.setString(11, claim.getSplitFrom());
            ps.setBoolean(12, claim.isLateEntry());
            return ps;
        }, keys);
        claim.setId(keys.getKey().longValue());
    }

    private static Claim claim(ResultSet rs, int i) throws SQLException {
        Claim claim = new Claim(rs.getLong("id"), rs.getString("sheet_id"), rs.getLong("person_id"),
                rs.getBigDecimal("amount"), rs.getDate("claim_date").toLocalDate(), rs.getString("note"),
                ClaimStatus.valueOf(rs.getString("status")), rs.getString("number"),
                rs.getBigDecimal("due"), rs.getString("opened_by"));
        claim.setStated(rs.getBoolean("stated"));
        claim.setStatedDue(rs.getBigDecimal("stated_due"));
        claim.setCorrects(rs.getString("corrects"));
        claim.setDueBy(rs.getDate("due_by").toLocalDate());
        claim.setProofs(rs.getInt("proofs"));
        claim.setSplitFrom(rs.getString("split_from"));
        claim.setLockedBy(rs.getString("locked_by"));
        claim.setLateEntry(rs.getBoolean("late_entry"));
        Date reminded = rs.getDate("reminded_on");
        claim.setRemindedOn(reminded != null ? reminded.toLocalDate() : null);
        Date until = rs.getDate("deferred_until");
        Date today = rs.getDate("sheet_today");
        claim.setDeferredUntil(until != null ? until.toLocalDate() : null);
        claim.setDeferred(until != null && today != null && today.toLocalDate().isBefore(until.toLocalDate()));
        return claim;
    }
}

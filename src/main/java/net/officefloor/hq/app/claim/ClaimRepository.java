package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Claims by sheet, newest first. */
@Repository
public class ClaimRepository {

    private static final RowMapper<Claim> ROW = (rs, i) -> new Claim(rs.getLong("id"), rs.getString("sheet_id"),
            rs.getLong("person_id"), rs.getBigDecimal("amount"), rs.getObject("claim_date", LocalDate.class),
            rs.getString("note"), ClaimStatus.valueOf(rs.getString("status")),
            rs.getString("claim_number"), rs.getBigDecimal("settled_due"),
            rs.getString("opened_by"), rs.getBoolean("stated"), rs.getBigDecimal("stated_due"),
            rs.getString("corrects"));

    private final JdbcTemplate jdbc;

    public ClaimRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Claim> list(String sheetId) {
        return jdbc.query("SELECT * FROM claim WHERE sheet_id = ? ORDER BY id DESC", ROW, sheetId);
    }

    /** The claim on this sheet, or null when absent. */
    public Claim find(String sheetId, long id) {
        return jdbc.query("SELECT * FROM claim WHERE sheet_id = ? AND id = ?", ROW, sheetId, id)
                .stream().findFirst().orElse(null);
    }

    public Claim insert(Claim claim) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, corrects) VALUES (?, ?, ?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, claim.sheetId());
            ps.setLong(2, claim.personId());
            ps.setBigDecimal(3, claim.amount());
            ps.setObject(4, claim.date());
            ps.setString(5, claim.note());
            ps.setString(6, claim.corrects());
            return ps;
        }, keys);
        return new Claim(keys.getKey().longValue(), claim.sheetId(), claim.personId(), claim.amount(),
                claim.date(), claim.note(), ClaimStatus.DRAFT, null, null, null, false, null, claim.corrects());
    }

    /** The person's claims, newest first. */
    public List<Claim> listForPerson(String sheetId, long personId) {
        return jdbc.query("SELECT * FROM claim WHERE sheet_id = ? AND person_id = ? ORDER BY id DESC", ROW, sheetId,
                personId);
    }

    /** Whether the person has a SETTLED claim with this number on the sheet. */
    public boolean settledNumbered(String sheetId, long personId, String number) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM claim WHERE sheet_id = ? AND person_id = ? AND claim_number = ? AND status = ?",
                Integer.class, sheetId, personId, number, ClaimStatus.SETTLED.name()) > 0;
    }

    /** Bind the claim to a statement's figure (null: n/a, its due stays live), or unbind it (stated false). */
    public void state(Claim claim, boolean stated, BigDecimal statedDue) {
        jdbc.update("UPDATE claim SET stated = ?, stated_due = ? WHERE sheet_id = ? AND id = ?",
                stated, statedDue, claim.sheetId(), claim.id());
    }

    /** Move the claim to another person, unbinding it from any statement: false when it no longer has the person
     *  and status it was loaded with (another user changed it first), and nothing is written. */
    public boolean reassign(Claim claim, long personId) {
        return jdbc.update(
                "UPDATE claim SET person_id = ?, stated = FALSE, stated_due = NULL WHERE sheet_id = ? AND id = ? AND person_id = ? AND status = ?",
                personId, claim.sheetId(), claim.id(), claim.personId(), claim.status().name()) == 1;
    }

    public void update(Claim claim) {
        jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ?, corrects = ? WHERE sheet_id = ? AND id = ?",
                claim.amount(), claim.date(), claim.note(), claim.corrects(), claim.sheetId(), claim.id());
    }

    /** Move the claim from one status to another, with its settled due and who opened it: false when it no longer has
     *  the status it was loaded with (another user changed it first), and nothing is written. */
    public boolean changeStatus(Claim claim, ClaimStatus to, String number, BigDecimal settledDue,
            String openedBy) {
        return jdbc.update(
                "UPDATE claim SET status = ?, claim_number = ?, settled_due = ?, opened_by = ? WHERE sheet_id = ? AND id = ? AND status = ?",
                to.name(), number, settledDue, openedBy, claim.sheetId(), claim.id(), claim.status().name()) == 1;
    }

    /** Take the next number for a claim dated on the date, as it is opened. While the sheet's period numbering from
     *  is a date and the sheet's today is on or after it, that is the next in the claim's period (<period name>-<n>),
     *  or for a claim in no period the next in its own sequence (X-<n>); otherwise it is the sheet's next number.
     *  Taking a number locks the sheet's row until the transaction ends, so concurrent openings are numbered one
     *  after the other; a rolled-back one gives its number back, so there are no gaps. */
    public String nextNumber(String sheetId, LocalDate date) {
        jdbc.query("SELECT id FROM sheet WHERE id = ? FOR UPDATE", rs -> null, sheetId);
        Boolean byPeriod = jdbc.query(
                "SELECT period_numbering_from IS NOT NULL AND today >= period_numbering_from FROM sheet WHERE id = ?",
                rs -> rs.next() ? rs.getBoolean(1) : false, sheetId);
        if (!Boolean.TRUE.equals(byPeriod)) {
            jdbc.update("UPDATE sheet SET last_claim_number = last_claim_number + 1 WHERE id = ?", sheetId);
            return String.valueOf(jdbc.queryForObject("SELECT last_claim_number FROM sheet WHERE id = ?",
                    Integer.class, sheetId));
        }
        String[] period = jdbc.query(
                "SELECT name, name_key FROM period WHERE sheet_id = ? AND from_date <= ? AND to_date >= ? ORDER BY from_date",
                rs -> rs.next() ? new String[] { rs.getString(1), rs.getString(2) } : null, sheetId, date, date);
        String prefix = period != null ? period[0] : "X";
        String key = period != null ? "p:" + period[1] : "x";
        if (jdbc.update("UPDATE period_number SET last_number = last_number + 1 WHERE sheet_id = ? AND seq_key = ?",
                sheetId, key) == 0) {
            jdbc.update("INSERT INTO period_number (sheet_id, seq_key, last_number) VALUES (?, ?, 1)", sheetId, key);
        }
        int n = jdbc.queryForObject("SELECT last_number FROM period_number WHERE sheet_id = ? AND seq_key = ?",
                Integer.class, sheetId, key);
        return prefix + "-" + n;
    }
}

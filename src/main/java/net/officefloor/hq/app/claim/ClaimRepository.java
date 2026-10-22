package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.holiday.BusinessDays;
import net.officefloor.hq.app.holiday.HolidayRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Claims by sheet, newest first. */
@Repository
public class ClaimRepository {

    /** A claim row, its due-by worked out from its terms and the sheet's holidays (their dates). */
    private static RowMapper<Claim> row(Set<LocalDate> holidays) {
        return (rs, i) -> new Claim(rs.getLong("id"), rs.getString("sheet_id"),
                rs.getLong("person_id"), rs.getBigDecimal("amount"), rs.getObject("claim_date", LocalDate.class),
                rs.getString("note"), ClaimStatus.valueOf(rs.getString("status")),
                rs.getString("claim_number"), rs.getBigDecimal("settled_due"),
                rs.getString("opened_by"), rs.getBoolean("stated"), rs.getBigDecimal("stated_due"),
                rs.getString("corrects"),
                BusinessDays.after(rs.getObject("due_from", LocalDate.class), rs.getInt("terms_days"), holidays),
                rs.getObject("deferred_until", LocalDate.class), rs.getInt("proofs"), rs.getString("split_from"),
                rs.getBoolean("late_entry"), rs.getObject("reminded_on", LocalDate.class), rs.getString("ref"), rs.getString("tags"));
    }

    private final JdbcTemplate jdbc;
    private final HolidayRepository holidays;

    public ClaimRepository(JdbcTemplate jdbc, HolidayRepository holidays) {
        this.jdbc = jdbc;
        this.holidays = holidays;
    }

    private RowMapper<Claim> row(String sheetId) {
        return row(holidays.dates(sheetId));
    }

    public List<Claim> list(String sheetId) {
        return jdbc.query("SELECT * FROM claim WHERE sheet_id = ? ORDER BY id DESC", row(sheetId), sheetId);
    }

    /** The claim on this sheet, or null when absent. */
    public Claim find(String sheetId, long id) {
        return jdbc.query("SELECT * FROM claim WHERE sheet_id = ? AND id = ?", row(sheetId), sheetId, id)
                .stream().findFirst().orElse(null);
    }

    /** Store a new claim fixed with the terms (business days), counted from its date. */
    public Claim insert(int terms, Claim claim) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, corrects, due_from, terms_days, split_from, late_entry, ref, tags) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, claim.sheetId());
            ps.setLong(2, claim.personId());
            ps.setBigDecimal(3, claim.amount());
            ps.setObject(4, claim.date());
            ps.setString(5, claim.note());
            ps.setString(6, claim.corrects());
            ps.setObject(7, claim.date());
            ps.setInt(8, terms);
            ps.setString(9, claim.splitFrom());
            ps.setBoolean(10, claim.lateEntry());
            ps.setString(11, claim.ref());
            ps.setString(12, claim.tags());
            return ps;
        }, keys);
        kept(claim.sheetId(), keys.getKey().longValue());
        used(claim.sheetId(), claim.tags());
        return new Claim(keys.getKey().longValue(), claim.sheetId(), claim.personId(), claim.amount(),
                claim.date(), claim.note(), ClaimStatus.DRAFT, null, null, null, false, null, claim.corrects(),
                claim.dueBy(), null, 0, claim.splitFrom(), claim.lateEntry(), null, claim.ref(), claim.tags());
    }

    /** The person's claims, newest first. */
    public List<Claim> listForPerson(String sheetId, long personId) {
        return jdbc.query("SELECT * FROM claim WHERE sheet_id = ? AND person_id = ? ORDER BY id DESC", row(sheetId), sheetId,
                personId);
    }

    /** Whether a claim on the sheet other than the one excluded (0: none) has the reference, compared without regard to case. */
    public boolean refUsed(String sheetId, String ref, long exceptId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM claim WHERE sheet_id = ? AND ref_key = LOWER(?) AND id <> ?",
                Integer.class, sheetId, ref, exceptId) > 0;
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
        kept(claim.sheetId(), claim.id());
    }

    /** Move the claim to another person, unbinding it from any statement: false when it no longer has the person
     *  and status it was loaded with (another user changed it first), and nothing is written. */
    public boolean reassign(Claim claim, long personId) {
        return kept(claim, jdbc.update(
                "UPDATE claim SET person_id = ?, stated = FALSE, stated_due = NULL WHERE sheet_id = ? AND id = ? AND person_id = ? AND status = ?",
                personId, claim.sheetId(), claim.id(), claim.personId(), claim.status().name()) == 1);
    }

    /** Defer the claim until the date: false when it is no longer OPEN (another user changed it first), and nothing
     *  is written. */
    public boolean defer(Claim claim, LocalDate until) {
        return kept(claim, jdbc.update("UPDATE claim SET deferred_until = ? WHERE sheet_id = ? AND id = ? AND status = ?",
                until, claim.sheetId(), claim.id(), ClaimStatus.OPEN.name()) == 1);
    }

    /** Mark the claim reminded on the day: false when it is no longer OPEN or PENDING, or was already reminded that
     *  day (another user got there first), and nothing is written. */
    public boolean remind(Claim claim, LocalDate on) {
        return kept(claim, jdbc.update(
                "UPDATE claim SET reminded_on = ? WHERE sheet_id = ? AND id = ? AND status IN (?, ?) AND (reminded_on IS NULL OR reminded_on <> ?)",
                on, claim.sheetId(), claim.id(), ClaimStatus.OPEN.name(), ClaimStatus.PENDING.name(), on) == 1);
    }

    /** Leave the OPEN claim with the rest of its amount after a split, unbinding it from any statement: false when
     *  it is no longer OPEN with the amount it was loaded with (another user changed it first), and nothing is
     *  written. */
    public boolean splitRest(Claim claim, BigDecimal rest) {
        return kept(claim, jdbc.update(
                "UPDATE claim SET amount = ?, stated = FALSE, stated_due = NULL WHERE sheet_id = ? AND id = ? AND status = ? AND amount = ?",
                rest, claim.sheetId(), claim.id(), ClaimStatus.OPEN.name(), claim.amount()) == 1);
    }

    /** Raise the claim's proofs count by one: false when it no longer has the status it was loaded with (another user
     *  changed it first), and nothing is written. */
    public boolean addProof(Claim claim) {
        return kept(claim, jdbc.update("UPDATE claim SET proofs = proofs + 1 WHERE sheet_id = ? AND id = ? AND status = ?",
                claim.sheetId(), claim.id(), claim.status().name()) == 1);
    }

    /** Store the user's edit of the claim, releasing its editor lock and recording them as its last editor. */
    public void update(Claim claim, String user) {
        jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ?, corrects = ?, ref = ?, tags = ?, locked_by = NULL, edited_by = ? WHERE sheet_id = ? AND id = ?",
                claim.amount(), claim.date(), claim.note(), claim.corrects(), claim.ref(), claim.tags(), user, claim.sheetId(), claim.id());
        kept(claim.sheetId(), claim.id());
        used(claim.sheetId(), claim.tags());
    }

    /** Keep each of the tags (joined by commas; null: none) among those ever used on the sheet. */
    private void used(String sheetId, String tags) {
        if (tags == null) {
            return;
        }
        for (String tag : tags.split(",")) {
            jdbc.update("MERGE INTO claim_tag KEY (sheet_id, tag) VALUES (?, ?)", sheetId, tag);
        }
    }

    /** Every tag ever used on the sheet's claims, sorted. */
    public List<String> tagsUsed(String sheetId) {
        return jdbc.queryForList("SELECT tag FROM claim_tag WHERE sheet_id = ? ORDER BY tag", String.class, sheetId);
    }

    /** Move the claim from one status to another, with its settled due and who opened it: false when it no longer has
     *  the status it was loaded with (another user changed it first), and nothing is written. */
    public boolean changeStatus(Claim claim, ClaimStatus to, String number, BigDecimal settledDue,
            String openedBy) {
        return kept(claim, jdbc.update(
                "UPDATE claim SET status = ?, claim_number = ?, settled_due = ?, opened_by = ? WHERE sheet_id = ? AND id = ? AND status = ?",
                to.name(), number, settledDue, openedBy, claim.sheetId(), claim.id(), claim.status().name()) == 1);
    }

    /** Re-date the claim with its number (a new one when its period changed): false when it no longer has the
     *  date and status it was loaded with (another user changed it first), and nothing is written. */
    public boolean redate(Claim claim, LocalDate date, String number) {
        return kept(claim, jdbc.update(
                "UPDATE claim SET claim_date = ?, claim_number = ? WHERE sheet_id = ? AND id = ? AND claim_date = ? AND status = ?",
                date, number, claim.sheetId(), claim.id(), claim.date(), claim.status().name()) == 1);
    }

    /** Whether the sheet numbers claims within their period now: its period numbering from is a date and its today
     *  is on or after it. */
    public boolean numberingByPeriod(String sheetId) {
        return Boolean.TRUE.equals(jdbc.query(
                "SELECT period_numbering_from IS NOT NULL AND today >= period_numbering_from FROM sheet WHERE id = ?",
                rs -> rs.next() ? rs.getBoolean(1) : false, sheetId));
    }

    /** Take the next number for a claim due by the date, as it is opened. While the sheet's period numbering from
     *  is a date and the sheet's today is on or after it, that is the next in the claim's period (the one holding its due-by: <period name>-<n>),
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

    /** Lock the claim's editor for the user: false when another user holds it, and nothing is written. */
    public boolean lock(Claim claim, String user) {
        return jdbc.update("UPDATE claim SET locked_by = ? WHERE sheet_id = ? AND id = ? AND (locked_by IS NULL OR locked_by = ?)",
                user, claim.sheetId(), claim.id(), user) == 1;
    }

    /** Release the user's lock on the claim's editor; another user's lock is left as it is. */
    public void unlock(Claim claim, String user) {
        jdbc.update("UPDATE claim SET locked_by = NULL WHERE sheet_id = ? AND id = ? AND locked_by = ?",
                claim.sheetId(), claim.id(), user);
    }

    /** Who last edited the claim, or null when no one has. */
    public String editedBy(Claim claim) {
        return jdbc.query("SELECT edited_by FROM claim WHERE sheet_id = ? AND id = ?",
                rs -> rs.next() ? rs.getString(1) : null, claim.sheetId(), claim.id());
    }

    /** Who holds the claim's editor locked, or null when no one does. */
    public String lockedBy(Claim claim) {
        return jdbc.query("SELECT locked_by FROM claim WHERE sheet_id = ? AND id = ?",
                rs -> rs.next() ? rs.getString(1) : null, claim.sheetId(), claim.id());
    }

    /** Release every editor lock on the sheet's claims. */
    public void unlockAll(String sheetId) {
        jdbc.update("UPDATE claim SET locked_by = NULL WHERE sheet_id = ? AND locked_by IS NOT NULL", sheetId);
    }

    /** Keep the claim as it stands now in its history, with the sheet's today. */
    private void kept(String sheetId, long id) {
        jdbc.update("INSERT INTO claim_history (sheet_id, claim_id, person_id, amount, claim_date, note, status, "
                + "claim_number, settled_due, opened_by, stated, stated_due, corrects, due_from, terms_days, deferred_until, proofs, "
                + "split_from, late_entry, reminded_on, ref, tags, made_on) SELECT c.sheet_id, c.id, c.person_id, c.amount, c.claim_date, c.note, "
                + "c.status, c.claim_number, c.settled_due, c.opened_by, c.stated, c.stated_due, c.corrects, c.due_from, c.terms_days, "
                + "c.deferred_until, c.proofs, c.split_from, c.late_entry, c.reminded_on, c.ref, c.tags, s.today FROM claim c JOIN sheet s ON s.id = "
                + "c.sheet_id WHERE c.sheet_id = ? AND c.id = ?", sheetId, id);
    }

    /** Keep the claim in its history when the change went through; whether it did. */
    private boolean kept(Claim claim, boolean changed) {
        if (changed) {
            kept(claim.sheetId(), claim.id());
        }
        return changed;
    }

    /** The sheet's claims as they stood at the end of the day: each claim's last kept state made while the sheet's
     *  today was on or before it (one kept before history was, at any day), newest first; a claim added after then is
     *  not among them. */
    public List<Claim> listAsOf(String sheetId, LocalDate end) {
        return jdbc.query("SELECT h.claim_id AS id, h.* FROM claim_history h WHERE h.sheet_id = ? AND h.id = ("
                + "SELECT MAX(l.id) FROM claim_history l WHERE l.sheet_id = h.sheet_id AND l.claim_id = h.claim_id "
                + "AND (l.made_on IS NULL OR l.made_on <= ?)) ORDER BY h.claim_id DESC", row(sheetId), sheetId, end);
    }

    /** The person's claims as they stood at the end of the day: each claim's last kept state made while the sheet's
     *  today was on or before it (one kept before history was, at any day), newest first; a claim added after then,
     *  or that was another person's then, is not among them. */
    public List<Claim> listForPersonAsOf(String sheetId, long personId, LocalDate end) {
        return jdbc.query("SELECT h.claim_id AS id, h.* FROM claim_history h WHERE h.sheet_id = ? AND h.id = ("
                + "SELECT MAX(l.id) FROM claim_history l WHERE l.sheet_id = h.sheet_id AND l.claim_id = h.claim_id "
                + "AND (l.made_on IS NULL OR l.made_on <= ?)) AND h.person_id = ? ORDER BY h.claim_id DESC",
                row(sheetId), sheetId, end, personId);
    }
}

package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.history.History;
import net.officefloor.hq.app.holiday.HolidayStore;
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
        claim.setDeferredUntil(rs.getObject("deferred_until", LocalDate.class));
        claim.setProofs(rs.getInt("proofs"));
        claim.setSplitFrom(rs.getString("split_from"));
        claim.setLateEntry(rs.getBoolean("late_entry"));
        claim.setRemindedOn(rs.getObject("reminded_on", LocalDate.class));
        claim.setRef(rs.getString("ref"));
        claim.setTags(rs.getString("tags"));
        claim.setVoidReason(rs.getString("void_reason"));
        claim.setEditedBy(rs.getString("edited_by"));
        return claim;
    };

    private final JdbcTemplate jdbc;

    public ClaimStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Claim> list(String sheetId) {
        return jdbc.query("SELECT * FROM claim WHERE sheet_id = ? ORDER BY listed DESC", ROW, sheetId);
    }

    /** The sheet's claims as they stood at the end of the date (see {@link History}), newest first. */
    public List<Claim> listAsOf(String sheetId, LocalDate date) {
        return jdbc.query("SELECT * FROM " + History.asOf("claim", "id", date) + " WHERE sheet_id = ? ORDER BY listed DESC",
                ROW, sheetId);
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

    /** Every tag ever carried by a claim on the sheet (by its history, see {@link History}), as stored (see
     *  {@link Tags}); up to the end of {@code date} when given, else up to now. */
    public List<String> tagsEverUsed(String sheetId, LocalDate date) {
        return date == null
                ? jdbc.queryForList("SELECT DISTINCT tags FROM claim_hist WHERE sheet_id = ? AND tags IS NOT NULL "
                        + "UNION SELECT DISTINCT tags FROM claim WHERE sheet_id = ? AND tags IS NOT NULL",
                        String.class, sheetId, sheetId)
                : jdbc.queryForList("SELECT DISTINCT tags FROM claim_hist WHERE sheet_id = ? AND tags IS NOT NULL "
                        + "AND hist_made_on <= ?", String.class, sheetId, Date.valueOf(date));
    }

    /** Whether a claim on the sheet other than {@code exceptId} carries the reference, compared without regard to case. */
    public boolean refTaken(String sheetId, String ref, long exceptId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM claim WHERE sheet_id = ? AND LOWER(ref) = LOWER(?) AND id <> ?",
                Integer.class, sheetId, ref, exceptId);
        return n != null && n > 0;
    }

    /** Adds a draft claim due by {@code dueBy}, fixed for {@code terms} business days after its date (see
     *  {@link #refixDueBy}), with the external reference and tags (null for none). */
    public Claim add(String sheetId, long personId, BigDecimal amount, LocalDate date, String note, String corrects,
            LocalDate dueBy, int terms, String ref, String tags) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, corrects, due_by, ref, tags, due_terms) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[] { "ID" });
            ps.setString(1, sheetId);
            ps.setLong(2, personId);
            ps.setBigDecimal(3, amount);
            ps.setDate(4, Date.valueOf(date));
            ps.setString(5, note);
            ps.setString(6, corrects);
            ps.setDate(7, Date.valueOf(dueBy));
            ps.setString(8, ref);
            ps.setString(9, tags);
            ps.setInt(10, terms);
            return ps;
        }, keys);
        Claim added = new Claim(keys.getKey().longValue(), sheetId, personId, amount, date, note, ClaimStatus.DRAFT, null, null, null,
                false, null, corrects, dueBy);
        added.setRef(ref);
        added.setTags(tags);
        return added;
    }

    /** Marks the added claim a late entry: added while the period its due-by falls in was soft-closed. */
    public void markLateEntry(Claim claim) {
        jdbc.update("UPDATE claim SET late_entry = TRUE WHERE sheet_id = ? AND id = ?", claim.getSheetId(), claim.getId());
        claim.setLateEntry(true);
    }

    public void save(Claim claim) {
        jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ?, status = ?, number = ?, settled_due = ?, opened_by = ?, void_reason = ? "
                + "WHERE sheet_id = ? AND id = ?",
                claim.getAmount(), Date.valueOf(claim.getDate()), claim.getNote(), claim.getStatus().name(),
                claim.getNumber(), claim.getSettledDue(), claim.getOpenedBy(), claim.getVoidReason(), claim.getSheetId(), claim.getId());
    }

    /** Stores a draft's amount, date, note, the claim it corrects, its reference, its tags and who edited it, releasing its lock; false when the stored claim is no longer a draft. */
    public boolean edit(Claim claim) {
        return jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ?, corrects = ?, ref = ?, tags = ?, edited_by = ?, locked_by = NULL WHERE sheet_id = ? AND id = ? AND status = ?",
                claim.getAmount(), Date.valueOf(claim.getDate()), claim.getNote(), claim.getCorrects(), claim.getRef(), claim.getTags(), claim.getEditedBy(), claim.getSheetId(), claim.getId(),
                ClaimStatus.DRAFT.name()) == 1;
    }

    /** Locks the claim for the user opening its editor (again, when they hold it already); false while
     *  another user holds its lock. */
    public boolean lock(Claim claim, String user) {
        return jdbc.update("UPDATE claim SET locked_by = ? WHERE sheet_id = ? AND id = ? AND (locked_by IS NULL OR locked_by = ?)",
                user, claim.getSheetId(), claim.getId(), user) == 1;
    }

    /** Releases the user's lock on the claim; another user's lock stays. */
    public void unlock(Claim claim, String user) {
        jdbc.update("UPDATE claim SET locked_by = NULL WHERE sheet_id = ? AND id = ? AND locked_by = ?",
                claim.getSheetId(), claim.getId(), user);
    }

    /** Whether a user other than this one holds the claim's lock. */
    public boolean lockedByOther(Claim claim, String user) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM claim WHERE sheet_id = ? AND id = ? AND locked_by IS NOT NULL AND locked_by <> ?",
                Integer.class, claim.getSheetId(), claim.getId(), user);
        return n != null && n > 0;
    }

    /** Releases every lock on the sheet's claims, as its today changes. */
    public void unlockAll(String sheetId) {
        jdbc.update("UPDATE claim SET locked_by = NULL WHERE sheet_id = ? AND locked_by IS NOT NULL", sheetId);
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

    /** Defers the stored OPEN claim until the date; false when it is no longer OPEN (another user moved it on). */
    public boolean defer(Claim claim, LocalDate until) {
        boolean deferred = jdbc.update("UPDATE claim SET deferred_until = ? WHERE sheet_id = ? AND id = ? AND status = ?",
                Date.valueOf(until), claim.getSheetId(), claim.getId(), ClaimStatus.OPEN.name()) == 1;
        if (deferred) {
            claim.setDeferredUntil(until);
        }
        return deferred;
    }

    /** Reminds the stored OPEN or PENDING claim on {@code today}; false when it is no longer one of those or was
     *  already reminded that day (another user got there first). */
    public boolean remind(Claim claim, LocalDate today) {
        boolean reminded = jdbc.update("UPDATE claim SET reminded_on = ? WHERE sheet_id = ? AND id = ? AND status IN (?, ?)"
                + " AND (reminded_on IS NULL OR reminded_on <> ?)", Date.valueOf(today), claim.getSheetId(), claim.getId(),
                ClaimStatus.OPEN.name(), ClaimStatus.PENDING.name(), Date.valueOf(today)) == 1;
        if (reminded) {
            claim.setRemindedOn(today);
        }
        return reminded;
    }

    /** Re-dates the stored OPEN or PENDING claim, under {@code number}; false when it is no longer one of
     *  those at the date it had (another user changed it first). Its due-by and any statement figure stay. */
    public boolean redate(Claim claim, LocalDate date, String number) {
        boolean redated = jdbc.update("UPDATE claim SET claim_date = ?, number = ? "
                + "WHERE sheet_id = ? AND id = ? AND status IN (?, ?) AND claim_date = ?",
                Date.valueOf(date), number, claim.getSheetId(), claim.getId(), ClaimStatus.OPEN.name(),
                ClaimStatus.PENDING.name(), Date.valueOf(claim.getDate())) == 1;
        if (redated) {
            claim.setDate(date);
            claim.setNumber(number);
        }
        return redated;
    }

    /** Leaves the stored OPEN claim with {@code rest} of its amount, unbound from any statement figure; false
     *  when it is no longer OPEN at the amount it had (another user changed it first). */
    public boolean shrink(Claim claim, BigDecimal rest) {
        boolean shrunk = jdbc.update("UPDATE claim SET amount = ?, stated = FALSE, stated_due = NULL, stated_na = FALSE "
                + "WHERE sheet_id = ? AND id = ? AND status = ? AND amount = ?",
                rest, claim.getSheetId(), claim.getId(), ClaimStatus.OPEN.name(), claim.getAmount()) == 1;
        if (shrunk) {
            claim.setAmount(rest);
            claim.setStated(false);
            claim.setStatedDue(null);
            claim.setStatedNa(false);
        }
        return shrunk;
    }

    /** Adds an OPEN claim split off {@code from}: its date, note and tags, the given number, opened by the user
     *  and due by {@code dueBy}, fixed for {@code terms} business days after its date, listed as its person's newest claim. */
    public Claim addSplit(Claim from, BigDecimal amount, String number, String openedBy, LocalDate dueBy, int terms) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, status, number, opened_by, due_by, split_from, tags, due_terms) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[] { "ID" });
            ps.setString(1, from.getSheetId());
            ps.setLong(2, from.getPersonId());
            ps.setBigDecimal(3, amount);
            ps.setDate(4, Date.valueOf(from.getDate()));
            ps.setString(5, from.getNote());
            ps.setString(6, ClaimStatus.OPEN.name());
            ps.setString(7, number);
            ps.setString(8, openedBy);
            ps.setDate(9, Date.valueOf(dueBy));
            ps.setString(10, from.getNumber());
            ps.setString(11, from.getTags());
            ps.setInt(12, terms);
            return ps;
        }, keys);
        Claim added = new Claim(keys.getKey().longValue(), from.getSheetId(), from.getPersonId(), amount, from.getDate(),
                from.getNote(), ClaimStatus.OPEN, number, null, openedBy, false, null, null, dueBy);
        added.setSplitFrom(from.getNumber());
        added.setTags(from.getTags());
        return added;
    }

    /** Works out again the due-by of every claim on the sheet that is not SETTLED or VOID and is due by
     *  {@code from} or later: the business day the terms it was fixed with after its date, skipping weekends
     *  and the given holidays (see {@link HolidayStore#businessDaysAfter}). */
    public void refixDueBy(String sheetId, LocalDate from, Set<LocalDate> holidays) {
        jdbc.query("SELECT id, claim_date, due_by, due_terms FROM claim WHERE sheet_id = ? AND status NOT IN (?, ?) "
                + "AND due_by >= ? AND due_terms IS NOT NULL ORDER BY id", rs -> {
                    LocalDate dueBy = HolidayStore.businessDaysAfter(holidays, rs.getObject("claim_date", LocalDate.class),
                            rs.getInt("due_terms"));
                    if (!dueBy.equals(rs.getObject("due_by", LocalDate.class))) {
                        jdbc.update("UPDATE claim SET due_by = ? WHERE sheet_id = ? AND id = ?", Date.valueOf(dueBy),
                                sheetId, rs.getLong("id"));
                    }
                }, sheetId, ClaimStatus.SETTLED.name(), ClaimStatus.VOID.name(), Date.valueOf(from));
    }

    /** Raises the stored DRAFT, OPEN or PENDING claim's proofs count by one; false when it is no longer one of
     *  those (another user moved it on). */
    public boolean addProof(Claim claim) {
        boolean added = jdbc.update("UPDATE claim SET proofs = proofs + 1 WHERE sheet_id = ? AND id = ? AND status IN (?, ?, ?)",
                claim.getSheetId(), claim.getId(), ClaimStatus.DRAFT.name(), ClaimStatus.OPEN.name(),
                ClaimStatus.PENDING.name()) == 1;
        if (added) {
            claim.setProofs(jdbc.queryForObject("SELECT proofs FROM claim WHERE sheet_id = ? AND id = ?", Integer.class,
                    claim.getSheetId(), claim.getId()));
        }
        return added;
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

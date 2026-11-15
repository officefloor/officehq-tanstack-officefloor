package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ClaimRepository {

    private static final String COLUMNS = "id, sheet_id, person_id, amount, claim_date, note, status, number, settled_due, opened_by, stated, stated_due, corrects, due_by, deferred_until, proofs, split_from, locked_by, late_entry, reminded_on, ref, tags, void_reason, "
            // Deferred now: OPEN and deferred until a date after the sheet's today.
            + "(status = 'OPEN' AND deferred_until IS NOT NULL AND deferred_until > "
            + "(SELECT s.today FROM sheet s WHERE s.id = claim.sheet_id)) AS deferred, "
            // The date it rounds by: a correction's is the date of the claim it corrects.
            + "COALESCE((SELECT k.claim_date FROM claim k WHERE k.sheet_id = claim.sheet_id AND k.number = claim.corrects), "
            + "claim_date) AS round_date";

    private static final RowMapper<Claim> ROW = (rs, n) -> withVoidReason(withTags(withRef(withReminded(withCorrects(new Claim(rs.getLong("id"),
            rs.getString("sheet_id"), rs.getLong("person_id"),
            rs.getBigDecimal("amount").setScale(2).toPlainString(),
            rs.getObject("claim_date", LocalDate.class).toString(), rs.getString("note"),
            rs.getString("status"), rs.getString("number"),
            rs.getBigDecimal("settled_due") == null ? null
                    : rs.getBigDecimal("settled_due").setScale(2).toPlainString(),
            rs.getString("opened_by"), rs.getBoolean("stated"),
            rs.getBigDecimal("stated_due") == null ? null
                    : rs.getBigDecimal("stated_due").setScale(2).toPlainString()), rs.getString("corrects"),
            rs.getObject("round_date", LocalDate.class).toString(),
            rs.getObject("due_by", LocalDate.class).toString(),
            rs.getObject("deferred_until", LocalDate.class), rs.getBoolean("deferred"), rs.getInt("proofs"), rs.getString("split_from"), rs.getString("locked_by"), rs.getBoolean("late_entry")),
            rs.getObject("reminded_on", LocalDate.class)), rs.getString("ref")), rs.getString("tags")), rs.getString("void_reason"));

    private static Claim withVoidReason(Claim claim, String voidReason) {
        claim.setVoidReason(voidReason);
        return claim;
    }

    private static Claim withTags(Claim claim, String tags) {
        claim.setTags(tags);
        return claim;
    }

    private static Claim withRef(Claim claim, String ref) {
        claim.setRef(ref);
        return claim;
    }

    private static Claim withReminded(Claim claim, LocalDate reminded) {
        claim.setReminded(reminded == null ? null : reminded.toString());
        return claim;
    }

    private static Claim withCorrects(Claim claim, String corrects, String roundDate, String dueBy,
            LocalDate deferredUntil, boolean deferred, int proofs, String splitFrom, String lockedBy, boolean lateEntry) {
        claim.setLateEntry(lateEntry);
        claim.setLockedBy(lockedBy);
        claim.setSplitFrom(splitFrom);
        claim.setProofs(proofs);
        claim.setCorrects(corrects);
        claim.setDeferral(deferredUntil == null ? null : deferredUntil.toString(), deferred);
        claim.setDueBy(dueBy);
        claim.setRoundDate(roundDate);
        return claim;
    }

    /** The next place in a sheet's list (newest first); takes the sheet id as its parameter. */
    private static final String NEXT_SEQ = "(SELECT COALESCE(MAX(k.list_seq), 0) + 1 FROM claim k WHERE k.sheet_id = ?)";

    private final JdbcTemplate jdbc;

    public ClaimRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's claims, newest first. */
    public List<Claim> list(String sheetId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? ORDER BY list_seq DESC, id DESC",
                ROW, sheetId);
    }

    public Claim find(String sheetId, long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? AND id = ?",
                ROW, sheetId, id).stream().findFirst().orElse(null);
    }

    /** The claim on the sheet with that number; null when none has it. */
    public Claim findByNumber(String sheetId, String number) {
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? AND number = ?",
                ROW, sheetId, number).stream().findFirst().orElse(null);
    }

    /** Add a claim with its due-by, fixed now. */
    public Claim add(String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
            String corrects, LocalDate dueBy, String user) {
        return add(sheetId, personId, amount, date, note, corrects, dueBy, user, null);
    }

    /** Add a claim with its due-by, fixed now, and its reference (null: none; see {@link #requireRefFree}). */
    public Claim add(String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
            String corrects, LocalDate dueBy, String user, String ref) {
        return add(sheetId, personId, amount, date, note, corrects, dueBy, user, ref, null);
    }

    /** Add a claim with its due-by, fixed now, its reference and its tags (null: none; see {@link ClaimForm#tags}). */
    public Claim add(String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
            String corrects, LocalDate dueBy, String user, String ref, String tags) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, created_by, corrects, due_by, ref, ref_key, tags) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setLong(2, personId);
            ps.setBigDecimal(3, amount);
            ps.setObject(4, date);
            ps.setString(5, note);
            ps.setString(6, user);
            ps.setObject(7, corrects);
            ps.setObject(8, dueBy);
            ps.setString(9, ref);
            ps.setString(10, refKey(ref));
            ps.setString(11, tags);
            return ps;
        }, key);
        long id = key.getKey().longValue();
        jdbc.update("UPDATE claim SET list_seq = " + NEXT_SEQ + " WHERE sheet_id = ? AND id = ?", sheetId, sheetId, id);
        snapshot(sheetId, id);
        useTags(sheetId, tags);
        return find(sheetId, id);
    }

    /** A reference compared without regard to case (null: none). */
    private static String refKey(String ref) {
        return ref == null ? null : ref.toLowerCase(Locale.ROOT);
    }

    /** Refuse a reference (null: none, always free) that a claim on the sheet other than {@code exceptId}
     *  (0: none) already has, compared without regard to case. */
    public void requireRefFree(String sheetId, String ref, long exceptId) throws InvalidClaimException {
        if (ref == null) return;
        Integer used = jdbc.queryForObject("SELECT COUNT(*) FROM claim WHERE sheet_id = ? AND ref_key = ? AND id <> ?",
                Integer.class, sheetId, refKey(ref), exceptId);
        if (used != null && used > 0) {
            throw new InvalidClaimException("The reference is already used on the sheet: " + ref);
        }
    }

    public void update(Claim claim) {
        jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ?, status = ?, number = ?, settled_due = ?, opened_by = ?, corrects = ?, ref = ?, ref_key = ?, tags = ?, void_reason = ? WHERE sheet_id = ? AND id = ?",
                new BigDecimal(claim.getAmount()), LocalDate.parse(claim.getDate()), claim.getNote(),
                claim.getStatus(), claim.getNumber(),
                claim.getSettledDue() == null ? null : new BigDecimal(claim.getSettledDue()),
                claim.getOpenedBy(), claim.getCorrects(), claim.getRef(), refKey(claim.getRef()), claim.getTags(),
                claim.getVoidReason(), claim.getSheetId(), claim.getId());
        snapshot(claim.getSheetId(), claim.getId());
        useTags(claim.getSheetId(), claim.getTags());
    }

    /** Keep each of the tags (comma separated; null: none) among those ever used on the sheet. */
    private void useTags(String sheetId, String tags) {
        if (tags == null) return;
        for (String tag : tags.split(",")) {
            jdbc.update("MERGE INTO sheet_tag (sheet_id, tag) KEY (sheet_id, tag) VALUES (?, ?)", sheetId, tag);
        }
    }

    /** Every tag ever used on the sheet, sorted. */
    public List<String> tags(String sheetId) {
        return jdbc.queryForList("SELECT tag FROM sheet_tag WHERE sheet_id = ? ORDER BY tag", String.class, sheetId);
    }

    /** Save a claim moved to another person: its person, its binding (dropped), and its place in the
     *  list, now the newest. */
    public void reassign(Claim claim) {
        jdbc.update("UPDATE claim SET person_id = ?, stated = ?, stated_due = ?, list_seq = " + NEXT_SEQ
                + " WHERE sheet_id = ? AND id = ?",
                claim.getPersonId(), claim.isStated(),
                claim.getStatedDue() == null ? null : new BigDecimal(claim.getStatedDue()),
                claim.getSheetId(), claim.getSheetId(), claim.getId());
        snapshot(claim.getSheetId(), claim.getId());
    }

    /** Save a re-dated claim: its date (its due-by, period and number stay). */
    public void redate(Claim claim) {
        jdbc.update("UPDATE claim SET claim_date = ?, number = ? WHERE sheet_id = ? AND id = ?",
                LocalDate.parse(claim.getDate()), claim.getNumber(), claim.getSheetId(), claim.getId());
        snapshot(claim.getSheetId(), claim.getId());
    }

    /** The person's claims, newest first. */
    public List<Claim> listOf(String sheetId, long personId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? AND person_id = ? ORDER BY list_seq DESC, id DESC",
                ROW, sheetId, personId);
    }

    /** Keep the claim as it stands now in its history, with the sheet's today. */
    private void snapshot(String sheetId, long id) {
        jdbc.update("INSERT INTO claim_history (sheet_id, claim_id, on_date, person_id, amount, claim_date, status, "
                + "settled_due, stated, stated_due, deferred_until, due_by) SELECT c.sheet_id, c.id, s.today, c.person_id, "
                + "c.amount, c.claim_date, c.status, c.settled_due, c.stated, c.stated_due, c.deferred_until, c.due_by "
                + "FROM claim c JOIN sheet s ON s.id = c.sheet_id WHERE c.sheet_id = ? AND c.id = ?", sheetId, id);
    }

    /** The person's claims as they stood at the end of the date, newest first: each as its last change
     *  made while the sheet's today was on or before the date left it (a claim of theirs then, and only
     *  claims there were then), deferred when OPEN and deferred until after the date. */
    public List<Claim> listOfAsOf(String sheetId, long personId, LocalDate asOf) {
        return jdbc.query("SELECT h.claim_id, h.person_id, h.amount, h.claim_date, h.status, h.settled_due, h.stated, "
                + "h.stated_due, h.deferred_until, h.due_by, c.note, c.ref, c.tags, c.number, c.opened_by, c.corrects, "
                + "COALESCE((SELECT k.claim_date FROM claim k WHERE k.sheet_id = c.sheet_id AND k.number = c.corrects), "
                + "h.claim_date) AS round_date FROM claim_history h JOIN claim c ON c.id = h.claim_id "
                + "WHERE h.sheet_id = ? AND h.person_id = ? AND h.id = (SELECT MAX(h2.id) FROM claim_history h2 "
                + "WHERE h2.claim_id = h.claim_id AND (h2.on_date IS NULL OR h2.on_date <= ?)) "
                + "ORDER BY c.list_seq DESC, c.id DESC", (rs, n) -> {
                    BigDecimal settled = rs.getBigDecimal("settled_due");
                    BigDecimal stated = rs.getBigDecimal("stated_due");
                    LocalDate until = rs.getObject("deferred_until", LocalDate.class);
                    String status = rs.getString("status");
                    Claim claim = new Claim(rs.getLong("claim_id"), sheetId, rs.getLong("person_id"),
                            rs.getBigDecimal("amount").setScale(2).toPlainString(),
                            rs.getObject("claim_date", LocalDate.class).toString(), rs.getString("note"), status,
                            rs.getString("number"), settled == null ? null : settled.setScale(2).toPlainString(),
                            rs.getString("opened_by"), rs.getBoolean("stated"),
                            stated == null ? null : stated.setScale(2).toPlainString());
                    claim.setRef(rs.getString("ref"));
                    claim.setTags(rs.getString("tags"));
                    return withCorrects(claim, rs.getString("corrects"),
                            rs.getObject("round_date", LocalDate.class).toString(),
                            rs.getObject("due_by", LocalDate.class).toString(), until,
                            ClaimStatus.OPEN.name().equals(status) && until != null && until.isAfter(asOf),
                            0, null, null, false);
                }, sheetId, personId, asOf);
    }

    /** Save the date a claim is deferred until. */
    public void updateDeferral(Claim claim) {
        jdbc.update("UPDATE claim SET deferred_until = ? WHERE sheet_id = ? AND id = ?",
                claim.getDeferredUntil() == null ? null : LocalDate.parse(claim.getDeferredUntil()),
                claim.getSheetId(), claim.getId());
        snapshot(claim.getSheetId(), claim.getId());
    }

    /** Save the date a claim was last reminded on. */
    public void updateReminded(Claim claim) {
        jdbc.update("UPDATE claim SET reminded_on = ? WHERE sheet_id = ? AND id = ?",
                claim.getReminded() == null ? null : LocalDate.parse(claim.getReminded()),
                claim.getSheetId(), claim.getId());
    }

    /** Save the number of the claim a claim was split from. */
    public void updateSplitFrom(Claim claim) {
        jdbc.update("UPDATE claim SET split_from = ? WHERE sheet_id = ? AND id = ?",
                claim.getSplitFrom(), claim.getSheetId(), claim.getId());
    }

    /** Save a claim's proofs count. */
    public void updateProofs(Claim claim) {
        jdbc.update("UPDATE claim SET proofs = ? WHERE sheet_id = ? AND id = ?",
                claim.getProofs(), claim.getSheetId(), claim.getId());
    }

    /** Save a claim's binding by its person's last statement. */
    public void updateBinding(Claim claim) {
        jdbc.update("UPDATE claim SET stated = ?, stated_due = ? WHERE sheet_id = ? AND id = ?",
                claim.isStated(), claim.getStatedDue() == null ? null : new BigDecimal(claim.getStatedDue()),
                claim.getSheetId(), claim.getId());
        snapshot(claim.getSheetId(), claim.getId());
    }

    /** The sheet's next claim number, taken in one atomic statement so two claims opened at the same
     *  moment never get the same one; rolled back with the request when it is refused. */
    public String nextNumber(String sheetId) {
        return String.valueOf(jdbc.queryForObject("SELECT last_claim_number FROM FINAL TABLE ("
                + "UPDATE sheet SET last_claim_number = last_claim_number + 1 WHERE id = ?)",
                Long.class, sheetId));
    }

    /** The next number in a period's own sequence on the sheet ({@code <period>-<n>}, from 1), or
     *  for a claim in no period ({@code period} null) in the X sequence ({@code X-<n>}); taken
     *  atomically as {@link #nextNumber} is. */
    public String nextPeriodNumber(String sheetId, String period) {
        String key = period == null ? "" : period;
        try {
            jdbc.update("INSERT INTO sheet_period_number (sheet_id, period_key) SELECT ?, ? WHERE NOT EXISTS "
                    + "(SELECT 1 FROM sheet_period_number WHERE sheet_id = ? AND period_key = ?)",
                    sheetId, key, sheetId, key);
        } catch (DuplicateKeyException raced) {
            // Another request started the sequence at the same moment.
        }
        Long n = jdbc.queryForObject("SELECT last_number FROM FINAL TABLE (UPDATE sheet_period_number "
                + "SET last_number = last_number + 1 WHERE sheet_id = ? AND period_key = ?)",
                Long.class, sheetId, key);
        return (period == null ? "X" : period) + "-" + n;
    }

    /** Mark a claim as a late entry: added while the period its due-by falls in was soft-closed. */
    public void markLateEntry(Claim claim) {
        jdbc.update("UPDATE claim SET late_entry = TRUE WHERE sheet_id = ? AND id = ?", claim.getSheetId(), claim.getId());
        claim.setLateEntry(true);
    }

    /** Lock a claim for the user opening its editor, releasing any other claim of the same person that
     *  user held. */
    public void lock(Claim claim, String user) {
        jdbc.update("UPDATE claim SET locked_by = NULL WHERE sheet_id = ? AND person_id = ? AND locked_by = ? AND id <> ?",
                claim.getSheetId(), claim.getPersonId(), user, claim.getId());
        jdbc.update("UPDATE claim SET locked_by = ? WHERE sheet_id = ? AND id = ?", user, claim.getSheetId(), claim.getId());
        claim.setLockedBy(user);
    }

    /** Release a claim's lock when {@code user} holds it (null: whoever holds it). */
    public void unlock(Claim claim, String user) {
        if (user == null || user.equals(claim.getLockedBy())) {
            jdbc.update("UPDATE claim SET locked_by = NULL WHERE sheet_id = ? AND id = ?", claim.getSheetId(), claim.getId());
            claim.setLockedBy(null);
        }
    }

    /** Release every claim lock on the sheet. */
    public void unlockAll(String sheetId) {
        jdbc.update("UPDATE claim SET locked_by = NULL WHERE sheet_id = ? AND locked_by IS NOT NULL", sheetId);
    }
}

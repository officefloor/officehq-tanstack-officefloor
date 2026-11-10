package net.officefloor.hq.app.claim;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The claims store, kept per sheet. */
@Repository
public class ClaimRepository {

    private static final String COLUMNS = "id, sheet_id, person_id, amount, claim_date, note, status, claim_number, settled_due, opened_by, edited_by, stated, stated_due, corrects, due_by, terms, deferred_until, proofs, split_from, late_entry, reminded_on, ext_ref, tags, void_reason, settle_batch,"
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
        claim.setEditedBy(rs.getString("edited_by"));
        claim.bind(rs.getBoolean("stated"), rs.getBigDecimal("stated_due"));
        claim.setCorrects(rs.getString("corrects"));
        claim.setDueBy(rs.getDate("due_by").toLocalDate());
        claim.setTerms(rs.getInt("terms"));
        java.sql.Date deferred = rs.getDate("deferred_until");
        claim.setDeferredUntil(deferred == null ? null : deferred.toLocalDate());
        claim.setProofs(rs.getInt("proofs"));
        claim.setSplitFrom(rs.getString("split_from"));
        claim.setLateEntry(rs.getBoolean("late_entry"));
        java.sql.Date reminded = rs.getDate("reminded_on");
        claim.setRemindedOn(reminded == null ? null : reminded.toLocalDate());
        claim.setRef(rs.getString("ext_ref"));
        claim.setTags(rs.getString("tags"));
        claim.setVoidReason(rs.getString("void_reason"));
        claim.setSettleBatch((Integer) rs.getObject("settle_batch", Integer.class));
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
        boolean opened = jdbc.update("UPDATE claim SET status = 'OPEN' WHERE id = ? AND sheet_id = ? AND status = 'DRAFT'",
                claim.getId(), claim.getSheetId()) == 1;
        if (opened) keepHistory(claim);
        return opened;
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

    /**
     * The sheet's next bulk settle batch number, counting from 1; taken in one statement so two
     * bulk settles at the same moment never share a number, and never given out again.
     */
    public int nextBatch(String sheetId) {
        return jdbc.queryForObject("SELECT last_settle_batch FROM FINAL TABLE"
                + " (UPDATE sheet SET last_settle_batch = last_settle_batch + 1 WHERE id = ?)",
                Integer.class, sheetId);
    }

    /**
     * The number of the sheet's last bulk settle batch, as it stood at the end of {@code day}
     * (null: now); null when there has been none.
     */
    public Integer lastBatch(String sheetId, LocalDate day) {
        if (day == null) {
            Integer last = jdbc.queryForObject("SELECT last_settle_batch FROM sheet WHERE id = ?", Integer.class, sheetId);
            return last == null || last == 0 ? null : last;
        }
        return jdbc.queryForObject("SELECT MAX(settle_batch) FROM claim_history WHERE sheet_id = ? AND set_on <= ?",
                Integer.class, sheetId, Date.valueOf(day));
    }

    /** The sheet's next place in its claims list, taken in one statement; never given out again. */
    private long nextListed(String sheetId) {
        return jdbc.queryForObject("SELECT last_claim_listed FROM FINAL TABLE"
                + " (UPDATE sheet SET last_claim_listed = last_claim_listed + 1 WHERE id = ?)",
                Long.class, sheetId);
    }

    /** A claim's statement binding: bound (to a figure, null: n/a) or not. */
    public record Binding(boolean stated, BigDecimal statedDue) {
    }

    /**
     * Keeps the claim's binding in its history, with the sheet's today now, when it differs from
     * the one stored.
     */
    private void keepBinding(Claim claim) {
        List<Binding> stored = jdbc.query("SELECT stated, stated_due FROM claim WHERE id = ? AND sheet_id = ?",
                (rs, n) -> new Binding(rs.getBoolean("stated"), rs.getBigDecimal("stated_due")),
                claim.getId(), claim.getSheetId());
        if (!stored.isEmpty() && stored.get(0).stated() == claim.isStated()
                && same(stored.get(0).statedDue(), claim.getStatedDue())) return;
        jdbc.update("INSERT INTO claim_binding (sheet_id, claim_id, stated, stated_due, set_on)"
                + " VALUES (?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?))",
                claim.getSheetId(), claim.getId(), claim.isStated(), claim.getStatedDue(), claim.getSheetId());
    }

    private static boolean same(BigDecimal a, BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }

    /**
     * Each claim's binding at the end of {@code day}, by claim id: the last one given while the
     * sheet's today was on or before it. A claim with none then was unbound.
     */
    public Map<Long, Binding> bindingsOn(String sheetId, LocalDate day) {
        Map<Long, Binding> bindings = new HashMap<>();
        jdbc.query("SELECT claim_id, stated, stated_due FROM claim_binding WHERE sheet_id = ? AND set_on <= ? ORDER BY id",
                (rs) -> {
                    bindings.put(rs.getLong("claim_id"), new Binding(rs.getBoolean("stated"), rs.getBigDecimal("stated_due")));
                }, sheetId, Date.valueOf(day));
        return bindings;
    }

    /**
     * Record the claim's reminder on its reminded date: only while it is still OPEN or PENDING and
     * not already reminded that day, so of two reminders at once only one is written.
     */
    public boolean remind(Claim claim) {
        Date on = Date.valueOf(claim.getRemindedOn());
        return jdbc.update("UPDATE claim SET reminded_on = ? WHERE id = ? AND sheet_id = ?"
                + " AND status IN ('OPEN', 'PENDING') AND (reminded_on IS NULL OR reminded_on <> ?)",
                on, claim.getId(), claim.getSheetId(), on) == 1;
    }

    /** Whether another claim on the sheet than {@code exceptId} (null: any) has this reference, regardless of case. */
    public boolean refTaken(String sheetId, String ref, Long exceptId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM claim WHERE sheet_id = ? AND UPPER(ext_ref) = UPPER(?)"
                + " AND id <> ?", Integer.class, sheetId, ref, exceptId == null ? -1L : exceptId);
        return n != null && n > 0;
    }

    /** Insert a new claim (assigning its id) or update an existing one. */
    public void save(Claim claim) {
        if (claim.getId() != null) {
            if (claim.isRelisted()) {
                jdbc.update("UPDATE claim SET person_id = ?, listed = ? WHERE id = ? AND sheet_id = ?",
                        claim.getPersonId(), nextListed(claim.getSheetId()), claim.getId(), claim.getSheetId());
            }
            keepBinding(claim);
            jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ?, status = ?, claim_number = ?, settled_due = ?, opened_by = ?, edited_by = ?, stated = ?, stated_due = ?, corrects = ?, deferred_until = ?, proofs = ?, ext_ref = ?, tags = ?, void_reason = ?, settle_batch = ? WHERE id = ? AND sheet_id = ?",
                    claim.getAmount(), Date.valueOf(claim.getDate()), claim.getNote(),
                    claim.getStatus().name(), claim.getNumber(), claim.getSettledDue(), claim.getOpenedBy(), claim.getEditedBy(),
                    claim.isStated(), claim.getStatedDue(), claim.getCorrects(),
                    claim.getDeferredUntil() == null ? null : Date.valueOf(claim.getDeferredUntil()), claim.getProofs(), claim.getRef(), claim.getTags(), claim.getVoidReason(), claim.getSettleBatch(), claim.getId(),
                    claim.getSheetId());
            keepHistory(claim);
            return;
        }
        long listed = nextListed(claim.getSheetId());
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, status, corrects, listed, due_by, split_from, claim_number, opened_by, late_entry, ext_ref, tags, terms) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
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
            ps.setString(14, claim.getRef());
            ps.setString(15, claim.getTags());
            ps.setInt(16, claim.getTerms());
            return ps;
        }, keys);
        claim.setId(keys.getKey().longValue());
        keepHistory(claim);
    }

    /**
     * The sheet's today when the claim was opened: its first log line past DRAFT; null when it has none.
     */
    public LocalDate openedOn(Claim claim) {
        List<Date> found = jdbc.queryForList("SELECT on_date FROM claim_log WHERE sheet_id = ? AND claim_id = ?"
                + " AND status <> 'DRAFT' ORDER BY id", Date.class, claim.getSheetId(), claim.getId());
        return found.isEmpty() ? null : found.get(0).toLocalDate();
    }

    /**
     * Move the claim, with its log and comments, to another sheet, listed there as its newest: it takes
     * {@code number} (null: none) and names {@code corrects} and {@code splitFrom}; it loses any figure a
     * statement bound it to, its reference, its settle batch and any lock. Its history here is dropped
     * and kept again on the other sheet, with that sheet's today now.
     */
    public void transfer(Claim claim, String toSheet, String number, String corrects, String splitFrom) {
        jdbc.update("DELETE FROM claim_binding WHERE claim_id = ?", claim.getId());
        jdbc.update("DELETE FROM claim_history WHERE claim_id = ?", claim.getId());
        jdbc.update("UPDATE claim SET sheet_id = ?, listed = ?, claim_number = ?, corrects = ?, split_from = ?,"
                + " stated = FALSE, stated_due = NULL, ext_ref = NULL, settle_batch = NULL, locked_by = NULL,"
                + " locked_on = NULL WHERE id = ? AND sheet_id = ?", toSheet, nextListed(toSheet), number, corrects,
                splitFrom, claim.getId(), claim.getSheetId());
        jdbc.update("UPDATE claim_log SET sheet_id = ? WHERE claim_id = ?", toSheet, claim.getId());
        jdbc.update("UPDATE claim_comment SET sheet_id = ? WHERE claim_id = ?", toSheet, claim.getId());
        jdbc.update("INSERT INTO claim_history (sheet_id, claim_id, " + HISTORY_COLUMNS + ", set_on)"
                + " SELECT sheet_id, id, " + HISTORY_COLUMNS + ", (SELECT s.today FROM sheet s WHERE s.id = claim.sheet_id)"
                + " FROM claim WHERE id = ? AND sheet_id = ?", claim.getId(), toSheet);
    }

    /** Keep a due-by fixed again (its history kept, with the sheet's today now). */
    public void saveDueBy(Claim claim) {
        jdbc.update("UPDATE claim SET due_by = ? WHERE id = ? AND sheet_id = ?", Date.valueOf(claim.getDueBy()),
                claim.getId(), claim.getSheetId());
        keepHistory(claim);
    }

    /**
     * Every tag ever used on the sheet, sorted: those its claims have been saved with, up to the end of
     * {@code day} (null: up to now).
     */
    public List<String> tagsUsed(String sheetId, LocalDate day) {
        java.util.TreeSet<String> tags = new java.util.TreeSet<>();
        jdbc.query("SELECT DISTINCT tags FROM claim_history WHERE sheet_id = ? AND tags IS NOT NULL"
                + (day == null ? "" : " AND set_on <= ?"), (rs) -> {
                    tags.addAll(List.of(rs.getString("tags").split(",")));
                }, day == null ? new Object[] { sheetId } : new Object[] { sheetId, Date.valueOf(day) });
        return List.copyOf(tags);
    }

    private static final String HISTORY_COLUMNS = "person_id, amount, claim_date, note, status, claim_number, settled_due,"
            + " opened_by, stated, stated_due, corrects, due_by, deferred_until, proofs, split_from, late_entry, listed, tags, void_reason, settle_batch, ext_ref";

    /** Keeps the claim as the store now holds it in its history, with the sheet's today now. */
    private void keepHistory(Claim claim) {
        jdbc.update("INSERT INTO claim_history (sheet_id, claim_id, " + HISTORY_COLUMNS + ", set_on)"
                + " SELECT sheet_id, id, " + HISTORY_COLUMNS + ", (SELECT s.today FROM sheet s WHERE s.id = claim.sheet_id)"
                + " FROM claim WHERE id = ? AND sheet_id = ?", claim.getId(), claim.getSheetId());
    }

    /**
     * The sheet's claims as they stood at the end of {@code day}, newest first: each as last kept
     * while the sheet's today was on or before it (a claim added after is absent), locked by no one.
     */
    public List<Claim> findAllAsOf(String sheetId, LocalDate day) {
        Map<Long, Claim> latest = new HashMap<>();
        Map<Long, Long> listed = new HashMap<>();
        jdbc.query("SELECT claim_id, sheet_id, " + HISTORY_COLUMNS + " FROM claim_history WHERE sheet_id = ?"
                + " AND set_on <= ? ORDER BY id", (rs) -> {
                    Claim claim = new Claim(rs.getLong("claim_id"), rs.getString("sheet_id"), rs.getLong("person_id"),
                            rs.getBigDecimal("amount"), rs.getDate("claim_date").toLocalDate(), rs.getString("note"),
                            ClaimStatus.valueOf(rs.getString("status")), rs.getString("claim_number"),
                            rs.getBigDecimal("settled_due"), rs.getString("opened_by"));
                    claim.bind(rs.getBoolean("stated"), rs.getBigDecimal("stated_due"));
                    claim.setCorrects(rs.getString("corrects"));
                    claim.setDueBy(rs.getDate("due_by").toLocalDate());
                    Date deferred = rs.getDate("deferred_until");
                    claim.setDeferredUntil(deferred == null ? null : deferred.toLocalDate());
                    claim.setProofs(rs.getInt("proofs"));
                    claim.setSplitFrom(rs.getString("split_from"));
                    claim.setLateEntry(rs.getBoolean("late_entry"));
                    claim.setTags(rs.getString("tags"));
                    claim.setRef(rs.getString("ext_ref"));
                    claim.setVoidReason(rs.getString("void_reason"));
                    claim.setSettleBatch((Integer) rs.getObject("settle_batch", Integer.class));
                    latest.put(claim.getId(), claim);
                    listed.put(claim.getId(), rs.getLong("listed"));
                }, sheetId, Date.valueOf(day));
        for (Claim claim : latest.values()) {
            if (claim.getCorrects() == null) continue;
            latest.values().stream()
                    .filter((c) -> c.getPersonId() == claim.getPersonId() && claim.getCorrects().equals(c.getNumber()))
                    .findFirst().ifPresent((c) -> claim.setCorrectedDate(c.getDate()));
        }
        return latest.values().stream()
                .sorted(java.util.Comparator.comparing((Claim c) -> listed.get(c.getId())).thenComparing(Claim::getId)
                        .reversed())
                .toList();
    }
}

package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import net.officefloor.hq.app.holiday.HolidayRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** The claims on each sheet; ids are assigned in the order claims are added, and claims are listed newest first (a moved claim as its new person's newest). */
@Repository
public class ClaimRepository {

    // A correction rounds by the date of the claim it corrects (a claim of the same person, by number).
    private static final String COLUMNS = "id, sheet_id, person_id, amount, claim_date, note, status, claim_number, settled_due, opened_by, stated, stated_due, corrects, due_from, terms, due_by, deferred_until, proofs, proofs_photo, split_from, late_entry, reminded_on, waived, ref, tags, void_reason, edited_by, settle_batch, added_seq,"
            + " (SELECT o.claim_date FROM claim o WHERE o.sheet_id = claim.sheet_id AND o.person_id = claim.person_id"
            + " AND o.claim_number = claim.corrects) AS corrected_date";

    private static final RowMapper<Claim> ROW = (rs, n) -> {
        Claim claim = new Claim(rs.getLong("id"), rs.getString("sheet_id"),
                rs.getLong("person_id"), rs.getBigDecimal("amount"), rs.getObject("claim_date", LocalDate.class),
                rs.getString("note"), ClaimStatus.valueOf(rs.getString("status")),
                rs.getString("claim_number"), rs.getBigDecimal("settled_due"),
                rs.getString("opened_by"), rs.getBoolean("stated"), rs.getBigDecimal("stated_due"),
                rs.getString("corrects"));
        claim.setDueFrom(rs.getObject("due_from", LocalDate.class));
        claim.setTerms(rs.getInt("terms"));
        claim.setDueBy(rs.getObject("due_by", LocalDate.class));
        claim.setDeferredUntil(rs.getObject("deferred_until", LocalDate.class));
        claim.setProofs(rs.getInt("proofs"));
        claim.setPhotoProofs(rs.getInt("proofs_photo"));
        claim.setSplitFrom(rs.getString("split_from"));
        claim.setLateEntry(rs.getBoolean("late_entry"));
        claim.setRemindedOn(rs.getObject("reminded_on", LocalDate.class));
        claim.setWaived(rs.getBoolean("waived"));
        claim.setRef(rs.getString("ref"));
        claim.setTags(rs.getString("tags"));
        claim.setVoidReason(rs.getString("void_reason"));
        claim.setEditedBy(rs.getString("edited_by"));
        claim.setSettleBatch(rs.getObject("settle_batch", Integer.class));
        claim.setAddedSeq(rs.getObject("added_seq", Long.class));
        LocalDate corrected = rs.getObject("corrected_date", LocalDate.class);
        if (corrected != null) {
            claim.setRoundOn(corrected);
        }
        return claim;
    };

    private final JdbcTemplate jdbc;

    private final HolidayRepository holidays;

    public ClaimRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.holidays = new HolidayRepository(jdbc);
    }

    /**
     * Work out the due-by of each claim not yet fixed (added before due-bys were stored) from the date it counts from,
     * its terms and its sheet's holidays as they stand now.
     */
    private List<Claim> due(List<Claim> claims) {
        Map<String, Set<LocalDate>> bySheet = new HashMap<>();
        for (Claim claim : claims) {
            if (claim.getDueBy() != null) {
                continue;
            }
            Set<LocalDate> off = bySheet.computeIfAbsent(claim.getSheetId(), holidays::set);
            claim.setDueBy(HolidayRepository.businessDay(claim.getDueFrom(), claim.getTerms(), off));
        }
        return claims;
    }

    /** The person's claims, newest first. */
    public List<Claim> listOf(String sheetId, long personId) {
        return due(jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? AND person_id = ? ORDER BY listed DESC", ROW,
                sheetId, personId));
    }

    /** Bind the claim to a figure (null: n/a), or unbind it when not {@code stated}. */
    public void bind(long id, boolean stated, BigDecimal statedDue) {
        jdbc.update("UPDATE claim SET stated = ?, stated_due = ? WHERE id = ?", stated, statedDue, id);
    }

    /** Unbind every claim of the person from its statement figure, so each is live again. */
    public void unbindAllOf(String sheetId, long personId) {
        jdbc.update("UPDATE claim SET stated = FALSE, stated_due = NULL WHERE sheet_id = ? AND person_id = ?",
                sheetId, personId);
    }

    /** The sheet's claims, newest first. */
    public List<Claim> list(String sheetId) {
        return due(jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? ORDER BY listed DESC", ROW, sheetId));
    }

    /** Whether the person has a SETTLED claim with this number on the sheet. */
    public boolean settledNumbered(String sheetId, long personId, String number) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM claim WHERE sheet_id = ? AND person_id = ? AND claim_number = ? AND status = ?",
                Integer.class, sheetId, personId, number, ClaimStatus.SETTLED.name()) > 0;
    }

    /**
     * What remains of the person's SETTLED claim numbered {@code number}: its amount less the amounts of the corrections
     * made against it, VOID or DELETED ones and the claim {@code exceptId} left out.
     */
    public BigDecimal correctableRemaining(String sheetId, long personId, String number, long exceptId) {
        BigDecimal target = jdbc.queryForObject("SELECT COALESCE(SUM(amount), 0) FROM claim WHERE sheet_id = ? AND person_id = ?"
                + " AND claim_number = ? AND status = ?", BigDecimal.class, sheetId, personId, number, ClaimStatus.SETTLED.name());
        BigDecimal corrected = jdbc.queryForObject("SELECT COALESCE(SUM(amount), 0) FROM claim WHERE sheet_id = ? AND person_id = ?"
                + " AND corrects = ? AND status NOT IN (?, ?) AND id <> ?", BigDecimal.class, sheetId, personId, number,
                ClaimStatus.VOID.name(), ClaimStatus.DELETED.name(), exceptId);
        return target.add(corrected);
    }

    public Claim find(String sheetId, long id) {
        return due(jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? AND id = ?", ROW, sheetId, id))
                .stream().findFirst().orElse(null);
    }

    /** Whether a claim on the sheet other than {@code exceptId} carries the reference, comparing without regard to case. */
    public boolean refUsed(String sheetId, String ref, long exceptId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM claim WHERE sheet_id = ? AND LOWER(ref) = LOWER(?) AND id <> ?",
                Integer.class, sheetId, ref, exceptId) > 0;
    }

    /** Add a draft claim without a reference. */
    public Claim insert(String sheetId, long personId, BigDecimal amount, LocalDate date, String note, String corrects,
            int terms) {
        return insert(sheetId, personId, amount, date, note, corrects, null, null, terms);
    }

    /** Add a draft claim, its due-by counting {@code terms} business days from its date for good, with its reference and tags (null: none). */
    public Claim insert(String sheetId, long personId, BigDecimal amount, LocalDate date, String note, String corrects,
            String ref, String tags, int terms) {
        long listed = nextListed(sheetId);
        long added = jdbc.queryForObject("SELECT NEXT VALUE FOR change_seq", Long.class);
        LocalDate dueBy = holidays.dueBy(sheetId, date, terms);
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, corrects, listed, due_from, terms, ref, tags, due_by, added_seq) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setLong(2, personId);
            ps.setBigDecimal(3, amount);
            ps.setDate(4, Date.valueOf(date));
            ps.setString(5, note);
            ps.setString(6, corrects);
            ps.setLong(7, listed);
            ps.setDate(8, Date.valueOf(date));
            ps.setInt(9, terms);
            ps.setString(10, ref);
            ps.setString(11, tags);
            ps.setDate(12, Date.valueOf(dueBy));
            ps.setLong(13, added);
            return ps;
        }, keys);
        Claim claim = new Claim(keys.getKey().longValue(), sheetId, personId, amount, date, note, ClaimStatus.DRAFT, null, null, null, false, null, corrects);
        claim.setDueFrom(date);
        claim.setTerms(terms);
        claim.setDueBy(dueBy);
        claim.setRef(ref);
        claim.setTags(tags);
        claim.setAddedSeq(added);
        used(sheetId, tags);
        return claim;
    }

    /**
     * Re-fix, for a holiday just added on {@code holiday}, the due-by of every claim on the sheet that is neither SETTLED
     * nor VOID and is due on or after it: each is worked out again from the claim's date with the terms it was fixed
     * with, over the sheet's holidays as they stand now.
     */
    public void refix(String sheetId, LocalDate holiday) {
        Set<LocalDate> off = holidays.set(sheetId);
        for (Claim claim : list(sheetId)) {
            if (claim.getStatus() == ClaimStatus.SETTLED || claim.getStatus() == ClaimStatus.VOID
                    || claim.getDueBy().isBefore(holiday)) {
                continue;
            }
            LocalDate dueBy = HolidayRepository.businessDay(claim.getDate(), claim.getTerms(), off);
            jdbc.update("UPDATE claim SET due_by = ? WHERE id = ?", Date.valueOf(dueBy), claim.getId());
        }
    }

    /** Mark the claim, just added, a late entry: its period was soft-closed when it was added. */
    public void markLate(Claim claim) {
        jdbc.update("UPDATE claim SET late_entry = TRUE WHERE id = ?", claim.getId());
        claim.setLateEntry(true);
    }

    /**
     * Split {@code amount} off the OPEN claim: it keeps the rest, unbound from any statement figure, and a new OPEN claim
     * with the amount, its date, note and tags, opened by {@code openedBy}, due by {@code terms} business days from its date and split from its number is
     * listed as its person's newest. Null when the claim was no longer OPEN at its amount: another user changed it.
     */
    public Claim split(Claim claim, BigDecimal amount, String openedBy, int terms) {
        BigDecimal rest = claim.getAmount().subtract(amount);
        if (jdbc.update("UPDATE claim SET amount = ?, stated = FALSE, stated_due = NULL WHERE id = ? AND status = ? AND amount = ?",
                rest, claim.getId(), ClaimStatus.OPEN.name(), claim.getAmount()) != 1) {
            return null;
        }
        claim.keep(rest);
        Claim into = insert(claim.getSheetId(), claim.getPersonId(), amount, claim.getDate(), claim.getNote(), null, null,
                claim.getTags(), terms);
        into.setStatus(ClaimStatus.OPEN);
        into.setOpenedBy(openedBy);
        into.setSplitFrom(claim.getNumber());
        jdbc.update("UPDATE claim SET status = ?, opened_by = ?, split_from = ? WHERE id = ?",
                ClaimStatus.OPEN.name(), openedBy, claim.getNumber(), into.getId());
        return into;
    }

    public void update(Claim claim) {
        jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ?, corrects = ?, ref = ?, tags = ?, edited_by = ? WHERE id = ?",
                claim.getAmount(), Date.valueOf(claim.getDate()), claim.getNote(), claim.getCorrects(), claim.getRef(), claim.getTags(),
                claim.getEditedBy(), claim.getId());
        used(claim.getSheetId(), claim.getTags());
    }

    /** Note the tags (joined by commas; null: none) as used on the sheet, for good. */
    private void used(String sheetId, String tags) {
        if (tags == null) {
            return;
        }
        for (String tag : tags.split(",")) {
            jdbc.update("MERGE INTO claim_tag (sheet_id, tag) KEY (sheet_id, tag) VALUES (?, ?)", sheetId, tag);
        }
    }

    /** Every tag ever used on the sheet's claims, sorted. */
    public List<String> tagsUsed(String sheetId) {
        return jdbc.queryForList("SELECT tag FROM claim_tag WHERE sheet_id = ? ORDER BY tag", String.class, sheetId);
    }

    /** Defer the claim until its deferred-until date. Whether it was still OPEN: another user may have moved it. */
    public boolean defer(Claim claim) {
        return jdbc.update("UPDATE claim SET deferred_until = ? WHERE id = ? AND status = ?",
                Date.valueOf(claim.getDeferredUntil()), claim.getId(), ClaimStatus.OPEN.name()) == 1;
    }

    /**
     * Remind the claim on {@code today}. Whether it was still in its status and not yet reminded that day: another user
     * may have changed or reminded it.
     */
    public boolean remind(Claim claim, LocalDate today) {
        boolean reminded = jdbc.update("UPDATE claim SET reminded_on = ? WHERE id = ? AND status = ?"
                + " AND (reminded_on IS NULL OR reminded_on <> ?)", Date.valueOf(today), claim.getId(),
                claim.getStatus().name(), Date.valueOf(today)) == 1;
        if (reminded) {
            claim.setRemindedOn(today);
        }
        return reminded;
    }

    /**
     * Waive the claim's late charge. Whether it was still in its status and not yet waived: another user may have
     * changed or waived it.
     */
    public boolean waive(Claim claim) {
        boolean waived = jdbc.update("UPDATE claim SET waived = TRUE WHERE id = ? AND status = ? AND waived = FALSE",
                claim.getId(), claim.getStatus().name()) == 1;
        if (waived) {
            claim.setWaived(true);
        }
        return waived;
    }

    /** Escalate the claim. Whether it was not yet escalated: a claim escalates once ever. */
    public boolean escalate(Claim claim) {
        return jdbc.update("UPDATE claim SET escalated = TRUE WHERE id = ? AND escalated = FALSE", claim.getId()) == 1;
    }

    /**
     * Re-date the claim to {@code date}, its due-by still counting from the date it was added and any statement figure. Whether it was still in its status
     * at its old date: another user may have changed it.
     */
    public boolean redate(Claim claim, LocalDate date) {
        boolean redated = jdbc.update("UPDATE claim SET claim_date = ? WHERE id = ? AND status = ? AND claim_date = ?",
                Date.valueOf(date), claim.getId(), claim.getStatus().name(), Date.valueOf(claim.getDate())) == 1;
        if (redated) {
            claim.setDate(date);
        }
        return redated;
    }

    /** Raise the claim's proofs count by one, a photo's also its photo count. Whether it was still in its status: another user may have moved it. */
    public boolean addProof(Claim claim, String kind) {
        int photo = ProofKind.PHOTO.equals(kind) ? 1 : 0;
        boolean added = jdbc.update("UPDATE claim SET proofs = proofs + 1, proofs_photo = proofs_photo + ? WHERE id = ? AND status = ?",
                photo, claim.getId(), claim.getStatus().name()) == 1;
        if (added) {
            java.util.Map<String, Object> now = jdbc.queryForMap("SELECT proofs, proofs_photo FROM claim WHERE id = ?", claim.getId());
            claim.setProofs(((Number) now.get("proofs")).intValue());
            claim.setPhotoProofs(((Number) now.get("proofs_photo")).intValue());
        }
        return added;
    }

    /**
     * Move the claim from {@code from} to its status, number, settled due and opener. Whether it was still {@code from}:
     * the row is locked by the first of two users changing it, so the other one finds it moved.
     */
    public boolean updateStatus(Claim claim, ClaimStatus from) {
        return jdbc.update("UPDATE claim SET status = ?, claim_number = ?, settled_due = ?, opened_by = ?, void_reason = ? WHERE id = ? AND status = ?",
                claim.getStatus().name(), claim.getNumber(), claim.getSettledDue(), claim.getOpenedBy(), claim.getVoidReason(), claim.getId(), from.name()) == 1;
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

    /** Take the sheet's next bulk settle batch number (1, 2, ...), in one statement so two users never get the same one. */
    public int nextSettleBatch(String sheetId) {
        return jdbc.queryForObject("SELECT last_settle_batch FROM FINAL TABLE "
                + "(UPDATE sheet SET last_settle_batch = last_settle_batch + 1 WHERE id = ?)", Integer.class, sheetId);
    }

    /**
     * Run the work in a transaction of its own (nested, to a savepoint, inside one already open), undoing every change
     * it made to the store when it answers false. Answers what the work answered.
     */
    public boolean keepOnly(BooleanSupplier work) {
        TransactionTemplate tx = new TransactionTemplate(new DataSourceTransactionManager(jdbc.getDataSource()));
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        return Boolean.TRUE.equals(tx.execute(status -> {
            boolean keep = work.getAsBoolean();
            if (!keep) {
                status.setRollbackOnly();
            }
            return keep;
        }));
    }

    /** The number of the sheet's last bulk settle batch; null when there has been none. */
    public Integer lastSettleBatch(String sheetId) {
        return jdbc.query("SELECT last_settle_batch FROM sheet WHERE id = ?",
                rs -> rs.next() && rs.getInt(1) > 0 ? rs.getInt(1) : null, sheetId);
    }

    /** Mark the claim settled by the bulk settle batch. */
    public void markSettleBatch(Claim claim, int batch) {
        jdbc.update("UPDATE claim SET settle_batch = ? WHERE id = ?", batch, claim.getId());
        claim.setSettleBatch(batch);
    }

    /**
     * Take the next claim number of the sheet's tree within the fiscal year (its name, FY<yyyy>), from 1: one after
     * the last given in that year on any of its sheets, kept as this sheet's last. The tree's sheets are locked first
     * so two users never get the same one.
     */
    public int nextFiscalNumber(String sheetId, String fiscalYear) {
        List<String> tree = treeOf(sheetId);
        String in = String.join(",", java.util.Collections.nCopies(tree.size(), "?"));
        jdbc.queryForList("SELECT id FROM sheet WHERE id IN (" + in + ") ORDER BY id FOR UPDATE", String.class, tree.toArray());
        List<Object> args = new java.util.ArrayList<>(tree);
        args.add(fiscalYear);
        Integer last = jdbc.queryForObject("SELECT MAX(last_number) FROM claim_sequence WHERE sheet_id IN (" + in + ")"
                + " AND seq_key = ?", Integer.class, args.toArray());
        int next = (last == null ? 0 : last) + 1;
        if (jdbc.update("UPDATE claim_sequence SET last_number = ? WHERE sheet_id = ? AND seq_key = ?", next, sheetId, fiscalYear) == 0) {
            jdbc.update("INSERT INTO claim_sequence (sheet_id, seq_key, last_number) VALUES (?, ?, ?)", sheetId, fiscalYear, next);
        }
        return next;
    }

    /** The sheet's tree: its top ancestor (following parents up), then all the sheets below that, each once. */
    private List<String> treeOf(String sheetId) {
        java.util.Set<String> up = new java.util.LinkedHashSet<>();
        String root = sheetId;
        while (up.add(root)) {
            String parent = jdbc.query("SELECT setting_value FROM sheet_setting WHERE sheet_id = ? AND setting_key = 'parent'",
                    (rs, n) -> rs.getString(1), root).stream().filter(v -> v != null && !v.isBlank()).findFirst().orElse(null);
            if (parent == null) {
                break;
            }
            root = parent;
        }
        java.util.Set<String> tree = new java.util.LinkedHashSet<>(List.of(root));
        java.util.Deque<String> todo = new java.util.ArrayDeque<>(List.of(root));
        while (!todo.isEmpty()) {
            for (String child : jdbc.query("SELECT sheet_id FROM sheet_setting WHERE setting_key = 'parent' AND setting_value = ?",
                    (rs, n) -> rs.getString(1), todo.poll())) {
                if (tree.add(child)) {
                    todo.add(child);
                }
            }
        }
        tree.add(sheetId); // under a parent cycle, the walk down may not reach it
        return List.copyOf(tree);
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

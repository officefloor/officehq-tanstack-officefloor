package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.sql.DataSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The stored claims, kept per sheet. Ids are assigned in the order claims are added. */
@Repository
public class ClaimRepository {

    private static final String COLUMNS = "id, sheet_id, person_id, amount, claim_date, note, status, claim_number, due, opened_by, stated, stated_due, corrects, due_by, deferred_until, proofs, split_from, ref, tags";

    private final JdbcTemplate jdbc;
    private final DataSource dataSource;

    public ClaimRepository(JdbcTemplate jdbc, DataSource dataSource) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
    }

    /** The sheet's claims, newest first: as added, but a claim moved to another person counts as new when moved. */
    public List<Claim> list(String sheetId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? ORDER BY listed DESC, id DESC",
                ClaimRepository::map, sheetId);
    }

    public Claim find(String sheetId, long id) {
        List<Claim> found = jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? AND id = ?",
                ClaimRepository::map, sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Adds a DRAFT claim; {@code ref} is its external reference (null: none), {@code tags} its tags as stored (null: none). */
    public Claim add(String sheetId, long personId, BigDecimal amount, LocalDate date, String note, String corrects,
            String ref, String tags, LocalDate dueBy) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, corrects, due_by, ref, ref_key, tags) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setLong(2, personId);
            ps.setBigDecimal(3, amount);
            ps.setDate(4, Date.valueOf(date));
            ps.setString(5, note);
            ps.setString(6, corrects);
            ps.setDate(7, Date.valueOf(dueBy));
            ps.setString(8, ref);
            ps.setString(9, ClaimValues.refKey(ref));
            ps.setString(10, tags);
            return ps;
        }, keys);
        jdbc.update("UPDATE claim SET listed = (SELECT COALESCE(MAX(c.listed), 0) + 1 FROM claim c WHERE c.sheet_id = ?) WHERE id = ?",
                sheetId, keys.getKey().longValue());
        return new Claim(keys.getKey().longValue(), sheetId, personId, amount, date, note, ClaimStatus.DRAFT, null, null, null, false, null, corrects, dueBy, null, 0, null, ref, tags);
    }

    /** Whether a claim of the sheet other than {@code self} (null: none) has the reference, comparing without regard to case. */
    public boolean refUsed(String sheetId, String ref, Long self) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM claim WHERE sheet_id = ? AND ref_key = ? AND (CAST(? AS BIGINT) IS NULL OR id <> ?)",
                Long.class, sheetId, ClaimValues.refKey(ref), self, self) > 0;
    }

    /** The person's claim with the given number on the sheet; null when there is none. */
    public Claim findByNumber(String sheetId, long personId, String number) {
        List<Claim> found = jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? AND person_id = ? AND claim_number = ?",
                ClaimRepository::map, sheetId, personId, number);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Stores the claim's values and releases its lock, but only while it is still a DRAFT and no one but {@code by}
     *  holds its lock; false otherwise (nothing is written). The values it had are kept with {@code today}, the sheet's
     *  today, so the claim can be shown as it stood before (see editedAfter). */
    public boolean update(Claim claim, String by, LocalDate today) {
        jdbc.update("INSERT INTO claim_edit (sheet_id, claim_id, amount, claim_date, note, corrects, ref, tags, made_on) "
                + "SELECT sheet_id, id, amount, claim_date, note, corrects, ref, tags, ? FROM claim WHERE sheet_id = ? AND id = ? AND status = ? "
                + "AND (locked_by IS NULL OR locked_by = ?)",
                Date.valueOf(today), claim.sheetId(), claim.id(), ClaimStatus.DRAFT.name(), by);
        return jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ?, corrects = ?, ref = ?, ref_key = ?, tags = ?, locked_by = NULL WHERE sheet_id = ? AND id = ? AND status = ? "
                + "AND (locked_by IS NULL OR locked_by = ?)",
                claim.amount(), Date.valueOf(claim.date()), claim.note(), claim.corrects(), claim.ref(), ClaimValues.refKey(claim.ref()), claim.tags(), claim.sheetId(), claim.id(),
                ClaimStatus.DRAFT.name(), by) == 1;
    }

    /** Locks the claim for {@code by} (its editor is open), but only while it is still a DRAFT and no one else holds
     *  it; false otherwise (nothing is written). */
    public boolean lock(Claim claim, String by) {
        return jdbc.update("UPDATE claim SET locked_by = ? WHERE sheet_id = ? AND id = ? AND status = ? AND (locked_by IS NULL OR locked_by = ?)",
                by, claim.sheetId(), claim.id(), ClaimStatus.DRAFT.name(), by) == 1;
    }

    /** Releases the claim's lock when {@code by} holds it. */
    public void unlock(Claim claim, String by) {
        jdbc.update("UPDATE claim SET locked_by = NULL WHERE sheet_id = ? AND id = ? AND locked_by = ?", claim.sheetId(), claim.id(), by);
    }

    /** Releases every lock on the sheet's claims (its today changed). */
    public void unlockAll(String sheetId) {
        jdbc.update("UPDATE claim SET locked_by = NULL WHERE sheet_id = ? AND locked_by IS NOT NULL", sheetId);
    }

    /** Marks the claim a late entry: added while the period its due-by falls in was soft-closed. */
    public void markLateEntry(Claim claim) {
        jdbc.update("UPDATE claim SET late_entry = TRUE WHERE sheet_id = ? AND id = ?", claim.sheetId(), claim.id());
    }

    /** Whether the claim is a late entry (see markLateEntry). */
    public boolean lateEntry(String sheetId, long id) {
        List<Boolean> found = jdbc.queryForList("SELECT late_entry FROM claim WHERE sheet_id = ? AND id = ?", Boolean.class, sheetId, id);
        return !found.isEmpty() && Boolean.TRUE.equals(found.get(0));
    }

    /** The ids of the sheet's late-entry claims (see markLateEntry). */
    public Set<Long> lateEntries(String sheetId) {
        return new HashSet<>(jdbc.queryForList("SELECT id FROM claim WHERE sheet_id = ? AND late_entry = TRUE", Long.class, sheetId));
    }

    /** Who holds the claim's lock; null when no one does. */
    public String lockedBy(String sheetId, long id) {
        List<String> found = jdbc.queryForList("SELECT locked_by FROM claim WHERE sheet_id = ? AND id = ?", String.class, sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Who holds each locked claim's lock on the sheet, by claim id. */
    public Map<Long, String> locks(String sheetId) {
        Map<Long, String> out = new HashMap<>();
        jdbc.query("SELECT id, locked_by FROM claim WHERE sheet_id = ? AND locked_by IS NOT NULL",
                (rs) -> { out.put(rs.getLong("id"), rs.getString("locked_by")); }, sheetId);
        return out;
    }

    /**
     * Stores the claim's status, number, fixed due and who opened it, but only while its status is still {@code from}; false when another
     * change got there first (nothing is written).
     */
    public boolean updateStatus(Claim claim, ClaimStatus from) {
        return jdbc.update("UPDATE claim SET status = ?, claim_number = ?, due = ?, opened_by = ? WHERE sheet_id = ? AND id = ? AND status = ?",
                claim.status().name(), claim.number(), claim.due(), claim.openedBy(), claim.sheetId(), claim.id(), from.name()) == 1;
    }

    /**
     * Moves the claim to another person, unbound from any statement figure and listed as the sheet's newest claim, but
     * only while it is still the {@code from} person's and DRAFT, OPEN or PENDING; false when another change got there
     * first (nothing is written).
     */
    public boolean reassign(Claim claim, long from, long to) {
        return jdbc.update("UPDATE claim SET person_id = ?, stated = FALSE, stated_due = NULL, "
                + "listed = (SELECT COALESCE(MAX(c.listed), 0) + 1 FROM claim c WHERE c.sheet_id = ?) "
                + "WHERE sheet_id = ? AND id = ? AND person_id = ? AND status IN ('DRAFT', 'OPEN', 'PENDING')",
                to, claim.sheetId(), claim.sheetId(), claim.id(), from) == 1;
    }

    /** Stores the claim's new date and number, but only while it is still OPEN or PENDING and dated {@code was}; false
     *  when another change got there first (nothing is written). */
    public boolean redate(Claim claim, LocalDate was) {
        return jdbc.update("UPDATE claim SET claim_date = ?, claim_number = ? WHERE sheet_id = ? AND id = ? AND claim_date = ? "
                + "AND status IN ('OPEN', 'PENDING')",
                Date.valueOf(claim.date()), claim.number(), claim.sheetId(), claim.id(), Date.valueOf(was)) == 1;
    }

    /** Stores the date the claim is deferred until, but only while it is still OPEN; false when it no longer is
     *  (nothing is written). */
    public boolean defer(Claim claim) {
        return jdbc.update("UPDATE claim SET deferred_until = ? WHERE sheet_id = ? AND id = ? AND status = ?",
                Date.valueOf(claim.deferredUntil()), claim.sheetId(), claim.id(), ClaimStatus.OPEN.name()) == 1;
    }

    /** Stores the claim's remaining amount after a split, unbound from any statement figure, but only while it is
     *  still OPEN with the amount {@code was}; false when another change got there first (nothing is written). */
    public boolean split(Claim claim, BigDecimal was) {
        return jdbc.update("UPDATE claim SET amount = ?, stated = FALSE, stated_due = NULL WHERE sheet_id = ? AND id = ? AND status = ? AND amount = ?",
                claim.amount(), claim.sheetId(), claim.id(), ClaimStatus.OPEN.name(), was) == 1;
    }

    /** Stores the number of the claim this claim was split from. */
    public void splitFrom(Claim claim) {
        jdbc.update("UPDATE claim SET split_from = ? WHERE sheet_id = ? AND id = ?", claim.splitFrom(), claim.sheetId(),
                claim.id());
    }

    /** Raises the claim's proofs count by one, but only while it is still DRAFT, OPEN or PENDING; false when it no
     *  longer is (nothing is written). */
    public boolean addProof(Claim claim) {
        return jdbc.update("UPDATE claim SET proofs = proofs + 1 WHERE sheet_id = ? AND id = ? AND status IN ('DRAFT', 'OPEN', 'PENDING')",
                claim.sheetId(), claim.id()) == 1;
    }

    /** Writes a reminder for the claim on {@code today}, but only while it is still OPEN or PENDING and not yet
     *  reminded that day; false otherwise (nothing is written). */
    public boolean remind(Claim claim, LocalDate today) {
        try {
            return jdbc.update("INSERT INTO claim_reminder (sheet_id, claim_id, reminded_on) SELECT c.sheet_id, c.id, ? FROM claim c "
                    + "WHERE c.sheet_id = ? AND c.id = ? AND c.status IN ('OPEN', 'PENDING') AND NOT EXISTS "
                    + "(SELECT 1 FROM claim_reminder r WHERE r.sheet_id = c.sheet_id AND r.claim_id = c.id AND r.reminded_on = ?)",
                    Date.valueOf(today), claim.sheetId(), claim.id(), Date.valueOf(today)) == 1;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /** The last day a reminder was written for the claim; null when none. */
    public LocalDate remindedOn(String sheetId, long id) {
        return jdbc.queryForObject("SELECT MAX(reminded_on) FROM claim_reminder WHERE sheet_id = ? AND claim_id = ?",
                LocalDate.class, sheetId, id);
    }

    /** The last day a reminder was written for each reminded claim of the sheet, on or before {@code asOf} (null: any
     *  day), by claim id. */
    public Map<Long, LocalDate> reminders(String sheetId, LocalDate asOf) {
        Map<Long, LocalDate> out = new HashMap<>();
        jdbc.query("SELECT claim_id, MAX(reminded_on) AS reminded_on FROM claim_reminder WHERE sheet_id = ? "
                + "AND (CAST(? AS DATE) IS NULL OR reminded_on <= ?) GROUP BY claim_id",
                (rs) -> { out.put(rs.getLong("claim_id"), rs.getObject("reminded_on", LocalDate.class)); }, sheetId,
                asOf == null ? null : Date.valueOf(asOf), asOf == null ? null : Date.valueOf(asOf));
        return out;
    }

    /** For each claim of the sheet edited after {@code date} (while the sheet's today was later), the values it had
     *  before its first such edit, by claim id: the claim as it stood at the end of the date. */
    public Map<Long, Claim> editedAfter(String sheetId, LocalDate date) {
        Map<Long, Claim> out = new HashMap<>();
        jdbc.query("SELECT claim_id, amount, claim_date, note, corrects, ref, tags FROM claim_edit WHERE sheet_id = ? AND made_on > ? ORDER BY id",
                (rs) -> {
                    out.putIfAbsent(rs.getLong("claim_id"), new Claim(rs.getLong("claim_id"), sheetId, 0,
                            rs.getBigDecimal("amount").setScale(2), rs.getObject("claim_date", LocalDate.class),
                            rs.getString("note"), null, null, null, null, false, null, rs.getString("corrects"), null,
                            null, 0, null, rs.getString("ref"), rs.getString("tags")));
                }, sheetId, Date.valueOf(date));
        return out;
    }

    /** Every tag ever used on the sheet, sorted: those its claims carry and those they carried before an edit. */
    public List<String> tagsUsed(String sheetId) {
        Set<String> out = new TreeSet<>();
        for (String tags : jdbc.queryForList("SELECT tags FROM claim WHERE sheet_id = ? AND tags IS NOT NULL "
                + "UNION SELECT tags FROM claim_edit WHERE sheet_id = ? AND tags IS NOT NULL", String.class, sheetId, sheetId)) {
            out.addAll(List.of(tags.split(",")));
        }
        return List.copyOf(out);
    }

    /** Unbinds every claim of the person bound by their last statement (live again). */
    public void unbindAll(String sheetId, long personId) {
        jdbc.update("UPDATE claim SET stated = FALSE, stated_due = NULL WHERE sheet_id = ? AND person_id = ? AND stated",
                sheetId, personId);
    }

    /** Binds the claim to a statement figure (null: it had none), or unbinds it when {@code stated} is false. */
    public void bind(Claim claim, boolean stated, BigDecimal statedDue) {
        jdbc.update("UPDATE claim SET stated = ?, stated_due = ? WHERE sheet_id = ? AND id = ?",
                stated, stated ? statedDue : null, claim.sheetId(), claim.id());
    }

    /**
     * The sheet's next claim number: 1 for the first, then one more each time. The sheet's counter row stays locked
     * until the transaction ends, so two claims opened at once get different numbers, and a rolled back opening gives
     * its number back.
     */
    public long nextNumber(String sheetId) {
        ensureNumberRow(sheetId);
        jdbc.update("UPDATE claim_number_seq SET last_number = last_number + 1 WHERE sheet_id = ?", sheetId);
        return jdbc.queryForObject("SELECT last_number FROM claim_number_seq WHERE sheet_id = ?", Long.class, sheetId);
    }

    /**
     * The next number in the sheet's per-period sequence {@code key} (a period's name, or X for no period): 1 for the
     * first, then one more each time, its counter row locked until the transaction ends as for {@link #nextNumber}.
     */
    public long nextPeriodNumber(String sheetId, String key) {
        ensurePeriodNumberRow(sheetId, key);
        jdbc.update("UPDATE claim_period_number_seq SET last_number = last_number + 1 WHERE sheet_id = ? AND seq_key = ?",
                sheetId, key);
        return jdbc.queryForObject("SELECT last_number FROM claim_period_number_seq WHERE sheet_id = ? AND seq_key = ?",
                Long.class, sheetId, key);
    }

    /** Adds the per-period sequence's counter row if it has none, committed at once (see ensureNumberRow). */
    private synchronized void ensurePeriodNumberRow(String sheetId, String key) {
        try (Connection con = dataSource.getConnection()) {
            con.setAutoCommit(true);
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO claim_period_number_seq (sheet_id, seq_key, last_number) "
                    + "SELECT ?, ?, 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM claim_period_number_seq WHERE sheet_id = ? AND seq_key = ?)")) {
                ps.setString(1, sheetId);
                ps.setString(2, key);
                ps.setString(3, sheetId);
                ps.setString(4, key);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Adds the sheet's counter row if it has none, on its own connection so the row is committed at once and two
     * openings at the same moment never both try to add it.
     */
    private synchronized void ensureNumberRow(String sheetId) {
        try (Connection con = dataSource.getConnection()) {
            con.setAutoCommit(true);
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO claim_number_seq (sheet_id, last_number) "
                    + "SELECT ?, 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM claim_number_seq WHERE sheet_id = ?)")) {
                ps.setString(1, sheetId);
                ps.setString(2, sheetId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Claim map(ResultSet rs, int n) throws SQLException {
        BigDecimal due = rs.getBigDecimal("due");
        BigDecimal statedDue = rs.getBigDecimal("stated_due");
        return new Claim(rs.getLong("id"), rs.getString("sheet_id"), rs.getLong("person_id"),
                rs.getBigDecimal("amount").setScale(2), rs.getObject("claim_date", LocalDate.class),
                rs.getString("note"), ClaimStatus.valueOf(rs.getString("status")),
                rs.getString("claim_number"), due == null ? null : due.setScale(2),
                rs.getString("opened_by"), rs.getBoolean("stated"), statedDue == null ? null : statedDue.setScale(2),
                rs.getString("corrects"), rs.getObject("due_by", LocalDate.class),
                rs.getObject("deferred_until", LocalDate.class), rs.getInt("proofs"), rs.getString("split_from"), rs.getString("ref"), rs.getString("tags"));
    }
}

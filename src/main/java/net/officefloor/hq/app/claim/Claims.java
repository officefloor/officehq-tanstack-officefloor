package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.holiday.Holidays;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The claim store, kept per sheet. */
@Repository
public class Claims {

    private static final RowMapper<Claim> ROW = (rs, i) -> {
        Claim claim = new Claim(rs.getLong("id"), rs.getString("sheet_id"),
                rs.getLong("person_id"), rs.getBigDecimal("amount"), rs.getDate("claim_date").toLocalDate(),
                rs.getString("note"), ClaimStatus.valueOf(rs.getString("status")),
                rs.getString("claim_number"), rs.getBigDecimal("settled_due"), rs.getString("opened_by"));
        claim.setStated(rs.getBoolean("stated"));
        claim.setStatedDue(rs.getBigDecimal("stated_due"));
        claim.setCorrects(rs.getString("corrects"));
        java.sql.Date corrected = rs.getDate("corrected_date");
        claim.setCorrectedDate(corrected == null ? null : corrected.toLocalDate());
        java.sql.Date dueBy = rs.getDate("due_by");
        claim.setDueBy(dueBy == null ? null : dueBy.toLocalDate());
        int terms = rs.getInt("terms");
        claim.setTerms(rs.wasNull() ? null : terms);
        java.sql.Date deferredUntil = rs.getDate("deferred_until");
        claim.setDeferredUntil(deferredUntil == null ? null : deferredUntil.toLocalDate());
        java.sql.Date remindedOn = rs.getDate("reminded_on");
        claim.setRemindedOn(remindedOn == null ? null : remindedOn.toLocalDate());
        claim.setEscalated(rs.getBoolean("escalated"));
        claim.setProofs(rs.getInt("proofs"));
        claim.setSplitFrom(rs.getString("split_from"));
        claim.setLockedBy(rs.getString("locked_by"));
        claim.setEditedBy(rs.getString("edited_by"));
        claim.setLateEntry(rs.getBoolean("late_entry"));
        claim.setRef(rs.getString("ref"));
        claim.setTags(rs.getString("tags"));
        claim.setVoidReason(rs.getString("void_reason"));
        int batch = rs.getInt("settle_batch");
        claim.setSettleBatch(rs.wasNull() ? null : batch);
        claim.setRateMark(rs.getLong("rate_mark"));
        return claim;
    };

    /** A claim with the date of the claim it corrects (same sheet, same person, by number). */
    private static final String SELECT = "SELECT c.*, k.claim_date AS corrected_date FROM claim c LEFT JOIN claim k"
            + " ON k.sheet_id = c.sheet_id AND k.person_id = c.person_id AND k.claim_number = c.corrects";

    /** A new mark for a claim arriving on a sheet (see Rates). */
    private static final String NEXT_RATE_MARK = "NEXT VALUE FOR rate_mark_seq";

    /** The listed_seq after every claim's: a new or moved claim is listed first. */
    private static final String NEXT_LISTED = "(SELECT COALESCE(MAX(listed_seq), 0) + 1 FROM claim)";

    private final JdbcTemplate jdbc;

    private final Holidays holidays;

    public Claims(JdbcTemplate jdbc, Holidays holidays) {
        this.jdbc = jdbc;
        this.holidays = holidays;
    }

    /** Records the claim's figure-moving columns as they now stand in its history, dated the sheet's today. */
    private void history(String sheetId, long id) {
        jdbc.update("INSERT INTO claim_history (sheet_id, claim_id, set_on, person_id, amount, claim_date, status, settled_due,"
                + " stated, stated_due, due_by, deferred_until, corrects, claim_number, opened_by, note, proofs, split_from, late_entry, settle_batch, rate_mark)"
                + " SELECT c.sheet_id, c.id, s.today, c.person_id, c.amount, c.claim_date, c.status, c.settled_due, c.stated,"
                + " c.stated_due, c.due_by, c.deferred_until, c.corrects, c.claim_number, c.opened_by, c.note, c.proofs,"
                + " c.split_from, c.late_entry, c.settle_batch, c.rate_mark"
                + " FROM claim c JOIN sheet s ON s.id = c.sheet_id WHERE c.sheet_id = ? AND c.id = ?", sheetId, id);
    }

    /** The sheet's claims with those transferred away after the date (as they stood when they left), newest first. */
    public List<Claim> listWithTransferredAfter(String sheetId, LocalDate date) {
        List<Claim> found = new ArrayList<>(list(sheetId));
        found.addAll(jdbc.query("SELECT c.*, CAST(NULL AS DATE) AS corrected_date FROM claim_transferred c"
                + " WHERE c.sheet_id = ? AND c.transferred_on > ?", ROW, sheetId, Date.valueOf(date)));
        Map<Long, Long> listed = new HashMap<>();
        jdbc.query("SELECT id, COALESCE(listed_seq, 0) FROM claim WHERE sheet_id = ?"
                + " UNION ALL SELECT id, COALESCE(listed_seq, 0) FROM claim_transferred WHERE sheet_id = ?",
                (RowCallbackHandler) rs -> listed.put(rs.getLong(1), rs.getLong(2)), sheetId, sheetId);
        found.sort(Comparator.comparing((Claim c) -> listed.getOrDefault(c.getId(), 0L))
                .thenComparingLong(Claim::getId).reversed());
        return found;
    }

    /** The sheet's claims, newest first. */
    public List<Claim> list(String sheetId) {
        return withLogs(sheetId, jdbc.query(SELECT + " WHERE c.sheet_id = ? ORDER BY c.listed_seq DESC, c.id DESC", ROW, sheetId));
    }

    /** The claims with their status logs (see {@link ClaimLog}), oldest line first. */
    private List<Claim> withLogs(String sheetId, List<Claim> claims) {
        Map<Long, List<String>> logs = new HashMap<>();
        jdbc.query("SELECT claim_id, status, by_user, log_date, due FROM claim_log WHERE sheet_id = ? ORDER BY id", rs -> {
            logs.computeIfAbsent(rs.getLong("claim_id"), id -> new ArrayList<>())
                    .add(rs.getString("status") + " by " + rs.getString("by_user") + " on "
                            + rs.getDate("log_date").toLocalDate() + " due=" + rs.getString("due"));
        }, sheetId);
        claims.forEach(claim -> claim.setLog(logs.getOrDefault(claim.getId(), List.of())));
        Map<Long, List<ClaimComment>> comments = new HashMap<>();
        jdbc.query("SELECT claim_id, by_user, comment_date, text FROM claim_comment WHERE sheet_id = ? ORDER BY id", rs -> {
            comments.computeIfAbsent(rs.getLong("claim_id"), id -> new ArrayList<>()).add(new ClaimComment(
                    rs.getString("by_user"), rs.getDate("comment_date").toLocalDate(), rs.getString("text")));
        }, sheetId);
        claims.forEach(claim -> claim.setComments(comments.getOrDefault(claim.getId(), List.of())));
        Map<Long, List<ClaimLine>> lines = new HashMap<>();
        jdbc.query("SELECT claim_id, label, qty, unit FROM claim_line WHERE sheet_id = ? ORDER BY id", rs -> {
            lines.computeIfAbsent(rs.getLong("claim_id"), id -> new ArrayList<>()).add(new ClaimLine(
                    rs.getString("label"), rs.getInt("qty"), rs.getBigDecimal("unit")));
        }, sheetId);
        claims.forEach(claim -> claim.setLines(lines.getOrDefault(claim.getId(), List.of())));
        return claims;
    }

    /** A person's claims on the sheet, newest first. */
    public List<Claim> ofPerson(String sheetId, long personId) {
        return jdbc.query(SELECT + " WHERE c.sheet_id = ? AND c.person_id = ? ORDER BY c.listed_seq DESC, c.id DESC", ROW, sheetId, personId);
    }

    public Claim find(String sheetId, long id) {
        List<Claim> found = jdbc.query(SELECT + " WHERE c.sheet_id = ? AND c.id = ?", ROW, sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    public Claim add(String sheetId, long personId, BigDecimal amount, LocalDate date, String note, LocalDate dueBy) {
        return add(sheetId, personId, amount, date, note, null, dueBy);
    }

    public Claim add(String sheetId, long personId, BigDecimal amount, LocalDate date, String note, String corrects,
            LocalDate dueBy) {
        return add(sheetId, personId, amount, date, note, corrects, dueBy, null);
    }

    public Claim add(String sheetId, long personId, BigDecimal amount, LocalDate date, String note, String corrects,
            LocalDate dueBy, String ref) {
        return add(sheetId, personId, amount, date, note, corrects, dueBy, ref, null);
    }

    /** Adds a DRAFT claim with its due-by fixed as given (keeping the terms it was fixed with), its external reference
     *  and its tags (null for none). */
    public Claim add(String sheetId, long personId, BigDecimal amount, LocalDate date, String note, String corrects,
            LocalDate dueBy, String ref, String tags) {
        int terms = holidays.termsTo(sheetId, date, dueBy);
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, corrects, due_by, ref, tags, terms, listed_seq, rate_mark)"
                            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + NEXT_LISTED + ", " + NEXT_RATE_MARK + ")",
                    Statement.RETURN_GENERATED_KEYS);
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
        Claim claim = new Claim(keys.getKey().longValue(), sheetId, personId, amount, date, note, ClaimStatus.DRAFT, null, null, null);
        claim.setRateMark(jdbc.queryForObject("SELECT rate_mark FROM claim WHERE id = ?", Long.class, claim.getId()));
        history(sheetId, claim.getId());
        claim.setCorrects(corrects);
        claim.setDueBy(dueBy);
        claim.setTerms(terms);
        claim.setRef(ref);
        claim.setTags(tags);
        recordTags(sheetId, tags);
        claim.setCorrectedDate(correctedDate(sheetId, personId, corrects));
        return claim;
    }

    /** Remembers each of the tags (comma separated; null for none) as used on the sheet. */
    private void recordTags(String sheetId, String tags) {
        if (tags == null) return;
        for (String tag : tags.split(",")) {
            jdbc.update("MERGE INTO sheet_tag (sheet_id, tag) KEY (sheet_id, tag) VALUES (?, ?)", sheetId, tag);
        }
    }

    /** Every tag ever used on the sheet's claims and people, sorted. */
    public List<String> tagsUsed(String sheetId) {
        return jdbc.queryForList("SELECT tag FROM sheet_tag WHERE sheet_id = ? ORDER BY tag", String.class, sheetId);
    }

    /** Whether a claim on the sheet (other than exceptId, when given) carries the reference, compared without regard to case. */
    public boolean refUsed(String sheetId, String ref, Long exceptId) {
        Integer found = jdbc.queryForObject("SELECT COUNT(*) FROM claim WHERE sheet_id = ? AND LOWER(ref) = LOWER(?) AND id <> ?",
                Integer.class, sheetId, ref, exceptId == null ? -1L : exceptId);
        return found != null && found > 0;
    }

    /** The date of the person's claim with this number; null when there is none (or no number). */
    public LocalDate correctedDate(String sheetId, long personId, String number) {
        if (number == null) return null;
        List<LocalDate> found = jdbc.query("SELECT claim_date FROM claim WHERE sheet_id = ? AND person_id = ? AND claim_number = ?",
                (rs, i) -> rs.getDate(1).toLocalDate(), sheetId, personId, number);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Bind the claim to the figure a statement gives it (stated), or leave it unbound. */
    public void bind(Claim claim) {
        jdbc.update("UPDATE claim SET stated = ?, stated_due = ? WHERE sheet_id = ? AND id = ?",
                claim.isStated(), claim.getStatedDue(), claim.getSheetId(), claim.getId());
        history(claim.getSheetId(), claim.getId());
    }

    /** Whether the person has a SETTLED claim with this number on the sheet: one a correction may correct. */
    public boolean isSettled(String sheetId, long personId, String number) {
        Integer found = jdbc.queryForObject("SELECT COUNT(*) FROM claim WHERE sheet_id = ? AND person_id = ?"
                + " AND claim_number = ? AND status = ?", Integer.class, sheetId, personId, number, ClaimStatus.SETTLED.name());
        return found != null && found > 0;
    }

    /** What remains of the SETTLED claim of that number: its amount less the corrections made against it
     *  (VOID and DELETED ones left out, and the claim of exceptId when given). */
    public BigDecimal remainingToCorrect(String sheetId, long personId, String number, Long exceptId) {
        BigDecimal target = jdbc.queryForObject("SELECT COALESCE(SUM(amount), 0) FROM claim WHERE sheet_id = ? AND person_id = ?"
                + " AND claim_number = ? AND status = ?", BigDecimal.class, sheetId, personId, number, ClaimStatus.SETTLED.name());
        BigDecimal corrected = jdbc.queryForObject("SELECT COALESCE(SUM(amount), 0) FROM claim WHERE sheet_id = ? AND person_id = ?"
                + " AND corrects = ? AND status NOT IN (?, ?) AND id <> ?", BigDecimal.class, sheetId, personId, number,
                ClaimStatus.VOID.name(), ClaimStatus.DELETED.name(), exceptId == null ? -1L : exceptId);
        return target.add(corrected).setScale(2);
    }

    /** Moves the claim to its (new) person, listed as their newest claim, and leaves it unbound. */
    public void reassign(Claim claim) {
        jdbc.update("UPDATE claim SET person_id = ?, stated = ?, stated_due = ?, claim_number = ?, listed_seq = " + NEXT_LISTED
                + " WHERE sheet_id = ? AND id = ?", claim.getPersonId(), claim.isStated(), claim.getStatedDue(), claim.getNumber(),
                claim.getSheetId(), claim.getId());
        history(claim.getSheetId(), claim.getId());
    }

    /** Moves the claim to the person its person was merged into, listed as their newest claim, its number and date
     *  kept, still bound to the figure a statement gave it. */
    public void merge(Claim claim, long intoId) {
        jdbc.update("UPDATE claim SET person_id = ?, listed_seq = " + NEXT_LISTED
                + " WHERE sheet_id = ? AND id = ?", intoId, claim.getSheetId(), claim.getId());
        history(claim.getSheetId(), claim.getId());
    }

    /** Copies the claim to the person on another sheet, listed as their newest claim: its status, dates, note, tags,
     *  settled figure, log, comments and the figure a statement bound it to kept; unnumbered, without its reference, lock or
     *  settle batch. Its tags are remembered as used on the other sheet. Returns the copy's id. */
    public long transferIn(Claim claim, String toSheet, long personId) {
        long listed = jdbc.queryForObject("SELECT COALESCE(MAX(listed_seq), 0) + 1 FROM claim", Long.class);
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, status, settled_due, opened_by, due_by,"
                            + " deferred_until, proofs, late_entry, reminded_on, tags, void_reason, edited_by, terms, stated, stated_due, listed_seq,"
                            + " rate_mark)"
                            + " SELECT ?, ?, amount, claim_date, note, status, settled_due, opened_by, due_by, deferred_until, proofs,"
                            + " late_entry, reminded_on, tags, void_reason, edited_by, terms, stated, stated_due, ?, " + NEXT_RATE_MARK
                            + " FROM claim WHERE sheet_id = ? AND id = ?",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, toSheet);
            ps.setLong(2, personId);
            ps.setLong(3, listed);
            ps.setString(4, claim.getSheetId());
            ps.setLong(5, claim.getId());
            return ps;
        }, keys);
        long id = keys.getKey().longValue();
        jdbc.update("INSERT INTO claim_log (sheet_id, claim_id, status, by_user, log_date, due) SELECT ?, ?, status, by_user, log_date, due"
                + " FROM claim_log WHERE sheet_id = ? AND claim_id = ? ORDER BY id", toSheet, id, claim.getSheetId(), claim.getId());
        jdbc.update("INSERT INTO claim_comment (sheet_id, claim_id, by_user, comment_date, text) SELECT ?, ?, by_user, comment_date, text"
                + " FROM claim_comment WHERE sheet_id = ? AND claim_id = ? ORDER BY id", toSheet, id, claim.getSheetId(), claim.getId());
        jdbc.update("INSERT INTO claim_line (sheet_id, claim_id, label, qty, unit) SELECT ?, ?, label, qty, unit"
                + " FROM claim_line WHERE sheet_id = ? AND claim_id = ? ORDER BY id", toSheet, id, claim.getSheetId(), claim.getId());
        recordTags(toSheet, claim.getTags());
        history(toSheet, id);
        return id;
    }

    /**
     * Copies the claim to the person on a cloned sheet, listed after every claim: its status, figures, dates, notes,
     * reference, tags, log and comments as they are. Its number, the number it corrects and the one it was split from
     * are left for {@link #finishCloned}; a statement's binding and an editor's lock are not copied.
     */
    public long cloneIn(Claim claim, String toSheet, long personId) {
        long listed = jdbc.queryForObject("SELECT COALESCE(MAX(listed_seq), 0) + 1 FROM claim", Long.class);
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, status, settled_due, opened_by, due_by,"
                            + " deferred_until, proofs, late_entry, reminded_on, ref, tags, void_reason, edited_by, terms, settle_batch,"
                            + " listed_seq, rate_mark)"
                            + " SELECT ?, ?, amount, claim_date, note, status, settled_due, opened_by, due_by, deferred_until, proofs,"
                            + " late_entry, reminded_on, ref, tags, void_reason, edited_by, terms, settle_batch, ?, rate_mark"
                            + " FROM claim WHERE sheet_id = ? AND id = ?",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, toSheet);
            ps.setLong(2, personId);
            ps.setLong(3, listed);
            ps.setString(4, claim.getSheetId());
            ps.setLong(5, claim.getId());
            return ps;
        }, keys);
        long id = keys.getKey().longValue();
        jdbc.update("INSERT INTO claim_log (sheet_id, claim_id, status, by_user, log_date, due) SELECT ?, ?, status, by_user, log_date, due"
                + " FROM claim_log WHERE sheet_id = ? AND claim_id = ? ORDER BY id", toSheet, id, claim.getSheetId(), claim.getId());
        jdbc.update("INSERT INTO claim_comment (sheet_id, claim_id, by_user, comment_date, text) SELECT ?, ?, by_user, comment_date, text"
                + " FROM claim_comment WHERE sheet_id = ? AND claim_id = ? ORDER BY id", toSheet, id, claim.getSheetId(), claim.getId());
        jdbc.update("INSERT INTO claim_line (sheet_id, claim_id, label, qty, unit) SELECT ?, ?, label, qty, unit"
                + " FROM claim_line WHERE sheet_id = ? AND claim_id = ? ORDER BY id", toSheet, id, claim.getSheetId(), claim.getId());
        return id;
    }

    /** Gives a cloned claim its number (null for none), the number it corrects and the one it was split from on the
     *  clone, and records it in the clone's history. */
    public void finishCloned(String sheetId, long id, String number, String corrects, String splitFrom) {
        jdbc.update("UPDATE claim SET claim_number = ?, corrects = ?, split_from = ? WHERE sheet_id = ? AND id = ?",
                number, corrects, splitFrom, sheetId, id);
        history(sheetId, id);
    }

    /** The sheet's today when the claim was opened (its first OPEN or PENDING log line); null when not known. */
    public LocalDate openedOn(Claim claim) {
        List<Date> dates = jdbc.queryForList("SELECT log_date FROM claim_log WHERE sheet_id = ? AND claim_id = ?"
                + " AND status IN ('OPEN', 'PENDING') ORDER BY id", Date.class, claim.getSheetId(), claim.getId());
        return dates.isEmpty() ? null : dates.get(0).toLocalDate();
    }

    /** Gives a transferred claim its number on the sheet it arrived on. */
    public void numberTransferred(String sheetId, long id, String number) {
        jdbc.update("UPDATE claim SET claim_number = ? WHERE sheet_id = ? AND id = ?", number, sheetId, id);
        history(sheetId, id);
    }

    /** Points a transferred claim at the (renumbered) claim it corrects, when that one came with it. */
    public void correctsTransferred(String sheetId, long id, String corrects) {
        if (corrects == null) return;
        jdbc.update("UPDATE claim SET corrects = ? WHERE sheet_id = ? AND id = ?", corrects, sheetId, id);
        history(sheetId, id);
    }

    /** Removes the claim, its log, its comments and its lines from its sheet (its history is kept for the as-of view). */
    public void remove(Claim claim) {
        // Kept as it stood, so the as-of view of a date before it left still shows it.
        jdbc.update("INSERT INTO claim_transferred SELECT c.*, (SELECT today FROM sheet WHERE id = ?) FROM claim c"
                + " WHERE c.sheet_id = ? AND c.id = ?", claim.getSheetId(), claim.getSheetId(), claim.getId());
        jdbc.update("DELETE FROM claim_line WHERE sheet_id = ? AND claim_id = ?", claim.getSheetId(), claim.getId());
        jdbc.update("DELETE FROM claim_log WHERE sheet_id = ? AND claim_id = ?", claim.getSheetId(), claim.getId());
        jdbc.update("DELETE FROM claim_comment WHERE sheet_id = ? AND claim_id = ?", claim.getSheetId(), claim.getId());
        jdbc.update("DELETE FROM claim WHERE sheet_id = ? AND id = ?", claim.getSheetId(), claim.getId());
    }

    /** Keeps a comment on the claim, written by the user on the sheet's today. */
    public void comment(Claim claim, String user, String text) {
        jdbc.update("INSERT INTO claim_comment (sheet_id, claim_id, by_user, comment_date, text)"
                + " SELECT s.id, ?, ?, s.today, ? FROM sheet s WHERE s.id = ?", claim.getId(), user, text, claim.getSheetId());
    }

    /** The claim's lines, oldest first. */
    public List<ClaimLine> lines(Claim claim) {
        return jdbc.query("SELECT label, qty, unit FROM claim_line WHERE sheet_id = ? AND claim_id = ? ORDER BY id",
                (rs, n) -> new ClaimLine(rs.getString("label"), rs.getInt("qty"), rs.getBigDecimal("unit")),
                claim.getSheetId(), claim.getId());
    }

    /** Keeps a line on the claim, after its others. */
    public void addLine(Claim claim, ClaimLine line) {
        jdbc.update("INSERT INTO claim_line (sheet_id, claim_id, label, qty, unit) VALUES (?, ?, ?, ?, ?)",
                claim.getSheetId(), claim.getId(), line.label(), line.qty(), line.unit());
    }

    /** Records the claim's reminded date and whether it has been escalated. */
    public void remind(Claim claim) {
        jdbc.update("UPDATE claim SET reminded_on = ?, escalated = ? WHERE sheet_id = ? AND id = ?",
                Date.valueOf(claim.getRemindedOn()), claim.isEscalated(), claim.getSheetId(), claim.getId());
    }

    /** Defers the claim until its deferred-until date. */
    public void defer(Claim claim) {
        jdbc.update("UPDATE claim SET deferred_until = ? WHERE sheet_id = ? AND id = ?",
                Date.valueOf(claim.getDeferredUntil()), claim.getSheetId(), claim.getId());
        history(claim.getSheetId(), claim.getId());
    }

    /** Records the claim's (re-fixed) due-by. */
    public void refix(Claim claim) {
        jdbc.update("UPDATE claim SET due_by = ? WHERE sheet_id = ? AND id = ?",
                Date.valueOf(claim.getDueBy()), claim.getSheetId(), claim.getId());
        history(claim.getSheetId(), claim.getId());
    }

    /** Records the claim's (new) date and number. */
    public void redate(Claim claim) {
        jdbc.update("UPDATE claim SET claim_date = ?, claim_number = ? WHERE sheet_id = ? AND id = ?",
                Date.valueOf(claim.getDate()), claim.getNumber(), claim.getSheetId(), claim.getId());
        history(claim.getSheetId(), claim.getId());
    }

    /** Marks the claim a late entry: added while its period was soft-closed. */
    public void markLateEntry(Claim claim) {
        claim.setLateEntry(true);
        jdbc.update("UPDATE claim SET late_entry = TRUE WHERE sheet_id = ? AND id = ?", claim.getSheetId(), claim.getId());
        history(claim.getSheetId(), claim.getId());
    }

    /** Records the number of the claim this claim was split from. */
    public void splitFrom(Claim claim) {
        jdbc.update("UPDATE claim SET split_from = ? WHERE sheet_id = ? AND id = ?",
                claim.getSplitFrom(), claim.getSheetId(), claim.getId());
        history(claim.getSheetId(), claim.getId());
    }

    /** Records the bulk settle batch that settled the claim. */
    public void settleBatch(Claim claim) {
        jdbc.update("UPDATE claim SET settle_batch = ? WHERE sheet_id = ? AND id = ?",
                claim.getSettleBatch(), claim.getSheetId(), claim.getId());
        history(claim.getSheetId(), claim.getId());
    }

    /** Who holds the claim's editor lock; null when unlocked. */
    public String lockedBy(Claim claim) {
        List<String> found = jdbc.query("SELECT locked_by FROM claim WHERE sheet_id = ? AND id = ?",
                (rs, i) -> rs.getString(1), claim.getSheetId(), claim.getId());
        return found.isEmpty() ? null : found.get(0);
    }

    public void lock(Claim claim, String user) {
        claim.setLockedBy(user);
        jdbc.update("UPDATE claim SET locked_by = ? WHERE sheet_id = ? AND id = ?", user, claim.getSheetId(), claim.getId());
    }

    public void unlock(Claim claim) {
        claim.setLockedBy(null);
        jdbc.update("UPDATE claim SET locked_by = NULL WHERE sheet_id = ? AND id = ?", claim.getSheetId(), claim.getId());
    }

    /** Releases every claim's lock on the sheet (its today changed). */
    public void unlockAll(String sheetId) {
        jdbc.update("UPDATE claim SET locked_by = NULL WHERE sheet_id = ? AND locked_by IS NOT NULL", sheetId);
    }

    /** Records the claim's proofs count. */
    public void proofs(Claim claim) {
        jdbc.update("UPDATE claim SET proofs = ? WHERE sheet_id = ? AND id = ?",
                claim.getProofs(), claim.getSheetId(), claim.getId());
        history(claim.getSheetId(), claim.getId());
    }

    public void save(Claim claim) {
        jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ?, status = ?, claim_number = ?, settled_due = ?, opened_by = ?, corrects = ?, ref = ?, tags = ?, void_reason = ?, edited_by = ? WHERE sheet_id = ? AND id = ?",
                claim.getAmount(), Date.valueOf(claim.getDate()), claim.getNote(), claim.getStatus().name(),
                claim.getNumber(), claim.getSettledDue(), claim.getOpenedBy(), claim.getCorrects(), claim.getRef(), claim.getTags(), claim.getVoidReason(), claim.getEditedBy(),
                claim.getSheetId(), claim.getId());
        recordTags(claim.getSheetId(), claim.getTags());
        history(claim.getSheetId(), claim.getId());
    }
}

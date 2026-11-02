package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import net.officefloor.hq.app.holiday.Holidays;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The claims kept per sheet, each belonging to a person on that sheet. */
@Repository
public class Claims {

    private static final RowMapper<Claim> ROW = (rs, i) -> new Claim(rs.getLong("id"), rs.getLong("person"),
            rs.getBigDecimal("amount").setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
            rs.getDate("claimed_on").toLocalDate().toString(), rs.getString("note"),
            ClaimStatus.valueOf(rs.getString("status")), rs.getObject("number_on_sheet", Integer.class),
            rs.getBigDecimal("settled_due") == null ? null
                    : rs.getBigDecimal("settled_due").setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
            rs.getString("opened_by"), rs.getBoolean("stated"),
            rs.getBigDecimal("stated_due") == null ? null
                    : rs.getBigDecimal("stated_due").setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
            rs.getString("corrects"), rs.getDate("rounds_on").toLocalDate().toString(),
            rs.getString("period_number"), rs.getDate("due_by").toLocalDate().toString(), List.of(),
            rs.getDate("deferred_until") == null ? null : rs.getDate("deferred_until").toLocalDate().toString(),
            rs.getBoolean("deferred"), rs.getInt("proofs"), rs.getString("split_from"),
            rs.getString("locked_by"), rs.getBoolean("late_entry"),
            rs.getDate("reminded_on") == null ? null : rs.getDate("reminded_on").toLocalDate().toString(),
            rs.getString("ref"), tagList(rs.getString("tags")), tagList(rs.getString("tags_ever")), List.of(),
            rs.getObject("batch", Integer.class), List.of(), rs.getObject("added", Long.class),
            rs.getBoolean("waived"), rs.getInt("proof_photos"), rs.getBoolean("held"), List.of(),
            rs.getString("review_assigned"));

    /** The tags kept as comma separated text (empty for none). */
    private static List<String> tagList(String kept) {
        return kept == null || kept.isEmpty() ? List.of() : List.of(kept.split(","));
    }

    /** The tags as kept: comma separated, sorted. */
    private static String kept(List<String> tags) {
        return String.join(",", tags);
    }

    // A claim rounds on its own date; a correction on the date of the claim it corrects (its own when none has it).
    // It is deferred while OPEN (or VOID, voided while deferred) with its deferred-until after the sheet's today (the real date until one is set).
    private static final String COLUMNS = "SELECT claim.id, claim.person, claim.amount, claim.claimed_on, claim.note, claim.status,"
            + " claim.number_on_sheet, claim.settled_due, claim.opened_by, claim.stated, claim.stated_due, claim.corrects,"
            + " claim.period_number, claim.due_by, COALESCE(corrected.claimed_on, claim.claimed_on) AS rounds_on,"
            + " claim.deferred_until, claim.proofs, claim.split_from, claim.locked_by, claim.late_entry, claim.reminded_on, claim.ref, claim.tags, claim.tags_ever, claim.batch, claim.added, claim.waived, claim.proof_photos, claim.held, claim.review_assigned, (claim.status IN ('OPEN', 'VOID') AND claim.deferred_until IS NOT NULL AND claim.deferred_until >"
            + " COALESCE((SELECT s.today FROM sheet s WHERE s.id = claim.sheet), CURRENT_DATE)) AS deferred FROM claim"
            + " LEFT JOIN claim corrected ON corrected.sheet = claim.sheet AND "
            + " COALESCE(corrected.period_number, CAST(corrected.number_on_sheet AS VARCHAR)) = claim.corrects";

    // The place after every claim listed on the sheet (bound to the sheet), so the claim lists newest.
    private static final String NEXT_LISTED = "(SELECT COALESCE(MAX(l.listed), 0) + 1 FROM claim l WHERE l.sheet = ?)";

    private final JdbcTemplate jdbc;

    public Claims(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String LOG = "SELECT claim, status, by_user, on_date, due FROM claim_log WHERE sheet = ?";

    private static final String COMMENTS = "SELECT claim, by_user, on_date, text FROM claim_comment WHERE sheet = ?";

    private static final String LINES = "SELECT claim, label, qty, unit FROM claim_line WHERE sheet = ?";

    private static final String SHARES = "SELECT claim, person, share FROM claim_share WHERE sheet = ?";

    /** Every claim on the sheet, newest first. */
    public List<Claim> list(String sheet) {
        return withShares(withLines(withComments(withLogs(sheet, jdbc.query(COLUMNS + " WHERE claim.sheet = ? ORDER BY claim.listed DESC, claim.id DESC", ROW, sheet),
                jdbc.query(LOG + " ORDER BY id", LOG_ROW, sheet)), jdbc.query(COMMENTS + " ORDER BY id", COMMENT_ROW, sheet)),
                jdbc.query(LINES + " ORDER BY id", LINE_ROW, sheet)), jdbc.query(SHARES + " ORDER BY id", SHARE_ROW, sheet));
    }

    public Claim find(String sheet, long id) {
        List<Claim> found = jdbc.query(COLUMNS + " WHERE claim.sheet = ? AND claim.id = ?", ROW, sheet, id);
        return found.isEmpty() ? null
                : withShares(withLines(withComments(withLogs(sheet, found, jdbc.query(LOG + " AND claim = ? ORDER BY id", LOG_ROW, sheet, id)),
                        jdbc.query(COMMENTS + " AND claim = ? ORDER BY id", COMMENT_ROW, sheet, id)),
                        jdbc.query(LINES + " AND claim = ? ORDER BY id", LINE_ROW, sheet, id)),
                        jdbc.query(SHARES + " AND claim = ? ORDER BY id", SHARE_ROW, sheet, id)).get(0);
    }

    private record Shared(long claim, ClaimShare share) {
    }

    private static final RowMapper<Shared> SHARE_ROW = (rs, i) -> new Shared(rs.getLong("claim"),
            new ClaimShare(rs.getLong("person"), rs.getBigDecimal("share").setScale(2, RoundingMode.UNNECESSARY).toPlainString()));

    /** The claims, each with the shares of it others hold (in the order shared). */
    private static List<Claim> withShares(List<Claim> found, List<Shared> shared) {
        Map<Long, List<ClaimShare>> shares = new HashMap<>();
        for (Shared s : shared) {
            shares.computeIfAbsent(s.claim(), k -> new ArrayList<>()).add(s.share());
        }
        return found.stream().map(c -> c.withShares(shares.getOrDefault(c.id(), List.of()))).toList();
    }

    /**
     * Shares the claim with the person, conditional on the claim still having the status read and the shares read
     * (their count), so a concurrent change is refused: false when it was not.
     */
    public boolean share(String sheet, long id, ClaimStatus status, int was, long person, BigDecimal share) {
        // Locks the claim's row, so two shares at once are taken one after the other.
        if (jdbc.update("UPDATE claim SET status = status WHERE sheet = ? AND id = ? AND status = ?", sheet, id, status.name()) != 1) {
            return false;
        }
        Integer now = jdbc.queryForObject("SELECT COUNT(*) FROM claim_share WHERE sheet = ? AND claim = ?", Integer.class, sheet, id);
        if (now == null || now != was) {
            return false;
        }
        jdbc.update("INSERT INTO claim_share (sheet, claim, person, share) VALUES (?, ?, ?, ?)", sheet, id, person, share);
        return true;
    }

    private record Lined(long claim, ClaimLine line) {
    }

    private static final RowMapper<Lined> LINE_ROW = (rs, i) -> new Lined(rs.getLong("claim"),
            new ClaimLine(rs.getString("label"), rs.getInt("qty"),
                    rs.getBigDecimal("unit").setScale(2, RoundingMode.UNNECESSARY).toPlainString()));

    /** The claims, each with its own lines (in the order added). */
    private static List<Claim> withLines(List<Claim> found, List<Lined> lined) {
        Map<Long, List<ClaimLine>> lines = new HashMap<>();
        for (Lined l : lined) {
            lines.computeIfAbsent(l.claim(), k -> new ArrayList<>()).add(l.line());
        }
        return found.stream().map(c -> c.withLines(lines.getOrDefault(c.id(), List.of()))).toList();
    }

    /**
     * Adds a line to a draft claim not locked by another user's editor, setting its amount to the lines' total and
     * recording the user as its last editor; conditional on the amount read, so a concurrent change is refused: false
     * when it is no longer a draft with that amount, or another user's editor holds it.
     */
    public boolean addLine(String sheet, long id, String user, BigDecimal was, BigDecimal total, String label, int qty,
            BigDecimal unit) {
        if (jdbc.update("UPDATE claim SET amount = ?, edited_by = ? WHERE sheet = ? AND id = ? AND status = 'DRAFT'"
                + " AND amount = ? AND (locked_by IS NULL OR locked_by = ?)", total, user, sheet, id, was, user) != 1) {
            return false;
        }
        jdbc.update("INSERT INTO claim_line (sheet, claim, label, qty, unit) VALUES (?, ?, ?, ?, ?)", sheet, id, label,
                qty, unit);
        return true;
    }

    private record Commented(long claim, ClaimComment comment) {
    }

    private static final RowMapper<Commented> COMMENT_ROW = (rs, i) -> new Commented(rs.getLong("claim"),
            new ClaimComment(rs.getString("by_user"), rs.getDate("on_date").toLocalDate().toString(), rs.getString("text")));

    /** The claims, each with its own comments (in the order written). */
    private static List<Claim> withComments(List<Claim> found, List<Commented> commented) {
        Map<Long, List<ClaimComment>> comments = new HashMap<>();
        for (Commented c : commented) {
            comments.computeIfAbsent(c.claim(), k -> new ArrayList<>()).add(c.comment());
        }
        return found.stream().map(c -> c.withComments(comments.getOrDefault(c.id(), List.of()))).toList();
    }

    /** Adds a comment to the claim, by the user on the date (the sheet's today). */
    public void comment(String sheet, long id, String user, LocalDate on, String text) {
        jdbc.update("INSERT INTO claim_comment (sheet, claim, by_user, on_date, text) VALUES (?, ?, ?, ?, ?)",
                sheet, id, user, Date.valueOf(on), text);
    }

    private record Logged(long claim, ClaimLogLine line) {
    }

    private static final RowMapper<Logged> LOG_ROW = (rs, i) -> new Logged(rs.getLong("claim"),
            new ClaimLogLine(ClaimStatus.valueOf(rs.getString("status")), rs.getString("by_user"),
                    rs.getDate("on_date").toLocalDate().toString(), rs.getString("due")));

    /** The claims, each with its own lines of the status log (oldest first). */
    private static List<Claim> withLogs(String sheet, List<Claim> found, List<Logged> logged) {
        Map<Long, List<ClaimLogLine>> lines = new HashMap<>();
        for (Logged l : logged) {
            lines.computeIfAbsent(l.claim(), k -> new ArrayList<>()).add(l.line());
        }
        return found.stream().map(c -> c.withLog(lines.getOrDefault(c.id(), List.of()))).toList();
    }

    /** The claim showing this number on the sheet (within its period, or on the sheet), or null when none does. */
    public Claim findByNumber(String sheet, String number) {
        List<Claim> found = jdbc.query(COLUMNS + " WHERE claim.sheet = ?"
                + " AND COALESCE(claim.period_number, CAST(claim.number_on_sheet AS VARCHAR)) = ?", ROW, sheet, number);
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * Adds the claim with its due-by and the terms it was fixed with; the due-by moves after only when a holiday is
     * added (see {@link #refixDueBy}); returns its id.
     */
    public long add(String sheet, long person, ClaimInput input, LocalDate dueBy, int terms) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement("INSERT INTO claim (sheet, person, amount, claimed_on, note, corrects, due_by, ref, tags, tags_ever, due_terms, listed)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + NEXT_LISTED + ")", new String[] { "id" });
            ps.setString(1, sheet);
            ps.setLong(2, person);
            ps.setBigDecimal(3, input.amount());
            ps.setDate(4, Date.valueOf(input.date()));
            ps.setString(5, input.note());
            ps.setString(6, input.corrects());
            ps.setDate(7, Date.valueOf(dueBy));
            ps.setString(8, input.ref());
            ps.setString(9, kept(input.tags()));
            ps.setString(10, kept(input.tags()));
            ps.setInt(11, terms);
            ps.setString(12, sheet);
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }

    private record Fixed(long id, LocalDate date, int terms) {
    }

    /**
     * Re-fixes the due-by of every claim on the sheet that is not SETTLED or VOID and whose due-by is on or after the
     * date: the terms-th business day after its date, with the terms it was fixed with, skipping weekends and the
     * holidays given.
     */
    public void refixDueBy(String sheet, LocalDate from, Set<LocalDate> holidays) {
        List<Fixed> fixed = jdbc.query("SELECT id, claimed_on, due_terms FROM claim WHERE sheet = ?"
                + " AND status NOT IN ('SETTLED', 'VOID') AND due_by >= ?",
                (rs, i) -> new Fixed(rs.getLong("id"), rs.getDate("claimed_on").toLocalDate(), rs.getInt("due_terms")),
                sheet, Date.valueOf(from));
        for (Fixed claim : fixed) {
            LocalDate dueBy = Holidays.businessDaysAfter(claim.date(), claim.terms(), holidays);
            jdbc.update("UPDATE claim SET due_by = ? WHERE sheet = ? AND id = ?", Date.valueOf(dueBy), sheet, claim.id());
        }
    }

    /**
     * How much the corrections made against the claim showing this number already take off it: the sum of their
     * amounts as positive values, leaving out VOID and DELETED ones and the one excluded (null for none).
     */
    public BigDecimal corrected(String sheet, String number, Long excluded) {
        BigDecimal sum = jdbc.queryForObject("SELECT COALESCE(SUM(-amount), 0) FROM claim WHERE sheet = ? AND corrects = ?"
                + " AND status NOT IN ('VOID', 'DELETED') AND id <> ?", BigDecimal.class, sheet, number, excluded == null ? -1L : excluded);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    /** Whether a claim on the sheet other than the one excluded (null for none) has the reference, ignoring case. */
    public boolean refUsed(String sheet, String ref, Long excluded) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM claim WHERE sheet = ? AND LOWER(ref) = LOWER(?) AND id <> ?",
                Integer.class, sheet, ref, excluded == null ? -1L : excluded);
        return n != null && n > 0;
    }

    /** Marks the claim a late entry: added while the period its due-by falls in was soft-closed. */
    public void setLateEntry(String sheet, long id) {
        jdbc.update("UPDATE claim SET late_entry = TRUE WHERE sheet = ? AND id = ?", sheet, id);
    }

    /**
     * Adds the OPEN claim split off another: the amount, with the date, note and tags of the claim it was split from
     * (by the number that claim shows), opened by the user, with its due-by fixed now; returns its id.
     */
    public long addSplit(String sheet, long person, BigDecimal amount, LocalDate date, String note, List<String> tags,
            LocalDate dueBy, int terms, String splitFrom, String openedBy) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement("INSERT INTO claim (sheet, person, amount, claimed_on, note, due_by, status,"
                    + " split_from, opened_by, tags, tags_ever, due_terms, listed) VALUES (?, ?, ?, ?, ?, ?, 'OPEN', ?, ?, ?, ?, ?, "
                    + NEXT_LISTED + ")",
                    new String[] { "id" });
            ps.setString(1, sheet);
            ps.setLong(2, person);
            ps.setBigDecimal(3, amount);
            ps.setDate(4, Date.valueOf(date));
            ps.setString(5, note);
            ps.setDate(6, Date.valueOf(dueBy));
            ps.setString(7, splitFrom);
            ps.setString(8, openedBy);
            ps.setString(9, kept(tags));
            ps.setString(10, kept(tags));
            ps.setInt(11, terms);
            ps.setString(12, sheet);
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }

    /**
     * Leaves an OPEN claim the rest of its amount once split, unbound from any statement; conditional on the
     * amount read and on it still being OPEN and not deferred: false when it no longer is.
     */
    public boolean split(String sheet, long id, BigDecimal was, BigDecimal rest) {
        return jdbc.update("UPDATE claim SET amount = ?, stated = FALSE, stated_due = NULL WHERE sheet = ? AND id = ?"
                + " AND status = 'OPEN' AND amount = ? AND (deferred_until IS NULL OR deferred_until <="
                + " COALESCE((SELECT s.today FROM sheet s WHERE s.id = claim.sheet), CURRENT_DATE))",
                rest, sheet, id, was) == 1;
    }

    /**
     * Edits a draft claim not locked by another user's editor, releasing any lock on it and recording the user as its
     * last editor: false when it is no longer a draft, or another user's editor holds it. The tags it carried before stay among those it has ever carried.
     */
    public boolean update(String sheet, long id, ClaimInput input, List<String> tagsEver, String user) {
        List<String> ever = Stream.concat(tagsEver.stream(), input.tags().stream()).distinct().sorted().toList();
        return jdbc.update(
                "UPDATE claim SET amount = ?, claimed_on = ?, note = ?, corrects = ?, ref = ?, tags = ?, tags_ever = ?, locked_by = NULL, edited_by = ?"
                        + " WHERE sheet = ? AND id = ? AND status = 'DRAFT' AND (locked_by IS NULL OR locked_by = ?)",
                input.amount(), Date.valueOf(input.date()), input.note(), input.corrects(), input.ref(), kept(input.tags()),
                kept(ever), user, sheet, id, user) == 1;
    }

    /** Marks the claim settled by the bulk settle batch with the number; null when settled one at a time. */
    public void setBatch(String sheet, long id, Integer batch) {
        jdbc.update("UPDATE claim SET batch = ? WHERE sheet = ? AND id = ?", batch, sheet, id);
    }

    /** The user who last edited the claim (through its form or its amount entry); null when never edited. */
    public String editedBy(String sheet, long id) {
        return jdbc.query("SELECT edited_by FROM claim WHERE sheet = ? AND id = ?",
                rs -> rs.next() ? rs.getString(1) : null, sheet, id);
    }

    /**
     * Locks a draft claim for the user's editor, releasing any other claim the user's editor held on the sheet:
     * false when it is no longer a draft, or another user's editor holds it.
     */
    public boolean lock(String sheet, long id, String user) {
        if (jdbc.update("UPDATE claim SET locked_by = ? WHERE sheet = ? AND id = ? AND status = 'DRAFT'"
                + " AND (locked_by IS NULL OR locked_by = ?)", user, sheet, id, user) != 1) {
            return false;
        }
        jdbc.update("UPDATE claim SET locked_by = NULL WHERE sheet = ? AND id <> ? AND locked_by = ?", sheet, id, user);
        return true;
    }

    /** Releases the claim's lock when the user's editor holds it. */
    public void unlock(String sheet, long id, String user) {
        jdbc.update("UPDATE claim SET locked_by = NULL WHERE sheet = ? AND id = ? AND locked_by = ?", sheet, id, user);
    }

    /** Releases every lock on the sheet's claims, as when its today changes. */
    public void unlockAll(String sheet) {
        jdbc.update("UPDATE claim SET locked_by = NULL WHERE sheet = ? AND locked_by IS NOT NULL", sheet);
    }

    /**
     * Moves the claim of the person and status it was read with to another person, unbound from any statement and
     * listed as the sheet's newest: false when it no longer has that person or status.
     */
    public boolean reassign(String sheet, long id, long from, ClaimStatus status, long to) {
        return jdbc.update("UPDATE claim SET person = ?, stated = FALSE, stated_due = NULL, listed = " + NEXT_LISTED
                + " WHERE sheet = ? AND id = ? AND person = ? AND status = ?", to, sheet, sheet, id, from,
                status.name()) == 1;
    }

    /**
     * Moves the claim of the status and date it was read with to another date, keeping its due-by and any figure its
     * statement bound: false when it no longer has that status or date.
     */
    public boolean redate(String sheet, long id, ClaimStatus status, LocalDate was, LocalDate date) {
        return jdbc.update("UPDATE claim SET claimed_on = ? WHERE sheet = ? AND id = ? AND status = ? AND claimed_on = ?",
                Date.valueOf(date), sheet, id, status.name(), Date.valueOf(was)) == 1;
    }

    /** Moves the claim from one status to another: false when it no longer has the status it was read with. */
    public boolean changeStatus(String sheet, long id, ClaimStatus from, ClaimStatus to) {
        return jdbc.update("UPDATE claim SET status = ? WHERE sheet = ? AND id = ? AND status = ?", to.name(), sheet,
                id, from.name()) == 1;
    }

    /** Waives the claim's late charge, conditional on its status read and on it not yet being waived: false when it was not. */
    public boolean waive(String sheet, long id, ClaimStatus status) {
        return jdbc.update("UPDATE claim SET waived = TRUE WHERE sheet = ? AND id = ? AND status = ? AND NOT waived",
                sheet, id, status.name()) == 1;
    }

    /** Holds or releases the claim, conditional on its status read and on it not already being so: false when it was not. */
    public boolean hold(String sheet, long id, ClaimStatus status, boolean held) {
        return jdbc.update("UPDATE claim SET held = ? WHERE sheet = ? AND id = ? AND status = ? AND held = ?",
                held, sheet, id, status.name(), !held) == 1;
    }

    /** Assigns the REVIEW claim to the user to review, conditional on it still being REVIEW: false when it is not. */
    public boolean assignReview(String sheet, long id, String user) {
        return jdbc.update("UPDATE claim SET review_assigned = ? WHERE sheet = ? AND id = ? AND status = ?",
                user, sheet, id, ClaimStatus.REVIEW.name()) == 1;
    }

    /** Defers an OPEN claim until the date: false when it is no longer OPEN. */
    public boolean defer(String sheet, long id, LocalDate until) {
        return jdbc.update("UPDATE claim SET deferred_until = ? WHERE sheet = ? AND id = ? AND status = 'OPEN'",
                Date.valueOf(until), sheet, id) == 1;
    }

    /**
     * Reminds an OPEN or PENDING claim on the date (the sheet's today), conditional on its status read and on it not
     * yet being reminded that day: false when it was not.
     */
    public boolean remind(String sheet, long id, ClaimStatus status, LocalDate today) {
        return jdbc.update("UPDATE claim SET reminded_on = ? WHERE sheet = ? AND id = ? AND status = ?"
                + " AND (reminded_on IS NULL OR reminded_on <> ?)", Date.valueOf(today), sheet, id, status.name(),
                Date.valueOf(today)) == 1;
    }

    /** Marks the claim escalated, conditional on it not yet being so: false when it already was. */
    public boolean escalate(String sheet, long id) {
        return jdbc.update("UPDATE claim SET escalated = TRUE WHERE sheet = ? AND id = ? AND NOT escalated", sheet,
                id) == 1;
    }

    /**
     * Raises the claim's proofs count by one (and its photos when the proof is a photo), conditional on the status and count read, so a concurrent change by
     * another user is refused, not overwritten: false when it was not.
     */
    public boolean addProof(String sheet, long id, ClaimStatus status, int was, String kind) {
        return jdbc.update("UPDATE claim SET proofs = proofs + 1, proof_photos = proof_photos + ? WHERE sheet = ? AND id = ?"
                + " AND status = ? AND proofs = ?", ProofKind.PHOTO.equals(kind) ? 1 : 0, sheet, id, status.name(), was) == 1;
    }

    /** Fixes the claim's due as it is settled, or leaves none with null. */
    public void setSettledDue(String sheet, long id, BigDecimal due) {
        jdbc.update("UPDATE claim SET settled_due = ? WHERE sheet = ? AND id = ?", due, sheet, id);
    }

    /**
     * Re-fixes a SETTLED claim's due (null for none), conditional on the due read, so a concurrent change by another
     * user is refused, not overwritten; false when it was not.
     */
    public boolean resettle(String sheet, long id, BigDecimal was, BigDecimal due) {
        return jdbc.update("UPDATE claim SET settled_due = ? WHERE sheet = ? AND id = ? AND status = ?"
                + " AND settled_due IS NOT DISTINCT FROM ?", due, sheet, id, ClaimStatus.SETTLED.name(), was) == 1;
    }

    /** Binds the claim to a statement's figure (null when its grade had no rate), or unbinds it when not stated. */
    public void setStated(String sheet, long id, boolean stated, BigDecimal due) {
        jdbc.update("UPDATE claim SET stated = ?, stated_due = ? WHERE sheet = ? AND id = ?", stated, due, sheet, id);
    }

    /** Unbinds every claim of the person bound by their last statement, so each is live again. */
    public void unbindPerson(String sheet, long person) {
        jdbc.update("UPDATE claim SET stated = FALSE, stated_due = NULL WHERE sheet = ? AND person = ? AND stated",
                sheet, person);
    }

    /** Records the user who opened the claim. */
    public void setOpenedBy(String sheet, long id, String user) {
        jdbc.update("UPDATE claim SET opened_by = ? WHERE sheet = ? AND id = ?", user, sheet, id);
    }

    /** Sets the claim's number on the sheet, or clears it with null. */
    public void setNumber(String sheet, long id, Integer number) {
        jdbc.update("UPDATE claim SET number_on_sheet = ? WHERE sheet = ? AND id = ?", number, sheet, id);
    }

    /** Sets the claim's number within its period. */
    public void setPeriodNumber(String sheet, long id, String periodNumber) {
        jdbc.update("UPDATE claim SET period_number = ? WHERE sheet = ? AND id = ?", periodNumber, sheet, id);
    }

    /**
     * Takes the next number in the sheet's period of the name ("" for the claims in no period): 1 first, then 2,
     * and so on, each period counting on its own. Locked as {@link #nextNumber}.
     */
    public int nextPeriodNumber(String sheet, String period) {
        while (true) {
            if (jdbc.update("UPDATE claim_period_number SET last_number = last_number + 1 WHERE sheet = ? AND period = ?",
                    sheet, period) == 1) {
                return jdbc.queryForObject("SELECT last_number FROM claim_period_number WHERE sheet = ? AND period = ?",
                        Integer.class, sheet, period);
            }
            try {
                jdbc.update("INSERT INTO claim_period_number (sheet, period, last_number) VALUES (?, ?, 1)", sheet, period);
                return 1;
            } catch (DuplicateKeyException raced) {
                // Another user took the period's first number at the same moment: take the one after it.
            }
        }
    }

    /**
     * Takes the next number of the sheet-wide sequence the sheet shares with the other sheets of its tree: the one
     * after the last number given on any of them (1 when none has given one), recorded as the sheet's last number.
     * The counter rows of the whole tree stay locked until the transaction ends, so two claims opened at once
     * anywhere in the tree get different numbers.
     */
    public int nextNumber(String sheet, List<String> tree) {
        try {
            jdbc.update("INSERT INTO claim_number (sheet, last_number) VALUES (?, 0)", sheet);
        } catch (DuplicateKeyException exists) {
            // The sheet has given a number already: its counter is in place.
        }
        java.util.TreeSet<String> sheets = new java.util.TreeSet<>(tree);
        sheets.add(sheet);
        String in = String.join(", ", java.util.Collections.nCopies(sheets.size(), "?"));
        List<Integer> lasts = jdbc.queryForList("SELECT last_number FROM claim_number WHERE sheet IN (" + in
                + ") ORDER BY sheet FOR UPDATE", Integer.class, sheets.toArray());
        int next = lasts.stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
        jdbc.update("UPDATE claim_number SET last_number = ? WHERE sheet = ?", next, sheet);
        return next;
    }

    /**
     * Takes the next number of the fiscal year's sequence (FY<yyyy>) the sheet shares with the other sheets of its
     * tree: the one after the last number given in that year on any of them (1 when none has given one), recorded as
     * the sheet's last number in it. Locked as {@link #nextNumber}.
     */
    public int nextFiscalYearNumber(String sheet, List<String> tree, String fy) {
        try {
            jdbc.update("INSERT INTO claim_fy_number (sheet, fy, last_number) VALUES (?, ?, 0)", sheet, fy);
        } catch (DuplicateKeyException exists) {
            // The sheet has given a number in this fiscal year already: its counter is in place.
        }
        java.util.TreeSet<String> sheets = new java.util.TreeSet<>(tree);
        sheets.add(sheet);
        String in = String.join(", ", java.util.Collections.nCopies(sheets.size(), "?"));
        List<Object> args = new java.util.ArrayList<>();
        args.add(fy);
        args.addAll(sheets);
        List<Integer> lasts = jdbc.queryForList("SELECT last_number FROM claim_fy_number WHERE fy = ? AND sheet IN (" + in
                + ") ORDER BY sheet FOR UPDATE", Integer.class, args.toArray());
        int next = lasts.stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
        jdbc.update("UPDATE claim_fy_number SET last_number = ? WHERE sheet = ? AND fy = ?", next, sheet, fy);
        return next;
    }
}

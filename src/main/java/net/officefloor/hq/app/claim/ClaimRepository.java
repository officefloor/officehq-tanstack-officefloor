package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.officefloor.hq.app.holiday.BusinessDays;
import net.officefloor.hq.app.holiday.HolidayRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Claims by sheet, newest first (by when each came to its person: added, or moved to them). */
@Repository
public class ClaimRepository {

    /** A claim row, its due-by as fixed (one stored before due-bys were fixed: worked out from its terms and the
     *  sheet's holidays, their dates). */
    private static RowMapper<Claim> row(Set<LocalDate> holidays) {
        return (rs, i) -> new Claim(rs.getLong("id"), rs.getString("sheet_id"),
                rs.getLong("person_id"), rs.getBigDecimal("amount"), rs.getObject("claim_date", LocalDate.class),
                rs.getString("note"), ClaimStatus.valueOf(rs.getString("status")),
                rs.getString("claim_number"), rs.getBigDecimal("settled_due"),
                rs.getString("opened_by"), rs.getBoolean("stated"), rs.getBigDecimal("stated_due"),
                rs.getString("corrects"),
                dueBy(rs.getObject("due_by", LocalDate.class), rs.getObject("due_from", LocalDate.class),
                        rs.getInt("terms_days"), holidays),
                rs.getObject("deferred_until", LocalDate.class), rs.getInt("proofs"), rs.getString("split_from"),
                rs.getBoolean("late_entry"), rs.getObject("reminded_on", LocalDate.class), rs.getString("ref"), rs.getString("tags"));
    }

    private static LocalDate dueBy(LocalDate fixed, LocalDate from, int terms, Set<LocalDate> holidays) {
        return fixed != null ? fixed : BusinessDays.after(from, terms, holidays);
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
        return jdbc.query("SELECT * FROM claim WHERE sheet_id = ? ORDER BY listed DESC", row(sheetId), sheetId);
    }

    /** The claim on this sheet, or null when absent. */
    public Claim find(String sheetId, long id) {
        return jdbc.query("SELECT * FROM claim WHERE sheet_id = ? AND id = ?", row(sheetId), sheetId, id)
                .stream().findFirst().orElse(null);
    }

    /** Fix the due-by of each of the sheet's claims stored before due-bys were fixed, over the sheet's holidays as they
     *  are now. */
    public void fixDueBys(String sheetId) {
        var dates = holidays.dates(sheetId);
        jdbc.query("SELECT id, due_from, terms_days FROM claim WHERE sheet_id = ? AND due_by IS NULL", rs -> {
            jdbc.update("UPDATE claim SET due_by = ? WHERE sheet_id = ? AND id = ?",
                    BusinessDays.after(rs.getObject("due_from", LocalDate.class), rs.getInt("terms_days"), dates),
                    sheetId, rs.getLong("id"));
        }, sheetId);
    }

    /** Re-fix the due-by of each of the sheet's claims not SETTLED or VOID whose due-by is on or after the holiday:
     *  worked out again from its date, with the terms it was fixed with, over the sheet's holidays as they are now. */
    public void refixDueBys(String sheetId, LocalDate holiday) {
        var dates = holidays.dates(sheetId);
        record Fixed(long id, LocalDate dueBy) {
        }
        List<Fixed> moved = new java.util.ArrayList<>();
        jdbc.query("SELECT id, due_from, terms_days, due_by FROM claim WHERE sheet_id = ? AND due_by >= ? AND status NOT IN (?, ?)",
                rs -> {
                    LocalDate now = BusinessDays.after(rs.getObject("due_from", LocalDate.class), rs.getInt("terms_days"), dates);
                    if (!now.equals(rs.getObject("due_by", LocalDate.class))) {
                        moved.add(new Fixed(rs.getLong("id"), now));
                    }
                }, sheetId, holiday, ClaimStatus.SETTLED.name(), ClaimStatus.VOID.name());
        for (Fixed f : moved) {
            jdbc.update("UPDATE claim SET due_by = ? WHERE sheet_id = ? AND id = ?", f.dueBy(), sheetId, f.id());
            kept(sheetId, f.id());
        }
    }

    /** Store a new claim fixed with the terms (business days), counted from its date. */
    public Claim insert(int terms, Claim claim) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, corrects, due_from, terms_days, split_from, late_entry, ref, tags, due_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
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
            ps.setObject(13, claim.dueBy());
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
        return jdbc.query("SELECT * FROM claim WHERE sheet_id = ? AND person_id = ? ORDER BY listed DESC", row(sheetId), sheetId,
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

    /** Move the claim to another person with its new number (null for a draft), the newest in their list, unbinding it from any statement: false when it no longer has the person
     *  and status it was loaded with (another user changed it first), and nothing is written. */
    public boolean reassign(Claim claim, long personId, String number) {
        return kept(claim, jdbc.update(
                "UPDATE claim SET person_id = ?, claim_number = ?, stated = FALSE, stated_due = NULL, listed = NEXT VALUE FOR claim_listed_seq WHERE sheet_id = ? AND id = ? AND person_id = ? AND status = ?",
                personId, number, claim.sheetId(), claim.id(), claim.personId(), claim.status().name()) == 1);
    }

    /** Move every claim of one person to another in the order they came to the first, each the newest in the other's
     *  list, keeping its number and date and the figure a statement bound it to. */
    public void moveAll(String sheetId, long fromId, long toId) {
        for (Long id : jdbc.queryForList("SELECT id FROM claim WHERE sheet_id = ? AND person_id = ? ORDER BY listed",
                Long.class, sheetId, fromId)) {
            jdbc.update("UPDATE claim SET person_id = ?, "
                    + "listed = NEXT VALUE FOR claim_listed_seq WHERE sheet_id = ? AND id = ?", toId, sheetId, id);
            kept(sheetId, id);
        }
    }

    /** Move every claim of the person to another sheet, keeping its place in their list, its status, dates, note,
     *  settled due, tags, comments, lines and log: each claim that had a number takes the next number of the other sheet,
     *  in the order the claims were added; each keeps the figure a statement bound it to, loses its reference, editor lock and
     *  settle batch, and its history here is dropped (it is kept anew on the other sheet). */
    public void transfer(String sheetId, long personId, String toSheet) {
        List<Claim> theirs = listForPerson(sheetId, personId).stream()
                .sorted(java.util.Comparator.comparingLong(Claim::id)).toList();
        for (Claim c : theirs) {
            String number = c.number() != null ? nextNumber(toSheet, c.dueBy()) : null;
            jdbc.update("UPDATE claim SET sheet_id = ?, claim_number = ?, due_by = ?, "
                    + "ref = NULL, locked_by = NULL WHERE sheet_id = ? AND id = ?", toSheet, number, c.dueBy(), sheetId,
                    c.id());
            for (String table : List.of("claim_log", "claim_comment", "claim_line", "claim_void_reason")) {
                jdbc.update("UPDATE " + table + " SET sheet_id = ? WHERE sheet_id = ? AND claim_id = ?", toSheet,
                        sheetId, c.id());
            }
            for (String table : List.of("claim_batch", "claim_history")) {
                jdbc.update("DELETE FROM " + table + " WHERE sheet_id = ? AND claim_id = ?", sheetId, c.id());
            }
            kept(toSheet, c.id());
            used(toSheet, c.tags());
        }
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

    /** Store the amount a line gave the claim (its lines' total), recording the user as its last editor. */
    public void lined(Claim claim, java.math.BigDecimal amount, String user) {
        jdbc.update("UPDATE claim SET amount = ?, edited_by = ? WHERE sheet_id = ? AND id = ?",
                amount, user, claim.sheetId(), claim.id());
        kept(claim.sheetId(), claim.id());
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
     *  or for a claim in no period the next in its own sequence (X-<n>); otherwise it is the next number of the
     *  sheet's tree (the sheet's root, reached through its parents, and every descendant of the root) within the
     *  fiscal year of the due-by (by this sheet's fiscal year start month): FY<yyyy>-<n>, one after the last number
     *  given in that fiscal year on any sheet of the tree, recorded as this sheet's last number for that year.
     *  Taking a number locks the sheet's row (and, for a tree number, every row of the tree, in id order) until the
     *  transaction ends, so concurrent openings are numbered one after the other; a rolled-back one gives its number
     *  back, so there are no gaps. */
    public String nextNumber(String sheetId, LocalDate date) {
        jdbc.query("SELECT id FROM sheet WHERE id = ? FOR UPDATE", rs -> null, sheetId);
        Boolean byPeriod = jdbc.query(
                "SELECT period_numbering_from IS NOT NULL AND today >= period_numbering_from FROM sheet WHERE id = ?",
                rs -> rs.next() ? rs.getBoolean(1) : false, sheetId);
        if (!Boolean.TRUE.equals(byPeriod)) {
            Set<String> tree = tree(sheetId);
            String in = String.join(",", Collections.nCopies(tree.size(), "?"));
            Object[] ids = tree.toArray();
            jdbc.query("SELECT id FROM sheet WHERE id IN (" + in + ") ORDER BY id FOR UPDATE", rs -> null, ids);
            int fy = fiscalYear(sheetId, date);
            Object[] args = new Object[ids.length + 1];
            System.arraycopy(ids, 0, args, 0, ids.length);
            args[ids.length] = fy;
            Integer last = jdbc.queryForObject("SELECT MAX(last_number) FROM fiscal_year_number WHERE sheet_id IN ("
                    + in + ") AND fiscal_year = ?", Integer.class, args);
            int next = (last == null ? 0 : last) + 1;
            if (jdbc.update("UPDATE fiscal_year_number SET last_number = ? WHERE sheet_id = ? AND fiscal_year = ?",
                    next, sheetId, fy) == 0) {
                jdbc.update("INSERT INTO fiscal_year_number (sheet_id, fiscal_year, last_number) VALUES (?, ?, ?)",
                        sheetId, fy, next);
            }
            return "FY" + fy + "-" + next;
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

    /** The fiscal year holding the date by the sheet's fiscal year start month, named by the calendar year its
     *  first day falls in. */
    private int fiscalYear(String sheetId, LocalDate date) {
        Integer month = jdbc.query("SELECT fiscal_start_month FROM sheet WHERE id = ?",
                rs -> rs.next() ? (Integer) rs.getObject(1, Integer.class) : null, sheetId);
        int start = month != null ? month : 1;
        return date.getMonthValue() >= start ? date.getYear() : date.getYear() - 1;
    }

    /** The sheets of the sheet's tree, in id order: its root (its parent's parent, and so on, up to a sheet with
     *  none) and every descendant of the root. */
    private Set<String> tree(String sheetId) {
        Set<String> up = new LinkedHashSet<>();
        String root = sheetId;
        for (String at = sheetId; at != null && up.add(at); at = parentOf(at)) {
            root = at;
        }
        Set<String> tree = new TreeSet<>();
        Deque<String> todo = new ArrayDeque<>(List.of(root));
        while (!todo.isEmpty()) {
            String at = todo.pop();
            if (tree.add(at)) {
                todo.addAll(jdbc.queryForList("SELECT id FROM sheet WHERE parent = ?", String.class, at));
            }
        }
        tree.add(sheetId);
        return tree;
    }

    private String parentOf(String sheetId) {
        return jdbc.query("SELECT parent FROM sheet WHERE id = ?", rs -> rs.next() ? rs.getString(1) : null, sheetId);
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
                + "split_from, late_entry, reminded_on, ref, tags, due_by, made_on) SELECT c.sheet_id, c.id, c.person_id, c.amount, c.claim_date, c.note, "
                + "c.status, c.claim_number, c.settled_due, c.opened_by, c.stated, c.stated_due, c.corrects, c.due_from, c.terms_days, "
                + "c.deferred_until, c.proofs, c.split_from, c.late_entry, c.reminded_on, c.ref, c.tags, c.due_by, s.today FROM claim c JOIN sheet s ON s.id = "
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

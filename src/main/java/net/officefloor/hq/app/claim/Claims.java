package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.history.AsOf;
import net.officefloor.hq.app.period.Period;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.officefloor.hq.app.holiday.Holidays;
import net.officefloor.hq.app.person.People;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** The store of claims, kept per sheet. */
@Repository
public class Claims {

    private static final RowMapper<Claim> ROW = (rs, n) -> new Claim(rs.getLong("id"), rs.getString("sheet_id"),
            rs.getLong("person_id"), rs.getBigDecimal("amount"), rs.getObject("claim_date", LocalDate.class),
            rs.getString("note"), ClaimStatus.valueOf(rs.getString("status")),
            rs.getString("claim_number"), rs.getBigDecimal("settled_due"),
            rs.getString("opened_by"), rs.getBoolean("stated"), rs.getBigDecimal("stated_due"),
            rs.getString("corrects"), rs.getObject("due_by", LocalDate.class),
            rs.getObject("deferred_until", LocalDate.class), rs.getInt("proofs"),
            rs.getString("split_from"), rs.getBoolean("late_entry"), rs.getObject("reminded_on", LocalDate.class),
            rs.getString("ref"), tags(rs.getString("tags")));

    /** The next place in a sheet's newest-first list (the sheet's id its one parameter). */
    private static final String NEXT_LISTED = "(SELECT COALESCE(MAX(listed), 0) + 1 FROM claim WHERE sheet_id = ?)";

    private final JdbcTemplate jdbc;

    private final People people;

    public Claims(JdbcTemplate jdbc, People people) {
        this.jdbc = jdbc;
        this.people = people;
    }

    /** The sheet's claims, newest first (a reassigned claim as new as when it was moved). */
    public List<Claim> list(String sheetId) {
        return jdbc.query("SELECT * FROM " + AsOf.table("claim") + " WHERE sheet_id = ? ORDER BY listed DESC, id DESC", ROW, sheetId);
    }

    public Claim find(String sheetId, long id) {
        List<Claim> found = jdbc.query("SELECT * FROM " + AsOf.table("claim") + " WHERE sheet_id = ? AND id = ?", ROW, sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The person's claim with the number, or null when they have none. */
    public Claim findByNumber(String sheetId, long personId, String number) {
        List<Claim> found = jdbc.query("SELECT * FROM " + AsOf.table("claim") + " WHERE sheet_id = ? AND person_id = ? AND claim_number = ?",
                ROW, sheetId, personId, number);
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * How much of the person's claim numbered {@code number} its corrections other than {@code exceptId} (0: none)
     * already take back, as a positive amount; VOID and DELETED corrections left out.
     */
    public BigDecimal corrected(String sheetId, long personId, String number, long exceptId) {
        BigDecimal sum = jdbc.queryForObject("SELECT COALESCE(SUM(amount), 0) FROM " + AsOf.table("claim")
                + " WHERE sheet_id = ? AND person_id = ? AND corrects = ? AND id <> ? AND status NOT IN ('VOID', 'DELETED')",
                BigDecimal.class, sheetId, personId, number, exceptId);
        return sum == null ? BigDecimal.ZERO : sum.negate();
    }

    /**
     * Whether a claim of the sheet other than {@code exceptId} (0: none) carries the reference {@code ref}, compared
     * without regard to case.
     */
    public boolean refUsed(String sheetId, String ref, long exceptId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT COUNT(*) > 0 FROM claim WHERE sheet_id = ? AND ref_key = ? AND id <> ?", Boolean.class,
                sheetId, refKey(ref), exceptId));
    }

    /** The reference as the store compares it: in lower case (null for none). */
    private static String refKey(String ref) {
        return ref == null ? null : ref.toLowerCase(java.util.Locale.ROOT);
    }

    /** Tags as the store keeps them, comma separated (null for none), and back. */
    private static String tagsColumn(List<String> tags) {
        return tags.isEmpty() ? null : String.join(",", tags);
    }

    private static List<String> tags(String column) {
        return column == null || column.isEmpty() ? List.of() : List.of(column.split(","));
    }

    /**
     * Every tag ever carried by a claim or a person of the sheet (as of the date the store is read as of), sorted: a tag
     * stays once used, even when nothing carries it now.
     */
    public List<String> tagsUsed(String sheetId) {
        LocalDate on = AsOf.date();
        List<String> columns = jdbc.queryForList("SELECT DISTINCT tags FROM claim_history WHERE sheet_id = ? AND tags IS NOT NULL"
                + (on == null ? "" : " AND history_on <= DATE '" + on + "'")
                + " UNION SELECT DISTINCT tags FROM person_history WHERE sheet_id = ? AND tags IS NOT NULL"
                + (on == null ? "" : " AND history_on <= DATE '" + on + "'"), String.class, sheetId, sheetId);
        return columns.stream().flatMap(c -> tags(c).stream()).distinct().sorted().toList();
    }

    /**
     * Adds a draft claim, due by {@code dueBy}, fixed with {@code terms} (kept until a holiday is added, see
     * {@link #refixDueBys}); a late entry when its due-by is in a soft-closed period.
     */
    public Claim add(String sheetId, long personId, BigDecimal amount, LocalDate date, String note, String corrects,
            LocalDate dueBy, int terms, String ref, List<String> tags) {
        boolean late = lateEntry(sheetId, dueBy);
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, corrects, due_by, late_entry, ref, ref_key, tags, due_terms, listed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                            + NEXT_LISTED + ")",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setLong(2, personId);
            ps.setBigDecimal(3, amount);
            ps.setDate(4, Date.valueOf(date));
            ps.setString(5, note);
            ps.setObject(6, corrects);
            ps.setDate(7, Date.valueOf(dueBy));
            ps.setBoolean(8, late);
            ps.setString(9, ref);
            ps.setString(10, refKey(ref));
            ps.setString(11, tagsColumn(tags));
            ps.setInt(12, terms);
            ps.setString(13, sheetId);
            return ps;
        }, key);
        return new Claim(key.getKey().longValue(), sheetId, personId, amount, date, note, ClaimStatus.DRAFT, null, null, null,
                false, null, corrects, dueBy, null, 0, null, late, null, ref, tags);
    }

    /** Whether a claim made now, due by {@code dueBy}, is a late entry: its due-by is in a soft-closed period. */
    private boolean lateEntry(String sheetId, LocalDate dueBy) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT COUNT(*) > 0 FROM " + AsOf.table("period") + " WHERE sheet_id = ? AND soft AND from_date <= ? AND ? <= to_date",
                Boolean.class, sheetId, Date.valueOf(dueBy), Date.valueOf(dueBy)));
    }

    /** How many of the person's claims are OPEN or PENDING and due by a date inside the period. */
    public int openInPeriod(String sheetId, long personId, Period period) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM claim WHERE sheet_id = ? AND person_id = ? AND status IN (?, ?)"
                + " AND due_by >= ? AND due_by <= ?", Integer.class, sheetId, personId, ClaimStatus.OPEN.name(),
                ClaimStatus.PENDING.name(), Date.valueOf(period.from()), Date.valueOf(period.to()));
        return n == null ? 0 : n;
    }

    /** Saves an edit of the claim, recording {@code user} as its last editor. */
    public void save(Claim claim, String user) {
        jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ?, corrects = ?, ref = ?, ref_key = ?, tags = ?, edited_by = ? WHERE id = ?",
                claim.amount(), Date.valueOf(claim.date()), claim.note(), claim.corrects(), claim.ref(), refKey(claim.ref()),
                tagsColumn(claim.tags()), user, claim.id());
    }

    /** The user who last edited the claim, or null when it was never edited. */
    public String editedBy(Claim claim) {
        List<String> found = jdbc.queryForList("SELECT edited_by FROM claim WHERE id = ?", String.class, claim.id());
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * Moves the claim to another person if it still has its loaded person and status, and returns it as moved; null
     * when it has since changed. It keeps its date, is renumbered next in {@code scope} (as {@link #changeStatus};
     * a draft, having no number, gets none; its old number is never reused), is unbound from any statement (live
     * again until its new person's next statement) and lists as the newest claim on the sheet.
     */
    public Claim reassign(Claim claim, long personId, String scope) {
        if (jdbc.update("UPDATE claim SET person_id = ?, stated = FALSE, stated_due = NULL, listed = " + NEXT_LISTED
                + " WHERE id = ? AND person_id = ? AND status = ?", personId, claim.sheetId(), claim.id(),
                claim.personId(), claim.status().name()) != 1) {
            return null;
        }
        Claim moved = claim.withPerson(personId);
        if (claim.number() == null) {
            return moved;
        }
        String number = number(claim.sheetId(), scope, claim.dueBy() == null ? claim.date() : claim.dueBy());
        jdbc.update("UPDATE claim SET claim_number = ? WHERE id = ?", number, claim.id());
        return moved.withStatus(moved.status(), number, moved.settledDue(), moved.openedBy());
    }

    /**
     * Moves the claim to {@code next} if it still has its loaded status, and returns it as changed; null when
     * it has since changed. Opening gives it the next number of {@code scope} (null: the sheet-wide sequence; a
     * period's name, or "" for no period, while the sheet numbers by period), which it keeps
     * (voided or rejected back to OPEN too: a number is never reused). Settling
     * stores {@code due} as its fixed due (null keeps it live). Opening (or sending to REVIEW) a DRAFT records {@code user} as its
     * opener; a REVIEW claim is numbered when its review makes it OPEN.
     */
    public Claim changeStatus(Claim claim, ClaimStatus next, BigDecimal due, String user, String scope) {
        if (jdbc.update("UPDATE claim SET status = ? WHERE id = ? AND status = ?", next.name(), claim.id(),
                claim.status().name()) != 1) {
            return null;
        }
        String number = switch (next) {
            case OPEN -> claim.status() != ClaimStatus.DRAFT && claim.status() != ClaimStatus.REVIEW ? claim.number()
                    : number(claim.sheetId(), scope, claim.dueBy() == null ? claim.date() : claim.dueBy());
            default -> claim.number();
        };
        BigDecimal settledDue = next == ClaimStatus.SETTLED ? due : claim.settledDue();
        String openedBy = (next == ClaimStatus.OPEN || next == ClaimStatus.REVIEW) && claim.status() == ClaimStatus.DRAFT
                ? user : claim.openedBy();
        jdbc.update("UPDATE claim SET claim_number = ?, settled_due = ?, opened_by = ? WHERE id = ?", number,
                settledDue, openedBy, claim.id());
        return claim.withStatus(next, number, settledDue, openedBy);
    }

    /**
     * Splits {@code amount} off the claim if it is still OPEN at the amount it was loaded with, and returns the new
     * claim; null when it has since changed. The claim keeps the rest of its amount and is unbound from any statement;
     * the new claim is OPEN with the same person, date, note and tags, numbered next in {@code scope} (as
     * {@link #changeStatus}), opened by {@code user}, due by {@code dueBy} (fixed with {@code terms}), split from the claim's number, a late entry
     * when its due-by is in a soft-closed period, and listed as the newest claim on the sheet.
     */
    public Claim split(Claim claim, BigDecimal amount, String user, String scope, LocalDate dueBy, int terms) {
        BigDecimal rest = claim.amount().subtract(amount);
        if (jdbc.update("UPDATE claim SET amount = ?, stated = FALSE, stated_due = NULL WHERE id = ? AND status = ? AND amount = ?",
                rest, claim.id(), ClaimStatus.OPEN.name(), claim.amount()) != 1) {
            return null;
        }
        String number = number(claim.sheetId(), scope, dueBy);
        boolean late = lateEntry(claim.sheetId(), dueBy);
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, status, claim_number, opened_by, due_by, split_from, late_entry, tags, due_terms, listed) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + NEXT_LISTED + ")",
                    new String[] { "id" });
            ps.setString(1, claim.sheetId());
            ps.setLong(2, claim.personId());
            ps.setBigDecimal(3, amount);
            ps.setDate(4, Date.valueOf(claim.date()));
            ps.setString(5, claim.note());
            ps.setString(6, ClaimStatus.OPEN.name());
            ps.setString(7, number);
            ps.setString(8, user);
            ps.setDate(9, Date.valueOf(dueBy));
            ps.setString(10, claim.number());
            ps.setBoolean(11, late);
            ps.setString(12, tagsColumn(claim.tags()));
            ps.setInt(13, terms);
            ps.setString(14, claim.sheetId());
            return ps;
        }, key);
        return new Claim(key.getKey().longValue(), claim.sheetId(), claim.personId(), amount, claim.date(), claim.note(),
                ClaimStatus.OPEN, number, null, user, false, null, null, dueBy, null, 0, claim.number(), late, null, null, claim.tags());
    }

    /**
     * Works out again the due-by of every claim on the sheet that is not SETTLED, VOID or DELETED and is due on or after
     * {@code from}: the terms-th business day after its date, with the terms it was fixed with, over {@code holidays}.
     * Only a due-by that moves is written.
     */
    public void refixDueBys(String sheetId, LocalDate from, Set<LocalDate> holidays) {
        Map<Long, LocalDate> moved = new LinkedHashMap<>();
        jdbc.query("SELECT id, claim_date, due_by, due_terms FROM claim WHERE sheet_id = ? AND status NOT IN (?, ?, ?) AND due_by >= ? "
                + "AND due_terms IS NOT NULL ORDER BY id", rs -> {
                    LocalDate refixed = Holidays.businessDays(rs.getObject(2, LocalDate.class), rs.getInt(4), holidays);
                    if (!refixed.equals(rs.getObject(3, LocalDate.class))) {
                        moved.put(rs.getLong(1), refixed);
                    }
                }, sheetId, ClaimStatus.SETTLED.name(), ClaimStatus.VOID.name(), ClaimStatus.DELETED.name(), Date.valueOf(from));
        moved.forEach((id, dueBy) -> jdbc.update("UPDATE claim SET due_by = ? WHERE id = ?", Date.valueOf(dueBy), id));
    }

    /**
     * Re-dates the claim to {@code date} if it still has its loaded status and date, and returns it as re-dated; null
     * when it has since changed. Its due-by, statement figure and number stay.
     */
    public Claim redate(Claim claim, LocalDate date) {
        if (jdbc.update("UPDATE claim SET claim_date = ? WHERE id = ? AND status = ? AND claim_date = ?", Date.valueOf(date),
                claim.id(), claim.status().name(), Date.valueOf(claim.date())) != 1) {
            return null;
        }
        return claim.withDate(date, claim.number());
    }

    /** Defers the claim to {@code until} if it is still OPEN, and returns it as deferred; null when it has since changed. */
    public Claim defer(Claim claim, LocalDate until) {
        if (jdbc.update("UPDATE claim SET deferred_until = ? WHERE id = ? AND status = ?", Date.valueOf(until), claim.id(),
                ClaimStatus.OPEN.name()) != 1) {
            return null;
        }
        return claim.withDeferredUntil(until);
    }

    /**
     * Reminds the claim on {@code on} if it is still OPEN or PENDING and not yet reminded that day, and returns it as
     * reminded; null when it has since changed.
     */
    public Claim remind(Claim claim, LocalDate on) {
        if (jdbc.update("UPDATE claim SET reminded_on = ? WHERE id = ? AND status IN (?, ?) AND reminded_on IS DISTINCT FROM ?",
                Date.valueOf(on), claim.id(), ClaimStatus.OPEN.name(), ClaimStatus.PENDING.name(), Date.valueOf(on)) != 1) {
            return null;
        }
        return claim.withRemindedOn(on);
    }

    /** Escalates the claim if it was never escalated, and tells whether it was escalated now: a claim is escalated at most once ever. */
    public boolean escalate(Claim claim) {
        return jdbc.update("UPDATE claim SET escalated = TRUE WHERE id = ? AND NOT escalated", claim.id()) == 1;
    }

    /**
     * Raises the claim's proofs count by one if it still has its loaded status and count, and returns it as raised; null when
     * it has since changed.
     */
    public Claim addProof(Claim claim) {
        if (jdbc.update("UPDATE claim SET proofs = proofs + 1 WHERE id = ? AND status = ? AND proofs = ?", claim.id(),
                claim.status().name(), claim.proofs()) != 1) {
            return null;
        }
        return claim.withProofs(claim.proofs() + 1);
    }

    /**
     * Freezes the SETTLED claim again at {@code due} (null: n/a) if it is still SETTLED at the due it was loaded with,
     * and returns it as re-settled; null when it has since changed.
     */
    public Claim resettle(Claim claim, BigDecimal due) {
        if (jdbc.update("UPDATE claim SET settled_due = ? WHERE id = ? AND status = ? AND settled_due IS NOT DISTINCT FROM ?",
                due, claim.id(), ClaimStatus.SETTLED.name(), claim.settledDue()) != 1) {
            return null;
        }
        return claim.withStatus(ClaimStatus.SETTLED, claim.number(), due, claim.openedBy());
    }

    /** Appends a line to the claim's status log: its status now, by {@code user} on {@code on}, at {@code due} (null: n/a). */
    public void log(Claim claim, String user, LocalDate on, BigDecimal due) {
        jdbc.update("INSERT INTO claim_log (sheet_id, claim_id, status, by_user, logged_on, due) VALUES (?, ?, ?, ?, ?, ?)",
                claim.sheetId(), claim.id(), claim.status().name(), user, Date.valueOf(on),
                due == null ? "n/a" : due.toPlainString());
    }

    /** Adds a comment to the claim: {@code text}, written by {@code user} on {@code on}. */
    public void comment(Claim claim, String user, LocalDate on, String text) {
        jdbc.update("INSERT INTO claim_comment (sheet_id, claim_id, by_user, commented_on, text) VALUES (?, ?, ?, ?, ?)",
                claim.sheetId(), claim.id(), user, Date.valueOf(on), text);
    }

    /** Adds a line to the claim, after its others. */
    public void addLine(Claim claim, ClaimLine line) {
        jdbc.update("INSERT INTO claim_line (sheet_id, claim_id, label, qty, unit) VALUES (?, ?, ?, ?, ?)",
                claim.sheetId(), claim.id(), line.label(), line.qty(), line.unit());
    }

    /** The claim's lines, in the order added. */
    public List<ClaimLine> lines(Claim claim) {
        return jdbc.query("SELECT * FROM " + AsOf.table("claim_line") + " WHERE claim_id = ? ORDER BY id",
                (rs, n) -> new ClaimLine(rs.getString("label"), rs.getInt("qty"), rs.getBigDecimal("unit").setScale(2)),
                claim.id());
    }

    /** The lines of the sheet's claims, by claim id, each in the order added as "label qty x unit". */
    public java.util.Map<Long, List<String>> lines(String sheetId) {
        java.util.Map<Long, List<String>> lines = new java.util.HashMap<>();
        jdbc.query("SELECT * FROM " + AsOf.table("claim_line") + " WHERE sheet_id = ? ORDER BY id", rs -> {
            lines.computeIfAbsent(rs.getLong("claim_id"), k -> new java.util.ArrayList<>())
                    .add(new ClaimLine(rs.getString("label"), rs.getInt("qty"), rs.getBigDecimal("unit").setScale(2)).shown());
        }, sheetId);
        return lines;
    }

    /** The comments of the sheet's claims, by claim id, each oldest first as "user on date: text". */
    public java.util.Map<Long, List<String>> comments(String sheetId) {
        java.util.Map<Long, List<String>> comments = new java.util.HashMap<>();
        jdbc.query("SELECT * FROM " + AsOf.table("claim_comment") + " WHERE sheet_id = ? ORDER BY id", rs -> {
            comments.computeIfAbsent(rs.getLong("claim_id"), k -> new java.util.ArrayList<>())
                    .add(rs.getString("by_user") + " on " + rs.getObject("commented_on", LocalDate.class) + ": "
                            + rs.getString("text"));
        }, sheetId);
        return comments;
    }

    /** The sheet's today on which the claim was last logged SETTLED; null when its log records no settlement. */
    public LocalDate settledOn(Claim claim) {
        List<LocalDate> on = jdbc.query(
                "SELECT logged_on FROM " + AsOf.table("claim_log") + " WHERE claim_id = ? AND status = ? ORDER BY id DESC",
                (rs, n) -> rs.getObject("logged_on", LocalDate.class), claim.id(), ClaimStatus.SETTLED.name());
        return on.isEmpty() ? null : on.get(0);
    }

    /** The sheet's today on which the claim was last logged DELETED; null when its log records no deletion. */
    public LocalDate deletedOn(Claim claim) {
        List<LocalDate> on = jdbc.query(
                "SELECT logged_on FROM " + AsOf.table("claim_log") + " WHERE claim_id = ? AND status = ? ORDER BY id DESC",
                (rs, n) -> rs.getObject("logged_on", LocalDate.class), claim.id(), ClaimStatus.DELETED.name());
        return on.isEmpty() ? null : on.get(0);
    }

    /** The sheet's today on which each of the sheet's claims last went to REVIEW, by claim id (a claim that never did is left out). */
    public java.util.Map<Long, LocalDate> reviewOns(String sheetId) {
        java.util.Map<Long, LocalDate> on = new java.util.HashMap<>();
        jdbc.query("SELECT claim_id, logged_on FROM " + AsOf.table("claim_log") + " WHERE sheet_id = ? AND status = ? ORDER BY id", rs -> {
            on.put(rs.getLong("claim_id"), rs.getObject("logged_on", LocalDate.class));
        }, sheetId, ClaimStatus.REVIEW.name());
        return on;
    }

    /** The status logs of the sheet's claims, by claim id, each oldest first as "STATUS by user on date due=due". */
    public java.util.Map<Long, List<String>> logs(String sheetId) {
        java.util.Map<Long, List<String>> logs = new java.util.HashMap<>();
        jdbc.query("SELECT * FROM " + AsOf.table("claim_log") + " WHERE sheet_id = ? ORDER BY id", rs -> {
            logs.computeIfAbsent(rs.getLong("claim_id"), k -> new java.util.ArrayList<>())
                    .add(rs.getString("status") + " by " + rs.getString("by_user") + " on "
                            + rs.getObject("logged_on", LocalDate.class) + " due=" + rs.getString("due"));
        }, sheetId);
        return logs;
    }

    /**
     * Each logged claim's status at the end of {@code on}, by claim id: the last status its log records on or before it,
     * DRAFT (counting nowhere) when it was added after. A claim with no log is not in the map.
     */
    public java.util.Map<Long, ClaimStatus> statusesOn(String sheetId, LocalDate on) {
        java.util.Map<Long, ClaimStatus> statuses = new java.util.HashMap<>();
        jdbc.query("SELECT * FROM " + AsOf.table("claim_log") + " WHERE sheet_id = ? ORDER BY id", rs -> {
            long id = rs.getLong("claim_id");
            if (!rs.getObject("logged_on", LocalDate.class).isAfter(on)) {
                statuses.put(id, ClaimStatus.valueOf(rs.getString("status")));
            } else {
                statuses.putIfAbsent(id, ClaimStatus.DRAFT);
            }
        }, sheetId);
        return statuses;
    }

    /**
     * Binds the person's claims to the figures of a statement: each claim in {@code figures} is stated at
     * its figure (null: n/a), and every other claim of theirs is left unbound.
     */
    public void bind(String sheetId, long personId, java.util.Map<Long, BigDecimal> figures) {
        jdbc.update("UPDATE claim SET stated = FALSE, stated_due = NULL WHERE sheet_id = ? AND person_id = ?",
                sheetId, personId);
        figures.forEach((id, figure) -> jdbc.update(
                "UPDATE claim SET stated = TRUE, stated_due = ? WHERE id = ?", figure, id));
    }

    /**
     * The sheet's today on which each claim of the sheet was first logged OPEN, by claim id; a claim never logged OPEN
     * is not in the map.
     */
    public Map<Long, LocalDate> openedOn(String sheetId) {
        Map<Long, LocalDate> opened = new java.util.HashMap<>();
        jdbc.query("SELECT claim_id, logged_on FROM claim_log WHERE sheet_id = ? AND status = ? ORDER BY id",
                rs -> {
                    opened.putIfAbsent(rs.getLong(1), rs.getObject(2, LocalDate.class));
                }, sheetId, ClaimStatus.OPEN.name());
        return opened;
    }

    /**
     * The next number of {@code scope} on the sheet, as {@link #changeStatus} numbers an opened claim due by
     * {@code dueBy} (null: sheet-wide, FY<yyyy>-<n> in the fiscal year {@code dueBy} falls in).
     */
    public String number(String sheetId, String scope, LocalDate dueBy) {
        if (scope == null) {
            String fiscalYear = fiscalYear(sheetId, dueBy);
            return fiscalYear + "-" + nextNumber(sheetId, fiscalYear);
        }
        return (scope.isEmpty() ? "X" : scope) + "-" + nextPeriodNumber(sheetId, scope);
    }

    /**
     * The fiscal year (FY<yyyy>) the date falls in on the sheet: FY<yyyy> starts on the first of the sheet's fiscal
     * start month (January when it has none) in yyyy.
     */
    private String fiscalYear(String sheetId, LocalDate date) {
        List<String> values = jdbc.queryForList("SELECT setting_value FROM setting WHERE sheet_id = ? AND setting_key = ?",
                String.class, sheetId, "fiscal-start-month");
        String value = values.isEmpty() || values.get(0) == null ? "" : values.get(0).trim();
        int start = value.isEmpty() ? 1 : Integer.parseInt(value);
        return "FY" + (date.getMonthValue() >= start ? date.getYear() : date.getYear() - 1);
    }

    /**
     * The sheet's next claim number in the fiscal year: one after the last number given in that fiscal year anywhere
     * in the sheet's tree, which share one sequence. The tree's counter rows are locked (in sheet order) by the
     * increment until the transaction commits, so two claims opened at once anywhere in the tree get different numbers.
     */
    private long nextNumber(String sheetId, String fiscalYear) {
        Set<String> tree = new TreeSet<>(people.tree(sheetId));
        for (String sheet : tree) {
            try {
                jdbc.update("INSERT INTO claim_fiscal_number_seq (sheet_id, fiscal_year, last_number) SELECT ?, ?, 0 "
                        + "WHERE NOT EXISTS (SELECT 1 FROM claim_fiscal_number_seq WHERE sheet_id = ? AND fiscal_year = ?)",
                        sheet, fiscalYear, sheet, fiscalYear);
            } catch (DuplicateKeyException e) {
                // Another user started the sheet's counter at the same moment.
            }
        }
        long last = 0;
        for (String sheet : tree) {
            jdbc.update("UPDATE claim_fiscal_number_seq SET last_number = last_number WHERE sheet_id = ? AND fiscal_year = ?",
                    sheet, fiscalYear);
            last = Math.max(last, jdbc.queryForObject(
                    "SELECT last_number FROM claim_fiscal_number_seq WHERE sheet_id = ? AND fiscal_year = ?",
                    Long.class, sheet, fiscalYear));
        }
        jdbc.update("UPDATE claim_fiscal_number_seq SET last_number = ? WHERE sheet_id = ? AND fiscal_year = ?",
                last + 1, sheetId, fiscalYear);
        return last + 1;
    }

    /** The next number of the sheet's sequence for the period ("" for no period); locked like {@link #nextNumber}. */
    private long nextPeriodNumber(String sheetId, String scope) {
        try {
            jdbc.update("INSERT INTO claim_period_number_seq (sheet_id, scope, last_number) SELECT ?, ?, 0 "
                    + "WHERE NOT EXISTS (SELECT 1 FROM claim_period_number_seq WHERE sheet_id = ? AND scope = ?)",
                    sheetId, scope, sheetId, scope);
        } catch (DuplicateKeyException e) {
            // Another user started the period's counter at the same moment.
        }
        jdbc.update("UPDATE claim_period_number_seq SET last_number = last_number + 1 WHERE sheet_id = ? AND scope = ?",
                sheetId, scope);
        return jdbc.queryForObject("SELECT last_number FROM claim_period_number_seq WHERE sheet_id = ? AND scope = ?",
                Long.class, sheetId, scope);
    }
}

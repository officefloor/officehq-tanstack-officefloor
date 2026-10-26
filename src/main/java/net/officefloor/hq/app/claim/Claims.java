package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.history.AsOf;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
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

    public Claims(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
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
     * Every tag ever carried by a claim of the sheet (as of the date the store is read as of), sorted: a tag stays
     * once used, even when no claim carries it now.
     */
    public List<String> tagsUsed(String sheetId) {
        LocalDate on = AsOf.date();
        List<String> columns = jdbc.queryForList("SELECT DISTINCT tags FROM claim_history WHERE sheet_id = ? AND tags IS NOT NULL"
                + (on == null ? "" : " AND history_on <= DATE '" + on + "'"), String.class, sheetId);
        return columns.stream().flatMap(c -> tags(c).stream()).distinct().sorted().toList();
    }

    /** Adds a draft claim, due by {@code dueBy} for good; a late entry when its due-by is in a soft-closed period. */
    public Claim add(String sheetId, long personId, BigDecimal amount, LocalDate date, String note, String corrects,
            LocalDate dueBy, String ref, List<String> tags) {
        boolean late = lateEntry(sheetId, dueBy);
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, corrects, due_by, late_entry, ref, ref_key, tags, listed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
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
            ps.setString(12, sheetId);
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

    public void save(Claim claim) {
        jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ?, corrects = ?, ref = ?, ref_key = ?, tags = ? WHERE id = ?",
                claim.amount(), Date.valueOf(claim.date()), claim.note(), claim.corrects(), claim.ref(), refKey(claim.ref()),
                tagsColumn(claim.tags()), claim.id());
    }

    /**
     * Moves the claim to another person if it still has its loaded person and status, and returns it as moved; null
     * when it has since changed. It keeps its number and date, is unbound from any statement (live again until its
     * new person's next statement) and lists as the newest claim on the sheet.
     */
    public Claim reassign(Claim claim, long personId) {
        if (jdbc.update("UPDATE claim SET person_id = ?, stated = FALSE, stated_due = NULL, listed = " + NEXT_LISTED
                + " WHERE id = ? AND person_id = ? AND status = ?", personId, claim.sheetId(), claim.id(),
                claim.personId(), claim.status().name()) != 1) {
            return null;
        }
        return claim.withPerson(personId);
    }

    /**
     * Moves the claim to {@code next} if it still has its loaded status, and returns it as changed; null when
     * it has since changed. Opening gives it the next number of {@code scope} (null: the sheet-wide sequence; a
     * period's name, or "" for no period, while the sheet numbers by period), which it keeps
     * (voided or rejected back to OPEN too: a number is never reused). Settling
     * stores {@code due} as its fixed due (null keeps it live). Opening records {@code user} as its opener.
     */
    public Claim changeStatus(Claim claim, ClaimStatus next, BigDecimal due, String user, String scope) {
        if (jdbc.update("UPDATE claim SET status = ? WHERE id = ? AND status = ?", next.name(), claim.id(),
                claim.status().name()) != 1) {
            return null;
        }
        String number = switch (next) {
            case OPEN -> claim.status() != ClaimStatus.DRAFT ? claim.number() : scope == null ? Long.toString(nextNumber(claim.sheetId()))
                    : (scope.isEmpty() ? "X" : scope) + "-" + nextNumber(claim.sheetId(), scope);
            default -> claim.number();
        };
        BigDecimal settledDue = next == ClaimStatus.SETTLED ? due : claim.settledDue();
        String openedBy = next == ClaimStatus.OPEN && claim.status() == ClaimStatus.DRAFT ? user : claim.openedBy();
        jdbc.update("UPDATE claim SET claim_number = ?, settled_due = ?, opened_by = ? WHERE id = ?", number,
                settledDue, openedBy, claim.id());
        return claim.withStatus(next, number, settledDue, openedBy);
    }

    /**
     * Splits {@code amount} off the claim if it is still OPEN at the amount it was loaded with, and returns the new
     * claim; null when it has since changed. The claim keeps the rest of its amount and is unbound from any statement;
     * the new claim is OPEN with the same person, date, note and tags, numbered next in {@code scope} (as
     * {@link #changeStatus}), opened by {@code user}, due by {@code dueBy}, split from the claim's number, a late entry
     * when its due-by is in a soft-closed period, and listed as the newest claim on the sheet.
     */
    public Claim split(Claim claim, BigDecimal amount, String user, String scope, LocalDate dueBy) {
        BigDecimal rest = claim.amount().subtract(amount);
        if (jdbc.update("UPDATE claim SET amount = ?, stated = FALSE, stated_due = NULL WHERE id = ? AND status = ? AND amount = ?",
                rest, claim.id(), ClaimStatus.OPEN.name(), claim.amount()) != 1) {
            return null;
        }
        String number = scope == null ? Long.toString(nextNumber(claim.sheetId()))
                : (scope.isEmpty() ? "X" : scope) + "-" + nextNumber(claim.sheetId(), scope);
        boolean late = lateEntry(claim.sheetId(), dueBy);
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, status, claim_number, opened_by, due_by, split_from, late_entry, tags, listed) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + NEXT_LISTED + ")",
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
            ps.setString(13, claim.sheetId());
            return ps;
        }, key);
        return new Claim(key.getKey().longValue(), claim.sheetId(), claim.personId(), amount, claim.date(), claim.note(),
                ClaimStatus.OPEN, number, null, user, false, null, null, dueBy, null, 0, claim.number(), late, null, null, claim.tags());
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
     * The sheet's next claim number. The counter row is locked by the increment until the transaction
     * commits, so two claims opened at once get different numbers.
     */
    private long nextNumber(String sheetId) {
        try {
            jdbc.update("INSERT INTO claim_number_seq (sheet_id, last_number) SELECT ?, 0 "
                    + "WHERE NOT EXISTS (SELECT 1 FROM claim_number_seq WHERE sheet_id = ?)", sheetId, sheetId);
        } catch (DuplicateKeyException e) {
            // Another user started the sheet's counter at the same moment.
        }
        jdbc.update("UPDATE claim_number_seq SET last_number = last_number + 1 WHERE sheet_id = ?", sheetId);
        return jdbc.queryForObject("SELECT last_number FROM claim_number_seq WHERE sheet_id = ?", Long.class,
                sheetId);
    }

    /** The next number of the sheet's sequence for the period ("" for no period); locked like {@link #nextNumber(String)}. */
    private long nextNumber(String sheetId, String scope) {
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

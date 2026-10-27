package net.officefloor.hq.app.person;

import net.officefloor.hq.app.history.AsOf;
import java.sql.PreparedStatement;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** The store of people, kept per sheet. */
@Repository
public class People {

    private static final RowMapper<Person> ROW = (rs, n) -> new Person(rs.getLong("id"),
            rs.getString("sheet_id"), rs.getString("name"), rs.getString("grade"), rs.getLong("version"),
            rs.getObject("retired_from", LocalDate.class), List.of(), List.of(),
            rs.getObject("terms", Integer.class), rs.getString("contact"), List.of());

    private final JdbcTemplate jdbc;

    public People(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's people in the order they were added. */
    public List<Person> list(String sheetId) {
        return withDated(sheetId, jdbc.query("SELECT * FROM " + AsOf.table("person") + " WHERE sheet_id = ? ORDER BY id", ROW, sheetId));
    }

    public Person find(String sheetId, long id) {
        List<Person> found = jdbc.query("SELECT * FROM " + AsOf.table("person") + " WHERE sheet_id = ? AND id = ?", ROW, sheetId, id);
        return found.isEmpty() ? null : withDated(sheetId, found).get(0);
    }

    /** Aliases sort as names are compared: ignoring case (then as typed, so the order is stable). */
    private static final Comparator<String> BY_NAME = String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder());

    /** The people with the grades they were given from a date on, their holds and their aliases, read from the store. */
    private List<Person> withDated(String sheetId, List<Person> found) {
        Map<Long, List<DatedGrade>> dated = new HashMap<>();
        jdbc.query("SELECT g.* FROM " + AsOf.table("person_grade", "g") + " JOIN " + AsOf.table("person", "p") + " ON p.id = g.person_id WHERE p.sheet_id = ? "
                + "ORDER BY g.from_date", rs -> {
                    dated.computeIfAbsent(rs.getLong("person_id"), k -> new ArrayList<>()).add(
                            new DatedGrade(rs.getObject("from_date", LocalDate.class), rs.getString("grade")));
                }, sheetId);
        Map<Long, List<Hold>> holds = new HashMap<>();
        jdbc.query("SELECT h.* FROM " + AsOf.table("person_hold", "h") + " JOIN " + AsOf.table("person", "p") + " ON p.id = h.person_id WHERE p.sheet_id = ? "
                + "ORDER BY h.from_date, h.id", rs -> {
                    holds.computeIfAbsent(rs.getLong("person_id"), k -> new ArrayList<>()).add(
                            new Hold(rs.getObject("from_date", LocalDate.class), rs.getObject("to_date", LocalDate.class)));
                }, sheetId);
        Map<Long, List<String>> aliases = new HashMap<>();
        jdbc.query("SELECT person_id, alias FROM " + AsOf.table("person_alias") + " WHERE sheet_id = ?", rs -> {
            aliases.computeIfAbsent(rs.getLong("person_id"), k -> new ArrayList<>()).add(rs.getString("alias"));
        }, sheetId);
        aliases.values().forEach(a -> a.sort(BY_NAME));
        return found.stream().map(p -> new Person(p.id(), p.sheetId(), p.name(), p.grade(), p.version(),
                p.retiredFrom(), holds.getOrDefault(p.id(), List.of()), dated.getOrDefault(p.id(), List.of()), p.terms(), p.contact(),
                List.copyOf(aliases.getOrDefault(p.id(), List.of())))).toList();
    }

    /** The person on the sheet with this name or alias, ignoring case and surrounding spaces; null when none. */
    public Person findByName(String sheetId, String name) {
        List<Person> found = jdbc.query("SELECT * FROM " + AsOf.table("person") + " WHERE sheet_id = ? AND (name_key = LOWER(?) "
                + "OR id IN (SELECT person_id FROM person_alias WHERE sheet_id = ? AND alias_key = LOWER(?)))", ROW,
                sheetId, name.trim(), sheetId, name.trim());
        return found.isEmpty() ? null : withDated(sheetId, found).get(0);
    }

    /**
     * The sheets of the sheet's tree: the sheet itself, its ancestors (its parent, their parent and so on) and its
     * descendants (the sheets naming it as their parent, their children and so on), each once. Not dated, like the parent.
     */
    public Set<String> tree(String sheetId) {
        Set<String> tree = new LinkedHashSet<>(List.of(sheetId));
        String at = sheetId;
        while (true) {
            List<String> parent = jdbc.queryForList("SELECT setting_value FROM setting WHERE sheet_id = ? AND setting_key = 'parent'",
                    String.class, at);
            if (parent.isEmpty() || parent.get(0).isBlank() || !tree.add(parent.get(0).trim())) {
                break;
            }
            at = parent.get(0).trim();
        }
        Deque<String> todo = new ArrayDeque<>(List.of(sheetId));
        while (!todo.isEmpty()) {
            for (String child : jdbc.queryForList("SELECT sheet_id FROM setting WHERE setting_key = 'parent' AND setting_value = ?",
                    String.class, todo.pop())) {
                if (tree.add(child)) {
                    todo.add(child);
                }
            }
        }
        return tree;
    }

    /** The lower-cased names and aliases of everyone on the sheet's tree but the person {@code exceptId}. */
    private Set<String> namesInTree(String sheetId, long exceptId) {
        Set<String> names = new HashSet<>();
        for (String sheet : tree(sheetId)) {
            names.addAll(jdbc.queryForList("SELECT LOWER(name) FROM " + AsOf.table("person") + " WHERE sheet_id = ? AND id <> ?",
                    String.class, sheet, exceptId));
            names.addAll(jdbc.queryForList("SELECT LOWER(alias) FROM " + AsOf.table("person_alias") + " WHERE sheet_id = ? AND person_id <> ?",
                    String.class, sheet, exceptId));
        }
        return names;
    }

    private static String key(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Whether another person on any sheet of the sheet's tree (not {@code exceptId}) has this name or alias, or anyone on the
     * sheet has it as an alias (the person themselves included), ignoring case.
     */
    public boolean nameTaken(String sheetId, String name, long exceptId) {
        return namesInTree(sheetId, exceptId).contains(key(name)) || aliasTaken(sheetId, name);
    }

    /** Whether anyone on the sheet has this alias, ignoring case. */
    private boolean aliasTaken(String sheetId, String alias) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM person_alias WHERE sheet_id = ? AND alias_key = LOWER(?)",
                Integer.class, sheetId, alias.trim());
        return n != null && n > 0;
    }

    /** Whether anyone on any sheet of the sheet's tree, the person themselves included, has this as a name or an alias, ignoring case. */
    public boolean nameOrAliasTaken(String sheetId, String alias) {
        return nameTaken(sheetId, alias, -1);
    }

    /** Whether any of the person's aliases is a name or alias of someone else on the sheet's tree, ignoring case. */
    public boolean aliasClashes(Person person) {
        Set<String> others = namesInTree(person.sheetId(), person.id());
        return person.aliases().stream().anyMatch(a -> others.contains(key(a)));
    }

    /**
     * The ids of the sheet's people who clash: whose name or any alias is a name or alias of someone else on the sheet's
     * tree, ignoring case (people who were on the sheet before a name was refused across its tree).
     */
    public Set<Long> clashing(String sheetId) {
        Map<String, Set<Long>> users = new HashMap<>();
        for (String sheet : tree(sheetId)) {
            jdbc.query("SELECT id, LOWER(name) FROM " + AsOf.table("person") + " WHERE sheet_id = ?",
                    rs -> { users.computeIfAbsent(rs.getString(2), k -> new HashSet<>()).add(rs.getLong(1)); }, sheet);
            jdbc.query("SELECT person_id, LOWER(alias) FROM " + AsOf.table("person_alias") + " WHERE sheet_id = ?",
                    rs -> { users.computeIfAbsent(rs.getString(2), k -> new HashSet<>()).add(rs.getLong(1)); }, sheet);
        }
        Set<Long> own = new HashSet<>(jdbc.queryForList("SELECT id FROM " + AsOf.table("person") + " WHERE sheet_id = ?", Long.class, sheetId));
        Set<Long> clashing = new HashSet<>();
        users.values().stream().filter(ids -> ids.size() > 1).forEach(ids -> ids.stream().filter(own::contains).forEach(clashing::add));
        return clashing;
    }

    /** Give the person the alias (kept without surrounding spaces). */
    public void alias(String sheetId, long id, String alias) {
        jdbc.update("INSERT INTO person_alias (person_id, sheet_id, alias) VALUES (?, ?, ?)", id, sheetId, alias);
    }

    /** Whether another person on the sheet (not {@code exceptId}) has this contact. */
    public boolean contactTaken(String sheetId, String contact, long exceptId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + AsOf.table("person") + " WHERE sheet_id = ? AND contact = ? AND id <> ?",
                Integer.class, sheetId, contact, exceptId);
        return n != null && n > 0;
    }

    public Person add(String sheetId, PersonRequest request) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade, terms, contact) VALUES (?, ?, ?, ?, ?)", new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setString(2, request.name());
            ps.setString(3, request.grade());
            ps.setObject(4, request.terms() == null ? null : Integer.valueOf(request.terms()), java.sql.Types.INTEGER);
            ps.setString(5, request.contact());
            return ps;
        }, key);
        return new Person(key.getKey().longValue(), sheetId, request.name(), request.grade(), 0, null, List.of(),
                List.of(), request.terms() == null ? null : Integer.valueOf(request.terms()), request.contact(), List.of());
    }

    /**
     * Store an edited person, whose version is one past the version it was loaded at. False, storing
     * nothing, when the person has been saved since it was loaded.
     */
    public boolean save(Person person) {
        return jdbc.update("UPDATE person SET name = ?, terms = ?, contact = ?, version = ? WHERE id = ? AND version = ?",
                person.name(), person.terms(), person.contact(), person.version(), person.id(), person.version() - 1) == 1;
    }

    /** Give the person the grade from the date on, replacing any grade they were given from that same date. */
    public void gradeFrom(long id, LocalDate from, String grade) {
        jdbc.update("DELETE FROM person_grade WHERE person_id = ? AND from_date = ?", id, Date.valueOf(from));
        jdbc.update("INSERT INTO person_grade (person_id, from_date, grade) VALUES (?, ?, ?)", id, Date.valueOf(from),
                grade);
    }

    /** Retire the person from the date (replacing any date they were retired from before). */
    public void retire(long id, LocalDate from) {
        jdbc.update("UPDATE person SET retired_from = ? WHERE id = ?", from, id);
    }

    /** Add a hold to the person's holds. */
    public void hold(long id, Hold hold) {
        jdbc.update("INSERT INTO person_hold (person_id, from_date, to_date) VALUES (?, ?, ?)", id,
                Date.valueOf(hold.from()), hold.to() == null ? null : Date.valueOf(hold.to()));
    }

    /**
     * Merge the person {@code from} into {@code into}: from's claims move to into (numbers and dates kept, unbound from
     * any statement, listed after into's claims in the order they were added); from's name and aliases become into's
     * aliases; from's patrons are added to into's and anyone naming from names into instead (never themselves, no
     * repeats); from's pool memberships and closed-period figures move to into (added to into's own where into has
     * one). from's statements, holds, watchers, grades and contact are dropped, and from leaves the sheet.
     */
    public void merge(String sheetId, long from, long into) {
        for (Long claim : jdbc.queryForList("SELECT id FROM claim WHERE person_id = ? ORDER BY listed, id", Long.class, from)) {
            jdbc.update("UPDATE claim SET person_id = ?, stated = FALSE, stated_due = NULL, listed = "
                    + "(SELECT COALESCE(MAX(listed), 0) + 1 FROM claim WHERE sheet_id = ?) WHERE id = ?", into, sheetId, claim);
        }
        String name = jdbc.queryForObject("SELECT name FROM person WHERE id = ?", String.class, from);
        jdbc.update("UPDATE person_alias SET person_id = ? WHERE person_id = ?", into, from);
        for (Long patron : jdbc.queryForList("SELECT patron_id FROM person_patron WHERE person_id = ? ORDER BY named", Long.class, from)) {
            linkPatron(into, patron);
        }
        for (Long person : jdbc.queryForList("SELECT person_id FROM person_patron WHERE patron_id = ?", Long.class, from)) {
            linkPatron(person, into);
        }
        jdbc.update("DELETE FROM person_patron WHERE person_id = ? OR patron_id = ?", from, from);
        for (Map<String, Object> m : jdbc.queryForList("SELECT pool_name, share FROM pool_member WHERE sheet_id = ? AND person_id = ?",
                sheetId, from)) {
            if (jdbc.update("UPDATE pool_member SET share = share + ? WHERE sheet_id = ? AND pool_name = ? AND person_id = ?",
                    m.get("share"), sheetId, m.get("pool_name"), into) == 0) {
                jdbc.update("UPDATE pool_member SET person_id = ? WHERE sheet_id = ? AND pool_name = ? AND person_id = ?",
                        into, sheetId, m.get("pool_name"), from);
            }
        }
        jdbc.update("DELETE FROM pool_member WHERE sheet_id = ? AND person_id = ?", sheetId, from);
        for (Map<String, Object> m : jdbc.queryForList("SELECT period_name, total FROM period_person_total WHERE sheet_id = ? AND person_id = ?",
                sheetId, from)) {
            if (jdbc.update("UPDATE period_person_total SET total = total + ? WHERE sheet_id = ? AND period_name = ? AND person_id = ?",
                    m.get("total"), sheetId, m.get("period_name"), into) == 0) {
                jdbc.update("UPDATE period_person_total SET person_id = ? WHERE sheet_id = ? AND period_name = ? AND person_id = ?",
                        into, sheetId, m.get("period_name"), from);
            }
        }
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND person_id = ?", sheetId, from);
        jdbc.update("DELETE FROM statement WHERE person_id = ?", from);
        jdbc.update("DELETE FROM person_hold WHERE person_id = ?", from);
        jdbc.update("DELETE FROM person_watch WHERE person_id = ?", from);
        jdbc.update("DELETE FROM person_grade WHERE person_id = ?", from);
        jdbc.update("DELETE FROM person WHERE id = ?", from);
        alias(sheetId, into, name);
    }

    /**
     * Transfer the person to the sheet {@code to}, returning their id there. They arrive with their first grade and every
     * grade given since (with its from-date), their terms, contact and aliases, and their claims (listed in the order
     * they were, as the newest on that sheet) with their status, dates, notes, frozen figures, comments and logs; a
     * claim in {@code numbers} takes the number given there. The figures a statement bound and the references are not
     * carried. They leave their sheet entirely: their claims, statements, holds, watches, patron links, pool memberships
     * and closed-period figures are dropped with them.
     */
    public long transfer(Person person, String to, Map<Long, String> numbers) {
        long from = person.id();
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade, terms, contact) VALUES (?, ?, ?, ?, ?)", new String[] { "id" });
            ps.setString(1, to);
            ps.setString(2, person.name());
            ps.setString(3, person.grade());
            ps.setObject(4, person.terms(), java.sql.Types.INTEGER);
            ps.setString(5, person.contact());
            return ps;
        }, key);
        long arrived = key.getKey().longValue();
        jdbc.update("INSERT INTO person_grade (person_id, from_date, grade) SELECT ?, from_date, grade FROM person_grade "
                + "WHERE person_id = ? ORDER BY from_date", arrived, from);
        jdbc.update("INSERT INTO person_alias (person_id, sheet_id, alias) SELECT ?, ?, alias FROM person_alias "
                + "WHERE person_id = ? ORDER BY id", arrived, to, from);
        List<Long> claims = jdbc.queryForList("SELECT id FROM claim WHERE person_id = ? ORDER BY listed, id", Long.class, from);
        for (Long claim : claims) {
            KeyHolder claimKey = new GeneratedKeyHolder();
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, status, claim_number, settled_due, "
                                + "opened_by, corrects, due_by, deferred_until, proofs, split_from, late_entry, reminded_on, tags, "
                                + "void_reason, edited_by, due_terms, listed) "
                                + "SELECT ?, ?, amount, claim_date, note, status, ?, settled_due, opened_by, corrects, due_by, "
                                + "deferred_until, proofs, split_from, late_entry, reminded_on, tags, void_reason, edited_by, due_terms, "
                                + "(SELECT COALESCE(MAX(listed), 0) + 1 FROM claim WHERE sheet_id = ?) FROM claim WHERE id = ?",
                        new String[] { "id" });
                ps.setString(1, to);
                ps.setLong(2, arrived);
                ps.setString(3, numbers.get(claim));
                ps.setString(4, to);
                ps.setLong(5, claim);
                return ps;
            }, claimKey);
            long moved = claimKey.getKey().longValue();
            jdbc.update("INSERT INTO claim_log (sheet_id, claim_id, status, by_user, logged_on, due) "
                    + "SELECT ?, ?, status, by_user, logged_on, due FROM claim_log WHERE claim_id = ? ORDER BY id", to, moved, claim);
            jdbc.update("INSERT INTO claim_comment (sheet_id, claim_id, by_user, commented_on, text) "
                    + "SELECT ?, ?, by_user, commented_on, text FROM claim_comment WHERE claim_id = ? ORDER BY id", to, moved, claim);
        }
        for (Long claim : claims) {
            jdbc.update("DELETE FROM claim_log WHERE claim_id = ?", claim);
            jdbc.update("DELETE FROM claim_comment WHERE claim_id = ?", claim);
            jdbc.update("DELETE FROM claim_lock WHERE claim_id = ?", claim);
            jdbc.update("DELETE FROM claim_batch WHERE claim_id = ?", claim);
            jdbc.update("DELETE FROM claim WHERE id = ?", claim);
        }
        jdbc.update("DELETE FROM person_patron WHERE person_id = ? OR patron_id = ?", from, from);
        jdbc.update("DELETE FROM pool_member WHERE sheet_id = ? AND person_id = ?", person.sheetId(), from);
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND person_id = ?", person.sheetId(), from);
        jdbc.update("DELETE FROM statement WHERE person_id = ?", from);
        jdbc.update("DELETE FROM person_hold WHERE person_id = ?", from);
        jdbc.update("DELETE FROM person_watch WHERE person_id = ?", from);
        jdbc.update("DELETE FROM person_grade WHERE person_id = ?", from);
        jdbc.update("DELETE FROM person_alias WHERE person_id = ?", from);
        jdbc.update("DELETE FROM person WHERE id = ?", from);
        return arrived;
    }

    /** Have the person name the patron, unless it is themselves or already named. */
    private void linkPatron(long person, long patron) {
        if (person != patron) {
            jdbc.update("MERGE INTO person_patron (person_id, patron_id) KEY (person_id, patron_id) VALUES (?, ?)",
                    person, patron);
        }
    }
}

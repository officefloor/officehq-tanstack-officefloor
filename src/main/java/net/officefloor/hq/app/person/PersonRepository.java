package net.officefloor.hq.app.person;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.history.HistoryRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PersonRepository {

    /** The grade in force on the sheet's today: the one with the latest from-date on or before it. */
    private static final String GRADE_TODAY = "(SELECT g.grade FROM person_grade g WHERE g.person_id = person.id"
            + " AND (g.from_date IS NULL OR g.from_date <= (SELECT s.today FROM sheet s WHERE s.id = person.sheet_id))"
            + " ORDER BY g.from_date DESC NULLS LAST LIMIT 1) AS grade";

    private static final String COLUMNS = "id, name, " + GRADE_TODAY + ", version, statement_count, stated_total, statement_date, retired_from, terms, contact, origin_sheet";

    private static final RowMapper<Person> ROW = (rs, i) -> new Person(rs.getLong("id"), rs.getString("name"),
            rs.getString("grade"), rs.getLong("version"), rs.getLong("statement_count"), rs.getString("stated_total"),
            rs.getDate("statement_date") == null ? null : rs.getDate("statement_date").toLocalDate().toString(),
            rs.getDate("retired_from") == null ? null : rs.getDate("retired_from").toLocalDate().toString(),
            false, List.of(), List.of(), (Integer) rs.getObject("terms", Integer.class), rs.getString("contact"),
            rs.getString("origin_sheet"), List.of());

    /** The position after the last person's on the sheet being inserted into (the end of its list). */
    private static final String NEXT_POSITION = "(SELECT COALESCE(MAX(q.list_position), 0) + 1 FROM person q WHERE q.sheet_id = sheet.id)";

    private final JdbcTemplate jdbc;
    private final HistoryRepository history;

    public PersonRepository(JdbcTemplate jdbc, HistoryRepository history) {
        this.jdbc = jdbc;
        this.history = history;
    }

    public boolean sheetExists(String sheetId) {
        Integer known = jdbc.queryForObject("SELECT COUNT(*) FROM sheet WHERE id = ?", Integer.class, sheetId);
        return known != null && known > 0;
    }

    /** The sheet's people in list order (by their position; new people go to the end). */
    public List<Person> list(String sheetId) {
        List<Person> people = jdbc.query("SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? ORDER BY list_position, id", ROW, sheetId);
        Map<Long, List<GradeFrom>> grades = new HashMap<>();
        jdbc.query("SELECT person_id, grade, from_date FROM person_grade WHERE sheet_id = ? ORDER BY from_date NULLS FIRST",
                rs -> {
                    grades.computeIfAbsent(rs.getLong("person_id"), k -> new ArrayList<>()).add(gradeFrom(rs));
                }, sheetId);
        Map<Long, List<Hold>> holds = holds(sheetId);
        Map<Long, List<Retirement>> retirements = new HashMap<>();
        jdbc.query("SELECT person_id, from_date, to_date FROM person_retirement WHERE sheet_id = ? ORDER BY id",
                rs -> {
                    retirements.computeIfAbsent(rs.getLong("person_id"), k -> new ArrayList<>()).add(retirement(rs));
                }, sheetId);
        return people.stream().map(p -> p.withGrades(grades.getOrDefault(p.id(), List.of()))
                .withHolds(holds.getOrDefault(p.id(), List.of()))
                .withRetirements(retirements.getOrDefault(p.id(), List.of()))).toList();
    }

    private static Retirement retirement(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Retirement(rs.getDate("from_date").toLocalDate().toString(), rs.getDate("to_date").toLocalDate().toString());
    }

    /** A person's past retirements, oldest first. */
    public List<Retirement> retirements(String sheetId, long id) {
        return jdbc.query("SELECT from_date, to_date FROM person_retirement WHERE sheet_id = ? AND person_id = ? ORDER BY id",
                (rs, i) -> retirement(rs), sheetId, id);
    }

    private static Hold hold(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Hold(rs.getDate("from_date") == null ? null : rs.getDate("from_date").toLocalDate().toString(),
                rs.getDate("to_date") == null ? null : rs.getDate("to_date").toLocalDate().toString());
    }

    /** The holds of the sheet's people, by person id, each person's oldest first. */
    public Map<Long, List<Hold>> holds(String sheetId) {
        Map<Long, List<Hold>> holds = new HashMap<>();
        jdbc.query("SELECT person_id, from_date, to_date FROM person_hold WHERE sheet_id = ? ORDER BY id",
                rs -> {
                    holds.computeIfAbsent(rs.getLong("person_id"), k -> new ArrayList<>()).add(hold(rs));
                }, sheetId);
        return holds;
    }

    /** A person's holds, oldest first. */
    public List<Hold> holds(String sheetId, long id) {
        return jdbc.query("SELECT from_date, to_date FROM person_hold WHERE sheet_id = ? AND person_id = ? ORDER BY id",
                (rs, i) -> hold(rs), sheetId, id);
    }

    private static GradeFrom gradeFrom(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new GradeFrom(rs.getString("grade"),
                rs.getDate("from_date") == null ? null : rs.getDate("from_date").toLocalDate().toString());
    }

    /** A person's grades, oldest first. */
    public List<GradeFrom> grades(String sheetId, long id) {
        return jdbc.query("SELECT grade, from_date FROM person_grade WHERE sheet_id = ? AND person_id = ?"
                + " ORDER BY from_date NULLS FIRST", (rs, i) -> gradeFrom(rs), sheetId, id);
    }

    /** Give a person a grade from a from-date (null: from the beginning of time), replacing the one
     *  they had from that date. */
    public void grade(String sheetId, long id, String grade, LocalDate from) {
        Date on = from == null ? null : Date.valueOf(from);
        jdbc.update("DELETE FROM person_grade WHERE sheet_id = ? AND person_id = ? AND from_date IS NOT DISTINCT FROM ?",
                sheetId, id, on);
        jdbc.update("INSERT INTO person_grade (sheet_id, person_id, grade, from_date) VALUES (?, ?, ?, ?)",
                sheetId, id, grade, on);
        history.grade(sheetId, id, grade, from);
    }

    /** A person on the sheet; null when the sheet has no such person. */
    public Person find(String sheetId, long id) {
        List<Person> found = jdbc.query("SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? AND id = ?", ROW,
                sheetId, id);
        return found.isEmpty() ? null : found.get(0).withGrades(grades(sheetId, id)).withHolds(holds(sheetId, id))
                .withRetirements(retirements(sheetId, id));
    }

    /** Record a statement taken of a person: one more, with its due-total and date. */
    public void stated(String sheetId, long id, String total, LocalDate date) {
        jdbc.update("UPDATE person SET statement_count = statement_count + 1, stated_total = ?, statement_date = ?"
                + " WHERE id = ? AND sheet_id = ?", total, Date.valueOf(date), id, sheetId);
        jdbc.update("INSERT INTO person_statement (sheet_id, person_id, seq, due_total, statement_date)"
                + " SELECT sheet_id, id, statement_count, stated_total, statement_date FROM person WHERE id = ? AND sheet_id = ?",
                id, sheetId);
    }

    /** Cancel a person's last statement: one fewer, showing the due-total and date of the one before it
     *  (none when it was the first, or was taken before statements were kept). */
    public void unstated(String sheetId, long id) {
        jdbc.update("DELETE FROM person_statement WHERE sheet_id = ? AND person_id = ?"
                + " AND seq = (SELECT statement_count FROM person WHERE id = ? AND sheet_id = ?)", sheetId, id, id, sheetId);
        jdbc.update("UPDATE person SET statement_count = statement_count - 1,"
                + " stated_total = (SELECT s.due_total FROM person_statement s WHERE s.sheet_id = person.sheet_id"
                + " AND s.person_id = person.id AND s.seq = person.statement_count - 1),"
                + " statement_date = (SELECT s.statement_date FROM person_statement s WHERE s.sheet_id = person.sheet_id"
                + " AND s.person_id = person.id AND s.seq = person.statement_count - 1)"
                + " WHERE id = ? AND sheet_id = ? AND statement_count > 0", id, sheetId);
    }

    /** Retire a person from a date. */
    public void retire(String sheetId, long id, LocalDate from) {
        jdbc.update("UPDATE person SET retired_from = ? WHERE id = ? AND sheet_id = ?", Date.valueOf(from), id, sheetId);
        history.retire(sheetId, id, from);
    }

    /** Unretire a person on a day: they are no longer retired, and their retirement from {@code from} is kept
     *  as a past one to that day. */
    public void unretire(String sheetId, long id, LocalDate from, LocalDate on) {
        jdbc.update("UPDATE person SET retired_from = NULL WHERE id = ? AND sheet_id = ?", id, sheetId);
        jdbc.update("INSERT INTO person_retirement (sheet_id, person_id, from_date, to_date) VALUES (?, ?, ?, ?)",
                sheetId, id, Date.valueOf(from), Date.valueOf(on));
        history.retire(sheetId, id, null);
    }

    /** Put a person on hold from a from-date to a to-date (null: open-ended). */
    public void hold(String sheetId, long id, LocalDate from, LocalDate to) {
        jdbc.update("INSERT INTO person_hold (sheet_id, person_id, from_date, to_date, held_on)"
                + " SELECT ?, ?, ?, ?, today FROM sheet WHERE id = ?",
                sheetId, id, Date.valueOf(from), to == null ? null : Date.valueOf(to), sheetId);
    }

    /** Refuse a claim of the person dated after the date they are retired from, or after the from-date and
     *  before the to-date of one of their past retirements. */
    public void claimAllowed(String sheetId, long id, LocalDate date) throws BadRequestException {
        Person p = find(sheetId, id);
        if (p != null && p.retiredFrom() != null && date.isAfter(LocalDate.parse(p.retiredFrom()))) {
            throw new BadRequestException(p.name() + " is retired from " + p.retiredFrom());
        }
        if (p != null) {
            for (Retirement r : p.retirements()) {
                if (date.isAfter(LocalDate.parse(r.from())) && date.isBefore(LocalDate.parse(r.to()))) {
                    throw new BadRequestException(p.name() + " was retired from " + r.from() + " to " + r.to());
                }
            }
        }
    }

    /** Whether another person on the sheet (other than {@code exceptId}; 0 for none) already has this name,
     *  or anyone on the sheet has it as an alias, ignoring case; or anyone (other than {@code exceptId}) on another
     *  sheet of its tree has it as a name or alias (see {@link #tree}). */
    public boolean nameTaken(String sheetId, String name, long exceptId) {
        Integer taken = jdbc.queryForObject(
                "SELECT (SELECT COUNT(*) FROM person WHERE sheet_id = ? AND LOWER(name) = LOWER(?) AND id <> ?)"
                        + " + (SELECT COUNT(*) FROM person_alias WHERE sheet_id = ? AND LOWER(alias) = LOWER(?))",
                Integer.class, sheetId, name, exceptId, sheetId, name);
        return (taken != null && taken > 0) || takenInTree(sheetId, name, exceptId);
    }

    /** Whether anyone (other than {@code exceptId}; 0 for none) on another sheet of the sheet's tree has this name
     *  or alias, ignoring case. */
    public boolean takenInTree(String sheetId, String name, long exceptId) {
        for (String other : tree(sheetId)) {
            Integer taken = jdbc.queryForObject(
                    "SELECT (SELECT COUNT(*) FROM PUBLIC.person WHERE sheet_id = ? AND LOWER(name) = LOWER(?) AND id <> ?)"
                            + " + (SELECT COUNT(*) FROM PUBLIC.person_alias WHERE sheet_id = ? AND LOWER(alias) = LOWER(?)"
                            + " AND person_id <> ?)",
                    Integer.class, other, name, exceptId, other, name, exceptId);
            if (taken != null && taken > 0) return true;
        }
        return false;
    }

    /** Whether any of the person's aliases is a name or alias on another sheet of the sheet's tree. */
    public boolean aliasClashes(String sheetId, long id) {
        for (String alias : jdbc.queryForList("SELECT alias FROM PUBLIC.person_alias WHERE sheet_id = ? AND person_id = ?",
                String.class, sheetId, id)) {
            if (takenInTree(sheetId, alias, id)) return true;
        }
        return false;
    }

    /** The other sheets of the sheet's tree, among the sheets that exist: every sheet under its topmost ancestor
     *  (the ancestor itself included), following parents and children, each once and never the sheet itself. */
    public List<String> tree(String sheetId) {
        java.util.LinkedHashSet<String> found = new java.util.LinkedHashSet<>();
        String root = sheetId;
        java.util.Set<String> seen = new java.util.HashSet<>(List.of(sheetId));
        for (String at = parent(sheetId); at != null && seen.add(at); at = parent(at)) {
            root = at;
        }
        java.util.Deque<String> pending = new java.util.ArrayDeque<>(List.of(root));
        found.add(root);
        while (!pending.isEmpty()) {
            for (String child : jdbc.queryForList("SELECT id FROM PUBLIC.sheet WHERE parent_id = ? ORDER BY id",
                    String.class, pending.poll())) {
                if (found.add(child)) pending.add(child);
            }
        }
        found.remove(sheetId);
        return new ArrayList<>(found);
    }

    private String parent(String sheetId) {
        List<String> found = jdbc.query("SELECT parent_id FROM PUBLIC.sheet WHERE id = ?",
                (rs, i) -> rs.getString("parent_id"), sheetId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The ids of the sheet's people whose name or an alias is a name or alias on another sheet of its tree. */
    public List<Long> clashing(String sheetId) {
        List<String> others = tree(sheetId);
        if (others.isEmpty()) return List.of();
        java.util.Set<String> keys = new java.util.HashSet<>();
        for (String other : others) {
            keys.addAll(jdbc.queryForList("SELECT LOWER(name) FROM PUBLIC.person WHERE sheet_id = ?", String.class, other));
            keys.addAll(jdbc.queryForList("SELECT LOWER(alias) FROM PUBLIC.person_alias WHERE sheet_id = ?", String.class, other));
        }
        java.util.Set<Long> clash = new java.util.LinkedHashSet<>();
        jdbc.query("SELECT id, LOWER(name) AS k FROM PUBLIC.person WHERE sheet_id = ? ORDER BY id", rs -> {
            if (keys.contains(rs.getString("k"))) clash.add(rs.getLong("id"));
        }, sheetId);
        jdbc.query("SELECT person_id, LOWER(alias) AS k FROM PUBLIC.person_alias WHERE sheet_id = ?", rs -> {
            if (keys.contains(rs.getString("k"))) clash.add(rs.getLong("person_id"));
        }, sheetId);
        return new ArrayList<>(clash);
    }

    /** The person on the sheet with this name or alias, ignoring case; null when there is none. */
    public Person findByName(String sheetId, String name) {
        List<Person> found = jdbc.query("SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? AND (LOWER(name) = LOWER(?)"
                + " OR id IN (SELECT a.person_id FROM person_alias a WHERE a.sheet_id = ? AND LOWER(a.alias) = LOWER(?)))"
                + " ORDER BY id", ROW, sheetId, name, sheetId, name);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Whether another person on the sheet (other than {@code exceptId}; 0 for none) already has this contact. */
    public boolean contactTaken(String sheetId, String contact, long exceptId) {
        Integer taken = jdbc.queryForObject("SELECT COUNT(*) FROM person WHERE sheet_id = ? AND contact = ? AND id <> ?",
                Integer.class, sheetId, contact, exceptId);
        return taken != null && taken > 0;
    }

    public Person add(String sheetId, String name, String grade, Integer terms) {
        return add(sheetId, name, grade, terms, null);
    }

    /** Add a person with their own terms and contact (each null: none). */
    public Person add(String sheetId, String name, String grade, Integer terms, String contact) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade, terms, contact, added_on, list_position)"
                            + " SELECT ?, ?, ?, ?, ?, today, " + NEXT_POSITION + " FROM sheet WHERE id = ?", new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setString(2, name);
            ps.setString(3, grade);
            ps.setObject(4, terms, java.sql.Types.INTEGER);
            ps.setString(5, contact);
            ps.setString(6, sheetId);
            return ps;
        }, keys);
        long id = keys.getKey().longValue();
        grade(sheetId, id, grade, null);
        return new Person(id, name, grade, 1, 0, null, null, null, false, List.of(new GradeFrom(grade, null)), List.of(), terms, contact, null, List.of());
    }

    /** Swap two people's places in the sheet's list. */
    public void swapPositions(String sheetId, long id, long otherId) {
        Long mine = position(sheetId, id);
        Long theirs = position(sheetId, otherId);
        jdbc.update("UPDATE person SET list_position = ? WHERE id = ? AND sheet_id = ?", theirs, id, sheetId);
        jdbc.update("UPDATE person SET list_position = ? WHERE id = ? AND sheet_id = ?", mine, otherId, sheetId);
    }

    private Long position(String sheetId, long id) {
        return jdbc.queryForObject("SELECT list_position FROM person WHERE id = ? AND sheet_id = ?", Long.class, id, sheetId);
    }

    /** Whether the sheet has this person. */
    public boolean exists(String sheetId, long id) {
        Integer known = jdbc.queryForObject("SELECT COUNT(*) FROM person WHERE id = ? AND sheet_id = ?",
                Integer.class, id, sheetId);
        return known != null && known > 0;
    }

    public boolean update(String sheetId, long id, long version, String name, Integer terms) {
        return jdbc.update(
                "UPDATE person SET name = ?, terms = ?, version = version + 1 WHERE id = ? AND sheet_id = ? AND version = ?",
                name, terms, id, sheetId, version) > 0;
    }

    /** Change a person's name, own terms and contact (each null: none), if they are still at {@code version}, moving them to the next version;
     *  false when the sheet has no such person at that version. Their grades are given by {@link #grade}. */
    public boolean update(String sheetId, long id, long version, String name, Integer terms, String contact) {
        return jdbc.update(
                "UPDATE person SET name = ?, terms = ?, contact = ?, version = version + 1 WHERE id = ? AND sheet_id = ? AND version = ?",
                name, terms, contact, id, sheetId, version) > 0;
    }

    /** Copy a person onto another sheet, as added there on its today: their name, the grade they were first given,
     *  every grade they were given (with its from-date), their terms, contact and aliases (in the order they were
     *  added), with no statements, and the sheet they came from; the new person's id. Their claims are moved by the caller. */
    public long transferIn(String fromSheet, long id, String toSheet) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade, terms, contact, added_on, list_position, origin_sheet)"
                            + " SELECT ?, p.name, p.grade, p.terms, p.contact, (SELECT s.today FROM sheet s WHERE s.id = ?),"
                            + " (SELECT COALESCE(MAX(q.list_position), 0) + 1 FROM person q WHERE q.sheet_id = ?), ?"
                            + " FROM person p WHERE p.id = ? AND p.sheet_id = ?", new String[] { "id" });
            ps.setString(1, toSheet);
            ps.setString(2, toSheet);
            ps.setString(3, toSheet);
            ps.setString(4, fromSheet);
            ps.setLong(5, id);
            ps.setString(6, fromSheet);
            return ps;
        }, keys);
        long arrived = keys.getKey().longValue();
        for (GradeFrom g : grades(fromSheet, id)) {
            grade(toSheet, arrived, g.grade(), g.from() == null ? null : LocalDate.parse(g.from()));
        }
        jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias) SELECT ?, ?, alias FROM person_alias"
                + " WHERE sheet_id = ? AND person_id = ? ORDER BY id", toSheet, arrived, fromSheet, id);
        return arrived;
    }

    /** Take a person off the sheet entirely: their patron links (both ways), pool memberships, closed periods'
     *  figures, watchers, holds, past retirements, statements, grades and aliases go with them. Their claims must already be gone. */
    public void remove(String sheetId, long id) {
        jdbc.update("DELETE FROM patron WHERE sheet_id = ? AND (person_id = ? OR patron_id = ?)", sheetId, id, id);
        jdbc.update("DELETE FROM pool_member WHERE sheet_id = ? AND person_id = ?", sheetId, id);
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND person_id = ?", sheetId, id);
        jdbc.update("DELETE FROM person_watch WHERE sheet_id = ? AND person_id = ?", sheetId, id);
        jdbc.update("DELETE FROM person_hold WHERE sheet_id = ? AND person_id = ?", sheetId, id);
        jdbc.update("DELETE FROM person_retirement WHERE sheet_id = ? AND person_id = ?", sheetId, id);
        jdbc.update("DELETE FROM person_statement WHERE sheet_id = ? AND person_id = ?", sheetId, id);
        jdbc.update("DELETE FROM person_grade WHERE sheet_id = ? AND person_id = ?", sheetId, id);
        jdbc.update("DELETE FROM person_alias WHERE sheet_id = ? AND person_id = ?", sheetId, id);
        jdbc.update("DELETE FROM person WHERE id = ? AND sheet_id = ?", id, sheetId);
    }

    /** Merge one person into another: the merged name and aliases become the other's aliases (name first); the
     *  merged person's patrons are added to the other's (not themselves, no repeats), anyone naming the merged
     *  person names the other instead (no repeats); pool memberships move to the other (shares added, at most
     *  100.00, when the other is already a member); closed periods' figures are added to the other's. Their
     *  statements, holds, past retirements, grades, watchers and contact go with them, and the merged person leaves the sheet.
     *  Their claims must already have been moved. */
    public void merge(String sheetId, long fromId, long intoId) {
        String name = jdbc.queryForObject("SELECT name FROM person WHERE id = ? AND sheet_id = ?", String.class, fromId, sheetId);
        List<String> aliases = jdbc.queryForList("SELECT alias FROM person_alias WHERE sheet_id = ? AND person_id = ? ORDER BY id",
                String.class, sheetId, fromId);
        jdbc.update("DELETE FROM person_alias WHERE sheet_id = ? AND person_id = ?", sheetId, fromId);
        jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias) VALUES (?, ?, ?)", sheetId, intoId, name);
        for (String alias : aliases) {
            jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias) VALUES (?, ?, ?)", sheetId, intoId, alias);
        }

        // The merged person's patrons become the other's.
        jdbc.update("INSERT INTO patron (sheet_id, person_id, patron_id) SELECT sheet_id, ?, patron_id FROM patron p"
                + " WHERE p.sheet_id = ? AND p.person_id = ? AND p.patron_id <> ? AND NOT EXISTS (SELECT 1 FROM patron q"
                + " WHERE q.person_id = ? AND q.patron_id = p.patron_id) ORDER BY p.seq", intoId, sheetId, fromId, intoId, intoId);
        jdbc.update("DELETE FROM patron WHERE sheet_id = ? AND person_id = ?", sheetId, fromId);
        // Anyone naming the merged person names the other instead.
        jdbc.update("INSERT INTO patron (sheet_id, person_id, patron_id, seq) SELECT sheet_id, person_id, ?, seq FROM patron p"
                + " WHERE p.sheet_id = ? AND p.patron_id = ? AND p.person_id <> ? AND NOT EXISTS (SELECT 1 FROM patron q"
                + " WHERE q.person_id = p.person_id AND q.patron_id = ?)", intoId, sheetId, fromId, intoId, intoId);
        jdbc.update("DELETE FROM patron WHERE sheet_id = ? AND patron_id = ?", sheetId, fromId);

        // Pool memberships move; shares are added where the other is already a member.
        jdbc.update("UPDATE pool_member m SET share = LEAST(100.00, share + (SELECT f.share FROM pool_member f"
                + " WHERE f.sheet_id = m.sheet_id AND f.pool_name = m.pool_name AND f.person_id = ?))"
                + " WHERE m.sheet_id = ? AND m.person_id = ? AND EXISTS (SELECT 1 FROM pool_member f"
                + " WHERE f.sheet_id = m.sheet_id AND f.pool_name = m.pool_name AND f.person_id = ?)",
                fromId, sheetId, intoId, fromId);
        jdbc.update("DELETE FROM pool_member m WHERE m.sheet_id = ? AND m.person_id = ? AND EXISTS (SELECT 1 FROM"
                + " pool_member o WHERE o.sheet_id = m.sheet_id AND o.pool_name = m.pool_name AND o.person_id = ?)",
                sheetId, fromId, intoId);
        jdbc.update("UPDATE pool_member SET person_id = ? WHERE sheet_id = ? AND person_id = ?", intoId, sheetId, fromId);

        // A closed period's figure follows the claims.
        jdbc.update("UPDATE period_person_total t SET total = total + (SELECT f.total FROM period_person_total f"
                + " WHERE f.period_id = t.period_id AND f.person_id = ?) WHERE t.sheet_id = ? AND t.person_id = ?"
                + " AND EXISTS (SELECT 1 FROM period_person_total f WHERE f.period_id = t.period_id AND f.person_id = ?)",
                fromId, sheetId, intoId, fromId);
        jdbc.update("DELETE FROM period_person_total t WHERE t.sheet_id = ? AND t.person_id = ? AND EXISTS (SELECT 1"
                + " FROM period_person_total o WHERE o.period_id = t.period_id AND o.person_id = ?)", sheetId, fromId, intoId);
        jdbc.update("UPDATE period_person_total SET person_id = ? WHERE sheet_id = ? AND person_id = ?", intoId, sheetId, fromId);

        jdbc.update("DELETE FROM person_watch WHERE sheet_id = ? AND person_id = ?", sheetId, fromId);
        jdbc.update("DELETE FROM person_hold WHERE sheet_id = ? AND person_id = ?", sheetId, fromId);
        jdbc.update("DELETE FROM person_retirement WHERE sheet_id = ? AND person_id = ?", sheetId, fromId);
        jdbc.update("DELETE FROM person_statement WHERE sheet_id = ? AND person_id = ?", sheetId, fromId);
        jdbc.update("DELETE FROM person_grade WHERE sheet_id = ? AND person_id = ?", sheetId, fromId);
        jdbc.update("DELETE FROM person WHERE id = ? AND sheet_id = ?", fromId, sheetId);
    }
}

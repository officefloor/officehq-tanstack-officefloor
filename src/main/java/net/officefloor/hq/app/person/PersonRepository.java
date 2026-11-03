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

    private static final String COLUMNS = "id, name, " + GRADE_TODAY + ", version, statement_count, stated_total, statement_date, retired_from, terms, contact";

    private static final RowMapper<Person> ROW = (rs, i) -> new Person(rs.getLong("id"), rs.getString("name"),
            rs.getString("grade"), rs.getLong("version"), rs.getLong("statement_count"), rs.getString("stated_total"),
            rs.getDate("statement_date") == null ? null : rs.getDate("statement_date").toLocalDate().toString(),
            rs.getDate("retired_from") == null ? null : rs.getDate("retired_from").toLocalDate().toString(),
            false, List.of(), List.of(), (Integer) rs.getObject("terms", Integer.class), rs.getString("contact"));

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

    /** The sheet's people in the order they were added. */
    public List<Person> list(String sheetId) {
        List<Person> people = jdbc.query("SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? ORDER BY id", ROW, sheetId);
        Map<Long, List<GradeFrom>> grades = new HashMap<>();
        jdbc.query("SELECT person_id, grade, from_date FROM person_grade WHERE sheet_id = ? ORDER BY from_date NULLS FIRST",
                rs -> {
                    grades.computeIfAbsent(rs.getLong("person_id"), k -> new ArrayList<>()).add(gradeFrom(rs));
                }, sheetId);
        Map<Long, List<Hold>> holds = holds(sheetId);
        return people.stream().map(p -> p.withGrades(grades.getOrDefault(p.id(), List.of()))
                .withHolds(holds.getOrDefault(p.id(), List.of()))).toList();
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
        return found.isEmpty() ? null : found.get(0).withGrades(grades(sheetId, id)).withHolds(holds(sheetId, id));
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

    /** Put a person on hold from a from-date to a to-date (null: open-ended). */
    public void hold(String sheetId, long id, LocalDate from, LocalDate to) {
        jdbc.update("INSERT INTO person_hold (sheet_id, person_id, from_date, to_date, held_on)"
                + " SELECT ?, ?, ?, ?, today FROM sheet WHERE id = ?",
                sheetId, id, Date.valueOf(from), to == null ? null : Date.valueOf(to), sheetId);
    }

    /** Refuse a claim of the person dated after the date they are retired from. */
    public void claimAllowed(String sheetId, long id, LocalDate date) throws BadRequestException {
        Person p = find(sheetId, id);
        if (p != null && p.retiredFrom() != null && date.isAfter(LocalDate.parse(p.retiredFrom()))) {
            throw new BadRequestException(p.name() + " is retired from " + p.retiredFrom());
        }
    }

    /** Whether another person on the sheet (other than {@code exceptId}; 0 for none) already has this name,
     *  or anyone on the sheet has it as an alias, ignoring case. */
    public boolean nameTaken(String sheetId, String name, long exceptId) {
        Integer taken = jdbc.queryForObject(
                "SELECT (SELECT COUNT(*) FROM person WHERE sheet_id = ? AND LOWER(name) = LOWER(?) AND id <> ?)"
                        + " + (SELECT COUNT(*) FROM person_alias WHERE sheet_id = ? AND LOWER(alias) = LOWER(?))",
                Integer.class, sheetId, name, exceptId, sheetId, name);
        return taken != null && taken > 0;
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
                    "INSERT INTO person (sheet_id, name, grade, terms, contact, added_on)"
                            + " SELECT ?, ?, ?, ?, ?, today FROM sheet WHERE id = ?", new String[] { "id" });
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
        return new Person(id, name, grade, 1, 0, null, null, null, false, List.of(new GradeFrom(grade, null)), List.of(), terms, contact);
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

    /** Merge one person into another: the merged name and aliases become the other's aliases (name first); the
     *  merged person's patrons are added to the other's (not themselves, no repeats), anyone naming the merged
     *  person names the other instead (no repeats); pool memberships move to the other (shares added, at most
     *  100.00, when the other is already a member); closed periods' figures are added to the other's. Their
     *  statements, holds, grades, watchers and contact go with them, and the merged person leaves the sheet.
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
        jdbc.update("DELETE FROM person_statement WHERE sheet_id = ? AND person_id = ?", sheetId, fromId);
        jdbc.update("DELETE FROM person_grade WHERE sheet_id = ? AND person_id = ?", sheetId, fromId);
        jdbc.update("DELETE FROM person WHERE id = ? AND sheet_id = ?", fromId, sheetId);
    }
}

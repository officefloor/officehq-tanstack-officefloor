package net.officefloor.hq.app.person;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.BadRequestException;
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

    private static final String COLUMNS = "id, name, " + GRADE_TODAY + ", version, statement_count, stated_total, statement_date, retired_from";

    private static final RowMapper<Person> ROW = (rs, i) -> new Person(rs.getLong("id"), rs.getString("name"),
            rs.getString("grade"), rs.getLong("version"), rs.getLong("statement_count"), rs.getString("stated_total"),
            rs.getDate("statement_date") == null ? null : rs.getDate("statement_date").toLocalDate().toString(),
            rs.getDate("retired_from") == null ? null : rs.getDate("retired_from").toLocalDate().toString(),
            false, List.of(), List.of());

    private final JdbcTemplate jdbc;

    public PersonRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
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
    }

    /** Retire a person from a date. */
    public void retire(String sheetId, long id, LocalDate from) {
        jdbc.update("UPDATE person SET retired_from = ? WHERE id = ? AND sheet_id = ?", Date.valueOf(from), id, sheetId);
    }

    /** Put a person on hold from a from-date to a to-date (null: open-ended). */
    public void hold(String sheetId, long id, LocalDate from, LocalDate to) {
        jdbc.update("INSERT INTO person_hold (sheet_id, person_id, from_date, to_date) VALUES (?, ?, ?, ?)",
                sheetId, id, Date.valueOf(from), to == null ? null : Date.valueOf(to));
    }

    /** Refuse a claim of the person dated after the date they are retired from. */
    public void claimAllowed(String sheetId, long id, LocalDate date) throws BadRequestException {
        Person p = find(sheetId, id);
        if (p != null && p.retiredFrom() != null && date.isAfter(LocalDate.parse(p.retiredFrom()))) {
            throw new BadRequestException(p.name() + " is retired from " + p.retiredFrom());
        }
    }

    /** Whether another person on the sheet (other than {@code exceptId}; 0 for none) already has this name, ignoring case. */
    public boolean nameTaken(String sheetId, String name, long exceptId) {
        Integer taken = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND LOWER(name) = LOWER(?) AND id <> ?",
                Integer.class, sheetId, name, exceptId);
        return taken != null && taken > 0;
    }

    /** The person on the sheet with this name, ignoring case; null when there is none. */
    public Person findByName(String sheetId, String name) {
        List<Person> found = jdbc.query("SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? AND LOWER(name) = LOWER(?)",
                ROW, sheetId, name);
        return found.isEmpty() ? null : found.get(0);
    }

    public Person add(String sheetId, String name, String grade) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade) VALUES (?, ?, ?)", new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setString(2, name);
            ps.setString(3, grade);
            return ps;
        }, keys);
        long id = keys.getKey().longValue();
        grade(sheetId, id, grade, null);
        return new Person(id, name, grade, 1, 0, null, null, null, false, List.of(new GradeFrom(grade, null)), List.of());
    }

    /** Whether the sheet has this person. */
    public boolean exists(String sheetId, long id) {
        Integer known = jdbc.queryForObject("SELECT COUNT(*) FROM person WHERE id = ? AND sheet_id = ?",
                Integer.class, id, sheetId);
        return known != null && known > 0;
    }

    /** Change a person's name, if they are still at {@code version}, moving them to the next version;
     *  false when the sheet has no such person at that version. Their grades are given by {@link #grade}. */
    public boolean update(String sheetId, long id, long version, String name) {
        return jdbc.update(
                "UPDATE person SET name = ?, version = version + 1 WHERE id = ? AND sheet_id = ? AND version = ?",
                name, id, sheetId, version) > 0;
    }
}

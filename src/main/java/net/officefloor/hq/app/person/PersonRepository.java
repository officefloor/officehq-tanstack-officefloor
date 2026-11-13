package net.officefloor.hq.app.person;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PersonRepository {

    private static final String COLUMNS = "p.id, p.sheet_id, p.name, p.grade, p.version, p.statement_count, p.stated_total,"
            + " p.statement_date, p.retired_from, p.held FROM person p";

    private final JdbcTemplate jdbc;

    public PersonRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's people, in the order they were added. */
    public List<Person> findAll(String sheetId) {
        return withGrades(sheetId, jdbc.query("SELECT " + COLUMNS + " WHERE p.sheet_id = ? ORDER BY p.id",
                PersonRepository::map, sheetId));
    }

    /** The person with this id on this sheet, or null. */
    public Person find(String sheetId, long id) {
        List<Person> found = withGrades(sheetId, jdbc.query("SELECT " + COLUMNS + " WHERE p.sheet_id = ? AND p.id = ?",
                PersonRepository::map, sheetId, id));
        return found.isEmpty() ? null : found.get(0);
    }

    /** The person on this sheet with this name, ignoring case, or null. */
    public Person findByName(String sheetId, String name) {
        List<Person> found = withGrades(sheetId, jdbc.query(
                "SELECT " + COLUMNS + " WHERE p.sheet_id = ? AND p.name_key = LOWER(?)",
                PersonRepository::map, sheetId, name));
        return found.isEmpty() ? null : found.get(0);
    }

    /** Whether a person other than {@code exceptId} on the sheet has this name, ignoring case. */
    public boolean nameTaken(String sheetId, String name, Long exceptId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND name_key = LOWER(?) AND (? IS NULL OR id <> ?)",
                Integer.class, sheetId, name, exceptId, exceptId);
        return n != null && n > 0;
    }

    /**
     * Insert a new person (null id) or update an existing one, bumping its version and storing its grades as
     * the person carries them; the person as stored, or null when the stored version is no longer the one the
     * person was loaded at. A new person's grade is recorded from the beginning of time.
     */
    public Person save(Person person) {
        if (person.id() != null) {
            int n = jdbc.update(
                    "UPDATE person SET name = ?, version = version + 1 WHERE id = ? AND sheet_id = ? AND version = ?",
                    person.name(), person.id(), person.sheetId(), person.version());
            if (n == 0) {
                return null;
            }
            jdbc.update("DELETE FROM person_grade WHERE person_id = ? AND sheet_id = ?", person.id(), person.sheetId());
            for (GradeFrom g : person.grades()) {
                insertGrade(person.sheetId(), person.id(), g);
            }
            return new Person(person.id(), person.sheetId(), person.name(), person.grade(), person.version() + 1,
                    person.statementCount(), person.statedTotal(), person.statementDate(), person.retiredFrom(),
                    person.held(), person.grades());
        }
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade) VALUES (?, ?, ?)", new String[] { "id" });
            ps.setString(1, person.sheetId());
            ps.setString(2, person.name());
            ps.setString(3, person.grade());
            return ps;
        }, keys);
        long id = keys.getKey().longValue();
        insertGrade(person.sheetId(), id, new GradeFrom(person.grade(), null));
        return new Person(id, person.sheetId(), person.name(), person.grade(), 0, 0, null, null, null, false);
    }

    private void insertGrade(String sheetId, long personId, GradeFrom grade) {
        jdbc.update("INSERT INTO person_grade (sheet_id, person_id, grade, from_date) VALUES (?, ?, ?, ?)", sheetId,
                personId, grade.grade(), grade.from() == null ? null : Date.valueOf(grade.from()));
    }

    /** The people with the grades recorded for them, each showing the grade in force on the sheet's today. */
    private List<Person> withGrades(String sheetId, List<Person> found) {
        if (found.isEmpty()) {
            return found;
        }
        Map<Long, List<GradeFrom>> grades = new HashMap<>();
        jdbc.query("SELECT person_id, grade, from_date FROM person_grade WHERE sheet_id = ?", (rs) -> {
            grades.computeIfAbsent(rs.getLong("person_id"), (k) -> new ArrayList<>())
                    .add(new GradeFrom(rs.getString("grade"), rs.getObject("from_date", LocalDate.class)));
        }, sheetId);
        List<LocalDate> today = jdbc.query("SELECT today FROM sheet WHERE id = ?",
                (rs, n) -> rs.getObject("today", LocalDate.class), sheetId);
        LocalDate on = today.isEmpty() ? LocalDate.now() : today.get(0);
        List<Person> people = new ArrayList<>();
        for (Person p : found) {
            List<GradeFrom> own = grades.getOrDefault(p.id(), List.of(new GradeFrom(p.grade(), null)));
            people.add(new Person(p.id(), p.sheetId(), p.name(), Person.gradeOn(own, on, p.grade()), p.version(),
                    p.statementCount(), p.statedTotal(), p.statementDate(), p.retiredFrom(), p.held(),
                    List.copyOf(own)));
        }
        return people;
    }

    /**
     * Record a statement just taken of the person: one more statement, with its due-total and date; the
     * person as stored. Not an edit, so it leaves the version alone.
     */
    public Person recordStatement(Person person, String statedTotal, LocalDate date) {
        jdbc.update("UPDATE person SET statement_count = statement_count + 1, stated_total = ?, statement_date = ?"
                + " WHERE id = ? AND sheet_id = ?", statedTotal, Date.valueOf(date), person.id(), person.sheetId());
        return find(person.sheetId(), person.id());
    }

    /** Retire the person from this date; the person as stored. Not an edit, so it leaves the version alone. */
    public Person retire(Person person, LocalDate from) {
        jdbc.update("UPDATE person SET retired_from = ? WHERE id = ? AND sheet_id = ?", Date.valueOf(from), person.id(),
                person.sheetId());
        return find(person.sheetId(), person.id());
    }

    /** Put the person on hold or take them off it; the person as stored. Not an edit, so it leaves the version alone. */
    public Person hold(Person person, boolean held) {
        jdbc.update("UPDATE person SET held = ? WHERE id = ? AND sheet_id = ?", held, person.id(), person.sheetId());
        return find(person.sheetId(), person.id());
    }

    private static Person map(ResultSet rs, int n) throws SQLException {
        return new Person(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name"), rs.getString("grade"),
                rs.getInt("version"), rs.getInt("statement_count"), rs.getString("stated_total"),
                rs.getObject("statement_date", LocalDate.class),
                rs.getObject("retired_from", LocalDate.class), rs.getBoolean("held"));
    }
}

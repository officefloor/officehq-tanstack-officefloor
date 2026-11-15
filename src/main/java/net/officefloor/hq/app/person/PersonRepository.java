package net.officefloor.hq.app.person;

import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PersonRepository {

    private static final String COLUMNS = "id, sheet_id, name, version, statement_count, stated_total, statement_date, retired_from, terms";

    private static final RowMapper<Person> ROW = (rs, n) -> {
        LocalDate stated = rs.getObject("statement_date", LocalDate.class);
        LocalDate retired = rs.getObject("retired_from", LocalDate.class);
        Person person = new Person(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name"),
                null, rs.getLong("version"), rs.getInt("statement_count"),
                rs.getString("stated_total"), stated == null ? null : stated.toString());
        person.setRetiredFrom(retired == null ? null : retired.toString());
        person.setTerms(rs.getObject("terms", Integer.class));
        return person;
    };

    private final JdbcTemplate jdbc;

    public PersonRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's people in the order they were added, each with their patrons. */
    public List<Person> list(String sheetId) {
        List<Person> people = jdbc.query("SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? ORDER BY id",
                ROW, sheetId);
        Map<Long, Person> byId = new HashMap<>();
        people.forEach((p) -> byId.put(p.getId(), p));
        withGrades(sheetId, people);
        withHolds(sheetId, people);
        jdbc.query("SELECT person_id, patron_id FROM person_patron WHERE sheet_id = ? ORDER BY added_at, patron_id",
                (rs) -> {
                    Person p = byId.get(rs.getLong("person_id"));
                    if (p != null) {
                        p.getPatronIds().add(rs.getLong("patron_id"));
                    }
                }, sheetId);
        return people;
    }

    public Person find(String sheetId, long id) {
        Person person = jdbc.query("SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? AND id = ?",
                ROW, sheetId, id).stream().findFirst().orElse(null);
        if (person != null) {
            person.setPatronIds(new ArrayList<>(jdbc.queryForList(
                    "SELECT patron_id FROM person_patron WHERE sheet_id = ? AND person_id = ? ORDER BY added_at, patron_id",
                    Long.class, sheetId, id)));
            withGrades(sheetId, List.of(person));
            withHolds(sheetId, List.of(person));
        }
        return person;
    }

    /** Give each person their grades, by from-date (none first), and the one in force on the sheet's today. */
    private void withGrades(String sheetId, List<Person> people) {
        if (people.isEmpty()) {
            return;
        }
        LocalDate today = jdbc.queryForList("SELECT today FROM sheet WHERE id = ?", LocalDate.class, sheetId)
                .stream().findFirst().orElse(LocalDate.now());
        Map<Long, List<GradeFrom>> grades = new HashMap<>();
        jdbc.query("SELECT person_id, grade, from_date FROM person_grade WHERE sheet_id = ? ORDER BY person_id, from_key",
                (rs) -> {
                    LocalDate from = rs.getObject("from_date", LocalDate.class);
                    grades.computeIfAbsent(rs.getLong("person_id"), (k) -> new ArrayList<>())
                            .add(new GradeFrom(rs.getString("grade"), from == null ? null : from.toString()));
                }, sheetId);
        people.forEach((p) -> p.setGrades(grades.getOrDefault(p.getId(), new ArrayList<>()), today));
    }

    /** Give each person their holds, in the order they were added. */
    private void withHolds(String sheetId, List<Person> people) {
        if (people.isEmpty()) {
            return;
        }
        Map<Long, List<Hold>> holds = new HashMap<>();
        jdbc.query("SELECT person_id, from_date, to_date FROM person_hold WHERE sheet_id = ? ORDER BY person_id, id",
                (rs) -> {
                    LocalDate to = rs.getObject("to_date", LocalDate.class);
                    holds.computeIfAbsent(rs.getLong("person_id"), (k) -> new ArrayList<>())
                            .add(new Hold(rs.getObject("from_date", LocalDate.class).toString(),
                                    to == null ? null : to.toString()));
                }, sheetId);
        people.forEach((p) -> p.setHolds(holds.getOrDefault(p.getId(), new ArrayList<>())));
    }

    /** Name the patron as one of the person's patrons. */
    public void addPatron(Person person, Person patron, String user) {
        jdbc.update("INSERT INTO person_patron (sheet_id, person_id, patron_id, added_by) VALUES (?, ?, ?, ?)",
                person.getSheetId(), person.getId(), patron.getId(), user);
        person.getPatronIds().add(patron.getId());
    }

    /** The person on the sheet with this name, compared ignoring case and surrounding spaces; null when none. */
    public Person findByName(String sheetId, String name) {
        Person found = jdbc.query("SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? AND name_key = LOWER(TRIM(?))",
                ROW, sheetId, name).stream().findFirst().orElse(null);
        return found == null ? null : find(sheetId, found.getId());
    }

    /** Whether another person on the sheet already has this name, compared ignoring case and surrounding spaces. */
    public boolean nameTaken(String sheetId, String name, Long exceptId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND name_key = LOWER(TRIM(?)) AND (? IS NULL OR id <> ?)",
                Integer.class, sheetId, name, exceptId, exceptId);
        return n != null && n > 0;
    }

    public Person add(String sheetId, String name, String grade, String user) {
        return add(sheetId, name, grade, null, user);
    }

    /** Add a person with their grade (from the beginning of time) and own terms (null: none). */
    public Person add(String sheetId, String name, String grade, Integer terms, String user) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, created_by, terms) VALUES (?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setString(2, name);
            ps.setString(3, user);
            ps.setObject(4, terms);
            return ps;
        }, key);
        long id = key.getKey().longValue();
        // The first grade a person is given applies from the beginning of time.
        jdbc.update("INSERT INTO person_grade (sheet_id, person_id, grade, from_date, set_by) VALUES (?, ?, ?, NULL, ?)",
                sheetId, id, grade, user);
        Person person = new Person(id, sheetId, name, grade, 0);
        person.getGrades().add(new GradeFrom(grade, null));
        person.setTerms(terms);
        return person;
    }

    /** Record the grade the person was just given (see {@link Person#giveGrade}), replacing the one
     *  with the same from-date. */
    public void saveGrade(Person person, GradeFrom given, String user) {
        LocalDate from = given.from() == null ? null : LocalDate.parse(given.from());
        if (from == null) {
            jdbc.update("DELETE FROM person_grade WHERE sheet_id = ? AND person_id = ? AND from_date IS NULL",
                    person.getSheetId(), person.getId());
        } else {
            jdbc.update("DELETE FROM person_grade WHERE sheet_id = ? AND person_id = ? AND from_date = ?",
                    person.getSheetId(), person.getId(), from);
        }
        jdbc.update("INSERT INTO person_grade (sheet_id, person_id, grade, from_date, set_by) VALUES (?, ?, ?, ?, ?)",
                person.getSheetId(), person.getId(), given.grade(), from, user);
    }

    /** Save the person over the version it was loaded at, raising the version; false (nothing
     *  written) when someone else has saved the person since. */
    public boolean update(Person person) {
        int n = jdbc.update(
                "UPDATE person SET name = ?, terms = ?, version = version + 1 WHERE sheet_id = ? AND id = ? AND version = ?",
                person.getName(), person.getTerms(), person.getSheetId(), person.getId(), person.getVersion());
        if (n == 0) {
            return false;
        }
        person.setVersion(person.getVersion() + 1);
        return true;
    }

    /** Save the person's statements (not an edit: the version stays). */
    public void updateStatements(Person person) {
        jdbc.update("UPDATE person SET statement_count = ?, stated_total = ?, statement_date = ? WHERE sheet_id = ? AND id = ?",
                person.getStatementCount(), person.getStatedTotal(), LocalDate.parse(person.getStatementDate()),
                person.getSheetId(), person.getId());
    }

    /** Save the person's retirement date (not an edit: the version stays). */
    public void updateRetired(Person person) {
        jdbc.update("UPDATE person SET retired_from = ? WHERE sheet_id = ? AND id = ?",
                LocalDate.parse(person.getRetiredFrom()), person.getSheetId(), person.getId());
    }

    /** Add a hold on the person (not an edit: the version stays). */
    public void addHold(Person person, Hold hold, String user) {
        jdbc.update("INSERT INTO person_hold (sheet_id, person_id, from_date, to_date, held_by) VALUES (?, ?, ?, ?, ?)",
                person.getSheetId(), person.getId(), LocalDate.parse(hold.from()),
                hold.to() == null ? null : LocalDate.parse(hold.to()), user);
        person.getHolds().add(hold);
    }
}

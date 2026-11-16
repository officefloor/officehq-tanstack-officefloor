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
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The stored people, kept per sheet. Ids are assigned in the order people are added. A person's grades are kept
 *  dated in person_grade, and their holds in person_hold; a person read carries them all, and the grade in force on the
 *  sheet's today. */
@Repository
public class PersonRepository {

    private static final String COLUMNS = "id, sheet_id, name, version, retired_from";

    private final JdbcTemplate jdbc;
    private final SheetRepository sheets;

    public PersonRepository(JdbcTemplate jdbc, SheetRepository sheets) {
        this.jdbc = jdbc;
        this.sheets = sheets;
    }

    public List<Person> list(String sheetId) {
        return withGrades(sheetId, jdbc.query("SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? ORDER BY id",
                PersonRepository::map, sheetId));
    }

    public Person find(String sheetId, long id) {
        return first(sheetId, jdbc.query("SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? AND id = ?",
                PersonRepository::map, sheetId, id));
    }

    /** The person on the sheet with this name, case ignored; null when there is none. */
    public Person findByName(String sheetId, String name) {
        return first(sheetId, jdbc.query("SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? AND name_key = LOWER(?)",
                PersonRepository::map, sheetId, name));
    }

    private Person first(String sheetId, List<Person> found) {
        return found.isEmpty() ? null : withGrades(sheetId, found).get(0);
    }

    /** The people with their grades and holds, and the grade in force on the sheet's today. */
    private List<Person> withGrades(String sheetId, List<Person> found) {
        if (found.isEmpty()) {
            return found;
        }
        Map<Long, List<PersonGrade>> grades = new HashMap<>();
        jdbc.query("SELECT person_id, from_date, grade FROM person_grade WHERE sheet_id = ? ORDER BY person_id, from_date NULLS FIRST",
                (ResultSet rs) -> {
                    Date from = rs.getDate("from_date");
                    grades.computeIfAbsent(rs.getLong("person_id"), (k) -> new ArrayList<>())
                            .add(new PersonGrade(rs.getString("grade"), from == null ? null : from.toLocalDate()));
                }, sheetId);
        Map<Long, List<PersonHold>> holds = new HashMap<>();
        jdbc.query("SELECT person_id, from_date, to_date FROM person_hold WHERE sheet_id = ? ORDER BY person_id, from_date, id",
                (ResultSet rs) -> {
                    Date to = rs.getDate("to_date");
                    holds.computeIfAbsent(rs.getLong("person_id"), (k) -> new ArrayList<>())
                            .add(new PersonHold(rs.getDate("from_date").toLocalDate(), to == null ? null : to.toLocalDate()));
                }, sheetId);
        LocalDate today = sheets.today(sheetId);
        List<Person> people = new ArrayList<>();
        for (Person p : found) {
            List<PersonGrade> mine = List.copyOf(grades.getOrDefault(p.id(), List.of()));
            people.add(new Person(p.id(), p.sheetId(), p.name(), Person.gradeOn(mine, today), p.version(), p.retiredFrom(),
                    mine, List.copyOf(holds.getOrDefault(p.id(), List.of()))));
        }
        return people;
    }

    /** Whether a person on the sheet other than {@code exceptId} (0: anyone) has this name, case ignored. */
    public boolean nameTaken(String sheetId, String name, long exceptId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND name_key = LOWER(?) AND id <> ?",
                Integer.class, sheetId, name, exceptId);
        return n != null && n > 0;
    }

    /** The unique (sheet_id, name_key) index refuses a name taken by a concurrent add or edit. */
    public Person add(String sheetId, String name, String grade) throws DuplicatePersonNameException {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        try {
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement("INSERT INTO person (sheet_id, name, grade) VALUES (?, ?, ?)",
                        new String[] { "id" });
                ps.setString(1, sheetId);
                ps.setString(2, name);
                ps.setString(3, grade);
                return ps;
            }, keys);
        } catch (DuplicateKeyException e) {
            throw new DuplicatePersonNameException(name);
        }
        long id = keys.getKey().longValue();
        jdbc.update("INSERT INTO person_grade (sheet_id, person_id, from_date, grade) VALUES (?, ?, NULL, ?)", sheetId, id, grade);
        return new Person(id, sheetId, name, grade, 0, null, List.of(new PersonGrade(grade, null)), List.of());
    }

    /** Stores the person as its new version, with its grades, only while the stored one is still {@code fromVersion}:
     *  a save that lands in between refuses this one. */
    public void update(Person person, long fromVersion) throws DuplicatePersonNameException, StalePersonException {
        int n;
        try {
            n = jdbc.update("UPDATE person SET name = ?, version = ? WHERE sheet_id = ? AND id = ? AND version = ?",
                    person.name(), person.version(), person.sheetId(), person.id(), fromVersion);
        } catch (DuplicateKeyException e) {
            throw new DuplicatePersonNameException(person.name());
        }
        if (n == 0) {
            throw new StalePersonException(person.id());
        }
        jdbc.update("DELETE FROM person_grade WHERE sheet_id = ? AND person_id = ?", person.sheetId(), person.id());
        for (PersonGrade g : person.grades()) {
            jdbc.update("INSERT INTO person_grade (sheet_id, person_id, from_date, grade) VALUES (?, ?, ?, ?)",
                    person.sheetId(), person.id(), g.from() == null ? null : Date.valueOf(g.from()), g.grade());
        }
    }

    /** Retires the person from the date (replacing any earlier retirement date). */
    public void retire(String sheetId, long id, LocalDate from) {
        jdbc.update("UPDATE person SET retired_from = ? WHERE sheet_id = ? AND id = ?", Date.valueOf(from), sheetId, id);
    }

    /** Adds a hold on the person, from the date until the date (null: open-ended). */
    public void hold(String sheetId, long id, PersonHold hold) {
        jdbc.update("INSERT INTO person_hold (sheet_id, person_id, from_date, to_date) VALUES (?, ?, ?, ?)", sheetId, id,
                Date.valueOf(hold.from()), hold.to() == null ? null : Date.valueOf(hold.to()));
    }

    private static Person map(ResultSet rs, int n) throws SQLException {
        Date retired = rs.getDate("retired_from");
        return new Person(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name"), null, rs.getLong("version"),
                retired == null ? null : retired.toLocalDate().toString(), List.of(), List.of());
    }
}

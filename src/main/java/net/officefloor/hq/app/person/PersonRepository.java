package net.officefloor.hq.app.person;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The people of each sheet, kept in the order they were added. */
@Repository
public class PersonRepository {

    private static final RowMapper<Person> ROW = (rs, i) -> {
        Person person = new Person(rs.getLong("id"),
                rs.getString("sheet_id"), rs.getString("name"), null, rs.getLong("version"),
                rs.getInt("statement_count"), rs.getBigDecimal("stated_total"),
                rs.getDate("statement_date") == null ? null : rs.getDate("statement_date").toLocalDate());
        person.setRetiredFrom(rs.getDate("retired_from") == null ? null : rs.getDate("retired_from").toLocalDate());
        person.setHeld(rs.getBoolean("held"));
        return person;
    };

    private final JdbcTemplate jdbc;

    public PersonRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Person> findBySheet(String sheetId) {
        return withGrades(sheetId, withPatrons(sheetId, jdbc.query("SELECT * FROM person WHERE sheet_id = ? ORDER BY id", ROW, sheetId)));
    }

    public Person findById(String sheetId, long id) {
        List<Person> found = withGrades(sheetId, withPatrons(sheetId,
                jdbc.query("SELECT * FROM person WHERE sheet_id = ? AND id = ?", ROW, sheetId, id)));
        return found.isEmpty() ? null : found.get(0);
    }

    private record PatronLink(long personId, String name) {
    }

    /** Fills in each person's patrons' names, sorted. */
    private List<Person> withPatrons(String sheetId, List<Person> people) {
        if (people.isEmpty()) return people;
        Map<Long, List<String>> names = jdbc.query(
                "SELECT pp.person_id, p.name FROM person_patron pp"
                        + " JOIN person p ON p.sheet_id = pp.sheet_id AND p.id = pp.patron_id"
                        + " WHERE pp.sheet_id = ? ORDER BY p.name",
                (rs, i) -> new PatronLink(rs.getLong("person_id"), rs.getString("name")), sheetId).stream()
                .collect(Collectors.groupingBy(PatronLink::personId,
                        Collectors.mapping(PatronLink::name, Collectors.toList())));
        for (Person person : people) {
            List<String> patrons = names.getOrDefault(person.getId(), List.of()).stream().sorted().toList();
            person.setPatrons(patrons);
        }
        return people;
    }

    private record GradeLink(long personId, DatedGrade grade) {
    }

    /** Fills in each person's dated grades. */
    private List<Person> withGrades(String sheetId, List<Person> people) {
        if (people.isEmpty()) return people;
        Map<Long, List<DatedGrade>> grades = jdbc.query(
                "SELECT person_id, grade, from_date FROM person_grade WHERE sheet_id = ?",
                (rs, i) -> new GradeLink(rs.getLong("person_id"), new DatedGrade(rs.getString("grade"),
                        rs.getDate("from_date") == null ? null : rs.getDate("from_date").toLocalDate())), sheetId).stream()
                .collect(Collectors.groupingBy(GradeLink::personId,
                        Collectors.mapping(GradeLink::grade, Collectors.toList())));
        for (Person person : people) {
            person.setGrades(grades.getOrDefault(person.getId(), List.of()));
        }
        return people;
    }

    /** Stores the person's dated grades, replacing the ones stored. */
    private void saveGrades(Person person) {
        jdbc.update("DELETE FROM person_grade WHERE sheet_id = ? AND person_id = ?", person.getSheetId(), person.getId());
        for (DatedGrade dated : person.getGrades()) {
            jdbc.update("INSERT INTO person_grade (sheet_id, person_id, grade, from_date) VALUES (?, ?, ?, ?)",
                    person.getSheetId(), person.getId(), dated.grade(),
                    dated.from() == null ? null : Date.valueOf(dated.from()));
        }
    }

    /** Whether this person is already one of the person's patrons. */
    public boolean isPatron(Person person, long patronId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person_patron WHERE sheet_id = ? AND person_id = ? AND patron_id = ?",
                Integer.class, person.getSheetId(), person.getId(), patronId);
        return count != null && count > 0;
    }

    /** Names this person as one of the person's patrons. Not an edit, so the version stays. */
    public void addPatron(Person person, long patronId) {
        jdbc.update("INSERT INTO person_patron (sheet_id, person_id, patron_id) VALUES (?, ?, ?)",
                person.getSheetId(), person.getId(), patronId);
    }

    /** The person on the sheet with this name, ignoring case; null when there is none. */
    public Person findByName(String sheetId, String name) {
        List<Person> found = withGrades(sheetId, jdbc.query("SELECT * FROM person WHERE sheet_id = ? AND name_key = LOWER(?)",
                ROW, sheetId, name));
        return found.isEmpty() ? null : found.get(0);
    }

    /** Whether a person on the sheet other than {@code exceptId} (null: none) has this name, ignoring case. */
    public boolean nameInUse(String sheetId, String name, Long exceptId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND name_key = LOWER(?) AND (? IS NULL OR id <> ?)",
                Integer.class, sheetId, name, exceptId, exceptId);
        return count != null && count > 0;
    }

    public Person create(String sheetId, String name, String grade) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name) VALUES (?, ?)", new String[] {"id"});
            ps.setString(1, sheetId);
            ps.setString(2, name);
            return ps;
        }, keys);
        // The first grade a person is given applies from the beginning of time.
        Person person = new Person(keys.getKey().longValue(), sheetId, name, grade, 0);
        saveGrades(person);
        return person;
    }

    /** Saves the person over the version it was loaded at, moving its version on. False (nothing
     *  written) when the person has been saved since. */
    public boolean save(Person person) {
        int updated = jdbc.update("UPDATE person SET name = ?, version = version + 1"
                        + " WHERE sheet_id = ? AND id = ? AND version = ?",
                person.getName(), person.getSheetId(), person.getId(), person.getVersion());
        if (updated == 0) return false;
        saveGrades(person);
        person.setVersion(person.getVersion() + 1);
        return true;
    }

    /** Stores the person's statement fields. A statement is not an edit, so the version stays. */
    public void saveStatement(Person person) {
        jdbc.update("UPDATE person SET statement_count = ?, stated_total = ?, statement_date = ?"
                        + " WHERE sheet_id = ? AND id = ?",
                person.getStatementCount(), person.getStatedTotal(), Date.valueOf(person.getStatementDate()),
                person.getSheetId(), person.getId());
    }

    /** Stores the person's retirement date. Retiring is not an edit, so the version stays. */
    public void saveRetired(Person person) {
        jdbc.update("UPDATE person SET retired_from = ? WHERE sheet_id = ? AND id = ?",
                Date.valueOf(person.getRetiredFrom()), person.getSheetId(), person.getId());
    }

    /** Stores whether the person is on hold. Holding is not an edit, so the version stays. */
    public void saveHeld(Person person) {
        jdbc.update("UPDATE person SET held = ? WHERE sheet_id = ? AND id = ?",
                person.isHeld(), person.getSheetId(), person.getId());
    }
}

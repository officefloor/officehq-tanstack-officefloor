package net.officefloor.hq.app.person;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** People by sheet, in the order they were added. */
@Repository
public class PersonRepository {

    private static final RowMapper<Person> ROW = (rs, i) -> new Person(rs.getLong("id"),
            rs.getString("sheet_id"), rs.getString("name"), null, rs.getLong("version"),
            rs.getInt("statement_count"), rs.getBigDecimal("stated_total"), rs.getObject("statement_date", LocalDate.class),
            rs.getObject("retired_from", LocalDate.class), Grades.NONE, Holds.NONE);

    private record GradeRow(long personId, Grades.Grade grade) {
    }

    private static final RowMapper<GradeRow> GRADE_ROW = (rs, i) -> new GradeRow(rs.getLong("person_id"),
            new Grades.Grade(rs.getString("grade"), rs.getObject("from_date", LocalDate.class)));

    private record HoldRow(long personId, Holds.Hold hold) {
    }

    private static final RowMapper<HoldRow> HOLD_ROW = (rs, i) -> new HoldRow(rs.getLong("person_id"),
            new Holds.Hold(rs.getObject("from_date", LocalDate.class), rs.getObject("to_date", LocalDate.class)));

    private final JdbcTemplate jdbc;

    public PersonRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Person> list(String sheetId) {
        return graded(sheetId, jdbc.query("SELECT * FROM person WHERE sheet_id = ? ORDER BY id", ROW, sheetId));
    }

    /** The person on this sheet, or null when absent. */
    public Person find(String sheetId, long id) {
        return graded(sheetId, jdbc.query("SELECT * FROM person WHERE sheet_id = ? AND id = ?", ROW, sheetId, id))
                .stream().findFirst().orElse(null);
    }

    /** The person on this sheet with this name (compared as nameInUse compares), or null when absent. */
    public Person findByName(String sheetId, String name) {
        return graded(sheetId, jdbc.query("SELECT * FROM person WHERE sheet_id = ? AND name_key = LOWER(TRIM(?))", ROW,
                sheetId, name)).stream().findFirst().orElse(null);
    }

    /** The people with their dated grades, the grade in force on the sheet's today, and their holds. */
    private List<Person> graded(String sheetId, List<Person> people) {
        if (people.isEmpty()) {
            return people;
        }
        LocalDate today = jdbc.query("SELECT today FROM sheet WHERE id = ?",
                rs -> rs.next() ? rs.getObject(1, LocalDate.class) : null, sheetId);
        Map<Long, List<Grades.Grade>> byPerson = jdbc.query(
                "SELECT * FROM person_grade WHERE sheet_id = ? ORDER BY person_id, from_date NULLS FIRST", GRADE_ROW,
                sheetId).stream().collect(Collectors.groupingBy(GradeRow::personId,
                        Collectors.mapping(GradeRow::grade, Collectors.toList())));
        Map<Long, List<Holds.Hold>> holdsByPerson = jdbc.query(
                "SELECT * FROM person_hold WHERE sheet_id = ? ORDER BY person_id, id", HOLD_ROW, sheetId).stream()
                .collect(Collectors.groupingBy(HoldRow::personId,
                        Collectors.mapping(HoldRow::hold, Collectors.toList())));
        return people.stream().map(p -> {
            Grades grades = new Grades(byPerson.getOrDefault(p.id(), List.of()));
            return new Person(p.id(), p.sheetId(), p.name(), grades.on(today), p.version(), p.statementCount(),
                    p.statedTotal(), p.statementDate(), p.retiredFrom(), grades,
                    new Holds(holdsByPerson.getOrDefault(p.id(), List.of())));
        }).toList();
    }

    /** Whether another person on this sheet (other than exceptId, when given) has this name,
     *  compared without regard to case and ignoring surrounding spaces. */
    public boolean nameInUse(String sheetId, String name, Long exceptId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND name_key = LOWER(TRIM(?)) AND id <> ?",
                Integer.class, sheetId, name, exceptId == null ? -1L : exceptId);
        return n != null && n > 0;
    }

    /** Add a person with their first grade, which applies from the beginning of time. */
    public Person insert(String sheetId, String name, String grade) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement("INSERT INTO person (sheet_id, name) VALUES (?, ?)", new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setString(2, name);
            return ps;
        }, keys);
        long id = keys.getKey().longValue();
        jdbc.update("INSERT INTO person_grade (sheet_id, person_id, grade, from_date) VALUES (?, ?, ?, NULL)",
                sheetId, id, grade);
        return new Person(id, sheetId, name, grade, 0, 0, null, null, null,
                new Grades(List.of(new Grades.Grade(grade, null))), Holds.NONE);
    }

    /** Lock the person's row until the transaction ends, so concurrent statements of them go one after the other. */
    public void lock(Person person) {
        jdbc.query("SELECT id FROM person WHERE sheet_id = ? AND id = ? FOR UPDATE", rs -> null,
                person.sheetId(), person.id());
    }

    /** Record a statement taken of the person: one more taken, with its due-total (null: n/a) and date. Leaves the version alone: a statement is not an edit of the person. */
    public void stated(Person person, BigDecimal total, LocalDate date) {
        jdbc.update("UPDATE person SET statement_count = statement_count + 1, stated_total = ?, statement_date = ? "
                + "WHERE sheet_id = ? AND id = ?", total, date, person.sheetId(), person.id());
    }

    /** Retire the person from the date. Leaves the version alone: retiring is not an edit of the person. */
    public void retire(Person person, LocalDate from) {
        jdbc.update("UPDATE person SET retired_from = ? WHERE sheet_id = ? AND id = ?", from, person.sheetId(), person.id());
    }

    /** Add a hold to the person from the from-date to the to-date (null: open-ended), keeping the holds they have.
     *  Leaves the version alone: holding is not an edit of the person. */
    public void hold(Person person, LocalDate from, LocalDate to) {
        jdbc.update("INSERT INTO person_hold (sheet_id, person_id, from_date, to_date) VALUES (?, ?, ?, ?)",
                person.sheetId(), person.id(), from, to);
    }

    /** Store the person's name at its new version, only while the stored person is still at version
     *  `from`; false (nothing written) when it has been saved since. */
    public boolean update(Person person, long from) {
        return jdbc.update("UPDATE person SET name = ?, version = ? WHERE sheet_id = ? AND id = ? AND version = ?",
                person.name(), person.version(), person.sheetId(), person.id(), from) == 1;
    }

    /** Give the person the grade from the from-date (null: from the beginning of time), replacing any they were
     *  given from that same date and keeping the rest. */
    public void grade(Person person, String grade, LocalDate from) {
        Date fromDate = from != null ? Date.valueOf(from) : null;
        jdbc.update("DELETE FROM person_grade WHERE sheet_id = ? AND person_id = ? AND from_date IS NOT DISTINCT FROM ?",
                person.sheetId(), person.id(), fromDate);
        jdbc.update("INSERT INTO person_grade (sheet_id, person_id, grade, from_date) VALUES (?, ?, ?, ?)",
                person.sheetId(), person.id(), grade, fromDate);
    }
}

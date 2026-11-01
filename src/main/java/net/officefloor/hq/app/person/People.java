package net.officefloor.hq.app.person;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.sheet.SheetClock;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The people kept per sheet, each with their grades by from-date. */
@Repository
public class People {

    private static final String COLUMNS = "SELECT id, name, version, retired_from, held FROM person";

    private record Row(long id, String name, long version, String retiredFrom, boolean held) {
    }

    private final JdbcTemplate jdbc;
    private final SheetClock clock;

    public People(JdbcTemplate jdbc, SheetClock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    private List<Person> query(String sheet, String where, Object... args) {
        List<Row> rows = jdbc.query(COLUMNS + " " + where, (rs, i) -> new Row(rs.getLong("id"), rs.getString("name"),
                rs.getLong("version"),
                rs.getDate("retired_from") == null ? null : rs.getDate("retired_from").toLocalDate().toString(),
                rs.getBoolean("held")), args);
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, List<GradeFrom>> grades = new HashMap<>();
        jdbc.query("SELECT g.person_id, g.grade, g.from_date FROM person_grade g JOIN person p ON p.id = g.person_id"
                + " WHERE p.sheet = ? ORDER BY g.person_id, g.from_key", (rs) -> {
                    grades.computeIfAbsent(rs.getLong("person_id"), (k) -> new ArrayList<>())
                            .add(new GradeFrom(rs.getString("grade"),
                                    rs.getDate("from_date") == null ? null : rs.getDate("from_date").toLocalDate().toString()));
                }, sheet);
        LocalDate today = clock.today(sheet);
        List<Person> people = new ArrayList<>();
        for (Row r : rows) {
            List<GradeFrom> mine = List.copyOf(grades.getOrDefault(r.id(), List.of()));
            people.add(new Person(r.id(), r.name(), Person.gradeOn(mine, today), r.version(), r.retiredFrom(), r.held(),
                    mine));
        }
        return people;
    }

    /** Everyone on the sheet, in the order they were added. */
    public List<Person> list(String sheet) {
        return query(sheet, "WHERE sheet = ? ORDER BY id", sheet);
    }

    public Person find(String sheet, long id) {
        List<Person> found = query(sheet, "WHERE sheet = ? AND id = ?", sheet, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The person on the sheet with this name, ignoring case; null when there is none. */
    public Person findByName(String sheet, String name) {
        List<Person> found = query(sheet, "WHERE sheet = ? AND name_key = LOWER(?)", sheet, name);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Whether someone on the sheet other than {@code except} has this name, ignoring case. */
    private boolean nameTaken(String sheet, String name, Long except) {
        Integer taken = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet = ? AND name_key = LOWER(?) AND (? IS NULL OR id <> ?)",
                Integer.class, sheet, name, except, except);
        return taken != null && taken > 0;
    }

    /** Adds the person with their first grade, which applies from the beginning of time. */
    public Person add(String sheet, String name, String grade) throws DuplicatePersonNameException {
        if (nameTaken(sheet, name, null)) {
            throw new DuplicatePersonNameException(name);
        }
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        try {
            insert(sheet, name, keys);
        } catch (DuplicateKeyException e) {
            throw new DuplicatePersonNameException(name);
        }
        long id = keys.getKey().longValue();
        setGrade(id, grade, null);
        return find(sheet, id);
    }

    private void insert(String sheet, String name, GeneratedKeyHolder keys) {
        jdbc.update(con -> {
            var ps = con.prepareStatement("INSERT INTO person (sheet, name) VALUES (?, ?)", new String[] { "id" });
            ps.setString(1, sheet);
            ps.setString(2, name);
            return ps;
        }, keys);
    }

    /** Records the person's grade from the from-date (null: from the beginning of time), replacing the one there for it. */
    public void setGrade(long id, String grade, LocalDate from) {
        Date date = from == null ? null : Date.valueOf(from);
        if (jdbc.update("UPDATE person_grade SET grade = ? WHERE person_id = ? AND from_date IS NOT DISTINCT FROM ?",
                grade, id, date) == 0) {
            jdbc.update("INSERT INTO person_grade (person_id, grade, from_date) VALUES (?, ?, ?)", id, grade, date);
        }
    }

    /** Retire the person from the date. */
    public void retire(String sheet, long id, LocalDate from) {
        jdbc.update("UPDATE person SET retired_from = ? WHERE sheet = ? AND id = ?", Date.valueOf(from), sheet, id);
    }

    /** Put the person on hold, or take them off it. */
    public void hold(String sheet, long id, boolean held) {
        jdbc.update("UPDATE person SET held = ? WHERE sheet = ? AND id = ?", held, sheet, id);
    }

    /** Save the person's name over the version it carries, raising the version; refused when it has moved on. */
    public void update(String sheet, Person person) throws DuplicatePersonNameException, StalePersonException {
        if (nameTaken(sheet, person.name(), person.id())) {
            throw new DuplicatePersonNameException(person.name());
        }
        int updated;
        try {
            updated = jdbc.update(
                    "UPDATE person SET name = ?, version = version + 1 WHERE sheet = ? AND id = ? AND version = ?",
                    person.name(), sheet, person.id(), person.version());
        } catch (DuplicateKeyException e) {
            throw new DuplicatePersonNameException(person.name());
        }
        if (updated == 0) {
            throw new StalePersonException(person.id());
        }
    }
}

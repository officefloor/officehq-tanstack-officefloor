package net.officefloor.hq.app.person;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.alias.Aliases;
import net.officefloor.hq.app.sheet.SheetClock;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The people kept per sheet, each with their grades by from-date. */
@Repository
public class People {

    private static final String COLUMNS = "SELECT id, name, version, retired_from, terms, contact FROM person";

    private record Row(long id, String name, long version, String retiredFrom, Integer terms, String contact) {
    }

    private final JdbcTemplate jdbc;
    private final SheetClock clock;
    private final Aliases aliases;

    public People(JdbcTemplate jdbc, SheetClock clock, Aliases aliases) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.aliases = aliases;
    }

    private List<Person> query(String sheet, String where, Object... args) {
        List<Row> rows = jdbc.query(COLUMNS + " " + where, (rs, i) -> new Row(rs.getLong("id"), rs.getString("name"),
                rs.getLong("version"),
                rs.getDate("retired_from") == null ? null : rs.getDate("retired_from").toLocalDate().toString(),
                rs.getObject("terms", Integer.class), rs.getString("contact")), args);
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
        Map<Long, List<Hold>> holds = new HashMap<>();
        jdbc.query("SELECT h.person_id, h.from_date, h.to_date FROM person_hold h JOIN person p ON p.id = h.person_id"
                + " WHERE p.sheet = ? ORDER BY h.person_id, h.id", (rs) -> {
                    holds.computeIfAbsent(rs.getLong("person_id"), (k) -> new ArrayList<>())
                            .add(new Hold(rs.getDate("from_date").toLocalDate().toString(),
                                    rs.getDate("to_date") == null ? null : rs.getDate("to_date").toLocalDate().toString()));
                }, sheet);
        LocalDate today = clock.today(sheet);
        List<Person> people = new ArrayList<>();
        for (Row r : rows) {
            List<GradeFrom> mine = List.copyOf(grades.getOrDefault(r.id(), List.of()));
            people.add(new Person(r.id(), r.name(), Person.gradeOn(mine, today), r.version(), r.retiredFrom(),
                    List.copyOf(holds.getOrDefault(r.id(), List.of())), mine, r.terms(),
                    Person.retiredOn(r.retiredFrom(), today), r.contact()));
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

    /** The person on the sheet with this name or alias, ignoring case; null when there is none. */
    public Person findByName(String sheet, String name) {
        List<Person> found = query(sheet, "WHERE sheet = ? AND name_key = LOWER(?)", sheet, name);
        if (found.isEmpty()) {
            found = query(sheet, "WHERE sheet = ? AND id IN (SELECT person_id FROM person_alias WHERE sheet = ?"
                    + " AND alias_key = LOWER(?))", sheet, sheet, name);
        }
        return found.isEmpty() ? null : found.get(0);
    }

    /** Whether someone on the sheet other than {@code except} has this name, ignoring case. */
    public boolean nameTaken(String sheet, String name, Long except) {
        Integer taken = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet = ? AND name_key = LOWER(?) AND (? IS NULL OR id <> ?)",
                Integer.class, sheet, name, except, except);
        return taken != null && taken > 0;
    }

    /** Adds the person with their first grade, which applies from the beginning of time. */
    public Person add(String sheet, String name, String grade) throws DuplicatePersonNameException {
        return add(sheet, name, grade, null, null);
    }

    /** Adds the person with their first grade, which applies from the beginning of time, their own terms and contact (null: none). */
    public Person add(String sheet, String name, String grade, Integer terms, String contact)
            throws DuplicatePersonNameException {
        if (nameTaken(sheet, name, null) || aliases.taken(sheet, name)) {
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
        setTerms(id, terms);
        setContact(id, contact);
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

    /** Sets the person's own terms (null: none). */
    public void setTerms(long id, Integer terms) {
        jdbc.update("UPDATE person SET terms = ? WHERE id = ?", terms, id);
    }

    /** Sets the person's contact (null: none). */
    public void setContact(long id, String contact) {
        jdbc.update("UPDATE person SET contact = ? WHERE id = ?", contact, id);
    }

    /** Whether someone on the sheet other than {@code except} has this contact. */
    public boolean contactTaken(String sheet, String contact, Long except) {
        Integer taken = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet = ? AND contact = ? AND (? IS NULL OR id <> ?)",
                Integer.class, sheet, contact, except, except);
        return taken != null && taken > 0;
    }

    /** Retire the person from the date. */
    public void retire(String sheet, long id, LocalDate from) {
        jdbc.update("UPDATE person SET retired_from = ? WHERE sheet = ? AND id = ?", Date.valueOf(from), sheet, id);
    }

    /** Adds a hold to the person. */
    public void hold(long id, Hold hold) {
        jdbc.update("INSERT INTO person_hold (person_id, from_date, to_date) VALUES (?, ?, ?)", id,
                Date.valueOf(hold.from()), hold.to() == null ? null : Date.valueOf(hold.to()));
    }

    /** Save the person's name over the version it carries, raising the version; refused when it has moved on. */
    public void update(String sheet, Person person) throws DuplicatePersonNameException, StalePersonException {
        if (nameTaken(sheet, person.name(), person.id()) || aliases.taken(sheet, person.name())) {
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

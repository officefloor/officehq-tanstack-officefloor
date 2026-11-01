package net.officefloor.hq.app.person;

import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The people kept per sheet. */
@Repository
public class People {

    private static final RowMapper<Person> ROW = (rs, i) -> new Person(rs.getLong("id"), rs.getString("name"),
            rs.getString("grade"), rs.getLong("version"));

    private final JdbcTemplate jdbc;

    public People(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Everyone on the sheet, in the order they were added. */
    public List<Person> list(String sheet) {
        return jdbc.query("SELECT id, name, grade, version FROM person WHERE sheet = ? ORDER BY id", ROW, sheet);
    }

    public Person find(String sheet, long id) {
        List<Person> found = jdbc.query("SELECT id, name, grade, version FROM person WHERE sheet = ? AND id = ?", ROW,
                sheet, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The person on the sheet with this name, ignoring case; null when there is none. */
    public Person findByName(String sheet, String name) {
        List<Person> found = jdbc.query(
                "SELECT id, name, grade, version FROM person WHERE sheet = ? AND name_key = LOWER(?)", ROW, sheet, name);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Whether someone on the sheet other than {@code except} has this name, ignoring case. */
    private boolean nameTaken(String sheet, String name, Long except) {
        Integer taken = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet = ? AND name_key = LOWER(?) AND (? IS NULL OR id <> ?)",
                Integer.class, sheet, name, except, except);
        return taken != null && taken > 0;
    }

    public Person add(String sheet, String name, String grade) throws DuplicatePersonNameException {
        if (nameTaken(sheet, name, null)) {
            throw new DuplicatePersonNameException(name);
        }
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        try {
            insert(sheet, name, grade, keys);
        } catch (DuplicateKeyException e) {
            throw new DuplicatePersonNameException(name);
        }
        return new Person(keys.getKey().longValue(), name, grade, 0);
    }

    private void insert(String sheet, String name, String grade, GeneratedKeyHolder keys) {
        jdbc.update(con -> {
            var ps = con.prepareStatement("INSERT INTO person (sheet, name, grade) VALUES (?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheet);
            ps.setString(2, name);
            ps.setString(3, grade);
            return ps;
        }, keys);
    }

    /** Save the person over the version it carries, raising the version; refused when it has moved on. */
    public void update(String sheet, Person person) throws DuplicatePersonNameException, StalePersonException {
        if (nameTaken(sheet, person.name(), person.id())) {
            throw new DuplicatePersonNameException(person.name());
        }
        int updated;
        try {
            updated = jdbc.update(
                    "UPDATE person SET name = ?, grade = ?, version = version + 1 WHERE sheet = ? AND id = ? AND version = ?",
                    person.name(), person.grade(), sheet, person.id(), person.version());
        } catch (DuplicateKeyException e) {
            throw new DuplicatePersonNameException(person.name());
        }
        if (updated == 0) {
            throw new StalePersonException(person.id());
        }
    }
}

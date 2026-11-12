package net.officefloor.hq.app.person;

import java.sql.PreparedStatement;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The people kept per sheet, in the order they were added. */
@Repository
public class PersonStore {

    private static final RowMapper<Person> ROW = (rs, i) -> new Person(rs.getLong("id"),
            rs.getString("sheet_id"), rs.getString("name"), rs.getString("grade"), rs.getLong("version"));

    private final JdbcTemplate jdbc;

    public PersonStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Person> list(String sheetId) {
        return jdbc.query("SELECT * FROM person WHERE sheet_id = ? ORDER BY id", ROW, sheetId);
    }

    public Person find(String sheetId, long id) {
        List<Person> found = jdbc.query("SELECT * FROM person WHERE sheet_id = ? AND id = ?", ROW, sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The person on the sheet with this name, ignoring case and the spaces around it; null when none. */
    public Person findByName(String sheetId, String name) {
        List<Person> found = jdbc.query("SELECT * FROM person WHERE sheet_id = ? AND name_key = LOWER(?)", ROW,
                sheetId, name.trim());
        return found.isEmpty() ? null : found.get(0);
    }

    /** Whether another person on the sheet (not {@code exceptId}) has this name, ignoring case. */
    public boolean nameTaken(String sheetId, String name, Long exceptId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND name_key = LOWER(?) AND id <> ?",
                Integer.class, sheetId, name.trim(), exceptId == null ? -1L : exceptId);
        return n != null && n > 0;
    }

    public Person add(String sheetId, String name, String grade) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade) VALUES (?, ?, ?)", new String[] { "ID" });
            ps.setString(1, sheetId);
            ps.setString(2, name);
            ps.setString(3, grade);
            return ps;
        }, keys);
        return new Person(keys.getKey().longValue(), sheetId, name, grade, 0);
    }

    /**
     * Stores the person over the version it was loaded at, moving the version on; false (nothing
     * stored) when someone else has stored another version since.
     */
    public boolean save(Person person) {
        int n = jdbc.update(
                "UPDATE person SET name = ?, grade = ?, version = version + 1 WHERE sheet_id = ? AND id = ? AND version = ?",
                person.getName(), person.getGrade(), person.getSheetId(), person.getId(), person.getVersion());
        if (n == 1) {
            person.setVersion(person.getVersion() + 1);
        }
        return n == 1;
    }
}

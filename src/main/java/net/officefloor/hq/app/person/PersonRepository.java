package net.officefloor.hq.app.person;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PersonRepository {

    private final JdbcTemplate jdbc;

    public PersonRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's people, in the order they were added. */
    public List<Person> findAll(String sheetId) {
        return jdbc.query("SELECT id, sheet_id, name, grade, version FROM person WHERE sheet_id = ? ORDER BY id",
                PersonRepository::map, sheetId);
    }

    /** The person with this id on this sheet, or null. */
    public Person find(String sheetId, long id) {
        List<Person> found = jdbc.query("SELECT id, sheet_id, name, grade, version FROM person WHERE sheet_id = ? AND id = ?",
                PersonRepository::map, sheetId, id);
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
     * Insert a new person (null id) or update an existing one, bumping its version; the person as
     * stored, or null when the stored version is no longer the one the person was loaded at.
     */
    public Person save(Person person) {
        if (person.id() != null) {
            int n = jdbc.update(
                    "UPDATE person SET name = ?, grade = ?, version = version + 1 WHERE id = ? AND sheet_id = ? AND version = ?",
                    person.name(), person.grade(), person.id(), person.sheetId(), person.version());
            return n == 0 ? null
                    : new Person(person.id(), person.sheetId(), person.name(), person.grade(), person.version() + 1);
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
        return new Person(keys.getKey().longValue(), person.sheetId(), person.name(), person.grade(), 0);
    }

    private static Person map(ResultSet rs, int n) throws SQLException {
        return new Person(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name"), rs.getString("grade"),
                rs.getInt("version"));
    }
}

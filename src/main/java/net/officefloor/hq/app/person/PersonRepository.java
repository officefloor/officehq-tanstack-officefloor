package net.officefloor.hq.app.person;

import java.sql.PreparedStatement;
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

    public boolean sheetExists(String sheetId) {
        Integer known = jdbc.queryForObject("SELECT COUNT(*) FROM sheet WHERE id = ?", Integer.class, sheetId);
        return known != null && known > 0;
    }

    /** The sheet's people in the order they were added. */
    public List<Person> list(String sheetId) {
        return jdbc.query("SELECT id, name, grade, version FROM person WHERE sheet_id = ? ORDER BY id",
                (rs, i) -> new Person(rs.getLong("id"), rs.getString("name"), rs.getString("grade"),
                        rs.getLong("version")), sheetId);
    }

    /** Whether another person on the sheet (other than {@code exceptId}; 0 for none) already has this name, ignoring case. */
    public boolean nameTaken(String sheetId, String name, long exceptId) {
        Integer taken = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND LOWER(name) = LOWER(?) AND id <> ?",
                Integer.class, sheetId, name, exceptId);
        return taken != null && taken > 0;
    }

    public Person add(String sheetId, String name, String grade) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade) VALUES (?, ?, ?)", new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setString(2, name);
            ps.setString(3, grade);
            return ps;
        }, keys);
        return new Person(keys.getKey().longValue(), name, grade, 1);
    }

    /** Whether the sheet has this person. */
    public boolean exists(String sheetId, long id) {
        Integer known = jdbc.queryForObject("SELECT COUNT(*) FROM person WHERE id = ? AND sheet_id = ?",
                Integer.class, id, sheetId);
        return known != null && known > 0;
    }

    /** Change a person's name and grade, if they are still at {@code version}, moving them to the
     *  next version; false when the sheet has no such person at that version. */
    public boolean update(String sheetId, long id, long version, String name, String grade) {
        return jdbc.update(
                "UPDATE person SET name = ?, grade = ?, version = version + 1 WHERE id = ? AND sheet_id = ? AND version = ?",
                name, grade, id, sheetId, version) > 0;
    }
}

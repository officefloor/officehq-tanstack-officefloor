package net.officefloor.hq.app.person;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The people store, kept per sheet. */
@Repository
public class PersonRepository {

    private static final RowMapper<Person> ROW = (rs, n) -> new Person(rs.getLong("id"),
            rs.getString("sheet_id"), rs.getString("name"), rs.getString("grade"), rs.getLong("version"));

    private final JdbcTemplate jdbc;

    public PersonRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's people, in the order they were added. */
    public List<Person> findAll(String sheetId) {
        return jdbc.query("SELECT id, sheet_id, name, grade, version FROM person WHERE sheet_id = ? ORDER BY id",
                ROW, sheetId);
    }

    /** The person with this id on the sheet, or null when there is none. */
    public Person find(String sheetId, long id) {
        List<Person> found = jdbc.query(
                "SELECT id, sheet_id, name, grade, version FROM person WHERE sheet_id = ? AND id = ?",
                ROW, sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Whether a person other than {@code exceptId} has this name on the sheet, ignoring case. */
    public boolean nameTaken(String sheetId, String name, Long exceptId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND LOWER(TRIM(name)) = LOWER(TRIM(?))"
                        + " AND id <> ?",
                Integer.class, sheetId, name, exceptId == null ? -1L : exceptId);
        return count != null && count > 0;
    }

    /**
     * Insert a new person (assigning its id) or update an existing one, raising its version. False
     * when the stored person is no longer at the version it was loaded at: nothing is written.
     */
    public boolean save(Person person) {
        if (person.getId() != null) {
            int updated = jdbc.update("UPDATE person SET name = ?, grade = ?, version = version + 1"
                    + " WHERE id = ? AND sheet_id = ? AND version = ?",
                    person.getName(), person.getGrade(), person.getId(), person.getSheetId(),
                    person.getVersion());
            if (updated == 0) return false;
            person.setVersion(person.getVersion() + 1);
            return true;
        }
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, person.getSheetId());
            ps.setString(2, person.getName());
            ps.setString(3, person.getGrade());
            return ps;
        }, keys);
        person.setId(keys.getKey().longValue());
        return true;
    }
}

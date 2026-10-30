package net.officefloor.hq.app.person;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The person store, kept per sheet. */
@Repository
public class People {

    private static final RowMapper<Person> ROW = (rs, i) -> new Person(rs.getLong("id"),
            rs.getString("sheet_id"), rs.getString("name"), rs.getString("grade"), rs.getInt("version"));

    private final JdbcTemplate jdbc;

    public People(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's people in the order they were added. */
    public List<Person> list(String sheetId) {
        return jdbc.query("SELECT * FROM person WHERE sheet_id = ? ORDER BY id", ROW, sheetId);
    }

    public Person find(String sheetId, long id) {
        List<Person> found = jdbc.query("SELECT * FROM person WHERE sheet_id = ? AND id = ?", ROW, sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Whether another person on the sheet already has this name, compared without regard to case. */
    public boolean nameTaken(String sheetId, String name, Long exceptId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND LOWER(TRIM(name)) = LOWER(TRIM(?))"
                        + (exceptId == null ? "" : " AND id <> " + exceptId.longValue()),
                Integer.class, sheetId, name);
        return count != null && count > 0;
    }

    public Person add(String sheetId, String name, String grade) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, sheetId);
            ps.setString(2, name);
            ps.setString(3, grade);
            return ps;
        }, keys);
        return new Person(keys.getKey().longValue(), sheetId, name, grade);
    }

    /** Saves the person over the version it was loaded at, moving it on by one; false (nothing
     *  written) when someone else has saved the person since. */
    public boolean save(Person person) {
        int updated = jdbc.update(
                "UPDATE person SET name = ?, grade = ?, version = version + 1 WHERE sheet_id = ? AND id = ? AND version = ?",
                person.getName(), person.getGrade(), person.getSheetId(), person.getId(), person.getVersion());
        if (updated == 0) {
            return false;
        }
        person.setVersion(person.getVersion() + 1);
        return true;
    }
}

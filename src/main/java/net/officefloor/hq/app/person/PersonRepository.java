package net.officefloor.hq.app.person;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
        return jdbc.query("SELECT id, sheet_id, name, grade FROM person WHERE sheet_id = ? ORDER BY id",
                PersonRepository::map, sheetId);
    }

    /** The person with this id on this sheet, or null. */
    public Person find(String sheetId, long id) {
        List<Person> found = jdbc.query("SELECT id, sheet_id, name, grade FROM person WHERE sheet_id = ? AND id = ?",
                PersonRepository::map, sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Insert a new person (null id) or update an existing one; the person as stored. */
    public Person save(Person person) {
        if (person.id() != null) {
            jdbc.update("UPDATE person SET name = ?, grade = ? WHERE id = ? AND sheet_id = ?",
                    person.name(), person.grade(), person.id(), person.sheetId());
            return person;
        }
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, person.sheetId());
            ps.setString(2, person.name());
            ps.setString(3, person.grade());
            return ps;
        }, keys);
        return new Person(keys.getKey().longValue(), person.sheetId(), person.name(), person.grade());
    }

    private static Person map(ResultSet rs, int n) throws SQLException {
        return new Person(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name"), rs.getString("grade"));
    }
}

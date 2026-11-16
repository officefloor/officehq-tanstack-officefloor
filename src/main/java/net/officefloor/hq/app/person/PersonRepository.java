package net.officefloor.hq.app.person;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The stored people, kept per sheet. Ids are assigned in the order people are added. */
@Repository
public class PersonRepository {

    private final JdbcTemplate jdbc;

    public PersonRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Person> list(String sheetId) {
        return jdbc.query("SELECT id, sheet_id, name, grade FROM person WHERE sheet_id = ? ORDER BY id",
                PersonRepository::map, sheetId);
    }

    public Person find(String sheetId, long id) {
        List<Person> found = jdbc.query("SELECT id, sheet_id, name, grade FROM person WHERE sheet_id = ? AND id = ?",
                PersonRepository::map, sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    public Person add(String sheetId, String name, String grade) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("INSERT INTO person (sheet_id, name, grade) VALUES (?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setString(2, name);
            ps.setString(3, grade);
            return ps;
        }, keys);
        return new Person(keys.getKey().longValue(), sheetId, name, grade);
    }

    public void update(Person person) {
        jdbc.update("UPDATE person SET name = ?, grade = ? WHERE sheet_id = ? AND id = ?",
                person.name(), person.grade(), person.sheetId(), person.id());
    }

    private static Person map(ResultSet rs, int n) throws SQLException {
        return new Person(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name"), rs.getString("grade"));
    }
}

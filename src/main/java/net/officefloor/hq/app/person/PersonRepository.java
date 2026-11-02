package net.officefloor.hq.app.person;

import java.sql.PreparedStatement;
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

    public boolean sheetExists(String sheetId) {
        Integer known = jdbc.queryForObject("SELECT COUNT(*) FROM sheet WHERE id = ?", Integer.class, sheetId);
        return known != null && known > 0;
    }

    /** The sheet's people in the order they were added. */
    public List<Person> list(String sheetId) {
        return jdbc.query("SELECT id, name, grade FROM person WHERE sheet_id = ? ORDER BY id",
                (rs, i) -> new Person(rs.getLong("id"), rs.getString("name"), rs.getString("grade")), sheetId);
    }

    public Person add(String sheetId, String name, String grade) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, sheetId);
            ps.setString(2, name);
            ps.setString(3, grade);
            return ps;
        }, keys);
        return new Person(keys.getKey().longValue(), name, grade);
    }

    /** Change a person's name and grade; false when the sheet has no such person. */
    public boolean update(String sheetId, Person person) {
        return jdbc.update("UPDATE person SET name = ?, grade = ? WHERE id = ? AND sheet_id = ?",
                person.name(), person.grade(), person.id(), sheetId) > 0;
    }
}

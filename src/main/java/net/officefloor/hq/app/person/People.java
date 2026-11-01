package net.officefloor.hq.app.person;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The people kept per sheet. */
@Repository
public class People {

    private static final RowMapper<Person> ROW = (rs, i) -> new Person(rs.getLong("id"), rs.getString("name"),
            rs.getString("grade"));

    private final JdbcTemplate jdbc;

    public People(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Everyone on the sheet, in the order they were added. */
    public List<Person> list(String sheet) {
        return jdbc.query("SELECT id, name, grade FROM person WHERE sheet = ? ORDER BY id", ROW, sheet);
    }

    public Person find(String sheet, long id) {
        List<Person> found = jdbc.query("SELECT id, name, grade FROM person WHERE sheet = ? AND id = ?", ROW,
                sheet, id);
        return found.isEmpty() ? null : found.get(0);
    }

    public Person add(String sheet, String name, String grade) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement("INSERT INTO person (sheet, name, grade) VALUES (?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheet);
            ps.setString(2, name);
            ps.setString(3, grade);
            return ps;
        }, keys);
        return new Person(keys.getKey().longValue(), name, grade);
    }

    public void update(String sheet, Person person) {
        jdbc.update("UPDATE person SET name = ?, grade = ? WHERE sheet = ? AND id = ?", person.name(),
                person.grade(), sheet, person.id());
    }
}

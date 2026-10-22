package net.officefloor.hq.app.person;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** People by sheet, in the order they were added. */
@Repository
public class PersonRepository {

    private static final RowMapper<Person> ROW = (rs, i) -> new Person(rs.getLong("id"),
            rs.getString("sheet_id"), rs.getString("name"), rs.getString("grade"));

    private final JdbcTemplate jdbc;

    public PersonRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Person> list(String sheetId) {
        return jdbc.query("SELECT * FROM person WHERE sheet_id = ? ORDER BY id", ROW, sheetId);
    }

    /** The person on this sheet, or null when absent. */
    public Person find(String sheetId, long id) {
        return jdbc.query("SELECT * FROM person WHERE sheet_id = ? AND id = ?", ROW, sheetId, id)
                .stream().findFirst().orElse(null);
    }

    public Person insert(String sheetId, String name, String grade) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement("INSERT INTO person (sheet_id, name, grade) VALUES (?, ?, ?)",
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
}

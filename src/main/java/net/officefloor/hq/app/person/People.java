package net.officefloor.hq.app.person;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** The store of people, kept per sheet. */
@Repository
public class People {

    private static final RowMapper<Person> ROW = (rs, n) -> new Person(rs.getLong("id"),
            rs.getString("sheet_id"), rs.getString("name"), rs.getString("grade"));

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

    public Person add(String sheetId, PersonRequest request) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, sheetId);
            ps.setString(2, request.name());
            ps.setString(3, request.grade());
            return ps;
        }, key);
        return new Person(key.getKey().longValue(), sheetId, request.name(), request.grade());
    }

    public void save(Person person) {
        jdbc.update("UPDATE person SET name = ?, grade = ? WHERE id = ?", person.name(), person.grade(), person.id());
    }
}

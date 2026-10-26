package net.officefloor.hq.app.person;

import java.sql.PreparedStatement;
import java.time.LocalDate;
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
            rs.getString("sheet_id"), rs.getString("name"), rs.getString("grade"), rs.getLong("version"),
            rs.getObject("retired_from", LocalDate.class));

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

    /** The person on the sheet with this name, ignoring case and surrounding spaces; null when none. */
    public Person findByName(String sheetId, String name) {
        List<Person> found = jdbc.query("SELECT * FROM person WHERE sheet_id = ? AND name_key = LOWER(?)", ROW,
                sheetId, name.trim());
        return found.isEmpty() ? null : found.get(0);
    }

    /** Whether another person on the sheet (not {@code exceptId}) has this name, ignoring case. */
    public boolean nameTaken(String sheetId, String name, long exceptId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM person WHERE sheet_id = ? AND name_key = LOWER(?) AND id <> ?",
                Integer.class, sheetId, name.trim(), exceptId);
        return n != null && n > 0;
    }

    public Person add(String sheetId, PersonRequest request) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade) VALUES (?, ?, ?)", new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setString(2, request.name());
            ps.setString(3, request.grade());
            return ps;
        }, key);
        return new Person(key.getKey().longValue(), sheetId, request.name(), request.grade(), 0, null);
    }

    /**
     * Store an edited person, whose version is one past the version it was loaded at. False, storing
     * nothing, when the person has been saved since it was loaded.
     */
    public boolean save(Person person) {
        return jdbc.update("UPDATE person SET name = ?, grade = ?, version = ? WHERE id = ? AND version = ?",
                person.name(), person.grade(), person.version(), person.id(), person.version() - 1) == 1;
    }

    /** Retire the person from the date (replacing any date they were retired from before). */
    public void retire(long id, LocalDate from) {
        jdbc.update("UPDATE person SET retired_from = ? WHERE id = ?", from, id);
    }
}

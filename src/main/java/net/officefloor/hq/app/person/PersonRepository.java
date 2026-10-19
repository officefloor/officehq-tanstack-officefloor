package net.officefloor.hq.app.person;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The people of each sheet, kept in the order they were added. */
@Repository
public class PersonRepository {

    private static final RowMapper<Person> ROW = (rs, i) -> new Person(rs.getLong("id"),
            rs.getString("sheet_id"), rs.getString("name"), rs.getString("grade"), rs.getLong("version"),
            rs.getInt("statement_count"), rs.getBigDecimal("stated_total"),
            rs.getDate("statement_date") == null ? null : rs.getDate("statement_date").toLocalDate());

    private final JdbcTemplate jdbc;

    public PersonRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Person> findBySheet(String sheetId) {
        return jdbc.query("SELECT * FROM person WHERE sheet_id = ? ORDER BY id", ROW, sheetId);
    }

    public Person findById(String sheetId, long id) {
        List<Person> found = jdbc.query("SELECT * FROM person WHERE sheet_id = ? AND id = ?", ROW, sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Whether a person on the sheet other than {@code exceptId} (null: none) has this name, ignoring case. */
    public boolean nameInUse(String sheetId, String name, Long exceptId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND name_key = LOWER(?) AND (? IS NULL OR id <> ?)",
                Integer.class, sheetId, name, exceptId, exceptId);
        return count != null && count > 0;
    }

    public Person create(String sheetId, String name, String grade) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade) VALUES (?, ?, ?)", new String[] {"id"});
            ps.setString(1, sheetId);
            ps.setString(2, name);
            ps.setString(3, grade);
            return ps;
        }, keys);
        return new Person(keys.getKey().longValue(), sheetId, name, grade, 0);
    }

    /** Saves the person over the version it was loaded at, moving its version on. False (nothing
     *  written) when the person has been saved since. */
    public boolean save(Person person) {
        int updated = jdbc.update("UPDATE person SET name = ?, grade = ?, version = version + 1"
                        + " WHERE sheet_id = ? AND id = ? AND version = ?",
                person.getName(), person.getGrade(), person.getSheetId(), person.getId(), person.getVersion());
        if (updated == 0) return false;
        person.setVersion(person.getVersion() + 1);
        return true;
    }

    /** Stores the person's statement fields. A statement is not an edit, so the version stays. */
    public void saveStatement(Person person) {
        jdbc.update("UPDATE person SET statement_count = ?, stated_total = ?, statement_date = ?"
                        + " WHERE sheet_id = ? AND id = ?",
                person.getStatementCount(), person.getStatedTotal(), Date.valueOf(person.getStatementDate()),
                person.getSheetId(), person.getId());
    }
}

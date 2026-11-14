package net.officefloor.hq.app.person;

import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PersonRepository {

    private static final String COLUMNS = "id, sheet_id, name, grade, version, statement_count, stated_total, statement_date";

    private static final RowMapper<Person> ROW = (rs, n) -> {
        LocalDate stated = rs.getObject("statement_date", LocalDate.class);
        return new Person(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name"),
                rs.getString("grade"), rs.getLong("version"), rs.getInt("statement_count"),
                rs.getString("stated_total"), stated == null ? null : stated.toString());
    };

    private final JdbcTemplate jdbc;

    public PersonRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's people in the order they were added. */
    public List<Person> list(String sheetId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? ORDER BY id",
                ROW, sheetId);
    }

    public Person find(String sheetId, long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? AND id = ?",
                ROW, sheetId, id).stream().findFirst().orElse(null);
    }

    /** Whether another person on the sheet already has this name, compared ignoring case and surrounding spaces. */
    public boolean nameTaken(String sheetId, String name, Long exceptId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND name_key = LOWER(TRIM(?)) AND (? IS NULL OR id <> ?)",
                Integer.class, sheetId, name, exceptId, exceptId);
        return n != null && n > 0;
    }

    public Person add(String sheetId, String name, String grade, String user) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade, created_by) VALUES (?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setString(2, name);
            ps.setString(3, grade);
            ps.setString(4, user);
            return ps;
        }, key);
        return new Person(key.getKey().longValue(), sheetId, name, grade, 0);
    }

    /** Save the person over the version it was loaded at, raising the version; false (nothing
     *  written) when someone else has saved the person since. */
    public boolean update(Person person) {
        int n = jdbc.update(
                "UPDATE person SET name = ?, grade = ?, version = version + 1 WHERE sheet_id = ? AND id = ? AND version = ?",
                person.getName(), person.getGrade(), person.getSheetId(), person.getId(), person.getVersion());
        if (n == 0) {
            return false;
        }
        person.setVersion(person.getVersion() + 1);
        return true;
    }

    /** Save the person's statements (not an edit: the version stays). */
    public void updateStatements(Person person) {
        jdbc.update("UPDATE person SET statement_count = ?, stated_total = ?, statement_date = ? WHERE sheet_id = ? AND id = ?",
                person.getStatementCount(), person.getStatedTotal(), LocalDate.parse(person.getStatementDate()),
                person.getSheetId(), person.getId());
    }
}

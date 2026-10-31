package net.officefloor.hq.app.person;

import java.sql.PreparedStatement;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The people of each sheet, kept in the order they were added. */
@Repository
public class People {

    private final JdbcTemplate jdbc;

    public People(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Person> onSheet(String sheetId) {
        return jdbc.query("SELECT id, sheet_id, name, grade FROM person WHERE sheet_id = ? ORDER BY id",
                (rs, i) -> new Person(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name"),
                        rs.getString("grade")),
                sheetId);
    }

    /** The person with this id on this sheet, or null when there is none. */
    public Person find(String sheetId, long id) {
        return jdbc.query("SELECT id, sheet_id, name, grade FROM person WHERE sheet_id = ? AND id = ?",
                (rs, i) -> new Person(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name"),
                        rs.getString("grade")),
                sheetId, id).stream().findFirst().orElse(null);
    }

    /** Whether another person on this sheet (not `exceptId`, which may be null) already has this name. */
    public boolean nameTaken(String sheetId, String name, Long exceptId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND name_key = LOWER(TRIM(?)) AND id <> ?",
                Integer.class, sheetId, name, exceptId == null ? -1L : exceptId);
        return n != null && n > 0;
    }

    /** Insert a new person (assigning its id) or update an existing one. */
    public void save(Person person) {
        if (person.getId() != null) {
            jdbc.update("UPDATE person SET name = ?, grade = ? WHERE id = ?",
                    person.getName(), person.getGrade(), person.getId());
            return;
        }
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade) VALUES (?, ?, ?)", new String[] { "id" });
            ps.setString(1, person.getSheetId());
            ps.setString(2, person.getName());
            ps.setString(3, person.getGrade());
            return ps;
        }, keys);
        person.setId(keys.getKey().longValue());
    }
}

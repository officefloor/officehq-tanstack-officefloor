package net.officefloor.hq.app.person;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The people of each sheet, kept in the order they were added. */
@Repository
public class People {

    private static final String COLUMNS = "SELECT id, sheet_id, name, grade, version, statement_count, stated_total, statement_date, retired_from FROM person";

    private final JdbcTemplate jdbc;

    public People(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Person> onSheet(String sheetId) {
        return jdbc.query(COLUMNS + " WHERE sheet_id = ? ORDER BY id",
                People::person,
                sheetId);
    }

    /** The person with this id on this sheet, or null when there is none. */
    public Person find(String sheetId, long id) {
        return jdbc.query(COLUMNS + " WHERE sheet_id = ? AND id = ?",
                People::person,
                sheetId, id).stream().findFirst().orElse(null);
    }

    /** The person with this id on this sheet, read afresh and locked for the rest of the
     *  transaction, so two statements for one person at once happen one after the other. */
    public Person lock(String sheetId, long id) {
        return jdbc.query(COLUMNS + " WHERE sheet_id = ? AND id = ? FOR UPDATE", People::person, sheetId, id)
                .stream().findFirst().orElse(null);
    }

    /** The person on this sheet with this name (matched as names are kept unique: without the
     *  spaces around it and in any case), or null when there is none. */
    public Person named(String sheetId, String name) {
        return jdbc.query(COLUMNS + " WHERE sheet_id = ? AND name_key = LOWER(TRIM(?))", People::person,
                sheetId, name).stream().findFirst().orElse(null);
    }

    /** Whether another person on this sheet (not `exceptId`, which may be null) already has this name. */
    public boolean nameTaken(String sheetId, String name, Long exceptId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND name_key = LOWER(TRIM(?)) AND id <> ?",
                Integer.class, sheetId, name, exceptId == null ? -1L : exceptId);
        return n != null && n > 0;
    }

    /** Insert a new person (assigning its id) or update an existing one, if it is still at the
     *  version it was loaded at; false (nothing written) when someone has saved it since. */
    public boolean save(Person person) {
        if (person.getId() != null) {
            int n = jdbc.update("UPDATE person SET name = ?, grade = ?, version = version + 1 WHERE id = ? AND version = ?",
                    person.getName(), person.getGrade(), person.getId(), person.getVersion());
            if (n == 0) return false;
            person.setVersion(person.getVersion() + 1);
            return true;
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
        return true;
    }

    /** Record a statement taken for the person (their count, last due-total and date). Not an edit:
     *  the person's version is untouched, so an editor open on them can still save. */
    public void saveStatement(Person person) {
        jdbc.update("UPDATE person SET statement_count = ?, stated_total = ?, statement_date = ? WHERE id = ?",
                person.getStatementCount(), person.getStatedTotal(), Date.valueOf(person.getStatementDate()),
                person.getId());
    }

    /** Record the person's retirement date. Not an edit: the person's version is untouched. */
    public void saveRetirement(Person person) {
        jdbc.update("UPDATE person SET retired_from = ? WHERE id = ?",
                person.getRetiredFrom() != null ? Date.valueOf(person.getRetiredFrom()) : null, person.getId());
    }

    private static Person person(ResultSet rs, int i) throws SQLException {
        Person person = new Person(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name"),
                rs.getString("grade"), rs.getInt("version"));
        person.setStatementCount(rs.getInt("statement_count"));
        person.setStatedTotal(rs.getBigDecimal("stated_total"));
        Date date = rs.getDate("statement_date");
        person.setStatementDate(date != null ? date.toLocalDate() : null);
        Date retired = rs.getDate("retired_from");
        person.setRetiredFrom(retired != null ? retired.toLocalDate() : null);
        return person;
    }
}

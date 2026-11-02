package net.officefloor.hq.app.person;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.BadRequestException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PersonRepository {

    private static final String COLUMNS = "id, name, grade, version, statement_count, stated_total, statement_date, retired_from";

    private static final RowMapper<Person> ROW = (rs, i) -> new Person(rs.getLong("id"), rs.getString("name"),
            rs.getString("grade"), rs.getLong("version"), rs.getLong("statement_count"), rs.getString("stated_total"),
            rs.getDate("statement_date") == null ? null : rs.getDate("statement_date").toLocalDate().toString(),
            rs.getDate("retired_from") == null ? null : rs.getDate("retired_from").toLocalDate().toString());

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
        return jdbc.query("SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? ORDER BY id", ROW, sheetId);
    }

    /** A person on the sheet; null when the sheet has no such person. */
    public Person find(String sheetId, long id) {
        List<Person> found = jdbc.query("SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? AND id = ?", ROW,
                sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Record a statement taken of a person: one more, with its due-total and date. */
    public void stated(String sheetId, long id, String total, LocalDate date) {
        jdbc.update("UPDATE person SET statement_count = statement_count + 1, stated_total = ?, statement_date = ?"
                + " WHERE id = ? AND sheet_id = ?", total, Date.valueOf(date), id, sheetId);
    }

    /** Retire a person from a date. */
    public void retire(String sheetId, long id, LocalDate from) {
        jdbc.update("UPDATE person SET retired_from = ? WHERE id = ? AND sheet_id = ?", Date.valueOf(from), id, sheetId);
    }

    /** Refuse a claim of the person dated after the date they are retired from. */
    public void claimAllowed(String sheetId, long id, LocalDate date) throws BadRequestException {
        Person p = find(sheetId, id);
        if (p != null && p.retiredFrom() != null && date.isAfter(LocalDate.parse(p.retiredFrom()))) {
            throw new BadRequestException(p.name() + " is retired from " + p.retiredFrom());
        }
    }

    /** Whether another person on the sheet (other than {@code exceptId}; 0 for none) already has this name, ignoring case. */
    public boolean nameTaken(String sheetId, String name, long exceptId) {
        Integer taken = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND LOWER(name) = LOWER(?) AND id <> ?",
                Integer.class, sheetId, name, exceptId);
        return taken != null && taken > 0;
    }

    /** The person on the sheet with this name, ignoring case; null when there is none. */
    public Person findByName(String sheetId, String name) {
        List<Person> found = jdbc.query("SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? AND LOWER(name) = LOWER(?)",
                ROW, sheetId, name);
        return found.isEmpty() ? null : found.get(0);
    }

    public Person add(String sheetId, String name, String grade) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade) VALUES (?, ?, ?)", new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setString(2, name);
            ps.setString(3, grade);
            return ps;
        }, keys);
        return new Person(keys.getKey().longValue(), name, grade, 1, 0, null, null, null);
    }

    /** Whether the sheet has this person. */
    public boolean exists(String sheetId, long id) {
        Integer known = jdbc.queryForObject("SELECT COUNT(*) FROM person WHERE id = ? AND sheet_id = ?",
                Integer.class, id, sheetId);
        return known != null && known > 0;
    }

    /** Change a person's name and grade, if they are still at {@code version}, moving them to the
     *  next version; false when the sheet has no such person at that version. */
    public boolean update(String sheetId, long id, long version, String name, String grade) {
        return jdbc.update(
                "UPDATE person SET name = ?, grade = ?, version = version + 1 WHERE id = ? AND sheet_id = ? AND version = ?",
                name, grade, id, sheetId, version) > 0;
    }
}

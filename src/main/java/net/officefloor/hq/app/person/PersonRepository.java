package net.officefloor.hq.app.person;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PersonRepository {

    private static final String COLUMNS = "id, sheet_id, name, grade, version, statement_count, stated_total, statement_date, retired_from, held";

    private final JdbcTemplate jdbc;

    public PersonRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's people, in the order they were added. */
    public List<Person> findAll(String sheetId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? ORDER BY id",
                PersonRepository::map, sheetId);
    }

    /** The person with this id on this sheet, or null. */
    public Person find(String sheetId, long id) {
        List<Person> found = jdbc.query("SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? AND id = ?",
                PersonRepository::map, sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The person on this sheet with this name, ignoring case, or null. */
    public Person findByName(String sheetId, String name) {
        List<Person> found = jdbc.query("SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? AND name_key = LOWER(?)",
                PersonRepository::map, sheetId, name);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Whether a person other than {@code exceptId} on the sheet has this name, ignoring case. */
    public boolean nameTaken(String sheetId, String name, Long exceptId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND name_key = LOWER(?) AND (? IS NULL OR id <> ?)",
                Integer.class, sheetId, name, exceptId, exceptId);
        return n != null && n > 0;
    }

    /**
     * Insert a new person (null id) or update an existing one, bumping its version; the person as
     * stored, or null when the stored version is no longer the one the person was loaded at.
     */
    public Person save(Person person) {
        if (person.id() != null) {
            int n = jdbc.update(
                    "UPDATE person SET name = ?, grade = ?, version = version + 1 WHERE id = ? AND sheet_id = ? AND version = ?",
                    person.name(), person.grade(), person.id(), person.sheetId(), person.version());
            return n == 0 ? null
                    : new Person(person.id(), person.sheetId(), person.name(), person.grade(), person.version() + 1,
                            person.statementCount(), person.statedTotal(), person.statementDate(),
                            person.retiredFrom(), person.held());
        }
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade) VALUES (?, ?, ?)", new String[] { "id" });
            ps.setString(1, person.sheetId());
            ps.setString(2, person.name());
            ps.setString(3, person.grade());
            return ps;
        }, keys);
        return new Person(keys.getKey().longValue(), person.sheetId(), person.name(), person.grade(), 0, 0, null, null,
                null, false);
    }

    /**
     * Record a statement just taken of the person: one more statement, with its due-total and date; the
     * person as stored. Not an edit, so it leaves the version alone.
     */
    public Person recordStatement(Person person, String statedTotal, LocalDate date) {
        jdbc.update("UPDATE person SET statement_count = statement_count + 1, stated_total = ?, statement_date = ?"
                + " WHERE id = ? AND sheet_id = ?", statedTotal, Date.valueOf(date), person.id(), person.sheetId());
        return find(person.sheetId(), person.id());
    }

    /** Retire the person from this date; the person as stored. Not an edit, so it leaves the version alone. */
    public Person retire(Person person, LocalDate from) {
        jdbc.update("UPDATE person SET retired_from = ? WHERE id = ? AND sheet_id = ?", Date.valueOf(from), person.id(),
                person.sheetId());
        return find(person.sheetId(), person.id());
    }

    /** Put the person on hold or take them off it; the person as stored. Not an edit, so it leaves the version alone. */
    public Person hold(Person person, boolean held) {
        jdbc.update("UPDATE person SET held = ? WHERE id = ? AND sheet_id = ?", held, person.id(), person.sheetId());
        return find(person.sheetId(), person.id());
    }

    private static Person map(ResultSet rs, int n) throws SQLException {
        return new Person(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name"), rs.getString("grade"),
                rs.getInt("version"), rs.getInt("statement_count"), rs.getString("stated_total"),
                rs.getObject("statement_date", LocalDate.class),
                rs.getObject("retired_from", LocalDate.class), rs.getBoolean("held"));
    }
}

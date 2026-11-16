package net.officefloor.hq.app.person;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The stored people, kept per sheet. Ids are assigned in the order people are added. */
@Repository
public class PersonRepository {

    private final JdbcTemplate jdbc;

    public PersonRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Person> list(String sheetId) {
        return jdbc.query("SELECT id, sheet_id, name, grade, version, retired_from, held FROM person WHERE sheet_id = ? ORDER BY id",
                PersonRepository::map, sheetId);
    }

    public Person find(String sheetId, long id) {
        List<Person> found = jdbc.query("SELECT id, sheet_id, name, grade, version, retired_from, held FROM person WHERE sheet_id = ? AND id = ?",
                PersonRepository::map, sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The person on the sheet with this name, case ignored; null when there is none. */
    public Person findByName(String sheetId, String name) {
        List<Person> found = jdbc.query("SELECT id, sheet_id, name, grade, version, retired_from, held FROM person WHERE sheet_id = ? AND name_key = LOWER(?)",
                PersonRepository::map, sheetId, name);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Whether a person on the sheet other than {@code exceptId} (0: anyone) has this name, case ignored. */
    public boolean nameTaken(String sheetId, String name, long exceptId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND name_key = LOWER(?) AND id <> ?",
                Integer.class, sheetId, name, exceptId);
        return n != null && n > 0;
    }

    /** The unique (sheet_id, name_key) index refuses a name taken by a concurrent add or edit. */
    public Person add(String sheetId, String name, String grade) throws DuplicatePersonNameException {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        try {
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement("INSERT INTO person (sheet_id, name, grade) VALUES (?, ?, ?)",
                        new String[] { "id" });
                ps.setString(1, sheetId);
                ps.setString(2, name);
                ps.setString(3, grade);
                return ps;
            }, keys);
        } catch (DuplicateKeyException e) {
            throw new DuplicatePersonNameException(name);
        }
        return new Person(keys.getKey().longValue(), sheetId, name, grade, 0, null, false);
    }

    /** Stores the person as its new version, only while the stored one is still {@code fromVersion}: a save
     *  that lands in between refuses this one. */
    public void update(Person person, long fromVersion) throws DuplicatePersonNameException, StalePersonException {
        int n;
        try {
            n = jdbc.update("UPDATE person SET name = ?, grade = ?, version = ? WHERE sheet_id = ? AND id = ? AND version = ?",
                    person.name(), person.grade(), person.version(), person.sheetId(), person.id(), fromVersion);
        } catch (DuplicateKeyException e) {
            throw new DuplicatePersonNameException(person.name());
        }
        if (n == 0) {
            throw new StalePersonException(person.id());
        }
    }

    /** Retires the person from the date (replacing any earlier retirement date). */
    public void retire(String sheetId, long id, LocalDate from) {
        jdbc.update("UPDATE person SET retired_from = ? WHERE sheet_id = ? AND id = ?", Date.valueOf(from), sheetId, id);
    }

    /** Puts the person on hold (true) or takes them off it (false), only while they are not already so; false when
     *  they already were. */
    public boolean hold(String sheetId, long id, boolean held) {
        return jdbc.update("UPDATE person SET held = ? WHERE sheet_id = ? AND id = ? AND held = ?", held, sheetId, id, !held) == 1;
    }

    private static Person map(ResultSet rs, int n) throws SQLException {
        Date retired = rs.getDate("retired_from");
        return new Person(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name"), rs.getString("grade"), rs.getLong("version"),
                retired == null ? null : retired.toLocalDate().toString(), rs.getBoolean("held"));
    }
}

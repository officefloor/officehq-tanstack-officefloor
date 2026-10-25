package net.officefloor.hq.app.person;

import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The people on each sheet; ids are assigned in the order people are added. */
@Repository
public class PersonRepository {

    /** A person's columns, with their statement count and their last statement's due-total and date. */
    private static final String COLUMNS = "p.id, p.sheet_id, p.name, p.grade, p.version, p.retired_from, p.held,"
            + " (SELECT COUNT(*) FROM statement s WHERE s.person_id = p.id) AS statement_count,"
            + " (SELECT s.due_total FROM statement s WHERE s.person_id = p.id ORDER BY s.id DESC LIMIT 1) AS stated_total,"
            + " (SELECT s.taken_on FROM statement s WHERE s.person_id = p.id ORDER BY s.id DESC LIMIT 1) AS statement_date";

    private static final RowMapper<Person> ROW = (rs, n) -> {
        Person person = new Person(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name"),
                rs.getString("grade"), rs.getLong("version"));
        person.setStatements(new Statements(rs.getInt("statement_count"), rs.getBigDecimal("stated_total"),
                rs.getObject("statement_date", LocalDate.class)));
        person.setRetiredFrom(rs.getObject("retired_from", LocalDate.class));
        person.setHeld(rs.getBoolean("held"));
        return person;
    };

    static final String CHANGED = "Someone else has saved this person since you opened it; the form now shows their changes";

    static final String NAME_TAKEN = "That name is already in use on this sheet";

    private final JdbcTemplate jdbc;

    public PersonRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's people, in the order they were added. */
    public List<Person> list(String sheetId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM person p WHERE p.sheet_id = ? ORDER BY p.id", ROW, sheetId);
    }

    public Person find(String sheetId, long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM person p WHERE p.sheet_id = ? AND p.id = ?", ROW, sheetId, id)
                .stream().findFirst().orElse(null);
    }

    /** The person on the sheet with this name, ignoring case and surrounding spaces; null when none. */
    public Person findByName(String sheetId, String name) {
        return jdbc.query("SELECT " + COLUMNS + " FROM person p WHERE p.sheet_id = ? AND p.name_key = LOWER(TRIM(?))",
                ROW, sheetId, name).stream().findFirst().orElse(null);
    }

    /** Whether another person on the sheet has this name, ignoring case and surrounding spaces. */
    public boolean nameTaken(String sheetId, String name, Long exceptId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND name_key = LOWER(TRIM(?)) AND id <> ?",
                Integer.class, sheetId, name, exceptId == null ? -1L : exceptId);
        return n != null && n > 0;
    }

    public Person insert(String sheetId, String name, String grade) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade) VALUES (?, ?, ?)", new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setString(2, name);
            ps.setString(3, grade);
            return ps;
        }, keys);
        return new Person(keys.getKey().longValue(), sheetId, name, grade, 0);
    }

    /** Saves the person if it is still at the version it was loaded at, moving it to the next
     *  version; false (nothing written) when someone else saved it in the meantime. */
    public boolean update(Person person) {
        int n = jdbc.update("UPDATE person SET name = ?, grade = ?, version = version + 1 WHERE id = ? AND version = ?",
                person.getName(), person.getGrade(), person.getId(), person.getVersion());
        if (n == 0) {
            return false;
        }
        person.setVersion(person.getVersion() + 1);
        return true;
    }

    /** Retire the person from the date; false (nothing written) when they were retired already. */
    public boolean retire(long id, LocalDate from) {
        return jdbc.update("UPDATE person SET retired_from = ? WHERE id = ? AND retired_from IS NULL",
                java.sql.Date.valueOf(from), id) == 1;
    }

    /** Put the person on hold, or take them off it; false (nothing written) when they were in that state already. */
    public boolean hold(long id, boolean held) {
        return jdbc.update("UPDATE person SET held = ? WHERE id = ? AND held = ?", held, id, !held) == 1;
    }
}

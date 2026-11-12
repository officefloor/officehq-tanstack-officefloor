package net.officefloor.hq.app.person;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import net.officefloor.hq.app.history.History;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The people kept per sheet, in the order they were added, each with their dated grades. */
@Repository
public class PersonStore {

    private final JdbcTemplate jdbc;

    public PersonStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The people's grades on the sheet (only the one person when {@code personId} is given), by person. */
    private Map<Long, NavigableMap<LocalDate, String>> grades(String sheetId, Long personId, String table) {
        Map<Long, NavigableMap<LocalDate, String>> grades = new HashMap<>();
        jdbc.query("SELECT person_id, from_date, grade FROM " + table + " WHERE sheet_id = ?"
                + (personId == null ? "" : " AND person_id = " + personId), rs -> {
                    grades.computeIfAbsent(rs.getLong("person_id"), k -> new TreeMap<>())
                            .put(rs.getObject("from_date", LocalDate.class), rs.getString("grade"));
                }, sheetId);
        return grades;
    }

    /** The people's holds on the sheet (only the one person's when {@code personId} is given), by person, oldest first. */
    private Map<Long, List<Hold>> holds(String sheetId, Long personId, String table) {
        Map<Long, List<Hold>> holds = new HashMap<>();
        jdbc.query("SELECT person_id, from_date, to_date FROM " + table + " WHERE sheet_id = ?"
                + (personId == null ? "" : " AND person_id = " + personId) + " ORDER BY id", rs -> {
                    holds.computeIfAbsent(rs.getLong("person_id"), k -> new ArrayList<>()).add(new Hold(
                            rs.getObject("from_date", LocalDate.class), rs.getObject("to_date", LocalDate.class)));
                }, sheetId);
        return holds;
    }

    private List<Person> query(String sheetId, Long personId, String sql, Object... args) {
        return queryFrom(sheetId, personId, "person_grade", "person_hold", sql, args);
    }

    private List<Person> queryFrom(String sheetId, Long personId, String gradeTable, String holdTable, String sql,
            Object... args) {
        Map<Long, NavigableMap<LocalDate, String>> grades = grades(sheetId, personId, gradeTable);
        Map<Long, List<Hold>> holds = holds(sheetId, personId, holdTable);
        RowMapper<Person> row = (rs, i) -> {
            Person p = new Person(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name"),
                    grades.getOrDefault(rs.getLong("id"), new TreeMap<>()), rs.getLong("version"),
                    rs.getObject("retired_from", LocalDate.class), holds.getOrDefault(rs.getLong("id"), List.of()));
            p.setTerms(rs.getObject("terms", Integer.class));
            return p;
        };
        return jdbc.query(sql, row, args);
    }

    public List<Person> list(String sheetId) {
        return query(sheetId, null, "SELECT * FROM person WHERE sheet_id = ? ORDER BY id", sheetId);
    }

    /** The people on the sheet as they stood at the end of the date, with their grades and holds then (see
     *  {@link History}), in the order they were added. */
    public List<Person> listAsOf(String sheetId, LocalDate date) {
        return queryFrom(sheetId, null, History.asOf("person_grade", "person_id, from_date", date),
                History.asOf("person_hold", "id", date),
                "SELECT * FROM " + History.asOf("person", "id", date) + " WHERE sheet_id = ? ORDER BY id", sheetId);
    }

    public Person find(String sheetId, long id) {
        List<Person> found = query(sheetId, id, "SELECT * FROM person WHERE sheet_id = ? AND id = ?", sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The person as they stood at the end of the date, with their grades and holds then (see {@link History});
     *  null when they were not on the sheet then. */
    public Person findAsOf(String sheetId, long id, LocalDate date) {
        List<Person> found = queryFrom(sheetId, id, History.asOf("person_grade", "person_id, from_date", date),
                History.asOf("person_hold", "id", date),
                "SELECT * FROM " + History.asOf("person", "id", date) + " WHERE sheet_id = ? AND id = ?", sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The person on the sheet with this name, ignoring case and the spaces around it; null when none. */
    public Person findByName(String sheetId, String name) {
        List<Person> found = query(sheetId, null, "SELECT * FROM person WHERE sheet_id = ? AND name_key = LOWER(?)",
                sheetId, name.trim());
        return found.isEmpty() ? null : found.get(0);
    }

    /** Whether another person on the sheet (not {@code exceptId}) has this name, ignoring case. */
    public boolean nameTaken(String sheetId, String name, Long exceptId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND name_key = LOWER(?) AND id <> ?",
                Integer.class, sheetId, name.trim(), exceptId == null ? -1L : exceptId);
        return n != null && n > 0;
    }

    /** Adds the person with their first grade, which applies from the beginning of time, and no terms of their own. */
    public Person add(String sheetId, String name, String grade) {
        return add(sheetId, name, grade, null);
    }

    /** Adds the person with their first grade, which applies from the beginning of time, and their own
     *  terms (null for none). */
    public Person add(String sheetId, String name, String grade, Integer terms) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, terms) VALUES (?, ?, ?)", new String[] { "ID" });
            ps.setString(1, sheetId);
            ps.setString(2, name);
            ps.setObject(3, terms, java.sql.Types.INTEGER);
            return ps;
        }, keys);
        long id = keys.getKey().longValue();
        jdbc.update("INSERT INTO person_grade (sheet_id, person_id, from_date, grade) VALUES (?, ?, ?, ?)", sheetId,
                id, Person.START, grade);
        Person person = new Person(id, sheetId, name, new TreeMap<>(Map.of(Person.START, grade)), 0);
        person.setTerms(terms);
        return person;
    }

    /**
     * Stores the person over the version it was loaded at, moving the version on; false (nothing
     * stored) when someone else has stored another version since. The grade the edit records (if any)
     * is kept with the ones already recorded, replacing one from the same date.
     */
    public boolean save(Person person) {
        int n = jdbc.update(
                "UPDATE person SET name = ?, terms = ?, version = version + 1 WHERE sheet_id = ? AND id = ? AND version = ?",
                person.getName(), person.getTerms(), person.getSheetId(), person.getId(), person.getVersion());
        if (n == 1) {
            person.setVersion(person.getVersion() + 1);
            LocalDate from = person.getRecordFrom();
            if (from != null) {
                jdbc.update("MERGE INTO person_grade (sheet_id, person_id, from_date, grade) KEY (person_id, from_date)"
                        + " VALUES (?, ?, ?, ?)", person.getSheetId(), person.getId(), from, person.getGrades().get(from));
            }
        }
        return n == 1;
    }

    /** Retires the person from the date. An edit of their name or grade leaves it as it is. */
    public void retire(Person person, LocalDate from) {
        jdbc.update("UPDATE person SET retired_from = ? WHERE sheet_id = ? AND id = ?", from, person.getSheetId(),
                person.getId());
        person.setRetiredFrom(from);
    }

    /** Adds a hold to the person's holds. An edit of their name or grade leaves them as they are. */
    public void hold(Person person, Hold hold) {
        jdbc.update("INSERT INTO person_hold (sheet_id, person_id, from_date, to_date) VALUES (?, ?, ?, ?)",
                person.getSheetId(), person.getId(), hold.from(), hold.to());
        person.addHold(hold);
    }
}

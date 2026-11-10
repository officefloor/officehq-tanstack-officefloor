package net.officefloor.hq.app.person;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The people store, kept per sheet. */
@Repository
public class PersonRepository {

    private static final String COLUMNS = "id, sheet_id, name, grade, terms, version, statement_count, stated_total, statement_date, retired_from";

    private static final RowMapper<Person> ROW = (rs, n) -> {
        Person person = new Person(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name"),
                rs.getString("grade"), rs.getLong("version"));
        person.setTerms(rs.getObject("terms", Integer.class));
        person.setStatements(rs.getInt("statement_count"), rs.getString("stated_total"),
                rs.getObject("statement_date", LocalDate.class));
        person.setRetiredFrom(rs.getObject("retired_from", LocalDate.class));
        return person;
    };

    private final JdbcTemplate jdbc;

    public PersonRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's people, in the order they were added. */
    public List<Person> findAll(String sheetId) {
        List<Person> people = jdbc.query("SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? ORDER BY id",
                ROW, sheetId);
        Map<Long, Person> byId = new HashMap<>();
        people.forEach((p) -> byId.put(p.getId(), p));
        jdbc.query("SELECT person_id, from_date, grade FROM person_grade WHERE sheet_id = ?",
                (rs) -> {
                    Person person = byId.get(rs.getLong("person_id"));
                    if (person != null) person.setGradeFrom(rs.getString("grade"), rs.getObject("from_date", LocalDate.class));
                }, sheetId);
        jdbc.query("SELECT person_id, patron_id FROM person_patron WHERE sheet_id = ? ORDER BY person_id, patron_id",
                (rs) -> {
                    Person person = byId.get(rs.getLong("person_id"));
                    if (person != null) person.getPatronIds().add(rs.getLong("patron_id"));
                }, sheetId);
        jdbc.query("SELECT person_id, from_date, to_date FROM person_hold WHERE sheet_id = ? ORDER BY id",
                (rs) -> {
                    Person person = byId.get(rs.getLong("person_id"));
                    if (person != null) person.getHolds().add(hold(rs));
                }, sheetId);
        return people;
    }

    /** The person with this id on the sheet, or null when there is none. */
    public Person find(String sheetId, long id) {
        List<Person> found = jdbc.query(
                "SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? AND id = ?",
                ROW, sheetId, id);
        if (found.isEmpty()) return null;
        Person person = found.get(0);
        loadGrades(person);
        loadHolds(person);
        person.getPatronIds().addAll(jdbc.queryForList(
                "SELECT patron_id FROM person_patron WHERE sheet_id = ? AND person_id = ? ORDER BY patron_id",
                Long.class, sheetId, id));
        return person;
    }

    /** Record {@code patron} as one of the person's patrons. */
    public void addPatron(Person person, Person patron) {
        jdbc.update("INSERT INTO person_patron (sheet_id, person_id, patron_id) VALUES (?, ?, ?)",
                person.getSheetId(), person.getId(), patron.getId());
        person.getPatronIds().add(patron.getId());
    }

    /** The person on the sheet with this name, ignoring case and surrounding spaces, or null when there is none. */
    public Person findByName(String sheetId, String name) {
        List<Person> found = jdbc.query("SELECT " + COLUMNS + " FROM person WHERE sheet_id = ?"
                + " AND LOWER(TRIM(name)) = LOWER(TRIM(?)) ORDER BY id", ROW, sheetId, name);
        if (found.isEmpty()) return null;
        loadGrades(found.get(0));
        loadHolds(found.get(0));
        return found.get(0);
    }

    private void loadGrades(Person person) {
        jdbc.query("SELECT from_date, grade FROM person_grade WHERE sheet_id = ? AND person_id = ?",
                (rs) -> {
                    person.setGradeFrom(rs.getString("grade"), rs.getObject("from_date", LocalDate.class));
                }, person.getSheetId(), person.getId());
    }

    private void loadHolds(Person person) {
        jdbc.query("SELECT from_date, to_date FROM person_hold WHERE sheet_id = ? AND person_id = ? ORDER BY id",
                (rs) -> {
                    person.getHolds().add(hold(rs));
                }, person.getSheetId(), person.getId());
    }

    private static Hold hold(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Hold(rs.getObject("from_date", LocalDate.class), rs.getObject("to_date", LocalDate.class));
    }

    /** Record the person's dated grades as they hold them now, replacing those stored. */
    private void saveGrades(Person person) {
        jdbc.update("DELETE FROM person_grade WHERE sheet_id = ? AND person_id = ?",
                person.getSheetId(), person.getId());
        person.getDatedGrades().forEach((from, grade) -> jdbc.update(
                "INSERT INTO person_grade (sheet_id, person_id, from_date, grade) VALUES (?, ?, ?, ?)",
                person.getSheetId(), person.getId(), Date.valueOf(from), grade));
    }

    /** Whether a person other than {@code exceptId} has this name on the sheet, ignoring case. */
    public boolean nameTaken(String sheetId, String name, Long exceptId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND LOWER(TRIM(name)) = LOWER(TRIM(?))"
                        + " AND id <> ?",
                Integer.class, sheetId, name, exceptId == null ? -1L : exceptId);
        return count != null && count > 0;
    }

    /**
     * Record the person's statements (their count, last due-total and date), leaving the rest as stored.
     * The last statement is kept as the one numbered by the count; any numbered after it are dropped.
     */
    public void saveStatements(Person person) {
        Date date = person.getStatementDate() == null ? null : Date.valueOf(person.getStatementDate());
        jdbc.update("UPDATE person SET statement_count = ?, stated_total = ?, statement_date = ? WHERE id = ? AND sheet_id = ?",
                person.getStatementCount(), person.getStatedTotal(), date,
                person.getId(), person.getSheetId());
        jdbc.update("DELETE FROM person_statement WHERE sheet_id = ? AND person_id = ? AND seq >= ?",
                person.getSheetId(), person.getId(), person.getStatementCount());
        if (person.getStatementCount() > 0) {
            jdbc.update("INSERT INTO person_statement (sheet_id, person_id, seq, stated_total, statement_date) VALUES (?, ?, ?, ?, ?)",
                    person.getSheetId(), person.getId(), person.getStatementCount(), person.getStatedTotal(), date);
        }
    }

    /**
     * The person's statement numbered {@code seq} (from 1): its due-total and date, or null when it
     * was not kept (taken before statements were kept), or there is none.
     */
    public StatedFigure findStatement(Person person, int seq) {
        List<StatedFigure> found = jdbc.query(
                "SELECT stated_total, statement_date FROM person_statement WHERE sheet_id = ? AND person_id = ? AND seq = ?",
                (rs, n) -> new StatedFigure(rs.getString("stated_total"), rs.getObject("statement_date", LocalDate.class)),
                person.getSheetId(), person.getId(), seq);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Record the date the person is retired from, leaving the rest as stored. */
    public void saveRetired(Person person) {
        jdbc.update("UPDATE person SET retired_from = ? WHERE id = ? AND sheet_id = ?",
                person.getRetiredFrom() == null ? null : Date.valueOf(person.getRetiredFrom()),
                person.getId(), person.getSheetId());
    }

    /** Record a new hold on the person, leaving the rest as stored. */
    public void addHold(Person person, Hold hold) {
        jdbc.update("INSERT INTO person_hold (sheet_id, person_id, from_date, to_date) VALUES (?, ?, ?, ?)",
                person.getSheetId(), person.getId(), Date.valueOf(hold.from()),
                hold.to() == null ? null : Date.valueOf(hold.to()));
    }

    /**
     * Insert a new person (assigning its id) or update an existing one, raising its version. False
     * when the stored person is no longer at the version it was loaded at: nothing is written.
     */
    public boolean save(Person person) {
        if (person.getId() != null) {
            int updated = jdbc.update("UPDATE person SET name = ?, grade = ?, terms = ?, version = version + 1"
                    + " WHERE id = ? AND sheet_id = ? AND version = ?",
                    person.getName(), person.getGrade(), person.getTerms(), person.getId(), person.getSheetId(),
                    person.getVersion());
            if (updated == 0) return false;
            person.setVersion(person.getVersion() + 1);
            saveGrades(person);
            return true;
        }
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade, terms) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, person.getSheetId());
            ps.setString(2, person.getName());
            ps.setString(3, person.getGrade());
            ps.setObject(4, person.getTerms(), java.sql.Types.INTEGER);
            return ps;
        }, keys);
        person.setId(keys.getKey().longValue());
        saveGrades(person);
        return true;
    }
}

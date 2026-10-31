package net.officefloor.hq.app.person;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The people of each sheet, kept in the order they were added. */
@Repository
public class People {

    private static final String COLUMNS = "SELECT id, sheet_id, name, grade, terms, version, statement_count, stated_total, statement_date, retired_from, "
            + "(SELECT today FROM sheet WHERE sheet.id = person.sheet_id) AS sheet_today FROM person";

    private final JdbcTemplate jdbc;

    public People(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Person> onSheet(String sheetId) {
        List<Person> people = jdbc.query(COLUMNS + " WHERE sheet_id = ? ORDER BY id",
                People::person,
                sheetId);
        Map<Long, List<Long>> patrons = new HashMap<>();
        jdbc.query("SELECT person_id, patron_id FROM person_patron WHERE sheet_id = ? ORDER BY id",
                (ResultSet rs) -> {
                    patrons.computeIfAbsent(rs.getLong("person_id"), k -> new ArrayList<>()).add(rs.getLong("patron_id"));
                }, sheetId);
        Map<Long, TreeMap<LocalDate, String>> grades = new HashMap<>();
        jdbc.query("SELECT person_id, grade, from_date FROM person_grade WHERE sheet_id = ?",
                (ResultSet rs) -> {
                    grades.computeIfAbsent(rs.getLong("person_id"), k -> new TreeMap<>())
                            .put(fromKey(rs.getDate("from_date")), rs.getString("grade"));
                }, sheetId);
        Map<Long, List<Hold>> holds = new HashMap<>();
        jdbc.query("SELECT person_id, from_date, to_date FROM person_hold WHERE sheet_id = ? ORDER BY id",
                (ResultSet rs) -> {
                    holds.computeIfAbsent(rs.getLong("person_id"), k -> new ArrayList<>()).add(hold(rs));
                }, sheetId);
        for (Person person : people) {
            person.setHolds(holds.getOrDefault(person.getId(), new ArrayList<>()));
            person.setPatronIds(patrons.getOrDefault(person.getId(), new ArrayList<>()));
            TreeMap<LocalDate, String> dated = grades.get(person.getId());
            if (dated != null) person.setGrades(dated);
        }
        return people;
    }

    /** The person with this id on this sheet, or null when there is none. */
    public Person find(String sheetId, long id) {
        return withPatrons(jdbc.query(COLUMNS + " WHERE sheet_id = ? AND id = ?",
                People::person,
                sheetId, id).stream().findFirst().orElse(null));
    }

    /** The person with this id on this sheet, read afresh and locked for the rest of the
     *  transaction, so two statements for one person at once happen one after the other. */
    public Person lock(String sheetId, long id) {
        return withPatrons(jdbc.query(COLUMNS + " WHERE sheet_id = ? AND id = ? FOR UPDATE", People::person, sheetId, id)
                .stream().findFirst().orElse(null));
    }

    /** The person on this sheet with this name (matched as names are kept unique: without the
     *  spaces around it and in any case), or null when there is none. */
    public Person named(String sheetId, String name) {
        return withPatrons(jdbc.query(COLUMNS + " WHERE sheet_id = ? AND name_key = LOWER(TRIM(?))", People::person,
                sheetId, name).stream().findFirst().orElse(null));
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
            int n = jdbc.update("UPDATE person SET name = ?, terms = ?, version = version + 1 WHERE id = ? AND version = ?",
                    person.getName(), person.getTerms(), person.getId(), person.getVersion());
            if (n == 0) return false;
            person.setVersion(person.getVersion() + 1);
            Person.DatedGrade dated = person.getNewGrade();
            if (dated != null) saveGrade(person, dated);
            return true;
        }
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade, terms) VALUES (?, ?, ?, ?)", new String[] { "id" });
            ps.setString(1, person.getSheetId());
            ps.setString(2, person.getName());
            ps.setString(3, person.getFirstGrade());
            ps.setObject(4, person.getTerms(), java.sql.Types.INTEGER);
            return ps;
        }, keys);
        person.setId(keys.getKey().longValue());
        // The first grade a person is given applies from the beginning of time.
        saveGrade(person, new Person.DatedGrade(person.getFirstGrade(), null));
        return true;
    }

    /** Record a statement taken for the person (their count, last due-total and date). Not an edit:
     *  the person's version is untouched, so an editor open on them can still save. */
    public void saveStatement(Person person) {
        jdbc.update("UPDATE person SET statement_count = ?, stated_total = ?, statement_date = ? WHERE id = ?",
                person.getStatementCount(), person.getStatedTotal(), Date.valueOf(person.getStatementDate()),
                person.getId());
        jdbc.update("INSERT INTO person_statement (sheet_id, person_id, seq, stated_total, statement_date) VALUES (?, ?, ?, ?, ?)",
                person.getSheetId(), person.getId(), person.getStatementCount(), person.getStatedTotal(),
                Date.valueOf(person.getStatementDate()));
    }

    /** Cancel the person's last statement: their count goes down by one and they show the one
     *  before it (its due-total and date), or none. Not an edit: the person's version is untouched. */
    public void cancelStatement(Person person) {
        jdbc.update("DELETE FROM person_statement WHERE person_id = ? AND seq >= ?",
                person.getId(), person.getStatementCount());
        int count = person.getStatementCount() - 1;
        List<Object[]> before = jdbc.query(
                "SELECT stated_total, statement_date FROM person_statement WHERE person_id = ? AND seq = ?",
                (rs, n) -> new Object[] { rs.getBigDecimal(1), rs.getDate(2) }, person.getId(), count);
        person.setStatementCount(count);
        person.setStatedTotal(before.isEmpty() ? null : (java.math.BigDecimal) before.get(0)[0]);
        person.setStatementDate(before.isEmpty() ? null : ((Date) before.get(0)[1]).toLocalDate());
        jdbc.update("UPDATE person SET statement_count = ?, stated_total = ?, statement_date = ? WHERE id = ?",
                count, person.getStatedTotal(),
                person.getStatementDate() != null ? Date.valueOf(person.getStatementDate()) : null, person.getId());
    }

    /** Record the person's retirement date. Not an edit: the person's version is untouched. */
    public void saveRetirement(Person person) {
        jdbc.update("UPDATE person SET retired_from = ? WHERE id = ?",
                person.getRetiredFrom() != null ? Date.valueOf(person.getRetiredFrom()) : null, person.getId());
    }

    /** Add a hold to the person. Not an edit: the person's version is untouched. */
    public void addHold(Person person, Hold hold) {
        jdbc.update("INSERT INTO person_hold (sheet_id, person_id, from_date, to_date) VALUES (?, ?, ?, ?)",
                person.getSheetId(), person.getId(), Date.valueOf(hold.from()),
                hold.to() != null ? Date.valueOf(hold.to()) : null);
        person.getHolds().add(hold);
    }

    /** Name `patron` as one of the person's patrons. Not an edit: the person's version is untouched. */
    public void addPatron(Person person, Person patron) {
        jdbc.update("INSERT INTO person_patron (sheet_id, person_id, patron_id) VALUES (?, ?, ?)",
                person.getSheetId(), person.getId(), patron.getId());
        person.getPatronIds().add(patron.getId());
    }

    /** Record the person's grade from its date, replacing the one they had from that date. */
    private void saveGrade(Person person, Person.DatedGrade dated) {
        Date from = dated.from() != null ? Date.valueOf(dated.from()) : null;
        jdbc.update("DELETE FROM person_grade WHERE person_id = ? AND from_date IS NOT DISTINCT FROM ?",
                person.getId(), from);
        jdbc.update("INSERT INTO person_grade (sheet_id, person_id, grade, from_date) VALUES (?, ?, ?, ?)",
                person.getSheetId(), person.getId(), dated.grade(), from);
    }

    private static LocalDate fromKey(Date from) {
        return from != null ? from.toLocalDate() : LocalDate.MIN;
    }

    /** The person (may be null) with their patrons' ids and dated grades read in. */
    private Person withPatrons(Person person) {
        if (person != null) {
            TreeMap<LocalDate, String> grades = new TreeMap<>();
            jdbc.query("SELECT grade, from_date FROM person_grade WHERE person_id = ?",
                    (ResultSet rs) -> {
                        grades.put(fromKey(rs.getDate("from_date")), rs.getString("grade"));
                    }, person.getId());
            if (!grades.isEmpty()) person.setGrades(grades);
            person.setHolds(new ArrayList<>(jdbc.query(
                    "SELECT from_date, to_date FROM person_hold WHERE person_id = ? ORDER BY id",
                    (rs, i) -> hold(rs), person.getId())));
            person.setPatronIds(new ArrayList<>(jdbc.queryForList(
                    "SELECT patron_id FROM person_patron WHERE person_id = ? ORDER BY id", Long.class, person.getId())));
        }
        return person;
    }

    private static Hold hold(ResultSet rs) throws SQLException {
        Date to = rs.getDate("to_date");
        return new Hold(rs.getDate("from_date").toLocalDate(), to != null ? to.toLocalDate() : null);
    }

    private static Person person(ResultSet rs, int i) throws SQLException {
        Person person = new Person(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name"),
                rs.getString("grade"), rs.getInt("version"));
        person.setTerms(rs.getObject("terms", Integer.class));
        person.setStatementCount(rs.getInt("statement_count"));
        person.setStatedTotal(rs.getBigDecimal("stated_total"));
        Date date = rs.getDate("statement_date");
        person.setStatementDate(date != null ? date.toLocalDate() : null);
        Date retired = rs.getDate("retired_from");
        person.setRetiredFrom(retired != null ? retired.toLocalDate() : null);
        Date today = rs.getDate("sheet_today");
        person.setToday(today != null ? today.toLocalDate() : LocalDate.now());
        return person;
    }
}

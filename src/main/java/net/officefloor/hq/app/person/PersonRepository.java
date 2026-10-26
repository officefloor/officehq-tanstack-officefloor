package net.officefloor.hq.app.person;

import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The people on each sheet; ids are assigned in the order people are added. */
@Repository
public class PersonRepository {

    /** A person's columns, with the grade in force on the sheet's today, their statement count and their last statement's due-total and date. */
    private static final String COLUMNS = "p.id, p.sheet_id, p.name, p.version, p.retired_from, p.terms, p.contact,"
            + " (SELECT g.grade FROM person_grade g WHERE g.person_id = p.id AND (g.from_date IS NULL"
            + " OR g.from_date <= (SELECT t.today FROM sheet t WHERE t.id = p.sheet_id))"
            + " ORDER BY g.from_date DESC NULLS LAST LIMIT 1) AS grade,"
            + " (SELECT COUNT(*) FROM statement s WHERE s.person_id = p.id) AS statement_count,"
            + " (SELECT s.due_total FROM statement s WHERE s.person_id = p.id ORDER BY s.id DESC LIMIT 1) AS stated_total,"
            + " (SELECT s.taken_on FROM statement s WHERE s.person_id = p.id ORDER BY s.id DESC LIMIT 1) AS statement_date";

    private static final RowMapper<Person> ROW = (rs, n) -> {
        Person person = new Person(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name"),
                rs.getString("grade"), rs.getLong("version"));
        person.setStatements(new Statements(rs.getInt("statement_count"), rs.getBigDecimal("stated_total"),
                rs.getObject("statement_date", LocalDate.class)));
        person.setRetiredFrom(rs.getObject("retired_from", LocalDate.class));
        person.setTerms(rs.getObject("terms", Integer.class));
        person.setContact(rs.getString("contact"));
        return person;
    };

    private static final RowMapper<GradeFrom> GRADE = (rs, n) -> new GradeFrom(rs.getString("grade"),
            rs.getObject("from_date", LocalDate.class));

    static final String CHANGED = "Someone else has saved this person since you opened it; the form now shows their changes";

    static final String NAME_TAKEN = "That name is already in use on this sheet";

    static final String CONTACT_TAKEN = "That contact is already another person's on this sheet";

    private final JdbcTemplate jdbc;

    public PersonRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's people, in the order they were added. */
    public List<Person> list(String sheetId) {
        return withPatrons(sheetId,
                jdbc.query("SELECT " + COLUMNS + " FROM person p WHERE p.sheet_id = ? ORDER BY p.id", ROW, sheetId));
    }

    public Person find(String sheetId, long id) {
        return withPatrons(sheetId,
                jdbc.query("SELECT " + COLUMNS + " FROM person p WHERE p.sheet_id = ? AND p.id = ?", ROW, sheetId, id))
                .stream().findFirst().orElse(null);
    }

    /** The person on the sheet with this name, ignoring case and surrounding spaces; null when none. */
    public Person findByName(String sheetId, String name) {
        return withPatrons(sheetId, jdbc.query(
                "SELECT " + COLUMNS + " FROM person p WHERE p.sheet_id = ? AND p.name_key = LOWER(TRIM(?))", ROW, sheetId,
                name)).stream().findFirst().orElse(null);
    }

    /** Whether another person on the sheet has this name, ignoring case and surrounding spaces. */
    public boolean nameTaken(String sheetId, String name, Long exceptId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND name_key = LOWER(TRIM(?)) AND id <> ?",
                Integer.class, sheetId, name, exceptId == null ? -1L : exceptId);
        return n != null && n > 0;
    }

    /** Whether another person on the sheet has this contact; false for no contact (null). */
    public boolean contactTaken(String sheetId, String contact, Long exceptId) {
        if (contact == null) {
            return false;
        }
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM person WHERE sheet_id = ? AND contact = ? AND id <> ?",
                Integer.class, sheetId, contact, exceptId == null ? -1L : exceptId);
        return n != null && n > 0;
    }

    /** Add the person, their grade in force from the beginning of time. */
    public Person insert(String sheetId, String name, String grade) {
        return insert(sheetId, name, grade, null, null);
    }

    /** Add the person with their own terms and contact (null for none), their grade in force from the beginning of time. */
    public Person insert(String sheetId, String name, String grade, Integer terms, String contact) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, terms, contact) VALUES (?, ?, ?, ?)", new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setString(2, name);
            ps.setObject(3, terms, java.sql.Types.INTEGER);
            ps.setString(4, contact);
            return ps;
        }, keys);
        long id = keys.getKey().longValue();
        jdbc.update("INSERT INTO person_grade (person_id, grade, from_date) VALUES (?, ?, NULL)", id, grade);
        Person person = new Person(id, sheetId, name, grade, 0);
        person.setGrades(List.of(new GradeFrom(grade, null)));
        person.setTerms(terms);
        person.setContact(contact);
        return person;
    }

    /** Give the person the grade from the date, replacing one given from that same date and keeping the rest;
     *  the person's grades are then re-read, and their grade set to the one in force on {@code today}. */
    public void giveGrade(Person person, String grade, LocalDate from, LocalDate today) {
        jdbc.update("DELETE FROM person_grade WHERE person_id = ? AND from_date = ?", person.getId(),
                java.sql.Date.valueOf(from));
        jdbc.update("INSERT INTO person_grade (person_id, grade, from_date) VALUES (?, ?, ?)", person.getId(), grade,
                java.sql.Date.valueOf(from));
        person.setGrades(jdbc.query("SELECT grade, from_date FROM person_grade WHERE person_id = ?", GRADE,
                person.getId()));
        person.setGrade(person.gradeOn(today));
    }

    /** Saves the person if it is still at the version it was loaded at, moving it to the next
     *  version; false (nothing written) when someone else saved it in the meantime. */
    public boolean update(Person person) {
        int n = jdbc.update("UPDATE person SET name = ?, terms = ?, contact = ?, version = version + 1"
                + " WHERE id = ? AND version = ?", person.getName(), person.getTerms(), person.getContact(), person.getId(), person.getVersion());
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

    /** Add a hold to the person. */
    public void addHold(long personId, HoldSpan hold) {
        jdbc.update("INSERT INTO person_hold (person_id, from_date, to_date) VALUES (?, ?, ?)", personId,
                hold.from() == null ? null : java.sql.Date.valueOf(hold.from()),
                hold.to() == null ? null : java.sql.Date.valueOf(hold.to()));
    }

    /** Name the patron as the person's patron; false (nothing written) when they are one already. */
    public boolean addPatron(long personId, long patronId) {
        try {
            return jdbc.update("INSERT INTO person_patron (person_id, patron_id) SELECT ?, ? WHERE NOT EXISTS"
                    + " (SELECT 1 FROM person_patron WHERE person_id = ? AND patron_id = ?)",
                    personId, patronId, personId, patronId) == 1;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /** Sets each person's patrons' names, sorted, their grades and their holds. */
    private List<Person> withPatrons(String sheetId, List<Person> people) {
        if (people.isEmpty()) {
            return people;
        }
        Map<Long, List<GradeFrom>> grades = jdbc.query(
                "SELECT g.person_id, g.grade, g.from_date FROM person_grade g JOIN person q ON q.id = g.person_id"
                        + " WHERE q.sheet_id = ?",
                (rs, n) -> Map.entry(rs.getLong("person_id"), GRADE.mapRow(rs, n)), sheetId).stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        for (Person person : people) {
            person.setGrades(grades.getOrDefault(person.getId(), List.of()));
        }
        Map<Long, List<HoldSpan>> holds = jdbc.query(
                "SELECT h.person_id, h.from_date, h.to_date FROM person_hold h JOIN person q ON q.id = h.person_id"
                        + " WHERE q.sheet_id = ? ORDER BY h.id",
                (rs, n) -> Map.entry(rs.getLong("person_id"), new HoldSpan(rs.getObject("from_date", LocalDate.class),
                        rs.getObject("to_date", LocalDate.class))), sheetId).stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        for (Person person : people) {
            person.setHolds(holds.getOrDefault(person.getId(), List.of()));
        }
        Map<Long, List<String>> patrons = jdbc.query(
                "SELECT pp.person_id, q.name FROM person_patron pp JOIN person q ON q.id = pp.patron_id"
                        + " WHERE q.sheet_id = ?",
                (rs, n) -> Map.entry(rs.getLong("person_id"), rs.getString("name")), sheetId).stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        for (Person person : people) {
            person.setPatrons(patrons.getOrDefault(person.getId(), List.of()).stream().sorted().toList());
        }
        return people;
    }
}

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PersonRepository {

    private static final String COLUMNS = "p.id, p.sheet_id, p.name, p.grade, p.version, p.statement_count, p.stated_total,"
            + " p.statement_date, p.retired_from, p.terms, p.contact FROM person p";

    private final JdbcTemplate jdbc;

    public PersonRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's people, in the order they were added. */
    public List<Person> findAll(String sheetId) {
        return withGrades(sheetId, jdbc.query("SELECT " + COLUMNS + " WHERE p.sheet_id = ? ORDER BY p.id",
                PersonRepository::map, sheetId));
    }

    /** The person with this id on this sheet, or null. */
    public Person find(String sheetId, long id) {
        List<Person> found = withGrades(sheetId, jdbc.query("SELECT " + COLUMNS + " WHERE p.sheet_id = ? AND p.id = ?",
                PersonRepository::map, sheetId, id));
        return found.isEmpty() ? null : found.get(0);
    }

    /** The person on this sheet with this name or alias, ignoring case, or null. */
    public Person findByName(String sheetId, String name) {
        List<Person> found = withGrades(sheetId, jdbc.query(
                "SELECT " + COLUMNS + " WHERE p.sheet_id = ? AND (p.name_key = LOWER(?) OR EXISTS (SELECT 1 FROM"
                        + " person_alias a WHERE a.sheet_id = p.sheet_id AND a.person_id = p.id AND a.alias_key = LOWER(?)))",
                PersonRepository::map, sheetId, name, name));
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * Whether a person other than {@code exceptId} on the sheet has this name, or anyone on the sheet (themselves
     * included) has it as an alias, ignoring case.
     */
    public boolean nameTaken(String sheetId, String name, Long exceptId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND name_key = LOWER(?) AND (? IS NULL OR id <> ?)",
                Integer.class, sheetId, name, exceptId, exceptId);
        Integer a = jdbc.queryForObject("SELECT COUNT(*) FROM person_alias WHERE sheet_id = ? AND alias_key = LOWER(?)",
                Integer.class, sheetId, name);
        return (n != null && n > 0) || (a != null && a > 0);
    }

    /** Whether a person other than {@code exceptId} on the sheet has this contact. */
    public boolean contactTaken(String sheetId, String contact, Long exceptId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND contact = ? AND (? IS NULL OR id <> ?)",
                Integer.class, sheetId, contact, exceptId, exceptId);
        return n != null && n > 0;
    }

    /**
     * Insert a new person (null id) or update an existing one, bumping its version and storing its grades as
     * the person carries them; the person as stored, or null when the stored version is no longer the one the
     * person was loaded at. A new person's grade is recorded from the beginning of time.
     */
    public Person save(Person person) {
        if (person.id() != null) {
            int n = jdbc.update(
                    "UPDATE person SET name = ?, terms = ?, contact = ?, version = version + 1 WHERE id = ? AND sheet_id = ? AND version = ?",
                    person.name(), person.terms(), person.contact(), person.id(), person.sheetId(), person.version());
            if (n == 0) {
                return null;
            }
            jdbc.update("DELETE FROM person_grade WHERE person_id = ? AND sheet_id = ?", person.id(), person.sheetId());
            for (GradeFrom g : person.grades()) {
                insertGrade(person.sheetId(), person.id(), g);
            }
            return new Person(person.id(), person.sheetId(), person.name(), person.grade(), person.version() + 1,
                    person.statementCount(), person.statedTotal(), person.statementDate(), person.retiredFrom(),
                    person.holds(), person.grades(), person.terms(), person.contact());
        }
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade, terms, contact) VALUES (?, ?, ?, ?, ?)", new String[] { "id" });
            ps.setString(1, person.sheetId());
            ps.setString(2, person.name());
            ps.setString(3, person.grade());
            ps.setObject(4, person.terms());
            ps.setString(5, person.contact());
            return ps;
        }, keys);
        long id = keys.getKey().longValue();
        insertGrade(person.sheetId(), id, new GradeFrom(person.grade(), null));
        return new Person(id, person.sheetId(), person.name(), person.grade(), 0, 0, null, null, null, List.of())
                .withTerms(person.terms()).withContact(person.contact());
    }

    private void insertGrade(String sheetId, long personId, GradeFrom grade) {
        jdbc.update("INSERT INTO person_grade (sheet_id, person_id, grade, from_date) VALUES (?, ?, ?, ?)", sheetId,
                personId, grade.grade(), grade.from() == null ? null : Date.valueOf(grade.from()));
    }

    /**
     * The people with the grades and holds recorded for them, each showing the grade in force on the sheet's today;
     * their holds oldest first.
     */
    private List<Person> withGrades(String sheetId, List<Person> found) {
        if (found.isEmpty()) {
            return found;
        }
        Map<Long, List<GradeFrom>> grades = new HashMap<>();
        jdbc.query("SELECT person_id, grade, from_date FROM person_grade WHERE sheet_id = ?", (rs) -> {
            grades.computeIfAbsent(rs.getLong("person_id"), (k) -> new ArrayList<>())
                    .add(new GradeFrom(rs.getString("grade"), rs.getObject("from_date", LocalDate.class)));
        }, sheetId);
        Map<Long, List<Hold>> holds = new HashMap<>();
        jdbc.query("SELECT person_id, from_date, to_date FROM person_hold WHERE sheet_id = ? ORDER BY id", (rs) -> {
            holds.computeIfAbsent(rs.getLong("person_id"), (k) -> new ArrayList<>()).add(new Hold(
                    rs.getObject("from_date", LocalDate.class), rs.getObject("to_date", LocalDate.class)));
        }, sheetId);
        List<LocalDate> today = jdbc.query("SELECT today FROM sheet WHERE id = ?",
                (rs, n) -> rs.getObject("today", LocalDate.class), sheetId);
        LocalDate on = today.isEmpty() ? LocalDate.now() : today.get(0);
        List<Person> people = new ArrayList<>();
        for (Person p : found) {
            List<GradeFrom> own = grades.getOrDefault(p.id(), List.of(new GradeFrom(p.grade(), null)));
            people.add(new Person(p.id(), p.sheetId(), p.name(), Person.gradeOn(own, on, p.grade()), p.version(),
                    p.statementCount(), p.statedTotal(), p.statementDate(), p.retiredFrom(),
                    List.copyOf(holds.getOrDefault(p.id(), List.of())), List.copyOf(own), p.terms(), p.contact()));
        }
        return people;
    }

    /**
     * Record a statement just taken of the person: one more statement, with its due-total and date; the
     * person as stored. Not an edit, so it leaves the version alone.
     */
    public Person recordStatement(Person person, String statedTotal, LocalDate date) {
        jdbc.update("UPDATE person SET statement_count = statement_count + 1, stated_total = ?, statement_date = ?"
                + " WHERE id = ? AND sheet_id = ?", statedTotal, Date.valueOf(date), person.id(), person.sheetId());
        jdbc.update("INSERT INTO person_statement (sheet_id, person_id, seq, stated_total, statement_date) VALUES (?, ?,"
                + " (SELECT statement_count FROM person WHERE id = ? AND sheet_id = ?), ?, ?)", person.sheetId(),
                person.id(), person.id(), person.sheetId(), statedTotal, Date.valueOf(date));
        return find(person.sheetId(), person.id());
    }

    /**
     * Cancel the person's last statement: one statement fewer, showing the due-total and date of the one before it
     * (none when it is not known); the person as stored. Not an edit, so it leaves the version alone.
     */
    public Person cancelStatement(Person person) {
        jdbc.update("DELETE FROM person_statement WHERE sheet_id = ? AND person_id = ? AND seq = ?", person.sheetId(),
                person.id(), person.statementCount());
        List<Map<String, Object>> before = jdbc.queryForList("SELECT stated_total, statement_date FROM person_statement"
                + " WHERE sheet_id = ? AND person_id = ? AND seq = ?", person.sheetId(), person.id(),
                person.statementCount() - 1);
        jdbc.update("UPDATE person SET statement_count = statement_count - 1, stated_total = ?, statement_date = ?"
                + " WHERE id = ? AND sheet_id = ?", before.isEmpty() ? null : before.get(0).get("stated_total"),
                before.isEmpty() ? null : before.get(0).get("statement_date"), person.id(), person.sheetId());
        return find(person.sheetId(), person.id());
    }

    /** Retire the person from this date; the person as stored. Not an edit, so it leaves the version alone. */
    public Person retire(Person person, LocalDate from) {
        jdbc.update("UPDATE person SET retired_from = ? WHERE id = ? AND sheet_id = ?", Date.valueOf(from), person.id(),
                person.sheetId());
        return find(person.sheetId(), person.id());
    }

    /** Add a hold to the person; the person as stored. Not an edit, so it leaves the version alone. */
    public Person hold(Person person, Hold hold) {
        jdbc.update("INSERT INTO person_hold (sheet_id, person_id, from_date, to_date) VALUES (?, ?, ?, ?)",
                person.sheetId(), person.id(), hold.from() == null ? null : Date.valueOf(hold.from()),
                hold.to() == null ? null : Date.valueOf(hold.to()));
        return find(person.sheetId(), person.id());
    }

    /**
     * Merge the person ({@code from}, their claims already moved) into the other: their name and then their aliases,
     * in the order added, become the other's newest aliases; their patrons are added to the other's (not the other,
     * no repeats); anyone naming them as patron names the other instead (not themselves, no repeats); their pool
     * memberships move to the other (shares added where the other is already a member). Their statements, grades,
     * holds, watchers and contact are dropped with them.
     */
    public void merge(Person from, Person into) {
        String sheetId = from.sheetId();
        long gone = from.id();
        long kept = into.id();
        List<String> aliases = jdbc.queryForList(
                "SELECT alias FROM person_alias WHERE sheet_id = ? AND person_id = ? ORDER BY id", String.class,
                sheetId, gone);
        jdbc.update("DELETE FROM person_alias WHERE sheet_id = ? AND person_id = ?", sheetId, gone);
        List<String> added = new ArrayList<>();
        added.add(from.name());
        added.addAll(aliases);
        for (String alias : added) {
            jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias) VALUES (?, ?, ?)", sheetId, kept, alias);
        }

        jdbc.update("INSERT INTO person_patron (sheet_id, person_id, patron_id) SELECT sheet_id, ?, patron_id"
                + " FROM person_patron p WHERE sheet_id = ? AND person_id = ? AND patron_id <> ? AND NOT EXISTS"
                + " (SELECT 1 FROM person_patron q WHERE q.person_id = ? AND q.patron_id = p.patron_id)",
                kept, sheetId, gone, kept, kept);
        jdbc.update("INSERT INTO person_patron (sheet_id, person_id, patron_id) SELECT sheet_id, person_id, ?"
                + " FROM person_patron p WHERE sheet_id = ? AND patron_id = ? AND person_id <> ? AND NOT EXISTS"
                + " (SELECT 1 FROM person_patron q WHERE q.person_id = p.person_id AND q.patron_id = ?)",
                kept, sheetId, gone, kept, kept);
        jdbc.update("DELETE FROM person_patron WHERE sheet_id = ? AND (person_id = ? OR patron_id = ?)", sheetId, gone,
                gone);

        jdbc.update("UPDATE pool_member k SET share = share + (SELECT g.share FROM pool_member g WHERE"
                + " g.pool_id = k.pool_id AND g.person_id = ?) WHERE k.person_id = ? AND EXISTS (SELECT 1 FROM"
                + " pool_member g WHERE g.pool_id = k.pool_id AND g.person_id = ?)", gone, kept, gone);
        jdbc.update("DELETE FROM pool_member g WHERE g.person_id = ? AND EXISTS (SELECT 1 FROM pool_member k"
                + " WHERE k.pool_id = g.pool_id AND k.person_id = ?)", gone, kept);
        jdbc.update("UPDATE pool_member SET person_id = ? WHERE person_id = ?", kept, gone);

        jdbc.update("DELETE FROM person_statement WHERE sheet_id = ? AND person_id = ?", sheetId, gone);
        jdbc.update("DELETE FROM person_grade WHERE sheet_id = ? AND person_id = ?", sheetId, gone);
        jdbc.update("DELETE FROM person_hold WHERE sheet_id = ? AND person_id = ?", sheetId, gone);
        jdbc.update("DELETE FROM person_watch WHERE sheet_id = ? AND person_id = ?", sheetId, gone);
        jdbc.update("DELETE FROM person WHERE sheet_id = ? AND id = ?", sheetId, gone);
    }

    private static Person map(ResultSet rs, int n) throws SQLException {
        return new Person(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name"), rs.getString("grade"),
                rs.getInt("version"), rs.getInt("statement_count"), rs.getString("stated_total"),
                rs.getObject("statement_date", LocalDate.class),
                rs.getObject("retired_from", LocalDate.class), List.of()).withTerms(rs.getObject("terms", Integer.class))
                .withContact(rs.getString("contact"));
    }
}

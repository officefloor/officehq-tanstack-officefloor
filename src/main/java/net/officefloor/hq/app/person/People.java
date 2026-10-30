package net.officefloor.hq.app.person;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The person store, kept per sheet. */
@Repository
public class People {

    private static final RowMapper<Person> ROW = (rs, i) -> {
        Person person = new Person(rs.getLong("id"),
                rs.getString("sheet_id"), rs.getString("name"), rs.getString("grade"), rs.getInt("version"));
        person.setStatementCount(rs.getInt("statement_count"));
        person.setStatedTotal(rs.getBigDecimal("stated_total"));
        java.sql.Date stated = rs.getDate("statement_date");
        person.setStatementDate(stated == null ? null : stated.toLocalDate());
        java.sql.Date retired = rs.getDate("retired_from");
        person.setRetiredFrom(retired == null ? null : retired.toLocalDate());
        person.setHeld(rs.getBoolean("held"));
        return person;
    };

    /** Patron names sorted without regard to case, then by case. */
    private static final java.util.Comparator<String> BY_NAME =
            String.CASE_INSENSITIVE_ORDER.thenComparing(java.util.Comparator.naturalOrder());

    private final JdbcTemplate jdbc;
    private final net.officefloor.hq.app.sheet.Sheets sheets;

    public People(JdbcTemplate jdbc, net.officefloor.hq.app.sheet.Sheets sheets) {
        this.jdbc = jdbc;
        this.sheets = sheets;
    }

    /** Fills in each person's dated grades and the sheet's today (the grade in force on it is shown). */
    private List<Person> withGrades(String sheetId, List<Person> found) {
        if (found.isEmpty()) {
            return found;
        }
        java.time.LocalDate today = sheets.today(sheetId);
        java.util.Map<Long, java.util.Map<java.time.LocalDate, String>> grades = new java.util.HashMap<>();
        jdbc.query("SELECT person_id, from_date, grade FROM person_grade WHERE sheet_id = ? ORDER BY seq", rs -> {
            java.sql.Date from = rs.getDate("from_date");
            grades.computeIfAbsent(rs.getLong("person_id"), k -> new java.util.HashMap<>())
                    .put(from == null ? Person.BEGINNING : from.toLocalDate(), rs.getString("grade"));
        }, sheetId);
        for (Person person : found) {
            person.setGrades(grades.getOrDefault(person.getId(), java.util.Map.of()));
            person.setToday(today);
        }
        return found;
    }

    /** Writes the grade the person's edit recorded, replacing any recorded from the same date. */
    private void writeRecordedGrade(Person person) {
        java.util.Map.Entry<java.time.LocalDate, String> recorded = person.getRecordedGrade();
        if (recorded == null) {
            return;
        }
        java.sql.Date from = recorded.getKey().equals(Person.BEGINNING) ? null : java.sql.Date.valueOf(recorded.getKey());
        if (from == null) {
            jdbc.update("DELETE FROM person_grade WHERE sheet_id = ? AND person_id = ? AND from_date IS NULL",
                    person.getSheetId(), person.getId());
        } else {
            jdbc.update("DELETE FROM person_grade WHERE sheet_id = ? AND person_id = ? AND from_date = ?",
                    person.getSheetId(), person.getId(), from);
        }
        jdbc.update("INSERT INTO person_grade (sheet_id, person_id, from_date, grade) VALUES (?, ?, ?, ?)",
                person.getSheetId(), person.getId(), from, recorded.getValue());
        person.clearRecordedGrade();
    }

    /** The sheet's people in the order they were added. */
    public List<Person> list(String sheetId) {
        return withGrades(sheetId, withPatrons(sheetId, jdbc.query("SELECT * FROM person WHERE sheet_id = ? ORDER BY id", ROW, sheetId)));
    }

    /** Fills in each person's patrons (ids in the order named, names sorted). */
    private List<Person> withPatrons(String sheetId, List<Person> found) {
        if (found.isEmpty()) {
            return found;
        }
        java.util.Map<Long, java.util.List<Long>> ids = new java.util.HashMap<>();
        java.util.Map<Long, java.util.List<String>> names = new java.util.HashMap<>();
        jdbc.query("SELECT pp.person_id, pp.patron_id, p.name FROM person_patron pp"
                + " JOIN person p ON p.sheet_id = pp.sheet_id AND p.id = pp.patron_id"
                + " WHERE pp.sheet_id = ? ORDER BY pp.seq", rs -> {
                    long person = rs.getLong("person_id");
                    ids.computeIfAbsent(person, k -> new java.util.ArrayList<>()).add(rs.getLong("patron_id"));
                    names.computeIfAbsent(person, k -> new java.util.ArrayList<>()).add(rs.getString("name"));
                }, sheetId);
        for (Person person : found) {
            java.util.List<String> sorted = new java.util.ArrayList<>(names.getOrDefault(person.getId(), List.of()));
            sorted.sort(BY_NAME);
            person.setPatrons(new java.util.ArrayList<>(ids.getOrDefault(person.getId(), List.of())), sorted);
        }
        return found;
    }

    /** Records the patron as one of the person's patrons. The person's version is left as it is. */
    public void addPatron(Person person, Person patron) {
        jdbc.update("INSERT INTO person_patron (sheet_id, person_id, patron_id) VALUES (?, ?, ?)",
                person.getSheetId(), person.getId(), patron.getId());
        person.getPatronIds().add(patron.getId());
        person.getPatronNames().add(patron.getName());
        person.getPatronNames().sort(BY_NAME);
    }

    public Person find(String sheetId, long id) {
        List<Person> found = withGrades(sheetId, withPatrons(sheetId, jdbc.query("SELECT * FROM person WHERE sheet_id = ? AND id = ?", ROW, sheetId, id)));
        return found.isEmpty() ? null : found.get(0);
    }

    /** The person on the sheet with this name, compared without regard to case or outer spaces; null when none. */
    public Person findByName(String sheetId, String name) {
        List<Person> found = withGrades(sheetId, jdbc.query("SELECT * FROM person WHERE sheet_id = ? AND LOWER(TRIM(name)) = LOWER(TRIM(?)) ORDER BY id",
                ROW, sheetId, name));
        return found.isEmpty() ? null : found.get(0);
    }

    /** Whether another person on the sheet already has this name, compared without regard to case. */
    public boolean nameTaken(String sheetId, String name, Long exceptId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND LOWER(TRIM(name)) = LOWER(TRIM(?))"
                        + (exceptId == null ? "" : " AND id <> " + exceptId.longValue()),
                Integer.class, sheetId, name);
        return count != null && count > 0;
    }

    public Person add(String sheetId, String name, String grade) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, sheetId);
            ps.setString(2, name);
            ps.setString(3, grade);
            return ps;
        }, keys);
        Person person = new Person(keys.getKey().longValue(), sheetId, name, grade);
        // The first grade a person is given applies from the beginning of time.
        person.recordGrade(Person.BEGINNING, grade);
        writeRecordedGrade(person);
        person.setToday(sheets.today(sheetId));
        return person;
    }

    /** Records a statement taken for the person: one more, with its due-total and date. The person's
     *  version is left as it is — a statement is not an edit. */
    public void saveStatement(Person person) {
        jdbc.update("UPDATE person SET statement_count = ?, stated_total = ?, statement_date = ? WHERE sheet_id = ? AND id = ?",
                person.getStatementCount(), person.getStatedTotal(),
                person.getStatementDate() == null ? null : java.sql.Date.valueOf(person.getStatementDate()),
                person.getSheetId(), person.getId());
    }

    /** Records the person's retirement date. The person's version is left as it is — retiring is
     *  not an edit of the person form. */
    public void saveRetired(Person person) {
        jdbc.update("UPDATE person SET retired_from = ? WHERE sheet_id = ? AND id = ?",
                person.getRetiredFrom() == null ? null : java.sql.Date.valueOf(person.getRetiredFrom()),
                person.getSheetId(), person.getId());
    }

    /** Records whether the person is on hold. The person's version is left as it is — holding is
     *  not an edit of the person form. */
    public void saveHeld(Person person) {
        jdbc.update("UPDATE person SET held = ? WHERE sheet_id = ? AND id = ?",
                person.isHeld(), person.getSheetId(), person.getId());
    }

    /** Saves the person over the version it was loaded at, moving it on by one, with any grade the
     *  edit recorded; false (nothing written) when someone else has saved the person since. */
    public boolean save(Person person) {
        int updated = jdbc.update(
                "UPDATE person SET name = ?, version = version + 1 WHERE sheet_id = ? AND id = ? AND version = ?",
                person.getName(), person.getSheetId(), person.getId(), person.getVersion());
        if (updated == 0) {
            return false;
        }
        writeRecordedGrade(person);
        person.setVersion(person.getVersion() + 1);
        return true;
    }
}

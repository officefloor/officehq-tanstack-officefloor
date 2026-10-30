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
        int terms = rs.getInt("terms");
        person.setTerms(rs.wasNull() ? null : terms);
        person.setContact(rs.getString("contact"));
        person.setStatedTotal(rs.getBigDecimal("stated_total"));
        java.sql.Date stated = rs.getDate("statement_date");
        person.setStatementDate(stated == null ? null : stated.toLocalDate());
        java.sql.Date retired = rs.getDate("retired_from");
        person.setRetiredFrom(retired == null ? null : retired.toLocalDate());
        java.sql.Date added = rs.getDate("added_on");
        person.setAddedOn(added == null ? null : added.toLocalDate());
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

    /** Fills in each person's holds, in the order they were added. */
    private List<Person> withHolds(String sheetId, List<Person> found) {
        if (found.isEmpty()) {
            return found;
        }
        java.util.Map<Long, java.util.List<Person.Hold>> holds = new java.util.HashMap<>();
        jdbc.query("SELECT person_id, from_date, to_date FROM person_hold WHERE sheet_id = ? ORDER BY seq", rs -> {
            java.sql.Date to = rs.getDate("to_date");
            holds.computeIfAbsent(rs.getLong("person_id"), k -> new java.util.ArrayList<>())
                    .add(new Person.Hold(rs.getDate("from_date").toLocalDate(), to == null ? null : to.toLocalDate()));
        }, sheetId);
        for (Person person : found) {
            person.setHolds(new java.util.ArrayList<>(holds.getOrDefault(person.getId(), List.of())));
        }
        return found;
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
        withHolds(sheetId, found);
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
        jdbc.update("INSERT INTO grade_history (sheet_id, person_id, set_on, from_date, grade) VALUES (?, ?, (SELECT today FROM sheet WHERE id = ?), ?, ?)",
                person.getSheetId(), person.getId(), person.getSheetId(), from, recorded.getValue());
        person.clearRecordedGrade();
    }

    /** Records the person's name, own terms and contact as they now stand in their history, dated the sheet's today. */
    private void writeHistory(Person person) {
        jdbc.update("INSERT INTO person_history (sheet_id, person_id, set_on, name, terms, contact) VALUES (?, ?, (SELECT today FROM sheet WHERE id = ?), ?, ?, ?)",
                person.getSheetId(), person.getId(), person.getSheetId(), person.getName(), person.getTerms(), person.getContact());
    }

    /** The sheet's people in the order they were added. */
    public List<Person> list(String sheetId) {
        return withGrades(sheetId, withAliases(sheetId, withPatrons(sheetId, jdbc.query("SELECT * FROM person WHERE sheet_id = ? ORDER BY id", ROW, sheetId))));
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

    /** Fills in each person's aliases, sorted as names are compared. */
    private List<Person> withAliases(String sheetId, List<Person> found) {
        if (found.isEmpty()) {
            return found;
        }
        java.util.Map<Long, java.util.List<String>> aliases = new java.util.HashMap<>();
        jdbc.query("SELECT person_id, alias FROM person_alias WHERE sheet_id = ? ORDER BY seq",
                rs -> {
                    aliases.computeIfAbsent(rs.getLong("person_id"), k -> new java.util.ArrayList<>()).add(rs.getString("alias"));
                }, sheetId);
        for (Person person : found) {
            java.util.List<String> sorted = new java.util.ArrayList<>(aliases.getOrDefault(person.getId(), List.of()));
            sorted.sort(BY_NAME);
            person.setAliases(sorted);
        }
        return found;
    }

    /** Records the alias as one of the person's. The person's version is left as it is. */
    public void addAlias(Person person, String alias) {
        jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias, set_on) VALUES (?, ?, ?, (SELECT today FROM sheet WHERE id = ?))",
                person.getSheetId(), person.getId(), alias, person.getSheetId());
        person.getAliases().add(alias);
        person.getAliases().sort(BY_NAME);
    }

    /** Records the patron as one of the person's patrons. The person's version is left as it is. */
    public void addPatron(Person person, Person patron) {
        jdbc.update("INSERT INTO person_patron (sheet_id, person_id, patron_id, set_on) VALUES (?, ?, ?, (SELECT today FROM sheet WHERE id = ?))",
                person.getSheetId(), person.getId(), patron.getId(), person.getSheetId());
        person.getPatronIds().add(patron.getId());
        person.getPatronNames().add(patron.getName());
        person.getPatronNames().sort(BY_NAME);
    }

    /** Merges the person into another (their claims already moved): their name and aliases become the other's
     *  aliases; their patrons are added to the other's and whoever named them names the other instead (never
     *  themselves, never twice); their pool memberships move to the other, shares added where the other is already
     *  a member; their fixed period figures add to the other's. Their statements, holds, watchers and contact go
     *  with them as they leave the sheet. */
    public void merge(Person from, Person into) {
        String sheetId = from.getSheetId();
        long fromId = from.getId();
        long intoId = into.getId();
        // Names.
        java.util.List<String> names = new java.util.ArrayList<>();
        names.add(from.getName());
        names.addAll(jdbc.queryForList("SELECT alias FROM person_alias WHERE sheet_id = ? AND person_id = ? ORDER BY seq",
                String.class, sheetId, fromId));
        jdbc.update("DELETE FROM person_alias WHERE sheet_id = ? AND person_id = ?", sheetId, fromId);
        for (String name : names) {
            addAlias(into, name);
        }
        // Patrons the merged person named.
        for (Long patron : from.getPatronIds()) {
            if (patron != intoId && !into.getPatronIds().contains(patron)) {
                jdbc.update("INSERT INTO person_patron (sheet_id, person_id, patron_id, set_on) VALUES (?, ?, ?, (SELECT today FROM sheet WHERE id = ?))",
                        sheetId, intoId, patron, sheetId);
                into.getPatronIds().add(patron);
            }
        }
        jdbc.update("DELETE FROM person_patron WHERE sheet_id = ? AND person_id = ?", sheetId, fromId);
        // Whoever named the merged person names the other instead.
        jdbc.update("DELETE FROM person_patron WHERE sheet_id = ? AND patron_id = ? AND (person_id = ? OR person_id IN"
                + " (SELECT person_id FROM person_patron WHERE sheet_id = ? AND patron_id = ?))",
                sheetId, fromId, intoId, sheetId, intoId);
        jdbc.update("UPDATE person_patron SET patron_id = ? WHERE sheet_id = ? AND patron_id = ?", intoId, sheetId, fromId);
        // Pool memberships.
        jdbc.update("UPDATE pool_member m SET share = share + (SELECT f.share FROM pool_member f WHERE f.sheet_id = m.sheet_id"
                + " AND f.pool_name = m.pool_name AND f.person_id = ?) WHERE m.sheet_id = ? AND m.person_id = ? AND EXISTS"
                + " (SELECT 1 FROM pool_member f WHERE f.sheet_id = m.sheet_id AND f.pool_name = m.pool_name AND f.person_id = ?)",
                fromId, sheetId, intoId, fromId);
        jdbc.update("DELETE FROM pool_member WHERE sheet_id = ? AND person_id = ? AND pool_name IN"
                + " (SELECT pool_name FROM pool_member WHERE sheet_id = ? AND person_id = ?)", sheetId, fromId, sheetId, intoId);
        jdbc.update("UPDATE pool_member SET person_id = ? WHERE sheet_id = ? AND person_id = ?", intoId, sheetId, fromId);
        // Fixed period figures.
        jdbc.update("UPDATE period_person_total t SET total = total + (SELECT f.total FROM period_person_total f WHERE f.sheet_id = t.sheet_id"
                + " AND f.period_name = t.period_name AND f.person_id = ?) WHERE t.sheet_id = ? AND t.person_id = ? AND EXISTS"
                + " (SELECT 1 FROM period_person_total f WHERE f.sheet_id = t.sheet_id AND f.period_name = t.period_name AND f.person_id = ?)",
                fromId, sheetId, intoId, fromId);
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND person_id = ? AND period_name IN"
                + " (SELECT period_name FROM period_person_total WHERE sheet_id = ? AND person_id = ?)", sheetId, fromId, sheetId, intoId);
        jdbc.update("UPDATE period_person_total SET person_id = ? WHERE sheet_id = ? AND person_id = ?", intoId, sheetId, fromId);
        // What is dropped with them.
        jdbc.update("DELETE FROM person_hold WHERE sheet_id = ? AND person_id = ?", sheetId, fromId);
        jdbc.update("DELETE FROM person_watch WHERE sheet_id = ? AND person_id = ?", sheetId, fromId);
        jdbc.update("DELETE FROM person_statement WHERE sheet_id = ? AND person_id = ?", sheetId, fromId);
        jdbc.update("DELETE FROM person_grade WHERE sheet_id = ? AND person_id = ?", sheetId, fromId);
        jdbc.update("DELETE FROM person WHERE sheet_id = ? AND id = ?", sheetId, fromId);
    }

    public Person find(String sheetId, long id) {
        List<Person> found = withGrades(sheetId, withAliases(sheetId, withPatrons(sheetId, jdbc.query("SELECT * FROM person WHERE sheet_id = ? AND id = ?", ROW, sheetId, id))));
        return found.isEmpty() ? null : found.get(0);
    }

    /** The person on the sheet with this name or alias, compared without regard to case or outer spaces; null when none. */
    public Person findByName(String sheetId, String name) {
        List<Person> found = withGrades(sheetId, withAliases(sheetId, jdbc.query("SELECT * FROM person WHERE sheet_id = ? AND LOWER(TRIM(name)) = LOWER(TRIM(?)) ORDER BY id",
                ROW, sheetId, name)));
        if (found.isEmpty()) {
            found = withGrades(sheetId, withAliases(sheetId, jdbc.query("SELECT * FROM person WHERE sheet_id = ? AND id IN"
                    + " (SELECT person_id FROM person_alias WHERE sheet_id = ? AND LOWER(TRIM(alias)) = LOWER(TRIM(?))) ORDER BY id",
                    ROW, sheetId, sheetId, name)));
        }
        return found.isEmpty() ? null : found.get(0);
    }

    /** Whether another person on the sheet already has this name, or anyone on the sheet has it as an alias,
     *  compared without regard to case. */
    public boolean nameTaken(String sheetId, String name, Long exceptId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND LOWER(TRIM(name)) = LOWER(TRIM(?))"
                        + (exceptId == null ? "" : " AND id <> " + exceptId.longValue()),
                Integer.class, sheetId, name);
        Integer aliases = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person_alias WHERE sheet_id = ? AND LOWER(TRIM(alias)) = LOWER(TRIM(?))",
                Integer.class, sheetId, name);
        return (count != null && count > 0) || (aliases != null && aliases > 0);
    }

    /** Whether another person on the sheet already has this contact. */
    public boolean contactTaken(String sheetId, String contact, Long exceptId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND contact = ?"
                        + (exceptId == null ? "" : " AND id <> " + exceptId.longValue()),
                Integer.class, sheetId, contact);
        return count != null && count > 0;
    }

    public Person add(String sheetId, String name, String grade) {
        return add(sheetId, name, grade, null, null);
    }

    /** Adds a person with their own terms in days (null for none) and contact (null for none). */
    public Person add(String sheetId, String name, String grade, Integer terms, String contact) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade, terms, contact, added_on) VALUES (?, ?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?))",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, sheetId);
            ps.setString(2, name);
            ps.setString(3, grade);
            ps.setObject(4, terms, java.sql.Types.INTEGER);
            ps.setString(5, contact);
            ps.setString(6, sheetId);
            return ps;
        }, keys);
        Person person = new Person(keys.getKey().longValue(), sheetId, name, grade);
        person.setTerms(terms);
        person.setContact(contact);
        writeHistory(person);
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

    /** Records a statement taken for the person in their statement history. */
    public void addStatement(Person person) {
        jdbc.update("INSERT INTO person_statement (sheet_id, person_id, stated_total, statement_date, set_on) VALUES (?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?))",
                person.getSheetId(), person.getId(), person.getStatedTotal(),
                person.getStatementDate() == null ? null : java.sql.Date.valueOf(person.getStatementDate()), person.getSheetId());
    }

    /** Marks the person's last statement cancelled in their history (kept, dated, for the as-of view) and sets the person's statement to the one
     *  before it (no due-total or date when there is none, or it is not known). The count goes down by one. */
    public void cancelStatement(Person person) {
        List<Long> last = jdbc.query("SELECT seq FROM person_statement WHERE sheet_id = ? AND person_id = ? AND NOT cancelled ORDER BY seq DESC LIMIT 1",
                (rs, i) -> rs.getLong(1), person.getSheetId(), person.getId());
        if (!last.isEmpty()) {
            jdbc.update("UPDATE person_statement SET cancelled = TRUE, cancelled_on = (SELECT today FROM sheet WHERE id = ?) WHERE seq = ?",
                    person.getSheetId(), last.get(0));
        }
        person.setStatementCount(person.getStatementCount() - 1);
        person.setStatedTotal(null);
        person.setStatementDate(null);
        jdbc.query("SELECT stated_total, statement_date FROM person_statement WHERE sheet_id = ? AND person_id = ? AND NOT cancelled ORDER BY seq DESC LIMIT 1",
                rs -> {
                    person.setStatedTotal(rs.getBigDecimal("stated_total"));
                    java.sql.Date date = rs.getDate("statement_date");
                    person.setStatementDate(date == null ? null : date.toLocalDate());
                }, person.getSheetId(), person.getId());
        saveStatement(person);
    }

    /** Records the person's retirement date. The person's version is left as it is — retiring is
     *  not an edit of the person form. */
    public void saveRetired(Person person) {
        jdbc.update("UPDATE person SET retired_from = ? WHERE sheet_id = ? AND id = ?",
                person.getRetiredFrom() == null ? null : java.sql.Date.valueOf(person.getRetiredFrom()),
                person.getSheetId(), person.getId());
        jdbc.update("INSERT INTO retire_history (sheet_id, person_id, set_on, retired_from) VALUES (?, ?, (SELECT today FROM sheet WHERE id = ?), ?)",
                person.getSheetId(), person.getId(), person.getSheetId(),
                person.getRetiredFrom() == null ? null : java.sql.Date.valueOf(person.getRetiredFrom()));
    }

    /** Adds a hold to the person. The person's version is left as it is — holding is not an edit of
     *  the person form. */
    public void addHold(Person person, Person.Hold hold) {
        jdbc.update("INSERT INTO person_hold (sheet_id, person_id, from_date, to_date, set_on) VALUES (?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?))",
                person.getSheetId(), person.getId(), java.sql.Date.valueOf(hold.from()),
                hold.to() == null ? null : java.sql.Date.valueOf(hold.to()), person.getSheetId());
        person.getHolds().add(hold);
    }

    /** Saves the person over the version it was loaded at, moving it on by one, with any grade the
     *  edit recorded; false (nothing written) when someone else has saved the person since. */
    public boolean save(Person person) {
        int updated = jdbc.update(
                "UPDATE person SET name = ?, terms = ?, contact = ?, version = version + 1 WHERE sheet_id = ? AND id = ? AND version = ?",
                person.getName(), person.getTerms(), person.getContact(), person.getSheetId(), person.getId(), person.getVersion());
        if (updated == 0) {
            return false;
        }
        writeRecordedGrade(person);
        writeHistory(person);
        person.setVersion(person.getVersion() + 1);
        return true;
    }
}

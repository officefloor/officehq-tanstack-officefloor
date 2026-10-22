package net.officefloor.hq.app.person;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** People by sheet, in the order they were added. */
@Repository
public class PersonRepository {

    private static final RowMapper<Person> ROW = (rs, i) -> new Person(rs.getLong("id"),
            rs.getString("sheet_id"), rs.getString("name"), null, rs.getLong("version"),
            rs.getInt("statement_count"), rs.getBigDecimal("stated_total"), rs.getObject("statement_date", LocalDate.class),
            rs.getObject("retired_from", LocalDate.class), Grades.NONE, Holds.NONE, (Integer) rs.getObject("terms"));

    private record GradeRow(long personId, Grades.Grade grade) {
    }

    private static final RowMapper<GradeRow> GRADE_ROW = (rs, i) -> new GradeRow(rs.getLong("person_id"),
            new Grades.Grade(rs.getString("grade"), rs.getObject("from_date", LocalDate.class)));

    private record HoldRow(long personId, Holds.Hold hold) {
    }

    private static final RowMapper<HoldRow> HOLD_ROW = (rs, i) -> new HoldRow(rs.getLong("person_id"),
            new Holds.Hold(rs.getObject("from_date", LocalDate.class), rs.getObject("to_date", LocalDate.class)));

    private final JdbcTemplate jdbc;

    public PersonRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Person> list(String sheetId) {
        return graded(sheetId, jdbc.query("SELECT * FROM person WHERE sheet_id = ? ORDER BY id", ROW, sheetId));
    }

    /** The person on this sheet, or null when absent. */
    public Person find(String sheetId, long id) {
        return graded(sheetId, jdbc.query("SELECT * FROM person WHERE sheet_id = ? AND id = ?", ROW, sheetId, id))
                .stream().findFirst().orElse(null);
    }

    /** The person on this sheet with this name (compared as nameInUse compares), or null when absent. */
    public Person findByName(String sheetId, String name) {
        return graded(sheetId, jdbc.query("SELECT * FROM person WHERE sheet_id = ? AND name_key = LOWER(TRIM(?))", ROW,
                sheetId, name)).stream().findFirst().orElse(null);
    }

    /** The people with their dated grades, the grade in force on the sheet's today, and their holds. */
    private List<Person> graded(String sheetId, List<Person> people) {
        if (people.isEmpty()) {
            return people;
        }
        LocalDate today = jdbc.query("SELECT today FROM sheet WHERE id = ?",
                rs -> rs.next() ? rs.getObject(1, LocalDate.class) : null, sheetId);
        Map<Long, List<Grades.Grade>> byPerson = jdbc.query(
                "SELECT * FROM person_grade WHERE sheet_id = ? ORDER BY person_id, from_date NULLS FIRST", GRADE_ROW,
                sheetId).stream().collect(Collectors.groupingBy(GradeRow::personId,
                        Collectors.mapping(GradeRow::grade, Collectors.toList())));
        Map<Long, List<Holds.Hold>> holdsByPerson = jdbc.query(
                "SELECT * FROM person_hold WHERE sheet_id = ? ORDER BY person_id, id", HOLD_ROW, sheetId).stream()
                .collect(Collectors.groupingBy(HoldRow::personId,
                        Collectors.mapping(HoldRow::hold, Collectors.toList())));
        return people.stream().map(p -> {
            Grades grades = new Grades(byPerson.getOrDefault(p.id(), List.of()));
            return new Person(p.id(), p.sheetId(), p.name(), grades.on(today), p.version(), p.statementCount(),
                    p.statedTotal(), p.statementDate(), p.retiredFrom(), grades,
                    new Holds(holdsByPerson.getOrDefault(p.id(), List.of())), p.terms());
        }).toList();
    }

    /** Whether another person on this sheet (other than exceptId, when given) has this name,
     *  compared without regard to case and ignoring surrounding spaces. */
    public boolean nameInUse(String sheetId, String name, Long exceptId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND name_key = LOWER(TRIM(?)) AND id <> ?",
                Integer.class, sheetId, name, exceptId == null ? -1L : exceptId);
        return n != null && n > 0;
    }

    /** Add a person with their first grade, which applies from the beginning of time, and no terms of their own. */
    public Person insert(String sheetId, String name, String grade) {
        return insert(sheetId, name, grade, null);
    }

    /** Add a person with their first grade, which applies from the beginning of time, and their own terms (null: none). */
    public Person insert(String sheetId, String name, String grade, Integer terms) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement("INSERT INTO person (sheet_id, name, terms, added_on) VALUES (?, ?, ?, "
                    + "(SELECT today FROM sheet WHERE id = ?))",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setString(2, name);
            ps.setObject(3, terms, java.sql.Types.INTEGER);
            ps.setString(4, sheetId);
            return ps;
        }, keys);
        long id = keys.getKey().longValue();
        jdbc.update("INSERT INTO person_grade (sheet_id, person_id, grade, from_date) VALUES (?, ?, ?, NULL)",
                sheetId, id, grade);
        jdbc.update("INSERT INTO person_grade_log (sheet_id, person_id, grade, from_date, made_on) VALUES (?, ?, ?, NULL, "
                + "(SELECT today FROM sheet WHERE id = ?))", sheetId, id, grade, sheetId);
        return new Person(id, sheetId, name, grade, 0, 0, null, null, null,
                new Grades(List.of(new Grades.Grade(grade, null))), Holds.NONE, terms);
    }

    /** Lock the person's row until the transaction ends, so concurrent statements of them go one after the other. */
    public void lock(Person person) {
        jdbc.query("SELECT id FROM person WHERE sheet_id = ? AND id = ? FOR UPDATE", rs -> null,
                person.sheetId(), person.id());
    }

    /** Record a statement taken of the person: one more taken, with its due-total (null: n/a) and date. Leaves the version alone: a statement is not an edit of the person. */
    public void stated(Person person, BigDecimal total, LocalDate date) {
        jdbc.update("UPDATE person SET statement_count = statement_count + 1, stated_total = ?, statement_date = ? "
                + "WHERE sheet_id = ? AND id = ?", total, date, person.sheetId(), person.id());
        jdbc.update("INSERT INTO person_statement (sheet_id, person_id, due_total, statement_date, made_on) VALUES (?, ?, ?, ?, "
                + "(SELECT today FROM sheet WHERE id = ?))", person.sheetId(), person.id(), total, date, person.sheetId());
    }

    /** Cancel the last statement taken of the person: one fewer taken, and their row shows the one before it (its
     *  due-total and date; none when there is no earlier one kept). The cancelled one is kept, marked cancelled with
     *  the sheet's today. Leaves the version alone. */
    public void unstated(Person person) {
        List<Long> kept = jdbc.queryForList("SELECT id FROM person_statement WHERE sheet_id = ? AND person_id = ? "
                + "AND NOT cancelled ORDER BY id DESC", Long.class, person.sheetId(), person.id());
        if (!kept.isEmpty()) {
            jdbc.update("UPDATE person_statement SET cancelled = TRUE, cancelled_on = (SELECT today FROM sheet WHERE id = ?) "
                    + "WHERE id = ?", person.sheetId(), kept.get(0));
        }
        jdbc.update("UPDATE person SET statement_count = statement_count - 1, stated_total = NULL, statement_date = NULL "
                + "WHERE sheet_id = ? AND id = ? AND statement_count > 0", person.sheetId(), person.id());
        if (kept.size() > 1) {
            jdbc.update("UPDATE person SET stated_total = (SELECT due_total FROM person_statement WHERE id = ?), "
                    + "statement_date = (SELECT statement_date FROM person_statement WHERE id = ?) "
                    + "WHERE sheet_id = ? AND id = ?", kept.get(1), kept.get(1), person.sheetId(), person.id());
        }
    }

    /** Retire the person from the date. Leaves the version alone: retiring is not an edit of the person. */
    public void retire(Person person, LocalDate from) {
        jdbc.update("UPDATE person SET retired_from = ? WHERE sheet_id = ? AND id = ?", from, person.sheetId(), person.id());
        jdbc.update("INSERT INTO person_retire_log (sheet_id, person_id, from_date, made_on) VALUES (?, ?, ?, "
                + "(SELECT today FROM sheet WHERE id = ?))", person.sheetId(), person.id(), from, person.sheetId());
    }

    /** Add a hold to the person from the from-date to the to-date (null: open-ended), keeping the holds they have.
     *  Leaves the version alone: holding is not an edit of the person. */
    public void hold(Person person, LocalDate from, LocalDate to) {
        jdbc.update("INSERT INTO person_hold (sheet_id, person_id, from_date, to_date, made_on) VALUES (?, ?, ?, ?, "
                + "(SELECT today FROM sheet WHERE id = ?))", person.sheetId(), person.id(), from, to, person.sheetId());
    }

    /** Set the person's own terms in days (null: none). */
    public void terms(Person person, Integer terms) {
        jdbc.update("UPDATE person SET terms = ? WHERE sheet_id = ? AND id = ?", terms, person.sheetId(), person.id());
    }

    /** Store the person's name at its new version, only while the stored person is still at version
     *  `from`; false (nothing written) when it has been saved since. */
    public boolean update(Person person, long from) {
        return jdbc.update("UPDATE person SET name = ?, version = ? WHERE sheet_id = ? AND id = ? AND version = ?",
                person.name(), person.version(), person.sheetId(), person.id(), from) == 1;
    }

    /** Give the person the grade from the from-date (null: from the beginning of time), replacing any they were
     *  given from that same date and keeping the rest. */
    public void grade(Person person, String grade, LocalDate from) {
        Date fromDate = from != null ? Date.valueOf(from) : null;
        jdbc.update("DELETE FROM person_grade WHERE sheet_id = ? AND person_id = ? AND from_date IS NOT DISTINCT FROM ?",
                person.sheetId(), person.id(), fromDate);
        jdbc.update("INSERT INTO person_grade (sheet_id, person_id, grade, from_date) VALUES (?, ?, ?, ?)",
                person.sheetId(), person.id(), grade, fromDate);
        jdbc.update("INSERT INTO person_grade_log (sheet_id, person_id, grade, from_date, made_on) VALUES (?, ?, ?, ?, "
                + "(SELECT today FROM sheet WHERE id = ?))", person.sheetId(), person.id(), grade, fromDate,
                person.sheetId());
    }

    // ---- as of a date: the changes made while the sheet's today was on or before it -------------------------

    /** The sheet's people as they stood at the end of the day, in the order they were added: those added by then,
     *  each with their grades, holds, retirement and statements then, and the grade in force on that day. */
    public List<Person> listAsOf(String sheetId, LocalDate end) {
        return jdbc.query("SELECT * FROM person WHERE sheet_id = ? AND (added_on IS NULL OR added_on <= ?) ORDER BY id",
                ROW, sheetId, end).stream().map(p -> asOf(p, end)).toList();
    }

    private Person asOf(Person p, LocalDate end) {
        Grades grades = gradesAsOf(p, end);
        int count = p.statementCount();
        int rows = 0;
        Long last = null;
        BigDecimal total = null;
        LocalDate date = null;
        for (var row : jdbc.queryForList("SELECT id, due_total, statement_date, made_on, cancelled, cancelled_on "
                + "FROM person_statement WHERE sheet_id = ? AND person_id = ? ORDER BY id", p.sheetId(), p.id())) {
            rows++;
            LocalDate made = toDate(row.get("made_on"));
            LocalDate cancelledOn = toDate(row.get("cancelled_on"));
            boolean cancelled = Boolean.TRUE.equals(row.get("cancelled"));
            boolean madeBy = made == null || !made.isAfter(end);
            boolean cancelledBy = cancelled && (cancelledOn == null || !cancelledOn.isAfter(end));
            if (!cancelled && !madeBy) {
                count--;
            } else if (cancelled && madeBy && !cancelledBy) {
                count++;
            }
            if (madeBy && !cancelledBy) {
                last = ((Number) row.get("id")).longValue();
                total = (BigDecimal) row.get("due_total");
                date = toDate(row.get("statement_date"));
            }
        }
        if (rows == 0) { // none kept: the person's last statement, if any, was taken before history was kept
            total = p.statedTotal();
            date = p.statementDate();
        }
        return new Person(p.id(), p.sheetId(), p.name(), grades.on(end), p.version(), Math.max(count, 0), total, date,
                retiredFromAsOf(p, end), grades, holdsAsOf(p, end), p.terms());
    }

    private static LocalDate toDate(Object value) {
        if (value instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        return value instanceof LocalDate d ? d : null;
    }

    /** Whether the person was on the sheet at the end of the day: added while the sheet's today was on or before it
     *  (or before history was kept). */
    public boolean addedBy(Person person, LocalDate end) {
        LocalDate added = jdbc.query("SELECT added_on FROM person WHERE sheet_id = ? AND id = ?",
                rs -> rs.next() ? rs.getObject(1, LocalDate.class) : null, person.sheetId(), person.id());
        return added == null || !added.isAfter(end);
    }

    /** The person's grades as they stood at the end of the day: each one given from a from-date replaces the one
     *  given before it from that same date. */
    public Grades gradesAsOf(Person person, LocalDate end) {
        Map<LocalDate, String> byFrom = new java.util.LinkedHashMap<>();
        jdbc.query("SELECT grade, from_date FROM person_grade_log WHERE sheet_id = ? AND person_id = ? "
                + "AND (made_on IS NULL OR made_on <= ?) ORDER BY id", rs -> {
                    byFrom.put(rs.getObject(2, LocalDate.class), rs.getString(1));
                }, person.sheetId(), person.id(), end);
        return new Grades(byFrom.entrySet().stream()
                .map(e -> new Grades.Grade(e.getValue(), e.getKey())).toList());
    }

    /** The person's holds as they stood at the end of the day: those put on while the sheet's today was on or before
     *  it. */
    public Holds holdsAsOf(Person person, LocalDate end) {
        return new Holds(jdbc.query("SELECT * FROM person_hold WHERE sheet_id = ? AND person_id = ? "
                + "AND (made_on IS NULL OR made_on <= ?) ORDER BY id", HOLD_ROW, person.sheetId(), person.id(), end)
                .stream().map(HoldRow::hold).toList());
    }

    /** The date the person was retired from at the end of the day (null: not retired then). */
    public LocalDate retiredFromAsOf(Person person, LocalDate end) {
        return jdbc.query("SELECT from_date FROM person_retire_log WHERE sheet_id = ? AND person_id = ? "
                + "AND (made_on IS NULL OR made_on <= ?) ORDER BY id DESC LIMIT 1",
                rs -> rs.next() ? rs.getObject(1, LocalDate.class) : null, person.sheetId(), person.id(), end);
    }
}

package net.officefloor.hq.app.person;

import net.officefloor.hq.app.history.AsOf;
import java.sql.PreparedStatement;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** The store of people, kept per sheet. */
@Repository
public class People {

    private static final RowMapper<Person> ROW = (rs, n) -> new Person(rs.getLong("id"),
            rs.getString("sheet_id"), rs.getString("name"), rs.getString("grade"), rs.getLong("version"),
            rs.getObject("retired_from", LocalDate.class), List.of(), List.of(),
            rs.getObject("terms", Integer.class), rs.getString("contact"), List.of());

    private final JdbcTemplate jdbc;

    public People(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's people in the order they were added. */
    public List<Person> list(String sheetId) {
        return withDated(sheetId, jdbc.query("SELECT * FROM " + AsOf.table("person") + " WHERE sheet_id = ? ORDER BY id", ROW, sheetId));
    }

    public Person find(String sheetId, long id) {
        List<Person> found = jdbc.query("SELECT * FROM " + AsOf.table("person") + " WHERE sheet_id = ? AND id = ?", ROW, sheetId, id);
        return found.isEmpty() ? null : withDated(sheetId, found).get(0);
    }

    /** Aliases sort as names are compared: ignoring case (then as typed, so the order is stable). */
    private static final Comparator<String> BY_NAME = String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder());

    /** The people with the grades they were given from a date on, their holds and their aliases, read from the store. */
    private List<Person> withDated(String sheetId, List<Person> found) {
        Map<Long, List<DatedGrade>> dated = new HashMap<>();
        jdbc.query("SELECT g.* FROM " + AsOf.table("person_grade", "g") + " JOIN " + AsOf.table("person", "p") + " ON p.id = g.person_id WHERE p.sheet_id = ? "
                + "ORDER BY g.from_date", rs -> {
                    dated.computeIfAbsent(rs.getLong("person_id"), k -> new ArrayList<>()).add(
                            new DatedGrade(rs.getObject("from_date", LocalDate.class), rs.getString("grade")));
                }, sheetId);
        Map<Long, List<Hold>> holds = new HashMap<>();
        jdbc.query("SELECT h.* FROM " + AsOf.table("person_hold", "h") + " JOIN " + AsOf.table("person", "p") + " ON p.id = h.person_id WHERE p.sheet_id = ? "
                + "ORDER BY h.from_date, h.id", rs -> {
                    holds.computeIfAbsent(rs.getLong("person_id"), k -> new ArrayList<>()).add(
                            new Hold(rs.getObject("from_date", LocalDate.class), rs.getObject("to_date", LocalDate.class)));
                }, sheetId);
        Map<Long, List<String>> aliases = new HashMap<>();
        jdbc.query("SELECT person_id, alias FROM person_alias WHERE sheet_id = ?", rs -> {
            aliases.computeIfAbsent(rs.getLong("person_id"), k -> new ArrayList<>()).add(rs.getString("alias"));
        }, sheetId);
        aliases.values().forEach(a -> a.sort(BY_NAME));
        return found.stream().map(p -> new Person(p.id(), p.sheetId(), p.name(), p.grade(), p.version(),
                p.retiredFrom(), holds.getOrDefault(p.id(), List.of()), dated.getOrDefault(p.id(), List.of()), p.terms(), p.contact(),
                List.copyOf(aliases.getOrDefault(p.id(), List.of())))).toList();
    }

    /** The person on the sheet with this name or alias, ignoring case and surrounding spaces; null when none. */
    public Person findByName(String sheetId, String name) {
        List<Person> found = jdbc.query("SELECT * FROM " + AsOf.table("person") + " WHERE sheet_id = ? AND (name_key = LOWER(?) "
                + "OR id IN (SELECT person_id FROM person_alias WHERE sheet_id = ? AND alias_key = LOWER(?)))", ROW,
                sheetId, name.trim(), sheetId, name.trim());
        return found.isEmpty() ? null : withDated(sheetId, found).get(0);
    }

    /** Whether another person on the sheet (not {@code exceptId}) has this name, or anyone has it as an alias, ignoring case. */
    public boolean nameTaken(String sheetId, String name, long exceptId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + AsOf.table("person") + " WHERE sheet_id = ? AND name_key = LOWER(?) AND id <> ?",
                Integer.class, sheetId, name.trim(), exceptId);
        return n != null && n > 0 || aliasTaken(sheetId, name);
    }

    /** Whether anyone on the sheet has this alias, ignoring case. */
    private boolean aliasTaken(String sheetId, String alias) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM person_alias WHERE sheet_id = ? AND alias_key = LOWER(?)",
                Integer.class, sheetId, alias.trim());
        return n != null && n > 0;
    }

    /** Whether anyone on the sheet, the person themselves included, has this as a name or an alias, ignoring case. */
    public boolean nameOrAliasTaken(String sheetId, String alias) {
        return nameTaken(sheetId, alias, -1);
    }

    /** Give the person the alias (kept without surrounding spaces). */
    public void alias(String sheetId, long id, String alias) {
        jdbc.update("INSERT INTO person_alias (person_id, sheet_id, alias) VALUES (?, ?, ?)", id, sheetId, alias);
    }

    /** Whether another person on the sheet (not {@code exceptId}) has this contact. */
    public boolean contactTaken(String sheetId, String contact, long exceptId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + AsOf.table("person") + " WHERE sheet_id = ? AND contact = ? AND id <> ?",
                Integer.class, sheetId, contact, exceptId);
        return n != null && n > 0;
    }

    public Person add(String sheetId, PersonRequest request) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade, terms, contact) VALUES (?, ?, ?, ?, ?)", new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setString(2, request.name());
            ps.setString(3, request.grade());
            ps.setObject(4, request.terms() == null ? null : Integer.valueOf(request.terms()), java.sql.Types.INTEGER);
            ps.setString(5, request.contact());
            return ps;
        }, key);
        return new Person(key.getKey().longValue(), sheetId, request.name(), request.grade(), 0, null, List.of(),
                List.of(), request.terms() == null ? null : Integer.valueOf(request.terms()), request.contact(), List.of());
    }

    /**
     * Store an edited person, whose version is one past the version it was loaded at. False, storing
     * nothing, when the person has been saved since it was loaded.
     */
    public boolean save(Person person) {
        return jdbc.update("UPDATE person SET name = ?, terms = ?, contact = ?, version = ? WHERE id = ? AND version = ?",
                person.name(), person.terms(), person.contact(), person.version(), person.id(), person.version() - 1) == 1;
    }

    /** Give the person the grade from the date on, replacing any grade they were given from that same date. */
    public void gradeFrom(long id, LocalDate from, String grade) {
        jdbc.update("DELETE FROM person_grade WHERE person_id = ? AND from_date = ?", id, Date.valueOf(from));
        jdbc.update("INSERT INTO person_grade (person_id, from_date, grade) VALUES (?, ?, ?)", id, Date.valueOf(from),
                grade);
    }

    /** Retire the person from the date (replacing any date they were retired from before). */
    public void retire(long id, LocalDate from) {
        jdbc.update("UPDATE person SET retired_from = ? WHERE id = ?", from, id);
    }

    /** Add a hold to the person's holds. */
    public void hold(long id, Hold hold) {
        jdbc.update("INSERT INTO person_hold (person_id, from_date, to_date) VALUES (?, ?, ?)", id,
                Date.valueOf(hold.from()), hold.to() == null ? null : Date.valueOf(hold.to()));
    }
}

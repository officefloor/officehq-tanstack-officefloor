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

/** The people kept per sheet, in list order (by their position: a new person goes to the end, a move swaps
 *  two neighbours), each with their dated grades. */
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

    /** The people's aliases on the sheet (only the one person's when {@code personId} is given), by person,
     *  sorted as names are compared. */
    private Map<Long, List<String>> aliases(String sheetId, Long personId, String table) {
        Map<Long, List<String>> aliases = new HashMap<>();
        jdbc.query("SELECT person_id, alias FROM " + table + " WHERE sheet_id = ?"
                + (personId == null ? "" : " AND person_id = " + personId) + " ORDER BY LOWER(alias), alias", rs -> {
                    aliases.computeIfAbsent(rs.getLong("person_id"), k -> new ArrayList<>()).add(rs.getString("alias"));
                }, sheetId);
        return aliases;
    }

    private List<Person> query(String sheetId, Long personId, String sql, Object... args) {
        return queryFrom(sheetId, personId, "person_grade", "person_hold", "person_alias", sql, args);
    }

    private List<Person> queryFrom(String sheetId, Long personId, String gradeTable, String holdTable,
            String aliasTable, String sql, Object... args) {
        Map<Long, NavigableMap<LocalDate, String>> grades = grades(sheetId, personId, gradeTable);
        Map<Long, List<Hold>> holds = holds(sheetId, personId, holdTable);
        Map<Long, List<String>> aliases = aliases(sheetId, personId, aliasTable);
        RowMapper<Person> row = (rs, i) -> {
            Person p = new Person(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name"),
                    grades.getOrDefault(rs.getLong("id"), new TreeMap<>()), rs.getLong("version"),
                    rs.getObject("retired_from", LocalDate.class), holds.getOrDefault(rs.getLong("id"), List.of()));
            p.setTerms(rs.getObject("terms", Integer.class));
            p.setContact(rs.getString("contact"));
            p.getAliases().addAll(aliases.getOrDefault(rs.getLong("id"), List.of()));
            return p;
        };
        return jdbc.query(sql, row, args);
    }

    public List<Person> list(String sheetId) {
        return query(sheetId, null, "SELECT * FROM person WHERE sheet_id = ? ORDER BY list_position, id", sheetId);
    }

    /** The people on the sheet as they stood at the end of the date, with their grades and holds then (see
     *  {@link History}), in list order then. */
    public List<Person> listAsOf(String sheetId, LocalDate date) {
        return queryFrom(sheetId, null, History.asOf("person_grade", "person_id, from_date", date),
                History.asOf("person_hold", "id", date), History.asOf("person_alias", "id", date),
                "SELECT * FROM " + History.asOf("person", "id", date) + " WHERE sheet_id = ? ORDER BY list_position, id", sheetId);
    }

    public Person find(String sheetId, long id) {
        List<Person> found = query(sheetId, id, "SELECT * FROM person WHERE sheet_id = ? AND id = ?", sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The person as they stood at the end of the date, with their grades and holds then (see {@link History});
     *  null when they were not on the sheet then. */
    public Person findAsOf(String sheetId, long id, LocalDate date) {
        List<Person> found = queryFrom(sheetId, id, History.asOf("person_grade", "person_id, from_date", date),
                History.asOf("person_hold", "id", date), History.asOf("person_alias", "id", date),
                "SELECT * FROM " + History.asOf("person", "id", date) + " WHERE sheet_id = ? AND id = ?", sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The person on the sheet with this name or alias, ignoring case and the spaces around it; null when none. */
    public Person findByName(String sheetId, String name) {
        List<Person> found = query(sheetId, null, "SELECT * FROM person WHERE sheet_id = ? AND (name_key = LOWER(?)"
                + " OR id IN (SELECT person_id FROM person_alias WHERE sheet_id = ? AND alias_key = LOWER(?)))",
                sheetId, name.trim(), sheetId, name.trim());
        return found.isEmpty() ? null : found.get(0);
    }

    /** Whether another person on the sheet (not {@code exceptId}) has this name, or anyone on the sheet has it
     *  as an alias, ignoring case. */
    public boolean nameTaken(String sheetId, String name, Long exceptId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND name_key = LOWER(?) AND id <> ?",
                Integer.class, sheetId, name.trim(), exceptId == null ? -1L : exceptId);
        return (n != null && n > 0) || aliasTaken(sheetId, name);
    }

    /** Whether anyone on any of the sheets has this name or alias, ignoring case and the spaces around it. */
    public boolean usedOn(List<String> sheetIds, String name) {
        for (String sheetId : sheetIds) {
            if (nameTaken(sheetId, name, null)) {
                return true;
            }
        }
        return false;
    }

    /** The names and aliases used on any of the sheets, lower-cased. */
    public java.util.Set<String> namesOn(List<String> sheetIds) {
        java.util.Set<String> names = new java.util.HashSet<>();
        for (String sheetId : sheetIds) {
            names.addAll(jdbc.queryForList("SELECT name_key FROM person WHERE sheet_id = ?", String.class, sheetId));
            names.addAll(jdbc.queryForList("SELECT alias_key FROM person_alias WHERE sheet_id = ?", String.class,
                    sheetId));
        }
        return names;
    }

    /** Whether the person's name or any of their aliases is among the names (lower-cased). */
    public static boolean clashes(Person person, java.util.Set<String> names) {
        if (names.contains(person.getName().trim().toLowerCase(java.util.Locale.ROOT))) {
            return true;
        }
        return person.getAliases().stream().anyMatch(a -> names.contains(a.trim().toLowerCase(java.util.Locale.ROOT)));
    }

    /** Whether anyone on the sheet has this alias, ignoring case and the spaces around it. */
    public boolean aliasTaken(String sheetId, String alias) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM person_alias WHERE sheet_id = ? AND alias_key = LOWER(?)",
                Integer.class, sheetId, alias.trim());
        return n != null && n > 0;
    }

    /** Adds the alias (trimmed) to the person's aliases. An edit of their name or grade leaves them as they are. */
    public void addAlias(Person person, String alias) {
        jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias) VALUES (?, ?, ?)", person.getSheetId(),
                person.getId(), alias.trim());
        person.getAliases().add(alias.trim());
        person.getAliases().sort(java.util.Comparator.comparing((String a) -> a.toLowerCase(java.util.Locale.ROOT))
                .thenComparing(java.util.Comparator.naturalOrder()));
    }

    /** Adds the person with their first grade, which applies from the beginning of time, and no terms of their own. */
    public Person add(String sheetId, String name, String grade) {
        return add(sheetId, name, grade, null);
    }

    /** Adds the person with their first grade, which applies from the beginning of time, and their own
     *  terms (null for none). */
    public Person add(String sheetId, String name, String grade, Integer terms) {
        return add(sheetId, name, grade, terms, null);
    }

    /** Whether another person on the sheet (not {@code exceptId}) has this contact (trimmed, lower-cased). */
    public boolean contactTaken(String sheetId, String contact, Long exceptId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND contact = ? AND id <> ?",
                Integer.class, sheetId, contact, exceptId == null ? -1L : exceptId);
        return n != null && n > 0;
    }

    /** Adds the person with their first grade, which applies from the beginning of time, their own
     *  terms (null for none) and their contact (null for none). */
    public Person add(String sheetId, String name, String grade, Integer terms, String contact) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, terms, contact, list_position) VALUES (?, ?, ?, ?, (SELECT"
                    + " COALESCE(MAX(list_position), 0) + 1 FROM person WHERE sheet_id = ?))", new String[] { "ID" });
            ps.setString(1, sheetId);
            ps.setString(2, name);
            ps.setObject(3, terms, java.sql.Types.INTEGER);
            ps.setString(4, contact);
            ps.setString(5, sheetId);
            return ps;
        }, keys);
        long id = keys.getKey().longValue();
        jdbc.update("INSERT INTO person_grade (sheet_id, person_id, from_date, grade) VALUES (?, ?, ?, ?)", sheetId,
                id, Person.START, grade);
        Person person = new Person(id, sheetId, name, new TreeMap<>(Map.of(Person.START, grade)), 0);
        person.setTerms(terms);
        person.setContact(contact);
        return person;
    }

    /**
     * Stores the person over the version it was loaded at, moving the version on; false (nothing
     * stored) when someone else has stored another version since. The grade the edit records (if any)
     * is kept with the ones already recorded, replacing one from the same date.
     */
    public boolean save(Person person) {
        int n = jdbc.update(
                "UPDATE person SET name = ?, terms = ?, contact = ?, version = version + 1 WHERE sheet_id = ? AND id = ? AND version = ?",
                person.getName(), person.getTerms(), person.getContact(), person.getSheetId(), person.getId(), person.getVersion());
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

    /**
     * Swaps the person with their neighbour in the list (the one above when {@code up}, else the one below);
     * answers the person's new position in the list, counting from 1, or 0 (nothing stored) when there is no
     * neighbour that way: the top cannot move up, nor the bottom down. An edit of them leaves it as it is.
     */
    public int move(Person person, boolean up) {
        List<long[]> order = jdbc.query("SELECT id, list_position FROM person WHERE sheet_id = ? ORDER BY list_position, id"
                + " FOR UPDATE", (rs, i) -> new long[] { rs.getLong("id"), rs.getLong("list_position") },
                person.getSheetId());
        int at = -1;
        for (int i = 0; i < order.size(); i++) {
            if (order.get(i)[0] == person.getId()) {
                at = i;
            }
        }
        int to = up ? at - 1 : at + 1;
        if (at < 0 || to < 0 || to >= order.size()) {
            return 0;
        }
        long[] mine = order.get(at);
        long[] theirs = order.get(to);
        if (mine[1] == theirs[1]) {
            // tied positions (never made by this store): number them apart first, so the swap moves them
            renumber(person.getSheetId(), order);
            return move(person, up);
        }
        jdbc.update("UPDATE person SET list_position = ? WHERE sheet_id = ? AND id = ?", theirs[1], person.getSheetId(),
                mine[0]);
        jdbc.update("UPDATE person SET list_position = ? WHERE sheet_id = ? AND id = ?", mine[1], person.getSheetId(),
                theirs[0]);
        return to + 1;
    }

    /** Gives the people positions 1, 2, ... in their present order. */
    private void renumber(String sheetId, List<long[]> order) {
        for (int i = 0; i < order.size(); i++) {
            jdbc.update("UPDATE person SET list_position = ? WHERE sheet_id = ? AND id = ?", i + 1, sheetId, order.get(i)[0]);
        }
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

    /** Adds a copy of the person to the other sheet: their name, every grade with its from-date, their terms,
     *  contact and aliases. Their holds, statements and watchers are not carried. */
    public Person copyTo(Person person, String toSheet) {
        Person copy = add(toSheet, person.getName(), person.gradeOn(Person.START), person.getTerms(), person.getContact());
        person.getGrades().forEach((from, grade) -> {
            if (!from.equals(Person.START)) {
                jdbc.update("INSERT INTO person_grade (sheet_id, person_id, from_date, grade) VALUES (?, ?, ?, ?)",
                        toSheet, copy.getId(), from, grade);
            }
        });
        for (String alias : person.getAliases()) {
            addAlias(copy, alias);
        }
        return find(toSheet, copy.getId());
    }

    /** Drops the person from their sheet with their grades, holds, aliases, statements and watchers. Their claims,
     *  patrons and pool memberships are dropped before this (see ClaimStore, PatronStore and PoolStore). */
    public void remove(Person person) {
        String sheetId = person.getSheetId();
        jdbc.update("DELETE FROM person_alias WHERE sheet_id = ? AND person_id = ?", sheetId, person.getId());
        jdbc.update("DELETE FROM person WHERE sheet_id = ? AND id = ?", sheetId, person.getId());
        jdbc.update("DELETE FROM person_grade WHERE sheet_id = ? AND person_id = ?", sheetId, person.getId());
        jdbc.update("DELETE FROM person_hold WHERE sheet_id = ? AND person_id = ?", sheetId, person.getId());
        jdbc.update("DELETE FROM statement WHERE sheet_id = ? AND person_id = ?", sheetId, person.getId());
        jdbc.update("DELETE FROM watch WHERE sheet_id = ? AND person_id = ?", sheetId, person.getId());
    }

    /** Makes the person's name and aliases aliases of {@code into}, then drops the person with their grades,
     *  holds, statements and watchers (their contact goes with them). Their claims, patrons and pool
     *  memberships are moved before this (see ClaimStore, PatronStore and PoolStore). */
    public void mergeInto(Person person, Person into) {
        String sheetId = person.getSheetId();
        jdbc.update("UPDATE person_alias SET person_id = ? WHERE sheet_id = ? AND person_id = ?", into.getId(), sheetId,
                person.getId());
        jdbc.update("DELETE FROM person WHERE sheet_id = ? AND id = ?", sheetId, person.getId());
        jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias) VALUES (?, ?, ?)", sheetId, into.getId(),
                person.getName());
        jdbc.update("DELETE FROM person_grade WHERE sheet_id = ? AND person_id = ?", sheetId, person.getId());
        jdbc.update("DELETE FROM person_hold WHERE sheet_id = ? AND person_id = ?", sheetId, person.getId());
        jdbc.update("DELETE FROM statement WHERE sheet_id = ? AND person_id = ?", sheetId, person.getId());
        jdbc.update("DELETE FROM watch WHERE sheet_id = ? AND person_id = ?", sheetId, person.getId());
    }
}

package net.officefloor.hq.app.person;

import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PersonRepository {

    private static final String COLUMNS = "id, sheet_id, name, version, statement_count, stated_total, statement_date, retired_from, terms, contact, tags, origin";

    private static final RowMapper<Person> ROW = (rs, n) -> {
        LocalDate stated = rs.getObject("statement_date", LocalDate.class);
        LocalDate retired = rs.getObject("retired_from", LocalDate.class);
        Person person = new Person(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name"),
                null, rs.getLong("version"), rs.getInt("statement_count"),
                rs.getString("stated_total"), stated == null ? null : stated.toString());
        person.setRetiredFrom(retired == null ? null : retired.toString());
        person.setTerms(rs.getObject("terms", Integer.class));
        person.setContact(rs.getString("contact"));
        person.setTags(rs.getString("tags"));
        person.setOrigin(rs.getString("origin"));
        return person;
    };

    private final JdbcTemplate jdbc;

    public PersonRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The end of a sheet's list: the place a person added or arriving there takes. */
    private static final String NEXT_POS = "(SELECT COALESCE(MAX(q.list_pos), 0) + 1 FROM person q WHERE q.sheet_id = ?)";

    /** The sheet's people in list order (by their place in it), each with their patrons. */
    public List<Person> list(String sheetId) {
        List<Person> people = jdbc.query("SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? ORDER BY list_pos, id",
                ROW, sheetId);
        Map<Long, Person> byId = new HashMap<>();
        people.forEach((p) -> byId.put(p.getId(), p));
        withGrades(sheetId, people);
        withHolds(sheetId, people);
        withAliases(sheetId, people);
        withClashing(sheetId, people);
        withRetirements(sheetId, people);
        jdbc.query("SELECT person_id, patron_id FROM person_patron WHERE sheet_id = ? ORDER BY added_at, patron_id",
                (rs) -> {
                    Person p = byId.get(rs.getLong("person_id"));
                    if (p != null) {
                        p.getPatronIds().add(rs.getLong("patron_id"));
                    }
                }, sheetId);
        return people;
    }

    public Person find(String sheetId, long id) {
        Person person = jdbc.query("SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? AND id = ?",
                ROW, sheetId, id).stream().findFirst().orElse(null);
        if (person != null) {
            person.setPatronIds(new ArrayList<>(jdbc.queryForList(
                    "SELECT patron_id FROM person_patron WHERE sheet_id = ? AND person_id = ? ORDER BY added_at, patron_id",
                    Long.class, sheetId, id)));
            withGrades(sheetId, List.of(person));
            withHolds(sheetId, List.of(person));
            withAliases(sheetId, List.of(person));
            withClashing(sheetId, List.of(person));
            withRetirements(sheetId, List.of(person));
        }
        return person;
    }

    /** Whether the person was added while the sheet's today was after the date: they were not on
     *  the sheet then (one added before this was kept always was). */
    public boolean addedAfter(String sheetId, long id, LocalDate date) {
        return jdbc.queryForList("SELECT added_on FROM person WHERE sheet_id = ? AND id = ?", LocalDate.class, sheetId, id)
                .stream().anyMatch((on) -> on != null && on.isAfter(date));
    }

    /** The person as they stood at the end of the date, the date standing in for the sheet's today:
     *  the grades, holds and retirement date given them while the sheet's today was on or before it. */
    public Person findAsOf(String sheetId, long id, LocalDate asOf) {
        Person person = jdbc.query("SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? AND id = ?",
                ROW, sheetId, id).stream().findFirst().orElse(null);
        if (person == null) {
            return null;
        }
        List<GradeFrom> grades = jdbc.query("SELECT grade, from_date FROM person_grade_history h WHERE h.sheet_id = ? "
                + "AND h.person_id = ? AND h.id = (SELECT MAX(h2.id) FROM person_grade_history h2 WHERE h2.person_id = h.person_id "
                + "AND h2.from_key = h.from_key AND (h2.on_date IS NULL OR h2.on_date <= ?)) ORDER BY from_key",
                (rs, n) -> {
                    LocalDate from = rs.getObject("from_date", LocalDate.class);
                    return new GradeFrom(rs.getString("grade"), from == null ? null : from.toString());
                }, sheetId, id, asOf);
        person.setGrades(new ArrayList<>(grades), asOf);
        person.setHolds(new ArrayList<>(jdbc.query("SELECT from_date, to_date FROM person_hold WHERE sheet_id = ? "
                + "AND person_id = ? AND (added_on IS NULL OR added_on <= ?) ORDER BY id", (rs, n) -> {
                    LocalDate to = rs.getObject("to_date", LocalDate.class);
                    return new Hold(rs.getObject("from_date", LocalDate.class).toString(), to == null ? null : to.toString());
                }, sheetId, id, asOf)));
        List<LocalDate> retired = jdbc.queryForList("SELECT retired_from FROM person_retired_history WHERE sheet_id = ? "
                + "AND person_id = ? AND (on_date IS NULL OR on_date <= ?) ORDER BY id DESC LIMIT 1", LocalDate.class,
                sheetId, id, asOf);
        person.setRetiredFrom(retired.isEmpty() || retired.get(0) == null ? null : retired.get(0).toString());
        person.setRetirements(new ArrayList<>(jdbc.query("SELECT from_date, to_date FROM person_retirement WHERE sheet_id = ? "
                + "AND person_id = ? AND to_date <= ? ORDER BY id", (rs, n) -> new Retirement(
                        rs.getObject("from_date", LocalDate.class).toString(), rs.getObject("to_date", LocalDate.class).toString()),
                sheetId, id, asOf)));
        return person;
    }

    /** Give each person their grades, by from-date (none first), and the one in force on the sheet's today. */
    private void withGrades(String sheetId, List<Person> people) {
        if (people.isEmpty()) {
            return;
        }
        LocalDate today = jdbc.queryForList("SELECT today FROM sheet WHERE id = ?", LocalDate.class, sheetId)
                .stream().findFirst().orElse(LocalDate.now());
        Map<Long, List<GradeFrom>> grades = new HashMap<>();
        jdbc.query("SELECT person_id, grade, from_date FROM person_grade WHERE sheet_id = ? ORDER BY person_id, from_key",
                (rs) -> {
                    LocalDate from = rs.getObject("from_date", LocalDate.class);
                    grades.computeIfAbsent(rs.getLong("person_id"), (k) -> new ArrayList<>())
                            .add(new GradeFrom(rs.getString("grade"), from == null ? null : from.toString()));
                }, sheetId);
        people.forEach((p) -> p.setGrades(grades.getOrDefault(p.getId(), new ArrayList<>()), today));
    }

    /** Give each person their holds, in the order they were added. */
    private void withHolds(String sheetId, List<Person> people) {
        if (people.isEmpty()) {
            return;
        }
        Map<Long, List<Hold>> holds = new HashMap<>();
        jdbc.query("SELECT person_id, from_date, to_date FROM person_hold WHERE sheet_id = ? ORDER BY person_id, id",
                (rs) -> {
                    LocalDate to = rs.getObject("to_date", LocalDate.class);
                    holds.computeIfAbsent(rs.getLong("person_id"), (k) -> new ArrayList<>())
                            .add(new Hold(rs.getObject("from_date", LocalDate.class).toString(),
                                    to == null ? null : to.toString()));
                }, sheetId);
        people.forEach((p) -> p.setHolds(holds.getOrDefault(p.getId(), new ArrayList<>())));
    }

    /** Give each person their past retirements, oldest first. */
    private void withRetirements(String sheetId, List<Person> people) {
        if (people.isEmpty()) {
            return;
        }
        Map<Long, List<Retirement>> retirements = new HashMap<>();
        jdbc.query("SELECT person_id, from_date, to_date FROM person_retirement WHERE sheet_id = ? ORDER BY person_id, id",
                (rs) -> {
                    retirements.computeIfAbsent(rs.getLong("person_id"), (k) -> new ArrayList<>())
                            .add(new Retirement(rs.getObject("from_date", LocalDate.class).toString(),
                                    rs.getObject("to_date", LocalDate.class).toString()));
                }, sheetId);
        people.forEach((p) -> p.setRetirements(retirements.getOrDefault(p.getId(), new ArrayList<>())));
    }

    /** Give each person their aliases, sorted as names are compared. */
    private void withAliases(String sheetId, List<Person> people) {
        if (people.isEmpty()) {
            return;
        }
        Map<Long, List<String>> aliases = new HashMap<>();
        jdbc.query("SELECT person_id, alias FROM person_alias WHERE sheet_id = ? ORDER BY person_id, alias_key, alias, id",
                (rs) -> {
                    aliases.computeIfAbsent(rs.getLong("person_id"), (k) -> new ArrayList<>()).add(rs.getString("alias"));
                }, sheetId);
        people.forEach((p) -> p.setAliases(aliases.getOrDefault(p.getId(), new ArrayList<>())));
    }

    /** The other sheets of the sheet's tree, among the sheets that exist: its ancestors (its parent,
     *  its parent's parent, ...) and its descendants (its children, their children, ...). */
    public java.util.Set<String> treeSheets(String sheetId) {
        java.util.Set<String> tree = new java.util.LinkedHashSet<>();
        java.util.Set<String> seen = new java.util.HashSet<>(List.of(sheetId));
        for (String at = parentOf(sheetId); at != null && seen.add(at); at = parentOf(at)) {
            tree.add(at);
        }
        List<String> below = new ArrayList<>(List.of(sheetId));
        java.util.Set<String> seenBelow = new java.util.HashSet<>(List.of(sheetId));
        for (int i = 0; i < below.size(); i++) {
            for (String child : jdbc.queryForList("SELECT sheet_id FROM sheet_setting WHERE setting = 'parent' AND val = ?",
                    String.class, below.get(i))) {
                if (seenBelow.add(child)) {
                    below.add(child);
                    tree.add(child);
                }
            }
        }
        tree.remove(sheetId);
        tree.removeIf((s) -> jdbc.queryForObject("SELECT COUNT(*) FROM sheet WHERE id = ?", Integer.class, s) == 0);
        return tree;
    }

    /** The sheet's descendants (its children, their children, ...) among the sheets that exist, by id. */
    public List<String> descendantSheets(String sheetId) {
        List<String> below = new ArrayList<>(List.of(sheetId));
        java.util.Set<String> seen = new java.util.HashSet<>(List.of(sheetId));
        for (int i = 0; i < below.size(); i++) {
            for (String child : jdbc.queryForList("SELECT sheet_id FROM sheet_setting WHERE setting = 'parent' AND val = ?",
                    String.class, below.get(i))) {
                if (seen.add(child)) below.add(child);
            }
        }
        below.remove(0);
        below.removeIf((s) -> jdbc.queryForObject("SELECT COUNT(*) FROM sheet WHERE id = ?", Integer.class, s) == 0);
        below.sort(null);
        return below;
    }

    private String parentOf(String sheetId) {
        return jdbc.queryForList("SELECT val FROM sheet_setting WHERE sheet_id = ? AND setting = 'parent'",
                String.class, sheetId).stream().filter((v) -> v != null && !v.isBlank()).findFirst().orElse(null);
    }

    /** The names and aliases used on the other sheets of the sheet's tree, as names are compared
     *  (lower-cased, trimmed). */
    public java.util.Set<String> treeNames(String sheetId) {
        java.util.Set<String> names = new java.util.HashSet<>();
        for (String other : treeSheets(sheetId)) {
            names.addAll(jdbc.queryForList("SELECT name_key FROM person WHERE sheet_id = ?", String.class, other));
            names.addAll(jdbc.queryForList("SELECT alias_key FROM person_alias WHERE sheet_id = ?", String.class, other));
        }
        return names;
    }

    /** Whether the name is used, as a name or alias, on another sheet of the sheet's tree. */
    public boolean nameTakenInTree(String sheetId, String name) {
        return treeNames(sheetId).contains(key(name));
    }

    private static String key(String name) {
        return name == null ? "" : name.trim().toLowerCase(java.util.Locale.ROOT);
    }

    /** Mark each person whose name or an alias is used on another sheet of the sheet's tree. */
    private void withClashing(String sheetId, List<Person> people) {
        if (people.isEmpty()) {
            return;
        }
        java.util.Set<String> names = treeNames(sheetId);
        people.forEach((p) -> p.setClashing(names.contains(key(p.getName()))
                || p.getAliases().stream().anyMatch((a) -> names.contains(key(a)))));
    }

    /** Give the person the alias (stored as given, trimmed). */
    public void addAlias(Person person, String alias, String user) {
        jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias, added_by) VALUES (?, ?, ?, ?)",
                person.getSheetId(), person.getId(), alias, user);
        person.getAliases().add(alias);
        person.getAliases().sort(String.CASE_INSENSITIVE_ORDER);
    }

    /** Name the patron as one of the person's patrons. */
    public void addPatron(Person person, Person patron, String user) {
        jdbc.update("INSERT INTO person_patron (sheet_id, person_id, patron_id, added_by) VALUES (?, ?, ?, ?)",
                person.getSheetId(), person.getId(), patron.getId(), user);
        person.getPatronIds().add(patron.getId());
    }

    /** The person on the sheet with this name or alias, compared ignoring case and surrounding spaces; null when none. */
    public Person findByName(String sheetId, String name) {
        Person found = jdbc.query("SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? AND (name_key = LOWER(TRIM(?)) "
                + "OR id IN (SELECT person_id FROM person_alias WHERE sheet_id = ? AND alias_key = LOWER(TRIM(?))))",
                ROW, sheetId, name, sheetId, name).stream().findFirst().orElse(null);
        return found == null ? null : find(sheetId, found.getId());
    }

    /** Whether another person on the sheet (not {@code exceptId}) already has this name, or anyone
     *  on the sheet has it as an alias, compared ignoring case and surrounding spaces. */
    public boolean nameTaken(String sheetId, String name, Long exceptId) {
        Integer n = jdbc.queryForObject(
                "SELECT (SELECT COUNT(*) FROM person WHERE sheet_id = ? AND name_key = LOWER(TRIM(?)) AND (? IS NULL OR id <> ?)) "
                        + "+ (SELECT COUNT(*) FROM person_alias WHERE sheet_id = ? AND alias_key = LOWER(TRIM(?)))",
                Integer.class, sheetId, name, exceptId, exceptId, sheetId, name);
        return n != null && n > 0;
    }

    /** Whether another person on the sheet (not {@code exceptId}) has this contact. */
    public boolean contactTaken(String sheetId, String contact, Long exceptId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND contact = ? AND (? IS NULL OR id <> ?)",
                Integer.class, sheetId, contact, exceptId, exceptId);
        return n != null && n > 0;
    }

    public Person add(String sheetId, String name, String grade, String user) {
        return add(sheetId, name, grade, null, null, null, user);
    }

    /** Add a person with their grade (from the beginning of time), own terms (null: none) and
     *  contact (null: none). */
    public Person add(String sheetId, String name, String grade, Integer terms, String contact, String user) {
        return add(sheetId, name, grade, terms, contact, null, user);
    }

    /** Add a person as above, with their tags (null: none; see {@link net.officefloor.hq.app.claim.ClaimForm#tags}). */
    public Person add(String sheetId, String name, String grade, Integer terms, String contact, String tags, String user) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, created_by, terms, contact, tags, added_on, list_pos) "
                            + "VALUES (?, ?, ?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?), " + NEXT_POS + ")",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setString(2, name);
            ps.setString(3, user);
            ps.setObject(4, terms);
            ps.setString(5, contact);
            ps.setString(6, tags);
            ps.setString(7, sheetId);
            ps.setString(8, sheetId);
            return ps;
        }, key);
        long id = key.getKey().longValue();
        // The first grade a person is given applies from the beginning of time.
        jdbc.update("INSERT INTO person_grade (sheet_id, person_id, grade, from_date, set_by) VALUES (?, ?, ?, NULL, ?)",
                sheetId, id, grade, user);
        gradeHistory(sheetId, id, grade, null);
        Person person = new Person(id, sheetId, name, grade, 0);
        person.getGrades().add(new GradeFrom(grade, null));
        person.setTerms(terms);
        person.setContact(contact);
        person.setTags(tags);
        useTags(sheetId, tags);
        return person;
    }

    /** Record the grade the person was just given (see {@link Person#giveGrade}), replacing the one
     *  with the same from-date. */
    public void saveGrade(Person person, GradeFrom given, String user) {
        LocalDate from = given.from() == null ? null : LocalDate.parse(given.from());
        if (from == null) {
            jdbc.update("DELETE FROM person_grade WHERE sheet_id = ? AND person_id = ? AND from_date IS NULL",
                    person.getSheetId(), person.getId());
        } else {
            jdbc.update("DELETE FROM person_grade WHERE sheet_id = ? AND person_id = ? AND from_date = ?",
                    person.getSheetId(), person.getId(), from);
        }
        jdbc.update("INSERT INTO person_grade (sheet_id, person_id, grade, from_date, set_by) VALUES (?, ?, ?, ?, ?)",
                person.getSheetId(), person.getId(), given.grade(), from, user);
        gradeHistory(person.getSheetId(), person.getId(), given.grade(), from);
    }

    /** Keep a grade given in the person's history, with the sheet's today. */
    private void gradeHistory(String sheetId, long personId, String grade, LocalDate from) {
        jdbc.update("INSERT INTO person_grade_history (sheet_id, person_id, grade, from_date, on_date) "
                + "VALUES (?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?))", sheetId, personId, grade, from, sheetId);
    }

    /** Save the person over the version it was loaded at, raising the version; false (nothing
     *  written) when someone else has saved the person since. */
    public boolean update(Person person) {
        int n = jdbc.update(
                "UPDATE person SET name = ?, terms = ?, contact = ?, tags = ?, version = version + 1 WHERE sheet_id = ? AND id = ? AND version = ?",
                person.getName(), person.getTerms(), person.getContact(), person.getTags(), person.getSheetId(), person.getId(), person.getVersion());
        if (n == 0) {
            return false;
        }
        person.setVersion(person.getVersion() + 1);
        useTags(person.getSheetId(), person.getTags());
        return true;
    }

    /** Keep each of the tags (comma separated; null: none) among those ever used on the sheet. */
    private void useTags(String sheetId, String tags) {
        if (tags == null) return;
        for (String tag : tags.split(",")) {
            jdbc.update("MERGE INTO sheet_tag (sheet_id, tag) KEY (sheet_id, tag) VALUES (?, ?)", sheetId, tag);
        }
    }

    /** Save the statement just taken of the person (not an edit: the version stays). */
    public void updateStatements(Person person) {
        saveStatements(person);
        jdbc.update("INSERT INTO person_statement (sheet_id, person_id, stated_total, statement_date) VALUES (?, ?, ?, ?)",
                person.getSheetId(), person.getId(), person.getStatedTotal(), LocalDate.parse(person.getStatementDate()));
    }

    /** Drop the person's last statement kept, and load the one before it into the person (none: they
     *  show no statement); their count goes down by one (not an edit: the version stays). */
    public void cancelStatement(Person person) {
        List<Long> kept = jdbc.queryForList(
                "SELECT id FROM person_statement WHERE sheet_id = ? AND person_id = ? ORDER BY id DESC LIMIT 2",
                Long.class, person.getSheetId(), person.getId());
        if (!kept.isEmpty()) {
            jdbc.update("DELETE FROM person_statement WHERE id = ?", kept.get(0));
        }
        String[] before = kept.size() < 2 ? null : jdbc.queryForObject(
                "SELECT stated_total, statement_date FROM person_statement WHERE id = ?",
                (rs, n) -> new String[] { rs.getString("stated_total"),
                        rs.getObject("statement_date", LocalDate.class).toString() }, kept.get(1));
        person.unstated(before == null ? null : before[0], before == null ? null : before[1]);
        saveStatements(person);
    }

    private void saveStatements(Person person) {
        jdbc.update("UPDATE person SET statement_count = ?, stated_total = ?, statement_date = ? WHERE sheet_id = ? AND id = ?",
                person.getStatementCount(), person.getStatedTotal(),
                person.getStatementDate() == null ? null : LocalDate.parse(person.getStatementDate()),
                person.getSheetId(), person.getId());
    }

    /** Save the person's retirement date (not an edit: the version stays). */
    public void updateRetired(Person person) {
        jdbc.update("UPDATE person SET retired_from = ? WHERE sheet_id = ? AND id = ?",
                LocalDate.parse(person.getRetiredFrom()), person.getSheetId(), person.getId());
        jdbc.update("INSERT INTO person_retired_history (sheet_id, person_id, retired_from, on_date) "
                + "VALUES (?, ?, ?, (SELECT today FROM sheet WHERE id = ?))", person.getSheetId(), person.getId(),
                LocalDate.parse(person.getRetiredFrom()), person.getSheetId());
    }

    /** Save the person's unretirement (not an edit: the version stays): not retired, their last past
     *  retirement kept, and kept in the retirement history as no date. */
    public void updateUnretired(Person person) {
        Retirement last = person.getRetirements().get(person.getRetirements().size() - 1);
        jdbc.update("UPDATE person SET retired_from = NULL WHERE sheet_id = ? AND id = ?", person.getSheetId(), person.getId());
        jdbc.update("INSERT INTO person_retirement (sheet_id, person_id, from_date, to_date) VALUES (?, ?, ?, ?)",
                person.getSheetId(), person.getId(), LocalDate.parse(last.from()), LocalDate.parse(last.to()));
        jdbc.update("INSERT INTO person_retired_history (sheet_id, person_id, retired_from, on_date) "
                + "VALUES (?, ?, NULL, (SELECT today FROM sheet WHERE id = ?))", person.getSheetId(), person.getId(),
                person.getSheetId());
    }

    /** Add a hold on the person (not an edit: the version stays). */
    public void addHold(Person person, Hold hold, String user) {
        jdbc.update("INSERT INTO person_hold (sheet_id, person_id, from_date, to_date, held_by, added_on) "
                + "VALUES (?, ?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?))",
                person.getSheetId(), person.getId(), LocalDate.parse(hold.from()),
                hold.to() == null ? null : LocalDate.parse(hold.to()), user, person.getSheetId());
        person.getHolds().add(hold);
    }

    /** Merge the person {@code from} into {@code into}: from's name and aliases become into's aliases;
     *  from's patrons are added to into's (not into themselves, no repeats); anyone naming from as
     *  patron names into instead (no repeats); into's statement count becomes theirs plus from's, their
     *  last statement staying theirs (having none, becoming from's), from's statements kept before
     *  into's; from's holds, watchers, grades and the person themselves are dropped; from's plans become into's. Their claims, pool memberships and period totals must have
     *  moved first. */
    public void merge(Person from, Person into, String user) {
        String sheetId = into.getSheetId();
        jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias, added_by) VALUES (?, ?, ?, ?)",
                sheetId, into.getId(), from.getName(), user);
        jdbc.update("UPDATE person_alias SET person_id = ? WHERE sheet_id = ? AND person_id = ?",
                into.getId(), sheetId, from.getId());
        for (Long patronId : from.getPatronIds()) {
            if (patronId != into.getId() && !into.getPatronIds().contains(patronId)) {
                jdbc.update("INSERT INTO person_patron (sheet_id, person_id, patron_id, added_by) VALUES (?, ?, ?, ?)",
                        sheetId, into.getId(), patronId, user);
                into.getPatronIds().add(patronId);
            }
        }
        jdbc.update("DELETE FROM person_patron WHERE sheet_id = ? AND person_id = ?", sheetId, from.getId());
        // Whoever named from names into instead, unless that is into themselves or they name into already.
        jdbc.update("DELETE FROM person_patron WHERE sheet_id = ? AND patron_id = ? AND (person_id = ? OR person_id IN "
                + "(SELECT k.person_id FROM person_patron k WHERE k.sheet_id = ? AND k.patron_id = ?))",
                sheetId, from.getId(), into.getId(), sheetId, into.getId());
        jdbc.update("UPDATE person_patron SET patron_id = ? WHERE sheet_id = ? AND patron_id = ?",
                into.getId(), sheetId, from.getId());
        into.getPatronIds().remove(Long.valueOf(from.getId()));
        // from's statements go before into's, so into's last stays their last.
        List<Long> own = jdbc.queryForList("SELECT id FROM person_statement WHERE sheet_id = ? AND person_id = ? ORDER BY id",
                Long.class, sheetId, into.getId());
        jdbc.update("UPDATE person_statement SET person_id = ? WHERE sheet_id = ? AND person_id = ?",
                into.getId(), sheetId, from.getId());
        for (Long id : own) {
            jdbc.update("INSERT INTO person_statement (sheet_id, person_id, stated_total, statement_date, taken_at) "
                    + "SELECT sheet_id, person_id, stated_total, statement_date, taken_at FROM person_statement WHERE id = ?", id);
            jdbc.update("DELETE FROM person_statement WHERE id = ?", id);
        }
        into.absorbStatements(from);
        saveStatements(into);
        for (String table : List.of("person_hold", "person_watch", "person_grade",
                "person_grade_history", "person_retired_history", "person_retirement")) {
            jdbc.update("DELETE FROM " + table + " WHERE sheet_id = ? AND person_id = ?", sheetId, from.getId());
        }
        // from's plans go on making claims, now for into.
        jdbc.update("UPDATE plan SET person_id = ? WHERE sheet_id = ? AND person_id = ?", into.getId(), sheetId, from.getId());
        jdbc.update("DELETE FROM person WHERE sheet_id = ? AND id = ?", sheetId, from.getId());
        withAliases(sheetId, List.of(into));
    }

    /** Move the person to the sheet {@code to} with their grades (and grade history) and aliases, their
     *  statements dropped (the count back to 0) and their plans going with them; their holds, watchers, patron links (either way), pool
     *  memberships and closed periods' fixed totals are dropped; they keep the sheet they came from as their
     *  origin. When {@code merging} (they are to be merged into someone there straight after) they keep
     *  their statements and arrive under a stand-in name, as theirs is taken there. Their claims must
     *  have moved first. */
    public void transfer(Person person, String to, boolean merging) {
        String sheetId = person.getSheetId();
        long id = person.getId();
        jdbc.update("DELETE FROM person_patron WHERE sheet_id = ? AND (person_id = ? OR patron_id = ?)", sheetId, id, id);
        for (String table : List.of("person_hold", "person_watch", "pool_member")) {
            jdbc.update("DELETE FROM " + table + " WHERE sheet_id = ? AND person_id = ?", sheetId, id);
        }
        if (merging) {
            jdbc.update("UPDATE person_statement SET sheet_id = ? WHERE sheet_id = ? AND person_id = ?", to, sheetId, id);
        } else {
            jdbc.update("DELETE FROM person_statement WHERE sheet_id = ? AND person_id = ?", sheetId, id);
            jdbc.update("UPDATE person SET statement_count = 0, stated_total = NULL, statement_date = NULL "
                    + "WHERE sheet_id = ? AND id = ?", sheetId, id);
        }
        jdbc.update("DELETE FROM period_person_total WHERE person_id = ?", id);
        for (String table : List.of("person_grade", "person_grade_history", "person_retired_history", "person_retirement", "person_alias", "plan")) {
            jdbc.update("UPDATE " + table + " SET sheet_id = ? WHERE sheet_id = ? AND person_id = ?", to, sheetId, id);
        }
        String name = merging ? "#merging-" + id : person.getName();
        jdbc.update("UPDATE person SET sheet_id = ?, name = ?, "
                + "origin = ?, list_pos = " + NEXT_POS + " WHERE sheet_id = ? AND id = ?", to, name, sheetId, to, sheetId, id);
    }

    /** Swap the two people's places in their sheet's list. */
    public void swap(String sheetId, long a, long b) {
        Long posA = jdbc.queryForObject("SELECT list_pos FROM person WHERE sheet_id = ? AND id = ?", Long.class, sheetId, a);
        Long posB = jdbc.queryForObject("SELECT list_pos FROM person WHERE sheet_id = ? AND id = ?", Long.class, sheetId, b);
        jdbc.update("UPDATE person SET list_pos = ? WHERE sheet_id = ? AND id = ?", posB, sheetId, a);
        jdbc.update("UPDATE person SET list_pos = ? WHERE sheet_id = ? AND id = ?", posA, sheetId, b);
    }
}

package net.officefloor.hq.app.person;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
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
        person.setTags(rs.getString("tags"));
        person.setStatedTotal(rs.getBigDecimal("stated_total"));
        java.sql.Date stated = rs.getDate("statement_date");
        person.setStatementDate(stated == null ? null : stated.toLocalDate());
        java.sql.Date retired = rs.getDate("retired_from");
        person.setRetiredFrom(retired == null ? null : retired.toLocalDate());
        java.sql.Date added = rs.getDate("added_on");
        person.setAddedOn(added == null ? null : added.toLocalDate());
        long place = rs.getLong("place");
        person.setPlace(rs.wasNull() ? person.getId() : place);
        person.setOrigin(rs.getString("origin"));
        java.sql.Date arrived = rs.getDate("arrived_on");
        person.setArrivedOn(arrived == null ? null : arrived.toLocalDate());
        java.sql.Date flagUntil = rs.getDate("flag_until");
        person.setFlag(rs.getString("flag_reason"), flagUntil == null ? null : flagUntil.toLocalDate());
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
        java.util.Map<Long, java.util.List<Person.Retirement>> retirements = new java.util.HashMap<>();
        jdbc.query("SELECT person_id, from_date, to_date FROM person_retirement WHERE sheet_id = ? ORDER BY seq", rs -> {
            retirements.computeIfAbsent(rs.getLong("person_id"), k -> new java.util.ArrayList<>())
                    .add(new Person.Retirement(rs.getDate("from_date").toLocalDate(), rs.getDate("to_date").toLocalDate()));
        }, sheetId);
        for (Person person : found) {
            person.setRetirements(new java.util.ArrayList<>(retirements.getOrDefault(person.getId(), List.of())));
        }
        java.util.Map<Long, java.util.List<String>> snapshots = new java.util.HashMap<>();
        jdbc.query("SELECT person_id, set_on, due_total FROM person_snapshot WHERE sheet_id = ? ORDER BY seq", rs -> {
            snapshots.computeIfAbsent(rs.getLong("person_id"), k -> new java.util.ArrayList<>())
                    .add(rs.getDate("set_on").toLocalDate() + " " + rs.getString("due_total"));
        }, sheetId);
        for (Person person : found) {
            person.setSnapshots(new java.util.ArrayList<>(snapshots.getOrDefault(person.getId(), List.of())));
        }
        java.util.Map<Long, java.util.List<String>> statements = new java.util.HashMap<>();
        jdbc.query("SELECT person_id, statement_date, stated_total FROM person_statement WHERE sheet_id = ? AND NOT cancelled"
                + " ORDER BY set_on NULLS FIRST, seq", rs -> {
            statements.computeIfAbsent(rs.getLong("person_id"), k -> new java.util.ArrayList<>())
                    .add(Person.statementLine(rs.getDate("statement_date"), rs.getBigDecimal("stated_total")));
        }, sheetId);
        for (Person person : found) {
            person.setStatements(new java.util.ArrayList<>(statements.getOrDefault(person.getId(), List.of())));
        }
        return found;
    }

    /** Keeps a SNAPSHOT line for the person: their due-total as shown, dated the sheet's today (the day being left), taken on
     *  the day the sheet moves to. */
    public void recordSnapshot(String sheetId, long personId, String dueTotal, LocalDate takenOn) {
        jdbc.update("INSERT INTO person_snapshot (sheet_id, person_id, due_total, set_on, taken_on) VALUES (?, ?, ?, (SELECT today FROM sheet WHERE id = ?), ?)",
                sheetId, personId, dueTotal, sheetId, takenOn == null ? null : Date.valueOf(takenOn));
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
        java.util.Map<Long, java.util.Map<java.time.LocalDate, Integer>> terms = new java.util.HashMap<>();
        jdbc.query("SELECT person_id, from_date, terms FROM person_terms WHERE sheet_id = ? ORDER BY seq", rs -> {
            int days = rs.getInt("terms");
            Integer value = rs.wasNull() ? null : days;
            terms.computeIfAbsent(rs.getLong("person_id"), k -> new java.util.HashMap<>())
                    .put(rs.getDate("from_date").toLocalDate(), value);
        }, sheetId);
        withHolds(sheetId, found);
        for (Person person : found) {
            person.setGrades(grades.getOrDefault(person.getId(), java.util.Map.of()));
            person.setDatedTerms(terms.getOrDefault(person.getId(), java.util.Map.of()));
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

    /** Writes the terms the person's edit recorded from a date, replacing any recorded from the same date. */
    private void writeRecordedTerms(Person person) {
        java.time.LocalDate recorded = person.getRecordedTermsFrom();
        if (recorded == null) {
            return;
        }
        java.sql.Date from = java.sql.Date.valueOf(recorded);
        jdbc.update("DELETE FROM person_terms WHERE sheet_id = ? AND person_id = ? AND from_date = ?",
                person.getSheetId(), person.getId(), from);
        jdbc.update("INSERT INTO person_terms (sheet_id, person_id, from_date, terms) VALUES (?, ?, ?, ?)",
                person.getSheetId(), person.getId(), from, person.getDatedTerms().get(recorded));
        person.clearRecordedTerms();
    }

    /** Records the person's name, own terms and contact as they now stand in their history, dated the sheet's today. */
    private void writeHistory(Person person) {
        jdbc.update("INSERT INTO person_history (sheet_id, person_id, set_on, name, terms, contact) VALUES (?, ?, (SELECT today FROM sheet WHERE id = ?), ?, ?, ?)",
                person.getSheetId(), person.getId(), person.getSheetId(), person.getName(), person.getTerms(), person.getContact());
    }

    /** The sheet's people in list order: by their place. */
    public List<Person> list(String sheetId) {
        return withClashing(sheetId, withGrades(sheetId, withAliases(sheetId, withPatrons(sheetId, jdbc.query("SELECT * FROM person WHERE sheet_id = ? ORDER BY place, id", ROW, sheetId)))));
    }

    /** Swaps the person's place with their neighbour's, above (up) or below; returns the person's new position in
     *  the list counting from 1, or 0 (nothing written) when they have no neighbour that way. */
    public int move(Person person, boolean up) {
        List<long[]> places = jdbc.query("SELECT id, place FROM person WHERE sheet_id = ? ORDER BY place, id",
                (rs, i) -> new long[] { rs.getLong(1), rs.getLong(2) }, person.getSheetId());
        int at = -1;
        for (int i = 0; i < places.size(); i++) {
            if (places.get(i)[0] == person.getId()) {
                at = i;
            }
        }
        int to = up ? at - 1 : at + 1;
        if (at < 0 || to < 0 || to >= places.size()) {
            return 0;
        }
        long[] mine = places.get(at);
        long[] theirs = places.get(to);
        jdbc.update("UPDATE person SET place = ? WHERE sheet_id = ? AND id = ?", theirs[1], person.getSheetId(), mine[0]);
        jdbc.update("UPDATE person SET place = ? WHERE sheet_id = ? AND id = ?", mine[1], person.getSheetId(), theirs[0]);
        return to + 1;
    }

    /** A name as names are compared: without regard to case or outer spaces. */
    private static String key(String name) {
        return name.trim().toLowerCase(java.util.Locale.ROOT);
    }

    /** Who holds a name: the sheet and the person's id. */
    private static String holder(String sheetId, long personId) {
        return sheetId + "\n" + personId;
    }

    /** The sheets of the sheet's tree: the sheet itself, its ancestors among the sheets that exist, and its descendants. */
    public List<String> tree(String sheetId) {
        java.util.LinkedHashSet<String> tree = new java.util.LinkedHashSet<>();
        tree.add(sheetId);
        for (String at = sheets.parent(sheetId); at != null && sheets.exists(at) && tree.add(at); at = sheets.parent(at)) {
            // climbs to the root, stopping at a loop
        }
        tree.addAll(sheets.descendants(sheetId));
        return new java.util.ArrayList<>(tree);
    }

    /** Every name and alias used on the sheets, as names are compared, with who holds it. */
    private java.util.Map<String, java.util.Set<String>> uses(List<String> sheetIds) {
        java.util.Map<String, java.util.Set<String>> uses = new java.util.HashMap<>();
        for (String sheetId : sheetIds) {
            jdbc.query("SELECT id, name FROM person WHERE sheet_id = ?", rs -> {
                uses.computeIfAbsent(key(rs.getString("name")), k -> new java.util.HashSet<>()).add(holder(sheetId, rs.getLong("id")));
            }, sheetId);
            jdbc.query("SELECT person_id, alias FROM person_alias WHERE sheet_id = ?", rs -> {
                uses.computeIfAbsent(key(rs.getString("alias")), k -> new java.util.HashSet<>()).add(holder(sheetId, rs.getLong("person_id")));
            }, sheetId);
        }
        return uses;
    }

    /** Whether anyone but the person holds the name in the uses. */
    private static boolean usedByOther(java.util.Map<String, java.util.Set<String>> uses, String name, Person person) {
        java.util.Set<String> holders = uses.getOrDefault(key(name), java.util.Set.of());
        return holders.stream().anyMatch(h -> !h.equals(holder(person.getSheetId(), person.getId())));
    }

    /** Marks each person clashing when their name or an alias is used by anyone else on a sheet of the sheet's tree. */
    private List<Person> withClashing(String sheetId, List<Person> found) {
        if (found.isEmpty()) {
            return found;
        }
        java.util.Map<String, java.util.Set<String>> uses = uses(tree(sheetId));
        for (Person person : found) {
            boolean clashing = usedByOther(uses, person.getName(), person);
            for (String alias : person.getAliases()) {
                clashing |= usedByOther(uses, alias, person);
            }
            person.setClashing(clashing);
        }
        return found;
    }

    /** Whether the person, named this, would clash: the name or one of their aliases is used by anyone else on a
     *  sheet of their sheet's tree. */
    public boolean wouldClash(Person person, String name) {
        java.util.Map<String, java.util.Set<String>> uses = uses(tree(person.getSheetId()));
        if (usedByOther(uses, name, person)) {
            return true;
        }
        for (String alias : person.getAliases()) {
            if (usedByOther(uses, alias, person)) {
                return true;
            }
        }
        return false;
    }

    /** Whether anyone on another sheet of the sheet's tree uses the name as a name or alias, leaving out the person
     *  (null for no one). */
    public boolean nameUsedInTree(String sheetId, String name, Person except) {
        List<String> others = new java.util.ArrayList<>(tree(sheetId));
        others.remove(sheetId);
        java.util.Set<String> holders = uses(others).getOrDefault(key(name), java.util.Set.of());
        return holders.stream().anyMatch(h -> except == null || !h.equals(holder(except.getSheetId(), except.getId())));
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
     *  a member; their fixed period figures add to the other's; their statement count adds to the other's (see
     *  {@link #takeStatements}). Their holds, watchers and contact go with them as they leave the sheet. */
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
        jdbc.update("UPDATE period_person_total t SET (total, carried) = (SELECT t.total + f.total,"
                + " CASE WHEN t.carried IS NULL OR f.carried IS NULL THEN NULL ELSE t.carried + f.carried END"
                + " FROM period_person_total f WHERE f.sheet_id = t.sheet_id AND f.period_name = t.period_name AND f.person_id = ?) WHERE t.sheet_id = ? AND t.person_id = ? AND EXISTS"
                + " (SELECT 1 FROM period_person_total f WHERE f.sheet_id = t.sheet_id AND f.period_name = t.period_name AND f.person_id = ?)",
                fromId, sheetId, intoId, fromId);
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND person_id = ? AND period_name IN"
                + " (SELECT period_name FROM period_person_total WHERE sheet_id = ? AND person_id = ?)", sheetId, fromId, sheetId, intoId);
        jdbc.update("UPDATE period_person_total SET person_id = ? WHERE sheet_id = ? AND person_id = ?", intoId, sheetId, fromId);
        // What is dropped with them.
        // Statements.
        takeStatements(from, into);
        jdbc.update("DELETE FROM person_hold WHERE sheet_id = ? AND person_id = ?", sheetId, fromId);
        jdbc.update("DELETE FROM person_retirement WHERE sheet_id = ? AND person_id = ?", sheetId, fromId);
        jdbc.update("DELETE FROM person_snapshot WHERE sheet_id = ? AND person_id = ?", sheetId, fromId);
        jdbc.update("DELETE FROM person_watch WHERE sheet_id = ? AND person_id = ?", sheetId, fromId);
        jdbc.update("DELETE FROM person_statement WHERE sheet_id = ? AND person_id = ?", sheetId, fromId);
        jdbc.update("DELETE FROM person_grade WHERE sheet_id = ? AND person_id = ?", sheetId, fromId);
        jdbc.update("DELETE FROM person_terms WHERE sheet_id = ? AND person_id = ?", sheetId, fromId);
        jdbc.update("INSERT INTO person_merged SELECT p.*, (SELECT today FROM sheet WHERE id = ?) FROM person p WHERE p.sheet_id = ? AND p.id = ?",
                sheetId, sheetId, fromId);
        jdbc.update("DELETE FROM person WHERE sheet_id = ? AND id = ?", sheetId, fromId);
    }

    /** Adds the person to another sheet as they stand: their name, terms, contact, every grade they were given (with
     *  its from-date) and their aliases, and their statements (see {@link #takeStatements}). Returns them as added. */
    public Person transferIn(Person from, String toSheet) {
        java.util.SortedMap<java.time.LocalDate, String> grades = from.getGrades();
        String first = grades.isEmpty() ? from.getGrade() : grades.get(grades.firstKey());
        Person person = add(toSheet, from.getName(), first, from.getTerms(), from.getContact());
        java.time.LocalDate arrivedOn = sheets.today(toSheet);
        jdbc.update("UPDATE person SET origin = ?, arrived_on = ? WHERE id = ?", from.getSheetId(),
                java.sql.Date.valueOf(arrivedOn), person.getId());
        person.setOrigin(from.getSheetId());
        person.setArrivedOn(arrivedOn);
        grades.forEach((on, grade) -> {
            if (!on.equals(Person.BEGINNING)) {
                person.recordGrade(on, grade);
                writeRecordedGrade(person);
            }
        });
        from.getDatedTerms().forEach((on, terms) -> {
            person.recordTerms(on, terms);
            writeRecordedTerms(person);
        });
        for (String alias : jdbc.queryForList("SELECT alias FROM person_alias WHERE sheet_id = ? AND person_id = ? ORDER BY seq",
                String.class, from.getSheetId(), from.getId())) {
            addAlias(person, alias);
        }
        takeStatements(from, person);
        return person;
    }

    /** Merges the person, arriving from another sheet, into the person on that sheet: their name and aliases become
     *  the other's aliases; their statement count adds to the other's (see {@link #takeStatements}); their grades,
     *  terms and contact are dropped. Returns the person merged into. */
    public Person transferInto(Person from, Person into) {
        addAlias(into, from.getName());
        for (String alias : jdbc.queryForList("SELECT alias FROM person_alias WHERE sheet_id = ? AND person_id = ? ORDER BY seq",
                String.class, from.getSheetId(), from.getId())) {
            addAlias(into, alias);
        }
        takeStatements(from, into);
        return into;
    }

    /** The person receiving another (merged or transferred into them, or arriving as them) takes their statements:
     *  the count becomes their own plus the other's and the other's statement history joins theirs; their last
     *  statement stays theirs (the other's come over as brought), or, having none, becomes the other's (so it can be
     *  cancelled as the other's could). */
    private void takeStatements(Person from, Person into) {
        if (from.getStatementCount() == 0) {
            return;
        }
        boolean hadNone = into.getStatementCount() == 0;
        into.setStatementCount(into.getStatementCount() + from.getStatementCount());
        if (hadNone) {
            into.setStatedTotal(from.getStatedTotal());
            into.setStatementDate(from.getStatementDate());
        }
        jdbc.update("INSERT INTO person_statement (sheet_id, person_id, stated_total, statement_date, set_on, cancelled, cancelled_on, brought)"
                + " SELECT ?, ?, stated_total, statement_date, set_on, cancelled, cancelled_on, " + (hadNone ? "brought" : "TRUE")
                + " FROM person_statement WHERE sheet_id = ? AND person_id = ? ORDER BY seq",
                into.getSheetId(), into.getId(), from.getSheetId(), from.getId());
        saveStatement(into);
    }

    /** Takes the person off their sheet entirely: their patron links both ways, pool memberships, fixed period
     *  figures, holds, watches, statements, grades and aliases are dropped. Kept as they stood for the as-of view. */
    public void transferOut(Person person) {
        String sheetId = person.getSheetId();
        long id = person.getId();
        jdbc.update("DELETE FROM person_patron WHERE sheet_id = ? AND (person_id = ? OR patron_id = ?)", sheetId, id, id);
        jdbc.update("DELETE FROM pool_member WHERE sheet_id = ? AND person_id = ?", sheetId, id);
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND person_id = ?", sheetId, id);
        jdbc.update("DELETE FROM person_hold WHERE sheet_id = ? AND person_id = ?", sheetId, id);
        jdbc.update("DELETE FROM person_retirement WHERE sheet_id = ? AND person_id = ?", sheetId, id);
        jdbc.update("DELETE FROM person_snapshot WHERE sheet_id = ? AND person_id = ?", sheetId, id);
        jdbc.update("DELETE FROM person_watch WHERE sheet_id = ? AND person_id = ?", sheetId, id);
        jdbc.update("DELETE FROM person_statement WHERE sheet_id = ? AND person_id = ?", sheetId, id);
        jdbc.update("DELETE FROM person_grade WHERE sheet_id = ? AND person_id = ?", sheetId, id);
        jdbc.update("DELETE FROM person_terms WHERE sheet_id = ? AND person_id = ?", sheetId, id);
        jdbc.update("DELETE FROM person_alias WHERE sheet_id = ? AND person_id = ?", sheetId, id);
        jdbc.update("INSERT INTO person_merged SELECT p.*, (SELECT today FROM sheet WHERE id = ?) FROM person p WHERE p.sheet_id = ? AND p.id = ?",
                sheetId, sheetId, id);
        jdbc.update("DELETE FROM person WHERE sheet_id = ? AND id = ?", sheetId, id);
    }

    /** The sheet the person by the name (compared as names are) on the sheet came from; null when none. */
    public String originOf(String sheetId, String name) {
        List<String> found = jdbc.queryForList("SELECT origin FROM person WHERE sheet_id = ? AND LOWER(TRIM(name)) = ?"
                + " AND origin IS NOT NULL", String.class, sheetId, name.trim().toLowerCase(java.util.Locale.ROOT));
        return found.isEmpty() ? null : found.get(0);
    }

    /** The people merged away after the date, as they stood when merged, in the order they were added. */
    public List<Person> mergedAfter(String sheetId, java.time.LocalDate date) {
        return jdbc.query("SELECT * FROM person_merged WHERE sheet_id = ? AND merged_on > ? ORDER BY id", ROW, sheetId,
                java.sql.Date.valueOf(date));
    }

    public Person find(String sheetId, long id) {
        List<Person> found = withClashing(sheetId, withGrades(sheetId, withAliases(sheetId, withPatrons(sheetId, jdbc.query("SELECT * FROM person WHERE sheet_id = ? AND id = ?", ROW, sheetId, id)))));
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

    /** Whether another person on the sheet already has this name, or anyone on the sheet has it as an alias, or
     *  anyone on another sheet of the sheet's tree has it as a name or alias, compared without regard to case. */
    public boolean nameTaken(String sheetId, String name, Long exceptId) {
        return nameTaken(sheetId, name, exceptId, null);
    }

    /** As {@link #nameTaken(String, String, Long)}, leaving out the person (null for no one) on the tree's other sheets. */
    public boolean nameTaken(String sheetId, String name, Long exceptId, Person exceptInTree) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND LOWER(TRIM(name)) = LOWER(TRIM(?))"
                        + (exceptId == null ? "" : " AND id <> " + exceptId.longValue()),
                Integer.class, sheetId, name);
        Integer aliases = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person_alias WHERE sheet_id = ? AND LOWER(TRIM(alias)) = LOWER(TRIM(?))",
                Integer.class, sheetId, name);
        return (count != null && count > 0) || (aliases != null && aliases > 0) || nameUsedInTree(sheetId, name, exceptInTree);
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
        return add(sheetId, name, grade, terms, contact, null);
    }

    /** Adds a person with their own terms in days, contact and tags (comma separated); each null for none. */
    public Person add(String sheetId, String name, String grade, Integer terms, String contact, String tags) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade, terms, contact, tags, added_on) VALUES (?, ?, ?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?))",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, sheetId);
            ps.setString(2, name);
            ps.setString(3, grade);
            ps.setObject(4, terms, java.sql.Types.INTEGER);
            ps.setString(5, contact);
            ps.setString(6, tags);
            ps.setString(7, sheetId);
            return ps;
        }, keys);
        Person person = new Person(keys.getKey().longValue(), sheetId, name, grade);
        // A new person goes to the end of the list.
        jdbc.update("UPDATE person SET place = id WHERE id = ?", person.getId());
        person.setPlace(person.getId());
        person.setTerms(terms);
        person.setContact(contact);
        person.setTags(tags);
        recordTags(sheetId, tags);
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
        List<Long> last = jdbc.query("SELECT seq FROM person_statement WHERE sheet_id = ? AND person_id = ? AND NOT cancelled ORDER BY brought, seq DESC LIMIT 1",
                (rs, i) -> rs.getLong(1), person.getSheetId(), person.getId());
        if (!last.isEmpty()) {
            jdbc.update("UPDATE person_statement SET cancelled = TRUE, cancelled_on = (SELECT today FROM sheet WHERE id = ?) WHERE seq = ?",
                    person.getSheetId(), last.get(0));
        }
        person.setStatementCount(person.getStatementCount() - 1);
        person.setStatedTotal(null);
        person.setStatementDate(null);
        jdbc.query("SELECT stated_total, statement_date FROM person_statement WHERE sheet_id = ? AND person_id = ? AND NOT cancelled AND NOT brought ORDER BY seq DESC LIMIT 1",
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

    /** Unretires the person on the date: their retirement is kept as a past one from its from-date to the date. The
     *  person's version is left as it is — unretiring is not an edit of the person form. */
    public void saveUnretired(Person person, java.time.LocalDate on) {
        Person.Retirement past = new Person.Retirement(person.getRetiredFrom(), on);
        jdbc.update("INSERT INTO person_retirement (sheet_id, person_id, from_date, to_date, set_on) VALUES (?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?))",
                person.getSheetId(), person.getId(), java.sql.Date.valueOf(past.from()), java.sql.Date.valueOf(past.to()),
                person.getSheetId());
        person.getRetirements().add(past);
        person.setRetiredFrom(null);
        saveRetired(person);
    }

    /** Adds a hold to the person. The person's version is left as it is — holding is not an edit of
     *  the person form. */
    public void addHold(Person person, Person.Hold hold) {
        jdbc.update("INSERT INTO person_hold (sheet_id, person_id, from_date, to_date, set_on) VALUES (?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?))",
                person.getSheetId(), person.getId(), java.sql.Date.valueOf(hold.from()),
                hold.to() == null ? null : java.sql.Date.valueOf(hold.to()), person.getSheetId());
        person.getHolds().add(hold);
    }

    /** Flags the person with a reason until a date, replacing any flag they had. The person's version is left as it
     *  is — flagging is not an edit of the person form. */
    public void flag(Person person, String reason, LocalDate until) {
        jdbc.update("UPDATE person SET flag_reason = ?, flag_until = ? WHERE sheet_id = ? AND id = ?",
                reason, java.sql.Date.valueOf(until), person.getSheetId(), person.getId());
        person.setFlag(reason, until);
    }

    /** Saves the person over the version it was loaded at, moving it on by one, with any grade the
     *  edit recorded; false (nothing written) when someone else has saved the person since. */
    public boolean save(Person person) {
        int updated = jdbc.update(
                "UPDATE person SET name = ?, terms = ?, contact = ?, tags = ?, version = version + 1 WHERE sheet_id = ? AND id = ? AND version = ?",
                person.getName(), person.getTerms(), person.getContact(), person.getTags(), person.getSheetId(), person.getId(), person.getVersion());
        if (updated == 0) {
            return false;
        }
        writeRecordedGrade(person);
        writeRecordedTerms(person);
        writeHistory(person);
        recordTags(person.getSheetId(), person.getTags());
        person.setVersion(person.getVersion() + 1);
        return true;
    }

    /** Remembers each of the person's tags (comma separated; null for none) as used on the sheet. */
    private void recordTags(String sheetId, String tags) {
        if (tags == null) return;
        for (String tag : tags.split(",")) {
            jdbc.update("MERGE INTO sheet_tag (sheet_id, tag) KEY (sheet_id, tag) VALUES (?, ?)", sheetId, tag);
        }
    }
}

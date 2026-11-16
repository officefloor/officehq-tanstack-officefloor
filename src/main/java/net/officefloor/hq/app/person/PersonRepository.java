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
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The stored people, kept per sheet. Ids are assigned in the order people are added; people are listed by their
 *  position, new ones at the end (see #move). A person's grades are kept
 *  dated in person_grade, their own terms from a date in person_terms_from (those from the beginning of time in
 *  person.terms), and their holds in person_hold; a person read carries them all, and the grade and own terms in force
 *  on the sheet's today. */
@Repository
public class PersonRepository {

    private static final String COLUMNS = "id, sheet_id, name, version, retired_from, terms, contact, origin, tags";

    private final JdbcTemplate jdbc;
    private final SheetRepository sheets;
    private final SettingRepository settings;

    public PersonRepository(JdbcTemplate jdbc, SheetRepository sheets, SettingRepository settings) {
        this.jdbc = jdbc;
        this.sheets = sheets;
        this.settings = settings;
    }

    /** A name or alias as names are compared: lower-cased (as the name_key and alias_key columns are). */
    public static String key(String name) {
        return name.toLowerCase(java.util.Locale.ROOT);
    }

    /** The names and aliases (as compared, see #key) used on the other sheets of the sheet's tree (see
     *  SettingRepository#tree): none may be taken on the sheet. */
    public java.util.Set<String> treeKeys(String sheetId) {
        java.util.Set<String> keys = new java.util.HashSet<>();
        for (String other : settings.tree(sheetId)) {
            keys.addAll(jdbc.queryForList("SELECT name_key FROM person WHERE sheet_id = ?", String.class, other));
            keys.addAll(jdbc.queryForList("SELECT alias_key FROM person_alias WHERE sheet_id = ?", String.class, other));
        }
        return keys;
    }

    /** Whether the name is used as a name or alias on another sheet of the sheet's tree, case ignored. */
    public boolean treeTaken(String sheetId, String name) {
        return treeKeys(sheetId).contains(key(name));
    }

    /** The people, each marked with whether their name or an alias is used on another sheet of the sheet's tree. */
    public List<Person> withClashing(String sheetId, List<Person> people) {
        java.util.Set<String> keys = treeKeys(sheetId);
        return people.stream().map((p) -> p.withClashing(p.usesAny(keys))).toList();
    }

    public List<Person> list(String sheetId) {
        return withGrades(sheetId, jdbc.query("SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? ORDER BY position, id",
                PersonRepository::map, sheetId));
    }

    public Person find(String sheetId, long id) {
        return first(sheetId, jdbc.query("SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? AND id = ?",
                PersonRepository::map, sheetId, id));
    }

    /** The person on the sheet with this name or alias, case ignored; null when there is none. */
    public Person findByName(String sheetId, String name) {
        return first(sheetId, jdbc.query("SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? AND (name_key = LOWER(?)"
                + " OR id IN (SELECT person_id FROM person_alias WHERE sheet_id = ? AND alias_key = LOWER(?)))",
                PersonRepository::map, sheetId, name, sheetId, name));
    }

    private Person first(String sheetId, List<Person> found) {
        return found.isEmpty() ? null : withGrades(sheetId, found).get(0);
    }

    /** The people with their grades and holds, and the grade in force on the sheet's today. */
    private List<Person> withGrades(String sheetId, List<Person> found) {
        if (found.isEmpty()) {
            return found;
        }
        Map<Long, List<PersonGrade>> grades = new HashMap<>();
        jdbc.query("SELECT person_id, from_date, grade FROM person_grade WHERE sheet_id = ? ORDER BY person_id, from_date NULLS FIRST",
                (ResultSet rs) -> {
                    Date from = rs.getDate("from_date");
                    grades.computeIfAbsent(rs.getLong("person_id"), (k) -> new ArrayList<>())
                            .add(new PersonGrade(rs.getString("grade"), from == null ? null : from.toLocalDate()));
                }, sheetId);
        Map<Long, List<PersonHold>> holds = new HashMap<>();
        jdbc.query("SELECT person_id, from_date, to_date FROM person_hold WHERE sheet_id = ? ORDER BY person_id, from_date, id",
                (ResultSet rs) -> {
                    Date to = rs.getDate("to_date");
                    holds.computeIfAbsent(rs.getLong("person_id"), (k) -> new ArrayList<>())
                            .add(new PersonHold(rs.getDate("from_date").toLocalDate(), to == null ? null : to.toLocalDate()));
                }, sheetId);
        Map<Long, List<PersonTerms>> dated = datedTerms(sheetId);
        Map<Long, List<String>> aliases = aliases(sheetId);
        Map<Long, List<PersonRetirement>> retirements = new HashMap<>();
        jdbc.query("SELECT person_id, from_date, to_date FROM person_retirement WHERE sheet_id = ? ORDER BY person_id, id",
                (ResultSet rs) -> {
                    retirements.computeIfAbsent(rs.getLong("person_id"), (k) -> new ArrayList<>()).add(new PersonRetirement(
                            rs.getDate("from_date").toLocalDate(), rs.getDate("to_date").toLocalDate()));
                }, sheetId);
        LocalDate today = sheets.today(sheetId);
        List<Person> people = new ArrayList<>();
        for (Person p : found) {
            List<PersonGrade> mine = List.copyOf(grades.getOrDefault(p.id(), List.of()));
            List<PersonTerms> own = ownTerms(p.terms(), dated.get(p.id()));
            people.add(new Person(p.id(), p.sheetId(), p.name(), Person.gradeOn(mine, today), p.version(), p.retiredFrom(),
                    mine, List.copyOf(holds.getOrDefault(p.id(), List.of())), p.terms(), p.contact(),
                    List.copyOf(aliases.getOrDefault(p.id(), List.of()))).withOrigin(p.origin())
                    .withRetirements(List.copyOf(retirements.getOrDefault(p.id(), List.of()))).withTags(p.tags())
                    .withOwnTerms(own, Person.termsOn(own, today)));
        }
        return people;
    }

    /** The sheet's own terms recorded from a date, by person, each person's by from-date. */
    private Map<Long, List<PersonTerms>> datedTerms(String sheetId) {
        Map<Long, List<PersonTerms>> dated = new HashMap<>();
        jdbc.query("SELECT person_id, from_date, terms FROM person_terms_from WHERE sheet_id = ? ORDER BY person_id, from_date",
                (ResultSet rs) -> {
                    dated.computeIfAbsent(rs.getLong("person_id"), (k) -> new ArrayList<>()).add(new PersonTerms(
                            rs.getObject("terms", Integer.class), rs.getDate("from_date").toLocalDate()));
                }, sheetId);
        return dated;
    }

    /** A person's own terms as recorded: those from the beginning of time first, then the dated ones. */
    private static List<PersonTerms> ownTerms(Integer base, List<PersonTerms> dated) {
        List<PersonTerms> terms = new ArrayList<>();
        terms.add(new PersonTerms(base, null));
        if (dated != null) {
            terms.addAll(dated);
        }
        return List.copyOf(terms);
    }

    /** The sheet's aliases by person, each person's sorted as names are compared. */
    private Map<Long, List<String>> aliases(String sheetId) {
        Map<Long, List<String>> aliases = new HashMap<>();
        jdbc.query("SELECT person_id, alias FROM person_alias WHERE sheet_id = ? ORDER BY person_id, alias_key, alias",
                (ResultSet rs) -> {
                    aliases.computeIfAbsent(rs.getLong("person_id"), (k) -> new ArrayList<>()).add(rs.getString("alias"));
                }, sheetId);
        return aliases;
    }

    /** Whether a person on the sheet other than {@code exceptId} (0: anyone) has this name, or anyone has it as an
     *  alias, case ignored. */
    public boolean nameTaken(String sheetId, String name, long exceptId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND name_key = LOWER(?) AND id <> ?",
                Integer.class, sheetId, name, exceptId);
        return (n != null && n > 0) || aliasTaken(sheetId, name);
    }

    /** Whether anyone on the sheet has this alias, case ignored. */
    private boolean aliasTaken(String sheetId, String alias) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM person_alias WHERE sheet_id = ? AND alias_key = LOWER(?)",
                Integer.class, sheetId, alias);
        return n != null && n > 0;
    }

    /** Gives the person the alias: refused when it equals, case ignored, any name or alias on the sheet or on another
     *  sheet of its tree (the unique (sheet_id, alias) index refuses one taken as written by a concurrent add). */
    public void addAlias(String sheetId, long personId, String alias) throws DuplicatePersonNameException {
        if (nameTaken(sheetId, alias, 0) || treeTaken(sheetId, alias)) {
            throw new DuplicatePersonNameException(alias);
        }
        try {
            jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias, added_on) VALUES (?, ?, ?, ?)", sheetId, personId,
                    alias, Date.valueOf(sheets.today(sheetId)));
        } catch (DuplicateKeyException e) {
            throw new DuplicatePersonNameException(alias);
        }
    }

    /** Whether a person on the sheet other than {@code exceptId} (0: anyone) has this contact. */
    public boolean contactTaken(String sheetId, String contact, long exceptId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM person WHERE sheet_id = ? AND contact = ? AND id <> ?",
                Integer.class, sheetId, contact, exceptId);
        return n != null && n > 0;
    }

    /** Whether the refused write broke the unique (sheet_id, contact) index rather than the name's. */
    private static boolean contactClash(DuplicateKeyException e) {
        return String.valueOf(e.getMessage()).toUpperCase(java.util.Locale.ROOT).contains("PERSON_SHEET_CONTACT");
    }

    /** The unique (sheet_id, name_key) and (sheet_id, contact) indexes refuse a name or contact taken by a concurrent
     *  add or edit. */
    public Person add(String sheetId, String name, String grade, Integer terms, String contact)
            throws DuplicatePersonNameException, DuplicatePersonContactException {
        return add(sheetId, name, grade, terms, contact, List.of());
    }

    /** Adds the person with their tags (see #add). */
    public Person add(String sheetId, String name, String grade, Integer terms, String contact, List<String> tags)
            throws DuplicatePersonNameException, DuplicatePersonContactException {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        try {
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement("INSERT INTO person (sheet_id, name, grade, terms, added_on, contact, tags) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        new String[] { "id" });
                ps.setString(1, sheetId);
                ps.setString(2, name);
                ps.setString(3, grade);
                ps.setObject(4, terms, java.sql.Types.INTEGER);
                ps.setDate(5, Date.valueOf(sheets.today(sheetId)));
                ps.setString(6, contact);
                ps.setString(7, stored(tags));
                return ps;
            }, keys);
        } catch (DuplicateKeyException e) {
            if (contactClash(e)) {
                throw new DuplicatePersonContactException(contact);
            }
            throw new DuplicatePersonNameException(name);
        }
        long id = keys.getKey().longValue();
        toEnd(sheetId, id);
        jdbc.update("INSERT INTO person_grade (sheet_id, person_id, from_date, grade) VALUES (?, ?, NULL, ?)", sheetId, id, grade);
        logGrades(sheetId, id, List.of(new PersonGrade(grade, null)));
        return new Person(id, sheetId, name, grade, 0, null, List.of(new PersonGrade(grade, null)), List.of(), terms, contact).withTags(tags);
    }

    /** Records the person's own terms from the terms' (non-null) from-date. */
    public void recordTerms(String sheetId, long id, PersonTerms terms) {
        jdbc.update("INSERT INTO person_terms_from (sheet_id, person_id, from_date, terms) VALUES (?, ?, ?, ?)", sheetId, id,
                Date.valueOf(terms.from()), terms.days());
    }

    /** Puts the person at the end of the sheet's list: their position is their id (ids only go up), or one after the
     *  last on the sheet when that is higher. */
    public void toEnd(String sheetId, long id) {
        Long last = jdbc.queryForObject("SELECT MAX(position) FROM person WHERE sheet_id = ? AND id <> ?", Long.class,
                sheetId, id);
        jdbc.update("UPDATE person SET position = ? WHERE sheet_id = ? AND id = ?", last == null ? id : Math.max(id, last + 1),
                sheetId, id);
    }

    /** Swaps the person with the one above (up) or below them in the sheet's list, returning their new position
     *  (counting from 1); null when they are already at the top (up) or bottom (down), nothing then being written. */
    public Integer move(String sheetId, long id, boolean up) {
        List<long[]> order = jdbc.query("SELECT id, position FROM person WHERE sheet_id = ? ORDER BY position, id",
                (rs, n) -> new long[] { rs.getLong("id"), rs.getLong("position") }, sheetId);
        int at = -1;
        for (int i = 0; i < order.size(); i++) {
            if (order.get(i)[0] == id) {
                at = i;
            }
        }
        int to = up ? at - 1 : at + 1;
        if (at < 0 || to < 0 || to >= order.size()) {
            return null;
        }
        long[] mine = order.get(at);
        long[] theirs = order.get(to);
        long mineTo = theirs[1];
        long theirsTo = mine[1];
        if (mineTo == theirsTo) {
            // Same position (added at once): the id breaks the tie, so make the two distinct around it.
            mineTo = up ? theirs[1] - 1 : theirs[1] + 1;
        }
        jdbc.update("UPDATE person SET position = ? WHERE sheet_id = ? AND id = ?", mineTo, sheetId, mine[0]);
        jdbc.update("UPDATE person SET position = ? WHERE sheet_id = ? AND id = ?", theirsTo, sheetId, theirs[0]);
        return to + 1;
    }

    /** Stores the person as its new version, with its grades, only while the stored one is still {@code fromVersion}:
     *  a save that lands in between refuses this one. */
    public void update(Person person, long fromVersion)
            throws DuplicatePersonNameException, DuplicatePersonContactException, StalePersonException {
        List<Integer> termsWas = jdbc.query("SELECT terms FROM person WHERE sheet_id = ? AND id = ?",
                (rs, n) -> rs.getObject(1, Integer.class), person.sheetId(), person.id());
        int n;
        try {
            n = jdbc.update("UPDATE person SET name = ?, version = ?, terms = ?, contact = ?, tags = ? WHERE sheet_id = ? AND id = ? AND version = ?",
                    person.name(), person.version(), person.baseTerms(), person.contact(), stored(person.tags()), person.sheetId(), person.id(), fromVersion);
        } catch (DuplicateKeyException e) {
            if (contactClash(e)) {
                throw new DuplicatePersonContactException(person.contact());
            }
            throw new DuplicatePersonNameException(person.name());
        }
        if (n == 0) {
            throw new StalePersonException(person.id());
        }
        jdbc.update("DELETE FROM person_grade WHERE sheet_id = ? AND person_id = ?", person.sheetId(), person.id());
        for (PersonGrade g : person.grades()) {
            jdbc.update("INSERT INTO person_grade (sheet_id, person_id, from_date, grade) VALUES (?, ?, ?, ?)",
                    person.sheetId(), person.id(), g.from() == null ? null : Date.valueOf(g.from()), g.grade());
        }
        logGrades(person.sheetId(), person.id(), person.grades());
        Integer was = termsWas.isEmpty() ? null : termsWas.get(0);
        if (!java.util.Objects.equals(was, person.baseTerms())) {
            jdbc.update("INSERT INTO person_terms_log (sheet_id, person_id, terms_was, terms, made_on) VALUES (?, ?, ?, ?, ?)",
                    person.sheetId(), person.id(), was, person.baseTerms(), Date.valueOf(sheets.today(person.sheetId())));
        }
        jdbc.update("DELETE FROM person_terms_from WHERE sheet_id = ? AND person_id = ?", person.sheetId(), person.id());
        for (PersonTerms t : person.ownTerms()) {
            if (t.from() != null) {
                recordTerms(person.sheetId(), person.id(), t);
            }
        }
    }

    /** The person's own terms from the beginning of time at the end of the date: those set by the last change made on
     *  or before it, else those before the first change made after it (none logged: the terms stored now). */
    private Integer termsAsOf(Person person, LocalDate date) {
        Integer[] terms = { person.baseTerms() };
        boolean[] found = { false };
        jdbc.query("SELECT terms_was, terms, made_on FROM person_terms_log WHERE sheet_id = ? AND person_id = ? ORDER BY id",
                (ResultSet rs) -> {
                    if (found[0]) {
                        return;
                    }
                    boolean after = rs.getObject("made_on", LocalDate.class).isAfter(date);
                    terms[0] = rs.getObject(after ? "terms_was" : "terms", Integer.class);
                    found[0] = after;
                }, person.sheetId(), person.id());
        return terms[0];
    }

    /** Logs the person's grades as stored now, with the sheet's today. */
    private void logGrades(String sheetId, long id, List<PersonGrade> grades) {
        StringBuilder text = new StringBuilder();
        for (PersonGrade g : grades) {
            text.append(text.length() == 0 ? "" : ";").append(g.grade()).append('@').append(g.from() == null ? "" : g.from());
        }
        jdbc.update("INSERT INTO person_grade_log (sheet_id, person_id, grades, made_on) VALUES (?, ?, ?, ?)", sheetId, id,
                text.toString(), Date.valueOf(sheets.today(sheetId)));
    }

    /** The sheet's today when the person was added; null when that was not kept (they were there from the beginning). */
    public LocalDate addedOn(String sheetId, long id) {
        List<LocalDate> found = jdbc.query("SELECT added_on FROM person WHERE sheet_id = ? AND id = ?",
                (rs, n) -> rs.getObject(1, LocalDate.class), sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The person as they stood at the end of the date: the grades last stored, the holds and aliases added, and the
     *  retirement date last given (or unretirement made) and the past retirements ended while the sheet's today was on or before it (a part never stored by then: none; grades never
     *  logged: those they have now). */
    public Person asOf(Person person, LocalDate date) {
        String sheetId = person.sheetId();
        List<String> logged = jdbc.queryForList("SELECT grades FROM person_grade_log WHERE sheet_id = ? AND person_id = ?"
                + " AND made_on <= ? ORDER BY id DESC LIMIT 1", String.class, sheetId, person.id(), Date.valueOf(date));
        List<PersonGrade> grades = person.grades();
        if (!logged.isEmpty()) {
            grades = new ArrayList<>();
            for (String g : logged.get(0).split(";")) {
                int at = g.indexOf('@');
                String from = g.substring(at + 1);
                grades.add(new PersonGrade(g.substring(0, at), from.isEmpty() ? null : LocalDate.parse(from)));
            }
        }
        List<PersonHold> holds = jdbc.query("SELECT from_date, to_date FROM person_hold WHERE sheet_id = ? AND person_id = ?"
                + " AND made_on <= ? ORDER BY from_date, id", (rs, n) -> {
                    Date to = rs.getDate("to_date");
                    return new PersonHold(rs.getDate("from_date").toLocalDate(), to == null ? null : to.toLocalDate());
                }, sheetId, person.id(), Date.valueOf(date));
        List<LocalDate> retired = jdbc.query("SELECT from_date FROM person_retire_log WHERE sheet_id = ? AND person_id = ?"
                + " AND made_on <= ? ORDER BY id DESC LIMIT 1", (rs, n) -> rs.getObject(1, LocalDate.class), sheetId,
                person.id(), Date.valueOf(date));
        List<PersonRetirement> retirements = jdbc.query("SELECT from_date, to_date FROM person_retirement WHERE sheet_id = ?"
                + " AND person_id = ? AND made_on <= ? ORDER BY id", (rs, n) -> new PersonRetirement(
                        rs.getDate("from_date").toLocalDate(), rs.getDate("to_date").toLocalDate()),
                sheetId, person.id(), Date.valueOf(date));
        List<String> given = jdbc.queryForList("SELECT alias FROM person_alias WHERE sheet_id = ? AND person_id = ?"
                + " AND (added_on IS NULL OR added_on <= ?)", String.class, sheetId, person.id(), Date.valueOf(date));
        List<String> aliases = person.aliases().stream().filter(given::contains).toList();
        Person then = new Person(person.id(), sheetId, person.name(), Person.gradeOn(grades, date), person.version(),
                retired.isEmpty() || retired.get(0) == null ? null : retired.get(0).toString(), List.copyOf(grades), holds,
                termsAsOf(person, date), person.contact(), aliases).withOrigin(person.origin()).withRetirements(retirements)
                .withTags(person.tags());
        List<PersonTerms> own = new ArrayList<>(person.ownTerms());
        own.replaceAll((t) -> t.from() == null ? new PersonTerms(then.terms(), null) : t);
        return then.withOwnTerms(List.copyOf(own), Person.termsOn(own, date));
    }

    /** The sheet's people as they stood at the end of the date (see #asOf), in list order (see #list; one merged away
     *  in the place they were added): one added while the
     *  sheet's today was after it is left out, and one merged into another while it was after it is still listed. */
    public List<Person> listAsOf(String sheetId, LocalDate date) {
        Map<Long, LocalDate> added = new HashMap<>();
        Map<Long, Long> positions = new HashMap<>();
        jdbc.query("SELECT id, added_on, position FROM person WHERE sheet_id = ?",
                (ResultSet rs) -> {
                    added.put(rs.getLong("id"), rs.getObject("added_on", LocalDate.class));
                    positions.put(rs.getLong("id"), rs.getLong("position"));
                }, sheetId);
        List<Person> people = new ArrayList<>();
        for (Person p : list(sheetId)) {
            LocalDate on = added.get(p.id());
            if (on == null || !on.isAfter(date)) {
                people.add(asOf(p, date));
            }
        }
        people.addAll(mergedAsOf(sheetId, date));
        people.sort(java.util.Comparator.<Person>comparingLong((p) -> positions.getOrDefault(p.id(), p.id()))
                .thenComparingLong(Person::id));
        return people;
    }

    /** The people merged into another after the date but added on or before it, as they stood when merged. */
    private List<Person> mergedAsOf(String sheetId, LocalDate date) {
        return jdbc.query("SELECT id, name, grades, retired_from, terms, contact FROM person_merged WHERE sheet_id = ?"
                + " AND merged_on > ? AND (added_on IS NULL OR added_on <= ?) ORDER BY id", (rs, n) -> {
                    List<PersonGrade> grades = new ArrayList<>();
                    for (String g : rs.getString("grades").split(";")) {
                        if (g.isEmpty()) {
                            continue;
                        }
                        int at = g.indexOf('@');
                        String from = g.substring(at + 1);
                        grades.add(new PersonGrade(g.substring(0, at), from.isEmpty() ? null : LocalDate.parse(from)));
                    }
                    Date retired = rs.getDate("retired_from");
                    return new Person(rs.getLong("id"), sheetId, rs.getString("name"), Person.gradeOn(grades, date), 0,
                            retired == null ? null : retired.toLocalDate().toString(), List.copyOf(grades), List.of(),
                            rs.getObject("terms", Integer.class), rs.getString("contact"));
                }, sheetId, Date.valueOf(date), Date.valueOf(date));
    }

    /** Retires the person from the date (replacing any earlier retirement date). */
    public void retire(String sheetId, long id, LocalDate from) {
        jdbc.update("UPDATE person SET retired_from = ? WHERE sheet_id = ? AND id = ?", Date.valueOf(from), sheetId, id);
        jdbc.update("INSERT INTO person_retire_log (sheet_id, person_id, from_date, made_on) VALUES (?, ?, ?, ?)", sheetId,
                id, Date.valueOf(from), Date.valueOf(sheets.today(sheetId)));
    }

    /** Unretires the person on the date: they are no longer retired, and the retirement is kept, from its date to this. */
    public void unretire(String sheetId, long id, LocalDate from, LocalDate on) {
        LocalDate today = sheets.today(sheetId);
        jdbc.update("UPDATE person SET retired_from = NULL WHERE sheet_id = ? AND id = ?", sheetId, id);
        jdbc.update("INSERT INTO person_retirement (sheet_id, person_id, from_date, to_date, made_on) VALUES (?, ?, ?, ?, ?)",
                sheetId, id, Date.valueOf(from), Date.valueOf(on), Date.valueOf(today));
        jdbc.update("INSERT INTO person_retire_log (sheet_id, person_id, from_date, made_on) VALUES (?, ?, NULL, ?)", sheetId,
                id, Date.valueOf(today));
    }

    /** Adds a hold on the person, from the date until the date (null: open-ended). */
    public void hold(String sheetId, long id, PersonHold hold) {
        jdbc.update("INSERT INTO person_hold (sheet_id, person_id, from_date, to_date, made_on) VALUES (?, ?, ?, ?, ?)", sheetId, id,
                Date.valueOf(hold.from()), hold.to() == null ? null : Date.valueOf(hold.to()), Date.valueOf(sheets.today(sheetId)));
    }

    private static Person map(ResultSet rs, int n) throws SQLException {
        Date retired = rs.getDate("retired_from");
        return new Person(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name"), null, rs.getLong("version"),
                retired == null ? null : retired.toLocalDate().toString(), List.of(), List.of(),
                rs.getObject("terms", Integer.class), rs.getString("contact")).withOrigin(rs.getString("origin"))
                .withTags(tags(rs.getString("tags")));
    }

    /** Tags as stored: joined by commas, null when none. */
    private static String stored(List<String> tags) {
        return tags == null || tags.isEmpty() ? null : String.join(",", tags);
    }

    /** Tags as read from a stored column (null: none). */
    private static List<String> tags(String stored) {
        return stored == null || stored.isEmpty() ? List.of() : List.of(stored.split(","));
    }
}

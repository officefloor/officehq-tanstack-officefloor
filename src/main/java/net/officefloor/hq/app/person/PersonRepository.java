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
import net.officefloor.hq.app.RowCopy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PersonRepository {

    private static final String COLUMNS = "p.id, p.sheet_id, p.name, p.grade, p.version, p.statement_count, p.stated_total,"
            + " p.statement_date, p.retired_from, p.terms, p.contact FROM person p";

    /** The position at the end of the sheet's list (the sheet id its one parameter). */
    private static final String NEXT_POSITION = "(SELECT COALESCE(MAX(position), 0) + 1 FROM person WHERE sheet_id = ?)";

    private final JdbcTemplate jdbc;

    public PersonRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's people, in their order in the list (by position). */
    public List<Person> findAll(String sheetId) {
        return withGrades(sheetId, jdbc.query("SELECT " + COLUMNS + " WHERE p.sheet_id = ? ORDER BY p.position, p.id",
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
                    person.name(), person.firstTerms(), person.contact(), person.id(), person.sheetId(), person.version());
            if (n == 0) {
                return null;
            }
            jdbc.update("DELETE FROM person_grade WHERE person_id = ? AND sheet_id = ?", person.id(), person.sheetId());
            for (GradeFrom g : person.grades()) {
                insertGrade(person.sheetId(), person.id(), g);
            }
            insertTerms(person.sheetId(), person.id(), person.termsFrom());
            return new Person(person.id(), person.sheetId(), person.name(), person.grade(), person.version() + 1,
                    person.statementCount(), person.statedTotal(), person.statementDate(), person.retiredFrom(),
                    person.holds(), person.grades(), person.terms(), person.contact(), person.pastRetirements(),
                    person.termsFrom());
        }
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade, terms, contact, position) VALUES (?, ?, ?, ?, ?, "
                            + NEXT_POSITION + ")", new String[] { "id" });
            ps.setString(1, person.sheetId());
            ps.setString(2, person.name());
            ps.setString(3, person.grade());
            ps.setObject(4, person.firstTerms());
            ps.setString(5, person.contact());
            ps.setString(6, person.sheetId());
            return ps;
        }, keys);
        long id = keys.getKey().longValue();
        insertGrade(person.sheetId(), id, new GradeFrom(person.grade(), null));
        insertTerms(person.sheetId(), id, person.termsFrom());
        return new Person(id, person.sheetId(), person.name(), person.grade(), 0, 0, null, null, null, List.of(),
                person.grades(), person.terms(), person.contact(), List.of(), person.termsFrom());
    }

    /** Replace the person's terms recorded from a from-date with these (those from the beginning of time skipped). */
    private void insertTerms(String sheetId, long personId, List<TermsFrom> terms) {
        jdbc.update("DELETE FROM person_terms WHERE person_id = ? AND sheet_id = ?", personId, sheetId);
        for (TermsFrom t : terms) {
            if (t.from() != null) {
                jdbc.update("INSERT INTO person_terms (sheet_id, person_id, terms, from_date) VALUES (?, ?, ?, ?)",
                        sheetId, personId, t.terms(), Date.valueOf(t.from()));
            }
        }
    }

    private void insertGrade(String sheetId, long personId, GradeFrom grade) {
        jdbc.update("INSERT INTO person_grade (sheet_id, person_id, grade, from_date) VALUES (?, ?, ?, ?)", sheetId,
                personId, grade.grade(), grade.from() == null ? null : Date.valueOf(grade.from()));
    }

    /**
     * The people with the grades, holds and past retirements recorded for them, each showing the grade in force on
     * the sheet's today; their holds and past retirements oldest first.
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
        Map<Long, List<TermsFrom>> terms = new HashMap<>();
        jdbc.query("SELECT person_id, terms, from_date FROM person_terms WHERE sheet_id = ? ORDER BY from_date", (rs) -> {
            terms.computeIfAbsent(rs.getLong("person_id"), (k) -> new ArrayList<>())
                    .add(new TermsFrom(rs.getObject("terms", Integer.class), rs.getObject("from_date", LocalDate.class)));
        }, sheetId);
        Map<Long, List<Hold>> holds = new HashMap<>();
        jdbc.query("SELECT person_id, from_date, to_date FROM person_hold WHERE sheet_id = ? ORDER BY id", (rs) -> {
            holds.computeIfAbsent(rs.getLong("person_id"), (k) -> new ArrayList<>()).add(new Hold(
                    rs.getObject("from_date", LocalDate.class), rs.getObject("to_date", LocalDate.class)));
        }, sheetId);
        Map<Long, List<PastRetirement>> retirements = new HashMap<>();
        jdbc.query("SELECT person_id, from_date, to_date FROM person_retirement WHERE sheet_id = ? ORDER BY id", (rs) -> {
            retirements.computeIfAbsent(rs.getLong("person_id"), (k) -> new ArrayList<>()).add(new PastRetirement(
                    rs.getObject("from_date", LocalDate.class), rs.getObject("to_date", LocalDate.class)));
        }, sheetId);
        List<LocalDate> today = jdbc.query("SELECT today FROM sheet WHERE id = ?",
                (rs, n) -> rs.getObject("today", LocalDate.class), sheetId);
        LocalDate on = today.isEmpty() ? LocalDate.now() : today.get(0);
        List<Person> people = new ArrayList<>();
        for (Person p : found) {
            List<GradeFrom> own = grades.getOrDefault(p.id(), List.of(new GradeFrom(p.grade(), null)));
            List<TermsFrom> ownTerms = new ArrayList<>();
            ownTerms.add(new TermsFrom(p.terms(), null));
            ownTerms.addAll(terms.getOrDefault(p.id(), List.of()));
            people.add(new Person(p.id(), p.sheetId(), p.name(), Person.gradeOn(own, on, p.grade()), p.version(),
                    p.statementCount(), p.statedTotal(), p.statementDate(), p.retiredFrom(),
                    List.copyOf(holds.getOrDefault(p.id(), List.of())), List.copyOf(own),
                    Person.ownTermsOn(ownTerms, on), p.contact(),
                    List.copyOf(retirements.getOrDefault(p.id(), List.of())), List.copyOf(ownTerms)));
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

    /**
     * The statements still standing of each person on the sheet, each as {@code <statement date> <due-total>}, oldest
     * first (in the order taken).
     */
    public Map<Long, List<String>> statementLinesAll(String sheetId) {
        Map<Long, List<String>> found = new HashMap<>();
        jdbc.query("SELECT person_id, stated_total, statement_date FROM person_statement WHERE sheet_id = ?"
                + " ORDER BY seq", (rs) -> {
                    found.computeIfAbsent(rs.getLong("person_id"), (k) -> new ArrayList<>()).add(statementLine(rs));
                }, sheetId);
        return found;
    }

    /** The person's statements still standing, as {@link #statementLinesAll} gives them. */
    public List<String> statementLines(String sheetId, long personId) {
        return jdbc.query("SELECT stated_total, statement_date FROM person_statement WHERE sheet_id = ? AND person_id = ?"
                + " ORDER BY seq", (rs, n) -> statementLine(rs), sheetId, personId);
    }

    private static String statementLine(ResultSet rs) throws SQLException {
        LocalDate date = rs.getObject("statement_date", LocalDate.class);
        String total = rs.getString("stated_total");
        return (date == null ? "" : date.toString()) + " " + (total == null ? "n/a" : total);
    }

    /** Retire the person from this date; the person as stored. Not an edit, so it leaves the version alone. */
    public Person retire(Person person, LocalDate from) {
        jdbc.update("UPDATE person SET retired_from = ? WHERE id = ? AND sheet_id = ?", Date.valueOf(from), person.id(),
                person.sheetId());
        return find(person.sheetId(), person.id());
    }

    /**
     * Unretire the retired person on this date, keeping their retirement as a past one up to it; the person as
     * stored. Not an edit, so it leaves the version alone.
     */
    public Person unretire(Person person, LocalDate on) {
        jdbc.update("INSERT INTO person_retirement (sheet_id, person_id, from_date, to_date) VALUES (?, ?, ?, ?)",
                person.sheetId(), person.id(), Date.valueOf(person.retiredFrom()), Date.valueOf(on));
        jdbc.update("UPDATE person SET retired_from = NULL WHERE id = ? AND sheet_id = ?", person.id(),
                person.sheetId());
        return find(person.sheetId(), person.id());
    }

    /** Add a hold to the person; the person as stored. Not an edit, so it leaves the version alone. */
    public Person hold(Person person, Hold hold) {
        jdbc.update("INSERT INTO person_hold (sheet_id, person_id, from_date, to_date, added_on)"
                + " SELECT ?, ?, ?, ?, today FROM sheet WHERE id = ?",
                person.sheetId(), person.id(), hold.from() == null ? null : Date.valueOf(hold.from()),
                hold.to() == null ? null : Date.valueOf(hold.to()), person.sheetId());
        return find(person.sheetId(), person.id());
    }

    /**
     * Merge the person ({@code from}, their claims already moved) into the other: their name and then their aliases,
     * in the order added, become the other's newest aliases; their patrons are added after the other's, in the order named (not the other,
     * no repeats); anyone naming them as patron names the other instead (not themselves, no repeats); their pool
     * and crew memberships, crews they lead, and plans move to the other (shares added where the other is already a member). Their statements join
     * the other's, before them: the other's count is both counts added, and the other's last statement stays theirs (or,
     * having none, becomes the merged person's). Their grades,
     * holds, past retirements, watchers, snapshot lines and contact are dropped with them.
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
                + " (SELECT 1 FROM person_patron q WHERE q.person_id = ? AND q.patron_id = p.patron_id) ORDER BY seq",
                kept, sheetId, gone, kept, kept);
        jdbc.update("INSERT INTO person_patron (sheet_id, person_id, patron_id) SELECT sheet_id, person_id, ?"
                + " FROM person_patron p WHERE sheet_id = ? AND patron_id = ? AND person_id <> ? AND NOT EXISTS"
                + " (SELECT 1 FROM person_patron q WHERE q.person_id = p.person_id AND q.patron_id = ?) ORDER BY seq",
                kept, sheetId, gone, kept, kept);
        jdbc.update("DELETE FROM person_patron WHERE sheet_id = ? AND (person_id = ? OR patron_id = ?)", sheetId, gone,
                gone);

        jdbc.update("UPDATE pool_member k SET share = share + (SELECT g.share FROM pool_member g WHERE"
                + " g.pool_id = k.pool_id AND g.person_id = ?) WHERE k.person_id = ? AND EXISTS (SELECT 1 FROM"
                + " pool_member g WHERE g.pool_id = k.pool_id AND g.person_id = ?)", gone, kept, gone);
        jdbc.update("DELETE FROM pool_member g WHERE g.person_id = ? AND EXISTS (SELECT 1 FROM pool_member k"
                + " WHERE k.pool_id = g.pool_id AND k.person_id = ?)", gone, kept);
        jdbc.update("UPDATE pool_member SET person_id = ? WHERE person_id = ?", kept, gone);

        jdbc.update("DELETE FROM crew_member g WHERE g.sheet_id = ? AND g.person_id = ? AND (EXISTS (SELECT 1 FROM"
                + " crew_member k WHERE k.crew_id = g.crew_id AND k.person_id = ?) OR EXISTS (SELECT 1 FROM crew c"
                + " WHERE c.id = g.crew_id AND c.lead_id = ?))", sheetId, gone, kept, kept);
        jdbc.update("DELETE FROM crew_member m WHERE m.sheet_id = ? AND m.person_id = ? AND EXISTS (SELECT 1 FROM"
                + " crew c WHERE c.id = m.crew_id AND c.lead_id = ?)", sheetId, kept, gone);
        jdbc.update("UPDATE crew_member SET person_id = ? WHERE sheet_id = ? AND person_id = ?", kept, sheetId, gone);
        jdbc.update("UPDATE crew SET lead_id = ? WHERE sheet_id = ? AND lead_id = ?", kept, sheetId, gone);

        jdbc.update("UPDATE person_plan SET person_id = ? WHERE sheet_id = ? AND person_id = ?", kept, sheetId, gone);

        mergeStatements(sheetId, gone, kept);
        jdbc.update("DELETE FROM person_grade WHERE sheet_id = ? AND person_id = ?", sheetId, gone);
        jdbc.update("DELETE FROM person_terms WHERE sheet_id = ? AND person_id = ?", sheetId, gone);
        jdbc.update("DELETE FROM person_hold WHERE sheet_id = ? AND person_id = ?", sheetId, gone);
        jdbc.update("DELETE FROM person_retirement WHERE sheet_id = ? AND person_id = ?", sheetId, gone);
        jdbc.update("DELETE FROM person_watch WHERE sheet_id = ? AND person_id = ?", sheetId, gone);
        jdbc.update("DELETE FROM person_snapshot WHERE sheet_id = ? AND person_id = ?", sheetId, gone);
        jdbc.update("DELETE FROM person WHERE sheet_id = ? AND id = ?", sheetId, gone);
    }

    /**
     * Swap the person with the one above (up) or below (down) them in the sheet's list; their new position, counting
     * from 1, or 0 when there is no one there (the top person moving up, the bottom one down).
     */
    public int move(Person person, boolean up) {
        List<Map<String, Object>> order = jdbc.queryForList(
                "SELECT id, position FROM person WHERE sheet_id = ? ORDER BY position, id", person.sheetId());
        int at = -1;
        for (int i = 0; i < order.size(); i++) {
            if (((Number) order.get(i).get("id")).longValue() == person.id()) {
                at = i;
            }
        }
        int to = up ? at - 1 : at + 1;
        if (at < 0 || to < 0 || to >= order.size()) {
            return 0;
        }
        // the list renumbered 1..n with the two swapped, so no two people ever share a position
        for (int i = 0; i < order.size(); i++) {
            long position = i == at ? to + 1 : i == to ? at + 1 : i + 1;
            jdbc.update("UPDATE person SET position = ? WHERE id = ? AND sheet_id = ?", position,
                    order.get(i).get("id"), person.sheetId());
        }
        return to + 1;
    }

    /** The sheet's today when the person arrived on it by transfer, or null when they were not transferred here. */
    public LocalDate arrivedOn(String sheetId, long personId) {
        List<LocalDate> found = jdbc.query("SELECT arrived_on FROM person WHERE sheet_id = ? AND id = ?",
                (rs, n) -> rs.getObject("arrived_on", LocalDate.class), sheetId, personId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The sheet the person was transferred from, or null when they were not transferred here. */
    public String origin(String sheetId, long personId) {
        List<String> found = jdbc.queryForList("SELECT origin FROM person WHERE sheet_id = ? AND id = ?", String.class,
                sheetId, personId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The sheet each transferred person on the sheet came from, by person id (those not transferred left out). */
    public Map<Long, String> origins(String sheetId) {
        Map<Long, String> found = new HashMap<>();
        jdbc.query("SELECT id, origin FROM person WHERE sheet_id = ? AND origin IS NOT NULL",
                (rs) -> {
                    found.put(rs.getLong("id"), rs.getString("origin"));
                }, sheetId);
        return found;
    }

    /** Flag the person with the reason until the date (inclusive), replacing any flag they had. */
    public void flag(String sheetId, long personId, String reason, LocalDate until) {
        jdbc.update("UPDATE person SET flag_reason = ?, flag_until = ? WHERE id = ? AND sheet_id = ?", reason,
                Date.valueOf(until), personId, sheetId);
    }

    /** The reason of the person's flag while it is in force on the day (on or before its until date), else null. */
    public String flagOn(String sheetId, long personId, LocalDate day) {
        List<String> found = jdbc.query("SELECT flag_reason FROM person WHERE sheet_id = ? AND id = ?"
                + " AND flag_reason IS NOT NULL AND flag_until >= ?", (rs, n) -> rs.getString("flag_reason"),
                sheetId, personId, Date.valueOf(day));
        return found.isEmpty() ? null : found.get(0);
    }

    /** The reason of each of the sheet's people's flags in force on the day, by person id. */
    public Map<Long, String> flagsOn(String sheetId, LocalDate day) {
        Map<Long, String> found = new HashMap<>();
        jdbc.query("SELECT id, flag_reason FROM person WHERE sheet_id = ? AND flag_reason IS NOT NULL AND flag_until >= ?",
                (rs) -> {
                    found.put(rs.getLong("id"), rs.getString("flag_reason"));
                }, sheetId, Date.valueOf(day));
        return found;
    }

    /** Store the person's approver (a user name; null: none). */
    public void saveApprover(String sheetId, long personId, String approver) {
        jdbc.update("UPDATE person SET approver = ? WHERE id = ? AND sheet_id = ?", approver, personId, sheetId);
    }

    /** The person's approver, or null when they have none. */
    public String approver(String sheetId, long personId) {
        List<String> found = jdbc.query("SELECT approver FROM person WHERE sheet_id = ? AND id = ?",
                (rs, n) -> rs.getString("approver"), sheetId, personId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The approver of each of the sheet's people who has one, by person id. */
    public Map<Long, String> approvers(String sheetId) {
        Map<Long, String> found = new HashMap<>();
        jdbc.query("SELECT id, approver FROM person WHERE sheet_id = ? AND approver IS NOT NULL",
                (rs) -> {
                    found.put(rs.getLong("id"), rs.getString("approver"));
                }, sheetId);
        return found;
    }

    /** Store the person's tags (stored form; null: none), adding each to the sheet's tag list. */
    public void saveTags(String sheetId, long personId, String tags) {
        jdbc.update("UPDATE person SET tags = ? WHERE id = ? AND sheet_id = ?", tags, personId, sheetId);
        if (tags != null) {
            for (String tag : tags.split(",")) {
                jdbc.update("MERGE INTO claim_tag (sheet_id, tag) KEY (sheet_id, tag) VALUES (?, ?)", sheetId, tag);
            }
        }
    }

    /** The person's tags (stored form), or null. */
    public String tags(String sheetId, long personId) {
        List<String> found = jdbc.queryForList("SELECT tags FROM person WHERE sheet_id = ? AND id = ?", String.class,
                sheetId, personId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The tags (stored form) of each person on the sheet that has any, by person id. */
    public Map<Long, String> tagsAll(String sheetId) {
        Map<Long, String> found = new HashMap<>();
        jdbc.query("SELECT id, tags FROM person WHERE sheet_id = ? AND tags IS NOT NULL",
                (rs) -> {
                    found.put(rs.getLong("id"), rs.getString("tags"));
                }, sheetId);
        return found;
    }

    /**
     * Give the kept person the gone person's statements, before their own: the kept person's count becomes both
     * counts added, and their last statement stays theirs (or, having none, becomes the gone person's).
     */
    private void mergeStatements(String sheetId, long gone, long kept) {
        int goneCount = jdbc.queryForObject("SELECT statement_count FROM person WHERE sheet_id = ? AND id = ?",
                Integer.class, sheetId, gone);
        if (goneCount > 0) {
            // shifted through negatives so no two statements of the kept person share a seq on the way
            jdbc.update("UPDATE person_statement SET seq = -(seq + ?) WHERE sheet_id = ? AND person_id = ?", goneCount,
                    sheetId, kept);
            jdbc.update("UPDATE person_statement SET seq = -seq WHERE sheet_id = ? AND person_id = ?", sheetId, kept);
            jdbc.update("UPDATE person_statement SET person_id = ? WHERE sheet_id = ? AND person_id = ?", kept, sheetId,
                    gone);
            jdbc.update("UPDATE person k SET stated_total = (SELECT g.stated_total FROM person g WHERE g.id = ?),"
                    + " statement_date = (SELECT g.statement_date FROM person g WHERE g.id = ?)"
                    + " WHERE k.sheet_id = ? AND k.id = ? AND k.statement_count = 0", gone, gone, sheetId, kept);
            jdbc.update("UPDATE person SET statement_count = statement_count + ? WHERE sheet_id = ? AND id = ?",
                    goneCount, sheetId, kept);
        }
        jdbc.update("DELETE FROM person_statement WHERE sheet_id = ? AND person_id = ?", sheetId, gone);
    }

    /** The person's aliases, in the order they were added. */
    public List<String> aliases(String sheetId, long personId) {
        return jdbc.queryForList("SELECT alias FROM person_alias WHERE sheet_id = ? AND person_id = ? ORDER BY id",
                String.class, sheetId, personId);
    }

    /**
     * Copy the person to the other sheet as its newest person, with every grade they were given (and its from-date),
     * their terms, contact, tags (joining its tag list), aliases, past retirements and statements; the new person's id. The person stays on this sheet
     * until {@link #dropTransferred}.
     */
    public long transfer(Person person, String to) {
        return transfer(person, to, person.name());
    }

    /**
     * As {@link #transfer(Person, String)}, the new person taking the name given: one no one on the other sheet has,
     * for a person merged on arrival into someone sharing their name.
     */
    public long transfer(Person person, String to, String name) {
        Map<String, Object> changed = new HashMap<>();
        changed.put("sheet_id", to);
        changed.put("name", name);
        changed.put("origin", person.sheetId());
        List<Date> today = jdbc.query("SELECT today FROM sheet WHERE id = ?", (rs, n) -> rs.getDate("today"), to);
        changed.put("arrived_on", today.isEmpty() || today.get(0) == null ? LocalDate.now() : today.get(0).toLocalDate());
        changed.put("position", jdbc.queryForObject("SELECT " + NEXT_POSITION, Long.class, to));
        long id = RowCopy.copy(jdbc, "person", person.id(), changed);
        String tags = tags(to, id);
        if (tags != null) {
            for (String tag : tags.split(",")) {
                jdbc.update("MERGE INTO claim_tag (sheet_id, tag) KEY (sheet_id, tag) VALUES (?, ?)", to, tag);
            }
        }
        jdbc.update("UPDATE person_grade SET sheet_id = ?, person_id = ? WHERE sheet_id = ? AND person_id = ?", to, id,
                person.sheetId(), person.id());
        jdbc.update("UPDATE person_terms SET sheet_id = ?, person_id = ? WHERE sheet_id = ? AND person_id = ?", to, id,
                person.sheetId(), person.id());
        jdbc.update("UPDATE person_alias SET sheet_id = ?, person_id = ? WHERE sheet_id = ? AND person_id = ?", to, id,
                person.sheetId(), person.id());
        jdbc.update("UPDATE person_retirement SET sheet_id = ?, person_id = ? WHERE sheet_id = ? AND person_id = ?", to,
                id, person.sheetId(), person.id());
        jdbc.update("UPDATE person_statement SET sheet_id = ?, person_id = ? WHERE sheet_id = ? AND person_id = ?", to,
                id, person.sheetId(), person.id());
        jdbc.update("UPDATE person_snapshot SET sheet_id = ?, person_id = ? WHERE sheet_id = ? AND person_id = ?", to,
                id, person.sheetId(), person.id());
        return id;
    }

    /**
     * Remove the person, transferred to another sheet (their claims already moved), from this sheet entirely: their
     * statements, holds, past retirements, patron links either way, pool and crew memberships, watches, plans and closed-period figures go with them; the crews they lead stay, with no lead.
     */
    public void dropTransferred(Person person) {
        String sheetId = person.sheetId();
        long gone = person.id();
        jdbc.update("DELETE FROM person_patron WHERE sheet_id = ? AND (person_id = ? OR patron_id = ?)", sheetId, gone,
                gone);
        jdbc.update("DELETE FROM pool_member WHERE person_id = ?", gone);
        jdbc.update("DELETE FROM crew_member WHERE sheet_id = ? AND person_id = ?", sheetId, gone);
        jdbc.update("UPDATE crew SET lead_id = NULL WHERE sheet_id = ? AND lead_id = ?", sheetId, gone);
        jdbc.update("DELETE FROM person_statement WHERE sheet_id = ? AND person_id = ?", sheetId, gone);
        jdbc.update("DELETE FROM person_snapshot WHERE sheet_id = ? AND person_id = ?", sheetId, gone);
        jdbc.update("DELETE FROM person_grade WHERE sheet_id = ? AND person_id = ?", sheetId, gone);
        jdbc.update("DELETE FROM person_terms WHERE sheet_id = ? AND person_id = ?", sheetId, gone);
        jdbc.update("DELETE FROM person_alias WHERE sheet_id = ? AND person_id = ?", sheetId, gone);
        jdbc.update("DELETE FROM person_hold WHERE sheet_id = ? AND person_id = ?", sheetId, gone);
        jdbc.update("DELETE FROM person_retirement WHERE sheet_id = ? AND person_id = ?", sheetId, gone);
        jdbc.update("DELETE FROM person_watch WHERE sheet_id = ? AND person_id = ?", sheetId, gone);
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND person_id = ?", sheetId, gone);
        jdbc.update("DELETE FROM period_pool_cut WHERE sheet_id = ? AND person_id = ?", sheetId, gone);
        jdbc.update("DELETE FROM person_plan_change WHERE plan_id IN (SELECT id FROM person_plan WHERE sheet_id = ? AND"
                + " person_id = ?)", sheetId, gone);
        jdbc.update("DELETE FROM person_plan_pause WHERE plan_id IN (SELECT id FROM person_plan WHERE sheet_id = ? AND"
                + " person_id = ?)", sheetId, gone);
        jdbc.update("DELETE FROM person_plan_date WHERE plan_id IN (SELECT id FROM person_plan WHERE sheet_id = ? AND"
                + " person_id = ?)", sheetId, gone);
        jdbc.update("DELETE FROM person_plan WHERE sheet_id = ? AND person_id = ?", sheetId, gone);
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

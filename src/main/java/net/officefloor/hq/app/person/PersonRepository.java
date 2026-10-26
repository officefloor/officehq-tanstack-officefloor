package net.officefloor.hq.app.person;

import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The people on each sheet; ids are assigned in the order people are added. */
@Repository
public class PersonRepository {

    /** A person's columns, with the grade in force on the sheet's today, their statement count and their last statement's due-total and date. */
    private static final String COLUMNS = "p.id, p.sheet_id, p.name, p.version, p.retired_from, p.terms, p.contact,"
            + " (SELECT g.grade FROM person_grade g WHERE g.person_id = p.id AND (g.from_date IS NULL"
            + " OR g.from_date <= (SELECT t.today FROM sheet t WHERE t.id = p.sheet_id))"
            + " ORDER BY g.from_date DESC NULLS LAST LIMIT 1) AS grade,"
            + " (SELECT COUNT(*) FROM statement s WHERE s.person_id = p.id) AS statement_count,"
            + " (SELECT s.due_total FROM statement s WHERE s.person_id = p.id ORDER BY s.id DESC LIMIT 1) AS stated_total,"
            + " (SELECT s.taken_on FROM statement s WHERE s.person_id = p.id ORDER BY s.id DESC LIMIT 1) AS statement_date";

    private static final RowMapper<Person> ROW = (rs, n) -> {
        Person person = new Person(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name"),
                rs.getString("grade"), rs.getLong("version"));
        person.setStatements(new Statements(rs.getInt("statement_count"), rs.getBigDecimal("stated_total"),
                rs.getObject("statement_date", LocalDate.class)));
        person.setRetiredFrom(rs.getObject("retired_from", LocalDate.class));
        person.setTerms(rs.getObject("terms", Integer.class));
        person.setContact(rs.getString("contact"));
        return person;
    };

    private static final RowMapper<GradeFrom> GRADE = (rs, n) -> new GradeFrom(rs.getString("grade"),
            rs.getObject("from_date", LocalDate.class));

    static final String CHANGED = "Someone else has saved this person since you opened it; the form now shows their changes";

    static final String NAME_TAKEN = "That name is already in use on this sheet";

    static final String CONTACT_TAKEN = "That contact is already another person's on this sheet";

    private final JdbcTemplate jdbc;

    public PersonRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's people, in the order they were added. */
    public List<Person> list(String sheetId) {
        return withPatrons(sheetId,
                jdbc.query("SELECT " + COLUMNS + " FROM person p WHERE p.sheet_id = ? ORDER BY p.id", ROW, sheetId));
    }

    public Person find(String sheetId, long id) {
        return withPatrons(sheetId,
                jdbc.query("SELECT " + COLUMNS + " FROM person p WHERE p.sheet_id = ? AND p.id = ?", ROW, sheetId, id))
                .stream().findFirst().orElse(null);
    }

    /** The person on the sheet with this name or alias, ignoring case and surrounding spaces; null when none. */
    public Person findByName(String sheetId, String name) {
        return withPatrons(sheetId, jdbc.query(
                "SELECT " + COLUMNS + " FROM person p WHERE p.sheet_id = ? AND (p.name_key = LOWER(TRIM(?))"
                        + " OR p.id IN (SELECT a.person_id FROM person_alias a WHERE a.sheet_id = ? AND a.alias_key = LOWER(TRIM(?))))",
                ROW, sheetId, name, sheetId, name)).stream().findFirst().orElse(null);
    }

    /** Whether another person on the sheet has this name, or anyone on the sheet has it as an alias, ignoring case and
     *  surrounding spaces. */
    public boolean nameTaken(String sheetId, String name, Long exceptId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND name_key = LOWER(TRIM(?)) AND id <> ?",
                Integer.class, sheetId, name, exceptId == null ? -1L : exceptId);
        return (n != null && n > 0) || aliasTaken(sheetId, name);
    }

    /** Whether anyone on the sheet has this alias, ignoring case and surrounding spaces. */
    public boolean aliasTaken(String sheetId, String alias) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person_alias WHERE sheet_id = ? AND alias_key = LOWER(TRIM(?))",
                Integer.class, sheetId, alias);
        return n != null && n > 0;
    }

    /** Give the person the alias (trimmed); false (nothing written) when the sheet has it as an alias already. */
    public boolean addAlias(Person person, String alias) {
        try {
            return jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias) SELECT ?, ?, ? WHERE NOT EXISTS"
                    + " (SELECT 1 FROM person_alias WHERE sheet_id = ? AND alias_key = LOWER(TRIM(?)))",
                    person.getSheetId(), person.getId(), alias, person.getSheetId(), alias) == 1;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /** Whether another person on the sheet has this contact; false for no contact (null). */
    public boolean contactTaken(String sheetId, String contact, Long exceptId) {
        if (contact == null) {
            return false;
        }
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM person WHERE sheet_id = ? AND contact = ? AND id <> ?",
                Integer.class, sheetId, contact, exceptId == null ? -1L : exceptId);
        return n != null && n > 0;
    }

    /** Add the person, their grade in force from the beginning of time. */
    public Person insert(String sheetId, String name, String grade) {
        return insert(sheetId, name, grade, null, null);
    }

    /** Add the person with their own terms and contact (null for none), their grade in force from the beginning of time. */
    public Person insert(String sheetId, String name, String grade, Integer terms, String contact) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, terms, contact) VALUES (?, ?, ?, ?)", new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setString(2, name);
            ps.setObject(3, terms, java.sql.Types.INTEGER);
            ps.setString(4, contact);
            return ps;
        }, keys);
        long id = keys.getKey().longValue();
        jdbc.update("INSERT INTO person_grade (person_id, grade, from_date) VALUES (?, ?, NULL)", id, grade);
        Person person = new Person(id, sheetId, name, grade, 0);
        person.setGrades(List.of(new GradeFrom(grade, null)));
        person.setTerms(terms);
        person.setContact(contact);
        return person;
    }

    /** Give the person the grade from the date, replacing one given from that same date and keeping the rest;
     *  the person's grades are then re-read, and their grade set to the one in force on {@code today}. */
    public void giveGrade(Person person, String grade, LocalDate from, LocalDate today) {
        jdbc.update("DELETE FROM person_grade WHERE person_id = ? AND from_date = ?", person.getId(),
                java.sql.Date.valueOf(from));
        jdbc.update("INSERT INTO person_grade (person_id, grade, from_date) VALUES (?, ?, ?)", person.getId(), grade,
                java.sql.Date.valueOf(from));
        person.setGrades(jdbc.query("SELECT grade, from_date FROM person_grade WHERE person_id = ?", GRADE,
                person.getId()));
        person.setGrade(person.gradeOn(today));
    }

    /** Saves the person if it is still at the version it was loaded at, moving it to the next
     *  version; false (nothing written) when someone else saved it in the meantime. */
    public boolean update(Person person) {
        int n = jdbc.update("UPDATE person SET name = ?, terms = ?, contact = ?, version = version + 1"
                + " WHERE id = ? AND version = ?", person.getName(), person.getTerms(), person.getContact(), person.getId(), person.getVersion());
        if (n == 0) {
            return false;
        }
        person.setVersion(person.getVersion() + 1);
        return true;
    }

    /** Retire the person from the date; false (nothing written) when they were retired already. */
    public boolean retire(long id, LocalDate from) {
        return jdbc.update("UPDATE person SET retired_from = ? WHERE id = ? AND retired_from IS NULL",
                java.sql.Date.valueOf(from), id) == 1;
    }

    /** Add a hold to the person. */
    public void addHold(long personId, HoldSpan hold) {
        jdbc.update("INSERT INTO person_hold (person_id, from_date, to_date) VALUES (?, ?, ?)", personId,
                hold.from() == null ? null : java.sql.Date.valueOf(hold.from()),
                hold.to() == null ? null : java.sql.Date.valueOf(hold.to()));
    }

    /** Name the patron as the person's patron; false (nothing written) when they are one already. */
    public boolean addPatron(long personId, long patronId) {
        try {
            return jdbc.update("INSERT INTO person_patron (person_id, patron_id) SELECT ?, ? WHERE NOT EXISTS"
                    + " (SELECT 1 FROM person_patron WHERE person_id = ? AND patron_id = ?)",
                    personId, patronId, personId, patronId) == 1;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /**
     * Merge the person into the other, both still at the versions they were loaded at: the merged person's claims move
     * to the other (numbers and dates kept, listed after the other's claims in the order they were added, each
     * unbound from any statement figure); their name and aliases become the other's aliases; their patrons are added
     * to the other's and anyone naming them as patron names the other instead (never the other themselves, no
     * repeats); their pool memberships move to the other (shares added where the other is a member already) and their
     * closed periods' totals add to the other's. Their statements, holds, watchers, grades and contact are dropped
     * with them. False (nothing written) when either was saved by someone else in the meantime.
     */
    public boolean merge(Person from, Person into) {
        String sheetId = from.getSheetId();
        long f = from.getId();
        long t = into.getId();
        if (jdbc.update("UPDATE person SET version = version + 1 WHERE id = ? AND version = ?", t, into.getVersion()) == 0
                || jdbc.update("UPDATE person SET version = version + 1 WHERE id = ? AND version = ?", f,
                        from.getVersion()) == 0) {
            return false;
        }
        for (Long claimId : jdbc.queryForList("SELECT id FROM claim WHERE sheet_id = ? AND person_id = ? ORDER BY id",
                Long.class, sheetId, f)) {
            Long listed = jdbc.queryForObject("SELECT last_claim_listed FROM FINAL TABLE "
                    + "(UPDATE sheet SET last_claim_listed = last_claim_listed + 1 WHERE id = ?)", Long.class, sheetId);
            jdbc.update("UPDATE claim SET person_id = ?, stated = FALSE, stated_due = NULL, listed = ? WHERE id = ?", t,
                    listed, claimId);
        }
        jdbc.update("UPDATE person_alias SET person_id = ? WHERE sheet_id = ? AND person_id = ?", t, sheetId, f);
        jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias) VALUES (?, ?, ?)", sheetId, t,
                from.getName());
        jdbc.update("INSERT INTO person_patron (person_id, patron_id) SELECT ?, pp.patron_id FROM person_patron pp"
                + " WHERE pp.person_id = ? AND pp.patron_id <> ? AND NOT EXISTS"
                + " (SELECT 1 FROM person_patron o WHERE o.person_id = ? AND o.patron_id = pp.patron_id)"
                + " ORDER BY pp.named", t, f, t, t);
        jdbc.update("INSERT INTO person_patron (person_id, patron_id, named) SELECT pp.person_id, ?, pp.named FROM person_patron pp"
                + " WHERE pp.patron_id = ? AND pp.person_id <> ? AND NOT EXISTS"
                + " (SELECT 1 FROM person_patron o WHERE o.person_id = pp.person_id AND o.patron_id = ?)", t, f, t, t);
        jdbc.update("DELETE FROM person_patron WHERE person_id = ? OR patron_id = ?", f, f);
        jdbc.update("UPDATE pool_member m SET share = share + (SELECT o.share FROM pool_member o WHERE o.pool_id = m.pool_id"
                + " AND o.person_id = ?) WHERE m.person_id = ? AND EXISTS (SELECT 1 FROM pool_member o"
                + " WHERE o.pool_id = m.pool_id AND o.person_id = ?)", f, t, f);
        jdbc.update("DELETE FROM pool_member m WHERE m.person_id = ? AND EXISTS (SELECT 1 FROM pool_member o"
                + " WHERE o.pool_id = m.pool_id AND o.person_id = ?)", f, t);
        jdbc.update("UPDATE pool_member SET person_id = ? WHERE person_id = ?", t, f);
        jdbc.update("UPDATE period_person_total m SET total = total + (SELECT o.total FROM period_person_total o"
                + " WHERE o.period_id = m.period_id AND o.person_id = ?) WHERE m.person_id = ? AND EXISTS"
                + " (SELECT 1 FROM period_person_total o WHERE o.period_id = m.period_id AND o.person_id = ?)", f, t, f);
        jdbc.update("DELETE FROM period_person_total m WHERE m.person_id = ? AND EXISTS (SELECT 1 FROM period_person_total o"
                + " WHERE o.period_id = m.period_id AND o.person_id = ?)", f, t);
        jdbc.update("UPDATE period_person_total SET person_id = ? WHERE person_id = ?", t, f);
        jdbc.update("DELETE FROM statement WHERE person_id = ?", f);
        jdbc.update("DELETE FROM person_hold WHERE person_id = ?", f);
        jdbc.update("DELETE FROM person_watch WHERE person_id = ?", f);
        jdbc.update("DELETE FROM person_grade WHERE person_id = ?", f);
        jdbc.update("DELETE FROM person WHERE id = ?", f);
        return true;
    }

    /** Sets each person's patrons' names, in the order named, their grades and their holds. */
    private List<Person> withPatrons(String sheetId, List<Person> people) {
        if (people.isEmpty()) {
            return people;
        }
        Map<Long, List<GradeFrom>> grades = jdbc.query(
                "SELECT g.person_id, g.grade, g.from_date FROM person_grade g JOIN person q ON q.id = g.person_id"
                        + " WHERE q.sheet_id = ?",
                (rs, n) -> Map.entry(rs.getLong("person_id"), GRADE.mapRow(rs, n)), sheetId).stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        for (Person person : people) {
            person.setGrades(grades.getOrDefault(person.getId(), List.of()));
        }
        Map<Long, List<HoldSpan>> holds = jdbc.query(
                "SELECT h.person_id, h.from_date, h.to_date FROM person_hold h JOIN person q ON q.id = h.person_id"
                        + " WHERE q.sheet_id = ? ORDER BY h.id",
                (rs, n) -> Map.entry(rs.getLong("person_id"), new HoldSpan(rs.getObject("from_date", LocalDate.class),
                        rs.getObject("to_date", LocalDate.class))), sheetId).stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        for (Person person : people) {
            person.setHolds(holds.getOrDefault(person.getId(), List.of()));
        }
        Map<Long, List<String>> patrons = jdbc.query(
                "SELECT pp.person_id, q.name FROM person_patron pp JOIN person q ON q.id = pp.patron_id"
                        + " WHERE q.sheet_id = ? ORDER BY pp.named",
                (rs, n) -> Map.entry(rs.getLong("person_id"), rs.getString("name")), sheetId).stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        for (Person person : people) {
            person.setPatrons(patrons.getOrDefault(person.getId(), List.of()));
        }
        Map<Long, List<String>> aliases = jdbc.query(
                "SELECT a.person_id, a.alias FROM person_alias a WHERE a.sheet_id = ?",
                (rs, n) -> Map.entry(rs.getLong("person_id"), rs.getString("alias")), sheetId).stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        for (Person person : people) {
            person.setAliases(PersonName.sorted(aliases.getOrDefault(person.getId(), List.of())));
        }
        return people;
    }
}

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

/** The people on each sheet; ids are assigned in the order people are added, and each has a place in the sheet's
 *  list (new people at the end, moved by swapping with a neighbour). */
@Repository
public class PersonRepository {

    /** A person's columns, with the grade in force on the sheet's today, their statement count and their last statement's due-total and date. */
    private static final String COLUMNS = "p.id, p.sheet_id, p.name, p.version, p.retired_from, p.terms, p.contact, p.tags, p.origin,"
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
        person.setTags(rs.getString("tags"));
        person.setOrigin(rs.getString("origin"));
        return person;
    };

    private static final RowMapper<GradeFrom> GRADE = (rs, n) -> new GradeFrom(rs.getString("grade"),
            rs.getObject("from_date", LocalDate.class));

    static final String CHANGED = "Someone else has saved this person since you opened it; the form now shows their changes";

    public static final String NAME_TAKEN = "That name is already in use on this sheet or another sheet of its tree";

    static final String CONTACT_TAKEN = "That contact is already another person's on this sheet";

    private final JdbcTemplate jdbc;

    public PersonRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's people, in list order. */
    public List<Person> list(String sheetId) {
        return withPatrons(sheetId, jdbc.query(
                "SELECT " + COLUMNS + " FROM person p WHERE p.sheet_id = ? ORDER BY p.list_position, p.id", ROW, sheetId));
    }

    /**
     * Swap the person with the one above (up) or below them in the sheet's list; answers the person's new place in
     * the list, counting from 1, or null (nothing written) when there is no one there to swap with.
     */
    public Integer move(Person person, boolean up) {
        String sheetId = person.getSheetId();
        List<Long> order = jdbc.queryForList(
                "SELECT id FROM person WHERE sheet_id = ? ORDER BY list_position, id", Long.class, sheetId);
        int at = order.indexOf(person.getId());
        int to = up ? at - 1 : at + 1;
        if (at < 0 || to < 0 || to >= order.size()) {
            return null;
        }
        java.util.Collections.swap(order, at, to);
        // Renumber the whole list, so the swap holds even where positions were equal.
        for (int i = 0; i < order.size(); i++) {
            jdbc.update("UPDATE person SET list_position = ? WHERE id = ?", i + 1, order.get(i));
        }
        return to + 1;
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

    /** Whether another person on the sheet has this name, or anyone on the sheet has it as an alias, or anyone on
     *  another sheet of its tree (see {@link #treeOf}) but that person has it as a name or alias, ignoring case and
     *  surrounding spaces. */
    public boolean nameTaken(String sheetId, String name, Long exceptId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND name_key = LOWER(TRIM(?)) AND id <> ?",
                Integer.class, sheetId, name, exceptId == null ? -1L : exceptId);
        return (n != null && n > 0) || aliasTaken(sheetId, name) || usedInTree(sheetId, name, exceptId);
    }

    /** Whether anyone (but that person) on another sheet of the sheet's tree has this name or alias, ignoring case and
     *  surrounding spaces. */
    public boolean usedInTree(String sheetId, String name, Long exceptId) {
        java.util.Set<String> key = java.util.Set.of(name.trim().toLowerCase());
        return !java.util.Collections.disjoint(namesInTree(sheetId, exceptId), key);
    }

    /** Whether the person's name or any of their aliases is used, as a name or alias, on another sheet of their tree. */
    public boolean clashing(Person person) {
        return clashing(person, namesInTree(person.getSheetId(), person.getId()));
    }

    static boolean clashing(Person person, java.util.Set<String> used) {
        if (used.contains(person.getName().trim().toLowerCase())) {
            return true;
        }
        return person.getAliases().stream().anyMatch(a -> used.contains(a.trim().toLowerCase()));
    }

    /** The names and aliases (lower case, trimmed) of everyone, but that person, on the other sheets of the sheet's tree. */
    public java.util.Set<String> namesInTree(String sheetId, Long exceptId) {
        java.util.Set<String> found = new java.util.HashSet<>();
        long except = exceptId == null ? -1L : exceptId;
        for (String other : treeOf(sheetId)) {
            found.addAll(jdbc.queryForList("SELECT name_key FROM person WHERE sheet_id = ? AND id <> ?", String.class,
                    other, except));
            found.addAll(jdbc.queryForList("SELECT alias_key FROM person_alias WHERE sheet_id = ? AND person_id <> ?",
                    String.class, other, except));
        }
        return found;
    }

    /** The other sheets of the sheet's tree, among the sheets that exist: its parent, theirs and so on up, and its
     *  children, theirs and so on down (each once, the sheet itself never among them). */
    public List<String> treeOf(String sheetId) {
        java.util.Set<String> seen = new java.util.LinkedHashSet<>(List.of(sheetId));
        for (String at = parentOf(sheetId); at != null && seen.add(at); at = parentOf(at)) {
        }
        java.util.Deque<String> todo = new java.util.ArrayDeque<>(List.of(sheetId));
        while (!todo.isEmpty()) {
            for (String child : jdbc.queryForList("SELECT sheet_id FROM sheet_setting WHERE setting_key = 'parent'"
                    + " AND setting_value = ? ORDER BY sheet_id", String.class, todo.poll())) {
                if (seen.add(child)) {
                    todo.add(child);
                }
            }
        }
        seen.remove(sheetId);
        return seen.stream().filter(s -> jdbc.queryForObject("SELECT COUNT(*) FROM sheet WHERE id = ?", Integer.class, s) > 0)
                .toList();
    }

    private String parentOf(String sheetId) {
        return jdbc.query("SELECT setting_value FROM sheet_setting WHERE sheet_id = ? AND setting_key = 'parent'",
                (rs, n) -> rs.getString(1), sheetId).stream().filter(v -> v != null && !v.isBlank()).findFirst()
                .orElse(null);
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
        return insert(sheetId, name, grade, terms, contact, null);
    }

    /** Add the person with their own terms, contact and tags (null for none), their grade in force from the beginning
     *  of time; their tags are noted as used on the sheet. */
    public Person insert(String sheetId, String name, String grade, Integer terms, String contact, String tags) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, terms, contact, tags, list_position) VALUES (?, ?, ?, ?, ?,"
                            + " (SELECT COALESCE(MAX(q.list_position), 0) + 1 FROM person q WHERE q.sheet_id = ?))",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setString(2, name);
            ps.setObject(3, terms, java.sql.Types.INTEGER);
            ps.setString(4, contact);
            ps.setString(5, tags);
            ps.setString(6, sheetId);
            return ps;
        }, keys);
        long id = keys.getKey().longValue();
        jdbc.update("INSERT INTO person_grade (person_id, grade, from_date) VALUES (?, ?, NULL)", id, grade);
        Person person = new Person(id, sheetId, name, grade, 0);
        person.setGrades(List.of(new GradeFrom(grade, null)));
        person.setTerms(terms);
        person.setContact(contact);
        person.setTags(tags);
        tagsUsed(sheetId, tags);
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

    /** Note the tags (joined by commas; null: none) as used on the sheet, for good, so the tag totals list them. */
    private void tagsUsed(String sheetId, String tags) {
        if (tags == null) {
            return;
        }
        for (String tag : tags.split(",")) {
            jdbc.update("MERGE INTO claim_tag (sheet_id, tag) KEY (sheet_id, tag) VALUES (?, ?)", sheetId, tag);
        }
    }

    /** Saves the person if it is still at the version it was loaded at, moving it to the next
     *  version; false (nothing written) when someone else saved it in the meantime. */
    public boolean update(Person person) {
        int n = jdbc.update("UPDATE person SET name = ?, terms = ?, contact = ?, tags = ?, version = version + 1"
                + " WHERE id = ? AND version = ?", person.getName(), person.getTerms(), person.getContact(), person.getTags(),
                person.getId(), person.getVersion());
        if (n == 0) {
            return false;
        }
        tagsUsed(person.getSheetId(), person.getTags());
        person.setVersion(person.getVersion() + 1);
        return true;
    }

    /** Retire the person from the date; false (nothing written) when they were retired already. */
    public boolean retire(long id, LocalDate from) {
        return jdbc.update("UPDATE person SET retired_from = ? WHERE id = ? AND retired_from IS NULL",
                java.sql.Date.valueOf(from), id) == 1;
    }

    /** Unretire the person, still retired from the date, on the day: the retirement is kept as a past one; false
     *  (nothing written) when they are no longer retired from that date. */
    public boolean unretire(long id, LocalDate from, LocalDate on) {
        if (jdbc.update("UPDATE person SET retired_from = NULL WHERE id = ? AND retired_from = ?", id,
                java.sql.Date.valueOf(from)) != 1) {
            return false;
        }
        jdbc.update("INSERT INTO person_retirement (person_id, from_date, to_date) VALUES (?, ?, ?)", id,
                java.sql.Date.valueOf(from), java.sql.Date.valueOf(on));
        return true;
    }

    /** Retire the person from the date, whether or not they were retired already (moving the date when they were). */
    public void retireFrom(long id, LocalDate from) {
        jdbc.update("UPDATE person SET retired_from = ? WHERE id = ?", java.sql.Date.valueOf(from), id);
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
     * keeping any figure a statement bound it to); their statements become the other's, taken before the other's own
     * (see {@link #carryStatements}); their name and aliases become the other's aliases; their patrons are added
     * to the other's and anyone naming them as patron names the other instead (never the other themselves, no
     * repeats); their plans and pool memberships move to the other (shares added where the other is a member already) and their
     * closed periods' totals add to the other's. Their holds, watchers, grades and contact are dropped
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
            jdbc.update("UPDATE claim SET person_id = ?, listed = ? WHERE id = ?", t,
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
        jdbc.update("UPDATE plan SET person_id = ? WHERE person_id = ?", t, f);
        jdbc.update("UPDATE period_person_total m SET total = total + (SELECT o.total FROM period_person_total o"
                + " WHERE o.period_id = m.period_id AND o.person_id = ?) WHERE m.person_id = ? AND EXISTS"
                + " (SELECT 1 FROM period_person_total o WHERE o.period_id = m.period_id AND o.person_id = ?)", f, t, f);
        jdbc.update("DELETE FROM period_person_total m WHERE m.person_id = ? AND EXISTS (SELECT 1 FROM period_person_total o"
                + " WHERE o.period_id = m.period_id AND o.person_id = ?)", f, t);
        jdbc.update("UPDATE period_person_total SET person_id = ? WHERE person_id = ?", t, f);
        carryStatements(f, sheetId, t);
        jdbc.update("DELETE FROM person_hold WHERE person_id = ?", f);
        jdbc.update("DELETE FROM person_retirement WHERE person_id = ?", f);
        jdbc.update("DELETE FROM person_watch WHERE person_id = ?", f);
        jdbc.update("DELETE FROM person_grade WHERE person_id = ?", f);
        jdbc.update("DELETE FROM person WHERE id = ?", f);
        return true;
    }

    /** Move the person to the next version; false (nothing written) when someone else saved them in the meantime. */
    public boolean touch(Person person) {
        if (jdbc.update("UPDATE person SET version = version + 1 WHERE id = ? AND version = ?", person.getId(),
                person.getVersion()) == 0) {
            return false;
        }
        person.setVersion(person.getVersion() + 1);
        return true;
    }

    /**
     * Transfer the person to the other sheet (stored already): they are added there with their name, terms, contact,
     * every grade they were given (with its from-date) and their aliases, noting this sheet as the one they came from; their claims move with them (status, dates,
     * notes, settled figures, logs and comments kept), listed there in the order they were listed here, each keeping
     * any figure a statement bound it to, without its reference or settle batch, and numbered by {@code numbers} (by claim id;
     * a claim absent keeps no number), a correction or split naming its claim by the new number; their tags are noted
     * as used there; their statements move with them. Everything else of theirs here — patron links either way, pool
     * memberships, plans, watches, holds and closed periods' totals — is dropped with them. Answers the person's id on the other sheet.
     */
    public long transfer(Person from, String toSheet, Map<Long, String> numbers) {
        long f = from.getId();
        String grade = from.getGrade() != null ? from.getGrade() : from.getGrades().get(0).grade();
        long t = insert(toSheet, from.getName(), grade, from.getTerms(), from.getContact(), from.getTags()).getId();
        jdbc.update("UPDATE person SET origin = ? WHERE id = ?", from.getSheetId(), t);
        jdbc.update("DELETE FROM person_grade WHERE person_id = ?", t);
        jdbc.update("INSERT INTO person_grade (person_id, grade, from_date) SELECT ?, grade, from_date FROM person_grade"
                + " WHERE person_id = ?", t, f);
        jdbc.update("UPDATE person_alias SET sheet_id = ?, person_id = ? WHERE person_id = ?", toSheet, t, f);
        moveClaims(from, toSheet, t, numbers);
        carryStatements(f, toSheet, t);
        dropRest(f);
        return t;
    }

    /**
     * Transfer the person to the other sheet merging them into a person there ({@code into}, still at the version they
     * were loaded at), as {@link #merge} does: their claims move to the other as {@link #transfer} moves them, listed
     * after the other's claims, and their statements become the other's as {@link #merge} carries them; their name and
     * aliases become the other's aliases; everything else of theirs is dropped with them (grades, terms and contact
     * too). False (nothing written) when the other was saved by someone else in the meantime.
     */
    public boolean transferInto(Person from, Person into, Map<Long, String> numbers) {
        long f = from.getId();
        long t = into.getId();
        String toSheet = into.getSheetId();
        if (jdbc.update("UPDATE person SET version = version + 1 WHERE id = ? AND version = ?", t,
                into.getVersion()) == 0) {
            return false;
        }
        jdbc.update("UPDATE person_alias SET sheet_id = ?, person_id = ? WHERE person_id = ?", toSheet, t, f);
        jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias) VALUES (?, ?, ?)", toSheet, t,
                from.getName());
        moveClaims(from, toSheet, t, numbers);
        carryStatements(f, toSheet, t);
        dropRest(f);
        return true;
    }

    /** Move the person's claims to the other sheet's person {@code t}, as {@link #transfer} describes. */
    private void moveClaims(Person from, String toSheet, long t, Map<Long, String> numbers) {
        long f = from.getId();
        List<Map<String, Object>> claims = jdbc.queryForList(
                "SELECT id, claim_number, corrects, split_from, tags FROM claim WHERE person_id = ? ORDER BY listed", f);
        Map<String, String> renumbered = new java.util.HashMap<>();
        for (Map<String, Object> claim : claims) {
            if (claim.get("claim_number") != null) {
                renumbered.put((String) claim.get("claim_number"), numbers.get(((Number) claim.get("id")).longValue()));
            }
        }
        for (Map<String, Object> claim : claims) {
            long id = ((Number) claim.get("id")).longValue();
            Long listed = jdbc.queryForObject("SELECT last_claim_listed FROM FINAL TABLE "
                    + "(UPDATE sheet SET last_claim_listed = last_claim_listed + 1 WHERE id = ?)", Long.class, toSheet);
            jdbc.update("UPDATE claim SET sheet_id = ?, person_id = ?, listed = ?, claim_number = ?, corrects = ?,"
                    + " split_from = ?, ref = NULL, settle_batch = NULL WHERE id = ?",
                    toSheet, t, listed, numbers.get(id), renumbered.get((String) claim.get("corrects")),
                    renumbered.get((String) claim.get("split_from")), id);
            jdbc.update("UPDATE claim_log SET sheet_id = ? WHERE claim_id = ?", toSheet, id);
            jdbc.update("UPDATE claim_comment SET sheet_id = ? WHERE claim_id = ?", toSheet, id);
            jdbc.update("DELETE FROM claim_lock WHERE claim_id = ?", id);
            String tags = (String) claim.get("tags");
            if (tags != null) {
                for (String tag : tags.split(",")) {
                    jdbc.update("MERGE INTO claim_tag (sheet_id, tag) KEY (sheet_id, tag) VALUES (?, ?)", toSheet, tag);
                }
            }
        }
    }

    /**
     * Carry person {@code f}'s statements to person {@code t} on {@code toSheet}: {@code t}'s count becomes their own
     * plus {@code f}'s, and {@code t}'s last statement stays theirs (or, having none, becomes {@code f}'s). The last
     * statement is the one with the highest id, so {@code t}'s own are taken again after {@code f}'s, in their order.
     */
    private void carryStatements(long f, String toSheet, long t) {
        List<Long> own = jdbc.queryForList("SELECT id FROM statement WHERE person_id = ? ORDER BY id", Long.class, t);
        jdbc.update("UPDATE statement SET sheet_id = ?, person_id = ? WHERE person_id = ?", toSheet, t, f);
        for (Long id : own) {
            jdbc.update("INSERT INTO statement (sheet_id, person_id, due_total, taken_on)"
                    + " SELECT sheet_id, person_id, due_total, taken_on FROM statement WHERE id = ?", id);
            jdbc.update("DELETE FROM statement WHERE id = ?", id);
        }
    }

    /** Drop everything else of the person's here, and the person. */
    private void dropRest(long f) {
        jdbc.update("DELETE FROM person_patron WHERE person_id = ? OR patron_id = ?", f, f);
        jdbc.update("DELETE FROM pool_member WHERE person_id = ?", f);
        jdbc.update("DELETE FROM plan_claim WHERE plan_id IN (SELECT id FROM plan WHERE person_id = ?)", f);
        jdbc.update("DELETE FROM plan WHERE person_id = ?", f);
        jdbc.update("DELETE FROM person_watch WHERE person_id = ?", f);
        jdbc.update("DELETE FROM person_hold WHERE person_id = ?", f);
        jdbc.update("DELETE FROM person_retirement WHERE person_id = ?", f);
        jdbc.update("DELETE FROM statement WHERE person_id = ?", f);
        jdbc.update("DELETE FROM period_person_total WHERE person_id = ?", f);
        jdbc.update("DELETE FROM person_grade WHERE person_id = ?", f);
        jdbc.update("DELETE FROM person WHERE id = ?", f);
    }

    /** Sets each person's patrons' names, in the order named, their grades, their holds and their past retirements. */
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
        Map<Long, List<RetirementSpan>> retirements = jdbc.query(
                "SELECT r.person_id, r.from_date, r.to_date FROM person_retirement r JOIN person q ON q.id = r.person_id"
                        + " WHERE q.sheet_id = ? ORDER BY r.id",
                (rs, n) -> Map.entry(rs.getLong("person_id"), new RetirementSpan(
                        rs.getObject("from_date", LocalDate.class), rs.getObject("to_date", LocalDate.class))), sheetId)
                .stream().collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        for (Person person : people) {
            person.setRetirements(retirements.getOrDefault(person.getId(), List.of()));
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

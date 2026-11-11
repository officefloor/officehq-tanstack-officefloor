package net.officefloor.hq.app.person;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The people store, kept per sheet. */
@Repository
public class PersonRepository {

    private static final String COLUMNS = "id, sheet_id, name, grade, terms, contact, tags, version, statement_count, stated_total, statement_date, retired_from, added_on, origin_sheet, flag_reason, flag_until, approver";

    private static final RowMapper<Person> ROW = (rs, n) -> {
        Person person = new Person(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name"),
                rs.getString("grade"), rs.getLong("version"));
        person.setTerms(rs.getObject("terms", Integer.class));
        person.setContact(rs.getString("contact"));
        person.setTags(rs.getString("tags"));
        person.setStatements(rs.getInt("statement_count"), rs.getString("stated_total"),
                rs.getObject("statement_date", LocalDate.class));
        person.setRetiredFrom(rs.getObject("retired_from", LocalDate.class));
        person.setAddedOn(rs.getObject("added_on", LocalDate.class));
        person.setOriginSheet(rs.getString("origin_sheet"));
        person.setFlag(rs.getString("flag_reason"), rs.getObject("flag_until", LocalDate.class));
        person.setApprover(rs.getString("approver"));
        return person;
    };

    /** A position after everyone's on the sheet given by the one parameter: the end of its list. */
    private static final String END_OF_LIST =
            "(SELECT COALESCE(MAX(e.list_pos), 0) + 1 FROM person e WHERE e.sheet_id = ?)";

    private final JdbcTemplate jdbc;

    public PersonRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's people, in list order (by their position). */
    public List<Person> findAll(String sheetId) {
        return findAll(sheetId, null);
    }

    /**
     * The sheet's people, in list order (by their position), as the list stood at the end of {@code day}
     * (null: now): those merged into another later than it are still on it.
     */
    public List<Person> findAll(String sheetId, LocalDate day) {
        List<Person> people = day == null
                ? jdbc.query("SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? AND merged_into IS NULL ORDER BY list_pos, id",
                        ROW, sheetId)
                : jdbc.query("SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? AND (merged_into IS NULL"
                        + " OR merged_on > ?) ORDER BY list_pos, id", ROW, sheetId, Date.valueOf(day));
        Map<Long, Person> byId = new HashMap<>();
        people.forEach((p) -> byId.put(p.getId(), p));
        jdbc.query("SELECT person_id, from_date, grade FROM person_grade WHERE sheet_id = ?",
                (rs) -> {
                    Person person = byId.get(rs.getLong("person_id"));
                    if (person != null) person.setGradeFrom(rs.getString("grade"), rs.getObject("from_date", LocalDate.class));
                }, sheetId);
        jdbc.query("SELECT person_id, from_date, terms FROM person_terms WHERE sheet_id = ?",
                (rs) -> {
                    Person person = byId.get(rs.getLong("person_id"));
                    if (person != null) person.setTermsFrom(rs.getObject("terms", Integer.class), rs.getObject("from_date", LocalDate.class));
                }, sheetId);
        jdbc.query("SELECT person_id, patron_id FROM person_patron WHERE sheet_id = ? ORDER BY person_id, named_seq NULLS FIRST, patron_id",
                (rs) -> {
                    Person person = byId.get(rs.getLong("person_id"));
                    if (person != null) person.getPatronIds().add(rs.getLong("patron_id"));
                }, sheetId);
        jdbc.query("SELECT person_id, from_date, to_date FROM person_hold WHERE sheet_id = ? ORDER BY id",
                (rs) -> {
                    Person person = byId.get(rs.getLong("person_id"));
                    if (person != null) person.getHolds().add(hold(rs));
                }, sheetId);
        jdbc.query("SELECT person_id, alias FROM person_alias WHERE sheet_id = ? ORDER BY id",
                (rs) -> {
                    Person person = byId.get(rs.getLong("person_id"));
                    if (person != null) person.getAliases().add(rs.getString("alias"));
                }, sheetId);
        jdbc.query("SELECT person_id, from_date, to_date FROM person_past_retirement WHERE sheet_id = ? ORDER BY id",
                (rs) -> {
                    Person person = byId.get(rs.getLong("person_id"));
                    if (person != null) person.getPastRetirements().add(pastRetirement(rs));
                }, sheetId);
        jdbc.query("SELECT person_id, snapshot_day, due_total FROM person_snapshot WHERE sheet_id = ? ORDER BY id",
                (rs) -> {
                    Person person = byId.get(rs.getLong("person_id"));
                    if (person != null) person.getSnapshots().add(rs.getObject("snapshot_day", LocalDate.class) + " " + rs.getString("due_total"));
                }, sheetId);
        jdbc.query("SELECT person_id, stated_total, statement_date FROM person_statement WHERE sheet_id = ? ORDER BY " + STATEMENT_ORDER,
                (rs) -> {
                    Person person = byId.get(rs.getLong("person_id"));
                    if (person != null) person.getStatementLines().add(statementLine(rs));
                }, sheetId);
        return people;
    }

    /** Record a SNAPSHOT written for the person: the day being left and their due-total on it. */
    public void addSnapshot(String sheetId, long personId, LocalDate day, String dueTotal) {
        jdbc.update("INSERT INTO person_snapshot (sheet_id, person_id, snapshot_day, due_total) VALUES (?, ?, ?, ?)",
                sheetId, personId, Date.valueOf(day), dueTotal);
    }

    /** The person with this id on the sheet, or null when there is none. */
    public Person find(String sheetId, long id) {
        List<Person> found = jdbc.query(
                "SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? AND id = ? AND merged_into IS NULL",
                ROW, sheetId, id);
        if (found.isEmpty()) return null;
        Person person = found.get(0);
        loadGrades(person);
        loadHolds(person);
        person.getPatronIds().addAll(jdbc.queryForList(
                "SELECT patron_id FROM person_patron WHERE sheet_id = ? AND person_id = ? ORDER BY named_seq NULLS FIRST, patron_id",
                Long.class, sheetId, id));
        loadAliases(person);
        loadPastRetirements(person);
        person.getSnapshots().addAll(jdbc.query(
                "SELECT snapshot_day, due_total FROM person_snapshot WHERE sheet_id = ? AND person_id = ? ORDER BY id",
                (rs, n) -> rs.getObject("snapshot_day", LocalDate.class) + " " + rs.getString("due_total"), sheetId, id));
        person.getStatementLines().addAll(jdbc.query(
                "SELECT stated_total, statement_date FROM person_statement WHERE sheet_id = ? AND person_id = ? ORDER BY " + STATEMENT_ORDER,
                (rs, n) -> statementLine(rs), sheetId, id));
        return person;
    }

    /** Statements oldest first: by date (one not kept first), then in the order taken. */
    private static final String STATEMENT_ORDER = "statement_date NULLS FIRST, seq";

    private static String statementLine(java.sql.ResultSet rs) throws java.sql.SQLException {
        return rs.getObject("statement_date", LocalDate.class) + " " + rs.getString("stated_total");
    }

    private void loadPastRetirements(Person person) {
        person.getPastRetirements().addAll(jdbc.query(
                "SELECT from_date, to_date FROM person_past_retirement WHERE sheet_id = ? AND person_id = ? ORDER BY id",
                (rs, n) -> pastRetirement(rs), person.getSheetId(), person.getId()));
    }

    private static PastRetirement pastRetirement(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new PastRetirement(rs.getObject("from_date", LocalDate.class), rs.getObject("to_date", LocalDate.class));
    }

    /**
     * Swap the person with the one next to them in the sheet's list: above ({@code up}) or below.
     * Returns their new position, counting from 1; null when there is no one there (the top row
     * cannot move up nor the bottom row down) and nothing is written.
     */
    public Integer move(Person person, boolean up) {
        List<long[]> list = jdbc.query("SELECT id, list_pos FROM person WHERE sheet_id = ? AND merged_into IS NULL"
                + " ORDER BY list_pos, id", (rs, n) -> new long[] { rs.getLong("id"), rs.getLong("list_pos") },
                person.getSheetId());
        int at = -1;
        for (int i = 0; i < list.size(); i++) if (list.get(i)[0] == person.getId()) at = i;
        int to = up ? at - 1 : at + 1;
        if (at < 0 || to < 0 || to >= list.size()) return null;
        long[] mine = list.get(at), theirs = list.get(to);
        // Positions may tie (never in practice): give them distinct ones in the order listed.
        long minePos = mine[1], theirsPos = theirs[1];
        if (minePos == theirsPos) {
            for (int i = 0; i < list.size(); i++) {
                jdbc.update("UPDATE person SET list_pos = ? WHERE id = ?", i + 1L, list.get(i)[0]);
            }
            minePos = at + 1L;
            theirsPos = to + 1L;
        }
        jdbc.update("UPDATE person SET list_pos = ? WHERE id = ?", theirsPos, mine[0]);
        jdbc.update("UPDATE person SET list_pos = ? WHERE id = ?", minePos, theirs[0]);
        return to + 1;
    }

    /** Record {@code alias} as one more of the person's aliases. */
    public void addAlias(Person person, String alias) {
        jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias, added_on)"
                + " VALUES (?, ?, ?, (SELECT today FROM sheet WHERE id = ?))",
                person.getSheetId(), person.getId(), alias, person.getSheetId());
        person.getAliases().add(alias);
    }

    private void loadAliases(Person person) {
        person.getAliases().addAll(jdbc.queryForList(
                "SELECT alias FROM person_alias WHERE sheet_id = ? AND person_id = ? ORDER BY id",
                String.class, person.getSheetId(), person.getId()));
    }

    /**
     * Merge {@code from} into {@code into}: {@code from}'s name and aliases become {@code into}'s aliases;
     * their patrons are added to {@code into}'s (not {@code into} themselves, no repeats); anyone naming
     * {@code from} as patron names {@code into} instead (no repeats). {@code from}'s holds, watchers and
     * contact are dropped, and they are marked merged, leaving the list. {@code into}'s statement count
     * becomes their own plus {@code from}'s, {@code from}'s statements numbered before {@code into}'s own;
     * {@code into}'s last statement stays theirs (or, having none, becomes {@code from}'s).
     */
    public void merge(Person from, Person into, long mergeId) {
        String sheetId = from.getSheetId();
        int moved = from.getStatementCount();
        if (moved > 0) {
            // Through negatives, so no two of theirs share a number on the way.
            jdbc.update("UPDATE person_statement SET seq = -(seq + ?) WHERE sheet_id = ? AND person_id = ?",
                    moved, sheetId, into.getId());
            jdbc.update("UPDATE person_statement SET seq = -seq WHERE sheet_id = ? AND person_id = ? AND seq < 0",
                    sheetId, into.getId());
            jdbc.update("UPDATE person_statement SET person_id = ? WHERE sheet_id = ? AND person_id = ?",
                    into.getId(), sheetId, from.getId());
            if (into.getStatementCount() > 0) {
                into.setStatements(into.getStatementCount() + moved, into.getStatedTotal(), into.getStatementDate());
            } else {
                into.setStatements(moved, from.getStatedTotal(), from.getStatementDate());
            }
            saveStatements(into);
        }
        // Their own aliases are kept with the merge, so undoing it gives them back as they were.
        jdbc.update("INSERT INTO person_merge_alias (merge_id, alias, added_on, alias_merge_id)"
                + " SELECT ?, alias, added_on, merge_id FROM person_alias WHERE sheet_id = ? AND person_id = ? ORDER BY id",
                mergeId, sheetId, from.getId());
        jdbc.update("DELETE FROM person_alias WHERE sheet_id = ? AND person_id = ?", sheetId, from.getId());
        mergedAlias(into, from.getName(), mergeId);
        for (String alias : from.getAliases()) mergedAlias(into, alias, mergeId);
        // Their own patrons go to the other, not the other themselves, nor one the other already names.
        jdbc.update("DELETE FROM person_patron WHERE sheet_id = ? AND person_id = ? AND (patron_id = ?"
                + " OR patron_id IN (SELECT p.patron_id FROM person_patron p WHERE p.person_id = ?))",
                sheetId, from.getId(), into.getId(), into.getId());
        jdbc.update("UPDATE person_patron SET person_id = ? WHERE sheet_id = ? AND person_id = ?",
                into.getId(), sheetId, from.getId());
        // Those naming them name the other instead: not the other themselves, nor anyone already naming the other.
        jdbc.update("DELETE FROM person_patron WHERE sheet_id = ? AND patron_id = ? AND (person_id = ?"
                + " OR person_id IN (SELECT p.person_id FROM person_patron p WHERE p.patron_id = ?))",
                sheetId, from.getId(), into.getId(), into.getId());
        jdbc.update("UPDATE person_patron SET patron_id = ? WHERE sheet_id = ? AND patron_id = ?",
                into.getId(), sheetId, from.getId());
        jdbc.update("DELETE FROM person_hold WHERE sheet_id = ? AND person_id = ?", sheetId, from.getId());
        jdbc.update("DELETE FROM person_watch WHERE sheet_id = ? AND person_id = ?", sheetId, from.getId());
        jdbc.update("UPDATE person SET contact = NULL, merged_into = ?, merged_on = (SELECT today FROM sheet WHERE id = ?),"
                + " version = version + 1 WHERE id = ? AND sheet_id = ?", into.getId(), sheetId, from.getId(), sheetId);
        jdbc.update("UPDATE person SET version = version + 1 WHERE id = ? AND sheet_id = ?", into.getId(), sheetId);
    }

    private void mergedAlias(Person into, String alias, long mergeId) {
        jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias, added_on, merge_id)"
                + " VALUES (?, ?, ?, (SELECT today FROM sheet WHERE id = ?), ?)",
                into.getSheetId(), into.getId(), alias, into.getSheetId(), mergeId);
        into.getAliases().add(alias);
    }

    /** Record that {@code from} is being merged into {@code into} on the sheet's today: the merge's id. */
    public long recordMerge(Person from, Person into) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update((con) -> {
            PreparedStatement ps = con.prepareStatement("INSERT INTO person_merge (sheet_id, from_id, into_id, merged_on)"
                    + " VALUES (?, ?, ?, (SELECT today FROM sheet WHERE id = ?))", new String[] { "id" });
            ps.setString(1, from.getSheetId());
            ps.setLong(2, from.getId());
            ps.setLong(3, into.getId());
            ps.setString(4, from.getSheetId());
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }

    /** Record that the merge moved this claim to the person merged into. */
    public void recordMergedClaim(long mergeId, long claimId) {
        jdbc.update("INSERT INTO person_merge_claim (merge_id, claim_id) VALUES (?, ?)", mergeId, claimId);
    }

    /** The last merge into the person not yet undone, or null when there is none. */
    public LastMerge lastMergeInto(Person into) {
        List<LastMerge> found = jdbc.query("SELECT id, from_id, merged_on FROM person_merge WHERE sheet_id = ? AND into_id = ?"
                + " ORDER BY id DESC FETCH FIRST 1 ROWS ONLY",
                (rs, n) -> new LastMerge(rs.getLong("id"), rs.getLong("from_id"), rs.getObject("merged_on", LocalDate.class)),
                into.getSheetId(), into.getId());
        return found.isEmpty() ? null : found.get(0);
    }

    /** The ids of the claims the merge moved to the person merged into. */
    public List<Long> mergedClaims(long mergeId) {
        return jdbc.queryForList("SELECT claim_id FROM person_merge_claim WHERE merge_id = ?", Long.class, mergeId);
    }

    /**
     * Undo the merge of {@code from} into {@code into}: the aliases it gave {@code into} are taken away,
     * {@code from}'s own aliases are given back as they were, and {@code from} is back on the list, at its
     * end. The merge is forgotten. Their claims are moved back by the caller; everything else stays.
     */
    public void undoMerge(LastMerge merge, Person into) {
        String sheetId = into.getSheetId();
        jdbc.update("DELETE FROM person_alias WHERE sheet_id = ? AND person_id = ? AND merge_id = ?",
                sheetId, into.getId(), merge.id());
        jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias, added_on, merge_id)"
                + " SELECT ?, ?, alias, added_on, alias_merge_id FROM person_merge_alias WHERE merge_id = ? ORDER BY id",
                sheetId, merge.fromId(), merge.id());
        // Their statements stayed with the person they were merged into.
        jdbc.update("UPDATE person SET merged_into = NULL, merged_on = NULL, list_pos = " + END_OF_LIST + ","
                + " statement_count = 0, stated_total = NULL, statement_date = NULL, version = version + 1"
                + " WHERE id = ? AND sheet_id = ?", sheetId, merge.fromId(), sheetId);
        jdbc.update("UPDATE person SET version = version + 1 WHERE id = ? AND sheet_id = ?", into.getId(), sheetId);
        jdbc.update("DELETE FROM person_merge_alias WHERE merge_id = ?", merge.id());
        jdbc.update("DELETE FROM person_merge_claim WHERE merge_id = ?", merge.id());
        jdbc.update("DELETE FROM person_merge WHERE id = ?", merge.id());
    }

    /** The sheet's today when the person arrived on their sheet by transfer; null when they never did. */
    public LocalDate arrivedOn(Person person) {
        return jdbc.queryForObject("SELECT arrived_on FROM person WHERE id = ?", LocalDate.class, person.getId());
    }

    /**
     * Move the person to another sheet, with their grades (and from-dates), terms, contact and aliases,
     * as added there on that sheet's today, and with their statements: they are not retired. Patron links either way, pool and crew memberships (a crew they lead is left without a lead), watches and holds are dropped. Their claims
     * are moved by the caller.
     */
    public void transfer(Person person, String toSheet) {
        long id = person.getId();
        jdbc.update("DELETE FROM person_patron WHERE person_id = ? OR patron_id = ?", id, id);
        jdbc.update("DELETE FROM pool_member WHERE person_id = ?", id);
        jdbc.update("DELETE FROM crew_member WHERE person_id = ?", id);
        jdbc.update("UPDATE crew SET lead_id = NULL WHERE lead_id = ?", id);
        jdbc.update("DELETE FROM person_watch WHERE person_id = ?", id);
        jdbc.update("DELETE FROM person_hold WHERE person_id = ?", id);
        jdbc.update("UPDATE person_statement SET sheet_id = ? WHERE person_id = ?", toSheet, id);
        jdbc.update("DELETE FROM person_statement_history WHERE person_id = ?", id);
        jdbc.update("DELETE FROM person_retirement WHERE person_id = ?", id);
        jdbc.update("DELETE FROM person_past_retirement WHERE person_id = ?", id);
        jdbc.update("DELETE FROM person_snapshot WHERE person_id = ?", id);
        jdbc.update("DELETE FROM person_grade_history WHERE person_id = ?", id);
        jdbc.update("DELETE FROM person_terms_history WHERE person_id = ?", id);
        jdbc.update("UPDATE person_grade SET sheet_id = ? WHERE person_id = ?", toSheet, id);
        jdbc.update("UPDATE person_terms SET sheet_id = ? WHERE person_id = ?", toSheet, id);
        jdbc.update("UPDATE person_alias SET sheet_id = ?, added_on = (SELECT today FROM sheet WHERE id = ?)"
                + " WHERE person_id = ?", toSheet, toSheet, id);
        jdbc.update("UPDATE person SET sheet_id = ?,"
                + " retired_from = NULL, added_on = (SELECT today FROM sheet WHERE id = ?), list_pos = " + END_OF_LIST + ","
                + " origin_sheet = ?, arrived_on = (SELECT today FROM sheet WHERE id = ?), version = version + 1 WHERE id = ?",
                toSheet, toSheet, toSheet, person.getSheetId(), toSheet, id);
        Person moved = find(toSheet, id);
        if (moved.getStatementCount() > 0) saveStatements(moved);
        gradeHistory(moved, null, moved.getGrade());
        termsHistory(moved, null, moved.getTerms());
        moved.getDatedTerms().forEach((from, terms) -> termsHistory(moved, from, terms));
        moved.getDatedGrades().forEach((from, grade) -> gradeHistory(moved, from, grade));
    }

    /** Record {@code patron} as one of the person's patrons. */
    public void addPatron(Person person, Person patron) {
        jdbc.update("INSERT INTO person_patron (sheet_id, person_id, patron_id, added_on, named_seq)"
                + " VALUES (?, ?, ?, (SELECT today FROM sheet WHERE id = ?), NEXT VALUE FOR person_patron_seq)",
                person.getSheetId(), person.getId(), patron.getId(), person.getSheetId());
        person.getPatronIds().add(patron.getId());
    }

    /**
     * The person on the sheet with this name or alias, ignoring case and surrounding spaces, or null
     * when there is none.
     */
    public Person findByName(String sheetId, String name) {
        List<Person> found = jdbc.query("SELECT " + COLUMNS + " FROM person WHERE sheet_id = ? AND merged_into IS NULL"
                + " AND (LOWER(TRIM(name)) = LOWER(TRIM(?)) OR id IN (SELECT person_id FROM person_alias"
                + " WHERE sheet_id = ? AND LOWER(TRIM(alias)) = LOWER(TRIM(?)))) ORDER BY id",
                ROW, sheetId, name, sheetId, name);
        if (found.isEmpty()) return null;
        loadGrades(found.get(0));
        loadHolds(found.get(0));
        loadAliases(found.get(0));
        return found.get(0);
    }

    private void loadGrades(Person person) {
        jdbc.query("SELECT from_date, grade FROM person_grade WHERE sheet_id = ? AND person_id = ?",
                (rs) -> {
                    person.setGradeFrom(rs.getString("grade"), rs.getObject("from_date", LocalDate.class));
                }, person.getSheetId(), person.getId());
        jdbc.query("SELECT from_date, terms FROM person_terms WHERE sheet_id = ? AND person_id = ?",
                (rs) -> {
                    person.setTermsFrom(rs.getObject("terms", Integer.class), rs.getObject("from_date", LocalDate.class));
                }, person.getSheetId(), person.getId());
    }

    private void loadHolds(Person person) {
        jdbc.query("SELECT from_date, to_date FROM person_hold WHERE sheet_id = ? AND person_id = ? ORDER BY id",
                (rs) -> {
                    person.getHolds().add(hold(rs));
                }, person.getSheetId(), person.getId());
    }

    private static Hold hold(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Hold(rs.getObject("from_date", LocalDate.class), rs.getObject("to_date", LocalDate.class));
    }

    /** Record the person's dated grades and terms as they hold them now, replacing those stored. */
    private void saveGrades(Person person) {
        jdbc.update("DELETE FROM person_terms WHERE sheet_id = ? AND person_id = ?",
                person.getSheetId(), person.getId());
        person.getDatedTerms().forEach((from, terms) -> jdbc.update(
                "INSERT INTO person_terms (sheet_id, person_id, from_date, terms) VALUES (?, ?, ?, ?)",
                person.getSheetId(), person.getId(), Date.valueOf(from), terms));
        jdbc.update("DELETE FROM person_grade WHERE sheet_id = ? AND person_id = ?",
                person.getSheetId(), person.getId());
        person.getDatedGrades().forEach((from, grade) -> jdbc.update(
                "INSERT INTO person_grade (sheet_id, person_id, from_date, grade) VALUES (?, ?, ?, ?)",
                person.getSheetId(), person.getId(), Date.valueOf(from), grade));
    }

    /**
     * Whether a person other than {@code exceptId} has this name on the sheet, or anyone has it as an
     * alias, ignoring case.
     */
    public boolean nameTaken(String sheetId, String name, Long exceptId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND LOWER(TRIM(name)) = LOWER(TRIM(?))"
                        + " AND id <> ? AND merged_into IS NULL",
                Integer.class, sheetId, name, exceptId == null ? -1L : exceptId);
        Integer aliases = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person_alias WHERE sheet_id = ? AND LOWER(TRIM(alias)) = LOWER(TRIM(?))",
                Integer.class, sheetId, name);
        return (count != null && count > 0) || (aliases != null && aliases > 0);
    }

    /**
     * Whether a person on any of the sheets, other than {@code exceptId}, has this as their name or
     * one of their aliases, ignoring case and surrounding spaces.
     */
    public boolean nameTakenIn(java.util.Collection<String> sheetIds, String name, Long exceptId) {
        for (String sheetId : sheetIds) {
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND LOWER(TRIM(name)) = LOWER(TRIM(?))"
                            + " AND id <> ? AND merged_into IS NULL",
                    Integer.class, sheetId, name, exceptId == null ? -1L : exceptId);
            Integer aliases = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM person_alias WHERE sheet_id = ? AND LOWER(TRIM(alias)) = LOWER(TRIM(?))"
                            + " AND person_id <> ?",
                    Integer.class, sheetId, name, exceptId == null ? -1L : exceptId);
            if ((count != null && count > 0) || (aliases != null && aliases > 0)) return true;
        }
        return false;
    }

    /** Every name and alias of the people on the sheets, trimmed and lower-cased. */
    public java.util.Set<String> namesIn(java.util.Collection<String> sheetIds) {
        java.util.Set<String> names = new java.util.HashSet<>();
        for (String sheetId : sheetIds) {
            jdbc.queryForList("SELECT name FROM person WHERE sheet_id = ? AND merged_into IS NULL", String.class, sheetId)
                    .forEach((n) -> names.add(asName(n)));
            jdbc.queryForList("SELECT alias FROM person_alias WHERE sheet_id = ?", String.class, sheetId)
                    .forEach((a) -> names.add(asName(a)));
        }
        return names;
    }

    /** Whether the person's name or any of their aliases is in {@code names} (trimmed and lower-cased). */
    public static boolean clashes(Person person, java.util.Set<String> names) {
        if (names.contains(asName(person.getName()))) return true;
        return person.getAliases().stream().anyMatch((a) -> names.contains(asName(a)));
    }

    private static String asName(String name) {
        return name.trim().toLowerCase(java.util.Locale.ROOT);
    }

    /** Whether a person other than {@code exceptId} has this contact on the sheet. */
    public boolean contactTaken(String sheetId, String contact, Long exceptId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND contact = ? AND id <> ?",
                Integer.class, sheetId, contact, exceptId == null ? -1L : exceptId);
        return count != null && count > 0;
    }

    /**
     * Record the person's statements (their count, last due-total and date), leaving the rest as stored.
     * The last statement is kept as the one numbered by the count; any numbered after it are dropped.
     */
    public void saveStatements(Person person) {
        Date date = person.getStatementDate() == null ? null : Date.valueOf(person.getStatementDate());
        jdbc.update("UPDATE person SET statement_count = ?, stated_total = ?, statement_date = ? WHERE id = ? AND sheet_id = ?",
                person.getStatementCount(), person.getStatedTotal(), date,
                person.getId(), person.getSheetId());
        jdbc.update("DELETE FROM person_statement WHERE sheet_id = ? AND person_id = ? AND seq >= ?",
                person.getSheetId(), person.getId(), person.getStatementCount());
        if (person.getStatementCount() > 0) {
            jdbc.update("INSERT INTO person_statement (sheet_id, person_id, seq, stated_total, statement_date) VALUES (?, ?, ?, ?, ?)",
                    person.getSheetId(), person.getId(), person.getStatementCount(), person.getStatedTotal(), date);
        }
        jdbc.update("INSERT INTO person_statement_history (sheet_id, person_id, statement_count, stated_total, statement_date, set_on)"
                + " VALUES (?, ?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?))",
                person.getSheetId(), person.getId(), person.getStatementCount(), person.getStatedTotal(), date,
                person.getSheetId());
    }

    /**
     * The person's statement numbered {@code seq} (from 1): its due-total and date, or null when it
     * was not kept (taken before statements were kept), or there is none.
     */
    public StatedFigure findStatement(Person person, int seq) {
        List<StatedFigure> found = jdbc.query(
                "SELECT stated_total, statement_date FROM person_statement WHERE sheet_id = ? AND person_id = ? AND seq = ?",
                (rs, n) -> new StatedFigure(rs.getString("stated_total"), rs.getObject("statement_date", LocalDate.class)),
                person.getSheetId(), person.getId(), seq);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Record the person's approver, leaving the rest as stored. */
    public void saveApprover(Person person) {
        jdbc.update("UPDATE person SET approver = ? WHERE id = ? AND sheet_id = ?",
                person.getApprover(), person.getId(), person.getSheetId());
    }

    /** Record the person's flag, leaving the rest as stored. */
    public void saveFlag(Person person) {
        jdbc.update("UPDATE person SET flag_reason = ?, flag_until = ? WHERE id = ? AND sheet_id = ?",
                person.getFlagReason(), person.getFlagUntil() == null ? null : Date.valueOf(person.getFlagUntil()),
                person.getId(), person.getSheetId());
    }

    /** Record the date the person is retired from, leaving the rest as stored. */
    public void saveRetired(Person person) {
        jdbc.update("UPDATE person SET retired_from = ? WHERE id = ? AND sheet_id = ?",
                person.getRetiredFrom() == null ? null : Date.valueOf(person.getRetiredFrom()),
                person.getId(), person.getSheetId());
        jdbc.update("INSERT INTO person_retirement (sheet_id, person_id, from_date, set_on)"
                + " VALUES (?, ?, ?, (SELECT today FROM sheet WHERE id = ?))",
                person.getSheetId(), person.getId(),
                person.getRetiredFrom() == null ? null : Date.valueOf(person.getRetiredFrom()), person.getSheetId());
    }

    /**
     * Record that the person is unretired: no longer retired (kept in the retirement history as no
     * date), their retirement kept as the past one given.
     */
    public void saveUnretired(Person person, PastRetirement past) {
        saveRetired(person);
        jdbc.update("INSERT INTO person_past_retirement (sheet_id, person_id, from_date, to_date) VALUES (?, ?, ?, ?)",
                person.getSheetId(), person.getId(), Date.valueOf(past.from()), Date.valueOf(past.to()));
    }

    /** Record a new hold on the person, leaving the rest as stored. */
    public void addHold(Person person, Hold hold) {
        jdbc.update("INSERT INTO person_hold (sheet_id, person_id, from_date, to_date, made_on)"
                + " VALUES (?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?))",
                person.getSheetId(), person.getId(), Date.valueOf(hold.from()),
                hold.to() == null ? null : Date.valueOf(hold.to()), person.getSheetId());
    }

    /**
     * Insert a new person (assigning its id) or update an existing one, raising its version. False
     * when the stored person is no longer at the version it was loaded at: nothing is written.
     */
    public boolean save(Person person) {
        if (person.getId() != null) {
            List<String> storedGrade = jdbc.queryForList("SELECT grade FROM person WHERE id = ? AND sheet_id = ?",
                    String.class, person.getId(), person.getSheetId());
            List<Integer> storedTerms = jdbc.query("SELECT terms FROM person WHERE id = ? AND sheet_id = ?",
                    (rs, n) -> rs.getObject("terms", Integer.class), person.getId(), person.getSheetId());
            Map<LocalDate, String> storedDated = new HashMap<>();
            jdbc.query("SELECT from_date, grade FROM person_grade WHERE sheet_id = ? AND person_id = ?",
                    (rs) -> {
                        storedDated.put(rs.getObject("from_date", LocalDate.class), rs.getString("grade"));
                    }, person.getSheetId(), person.getId());
            Map<LocalDate, Integer> storedDatedTerms = new HashMap<>();
            jdbc.query("SELECT from_date, terms FROM person_terms WHERE sheet_id = ? AND person_id = ?",
                    (rs) -> {
                        storedDatedTerms.put(rs.getObject("from_date", LocalDate.class), rs.getObject("terms", Integer.class));
                    }, person.getSheetId(), person.getId());
            int updated = jdbc.update("UPDATE person SET name = ?, grade = ?, terms = ?, contact = ?, tags = ?, version = version + 1"
                    + " WHERE id = ? AND sheet_id = ? AND version = ?",
                    person.getName(), person.getGrade(), person.getTerms(), person.getContact(), person.getTags(), person.getId(), person.getSheetId(),
                    person.getVersion());
            if (updated == 0) return false;
            person.setVersion(person.getVersion() + 1);
            saveGrades(person);
            if (storedGrade.isEmpty() || !storedGrade.get(0).equals(person.getGrade())) {
                gradeHistory(person, null, person.getGrade());
            }
            if (storedTerms.isEmpty() || !Objects.equals(storedTerms.get(0), person.getTerms())) termsHistory(person, null, person.getTerms());
            person.getDatedTerms().forEach((from, terms) -> {
                if (!storedDatedTerms.containsKey(from) || !Objects.equals(terms, storedDatedTerms.get(from))) termsHistory(person, from, terms);
            });
            person.getDatedGrades().forEach((from, grade) -> {
                if (!grade.equals(storedDated.get(from))) gradeHistory(person, from, grade);
            });
            return true;
        }
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade, terms, contact, tags, added_on, list_pos) VALUES (?, ?, ?, ?, ?, ?,"
                            + " (SELECT today FROM sheet WHERE id = ?), " + END_OF_LIST + ")",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, person.getSheetId());
            ps.setString(2, person.getName());
            ps.setString(3, person.getGrade());
            ps.setObject(4, person.getTerms(), java.sql.Types.INTEGER);
            ps.setString(5, person.getContact());
            ps.setString(6, person.getTags());
            ps.setString(7, person.getSheetId());
            ps.setString(8, person.getSheetId());
            return ps;
        }, keys);
        person.setId(keys.getKey().longValue());
        person.setAddedOn(jdbc.queryForObject("SELECT added_on FROM person WHERE id = ?", LocalDate.class, person.getId()));
        saveGrades(person);
        gradeHistory(person, null, person.getGrade());
        termsHistory(person, null, person.getTerms());
        person.getDatedTerms().forEach((from, terms) -> termsHistory(person, from, terms));
        person.getDatedGrades().forEach((from, grade) -> gradeHistory(person, from, grade));
        return true;
    }

    /** Keeps a grade given to the person (from {@code from}; null: their first grade) in its history, with the sheet's today now. */
    private void gradeHistory(Person person, LocalDate from, String grade) {
        jdbc.update("INSERT INTO person_grade_history (sheet_id, person_id, from_date, grade, set_on)"
                + " VALUES (?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?))",
                person.getSheetId(), person.getId(), from == null ? null : Date.valueOf(from), grade, person.getSheetId());
    }

    /**
     * Keeps terms given to the person (null: none; from {@code from}, null: from the beginning of time)
     * in their history, with the sheet's today now.
     */
    private void termsHistory(Person person, LocalDate from, Integer terms) {
        jdbc.update("INSERT INTO person_terms_history (sheet_id, person_id, terms, from_date, set_on)"
                + " VALUES (?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?))",
                person.getSheetId(), person.getId(), terms, from == null ? null : Date.valueOf(from), person.getSheetId());
    }

    /**
     * The person as they stood at the end of {@code day}: their grades, terms, holds, retirement, statements,
     * aliases and patrons as given while the sheet's today was on or before it; the rest as they are now.
     */
    public Person asOf(Person person, LocalDate day) {
        Date on = Date.valueOf(day);
        String[] first = { person.getGrade() };
        Map<LocalDate, String> dated = new HashMap<>();
        jdbc.query("SELECT from_date, grade FROM person_grade_history WHERE sheet_id = ? AND person_id = ?"
                + " AND set_on <= ? ORDER BY id", (rs) -> {
                    LocalDate from = rs.getObject("from_date", LocalDate.class);
                    if (from == null) first[0] = rs.getString("grade");
                    else dated.put(from, rs.getString("grade"));
                }, person.getSheetId(), person.getId(), on);
        Person then = new Person(person.getId(), person.getSheetId(), person.getName(), first[0], person.getVersion());
        jdbc.query("SELECT from_date, terms FROM person_terms_history WHERE sheet_id = ? AND person_id = ?"
                + " AND set_on <= ? ORDER BY id", (rs) -> {
                    LocalDate from = rs.getObject("from_date", LocalDate.class);
                    Integer terms = rs.getObject("terms", Integer.class);
                    if (from == null) then.setTerms(terms);
                    else then.setTermsFrom(terms, from);
                }, person.getSheetId(), person.getId(), on);
        then.setContact(person.getContact());
        then.setTags(person.getTags());
        then.getAliases().addAll(jdbc.queryForList("SELECT alias FROM person_alias WHERE sheet_id = ? AND person_id = ?"
                + " AND (added_on IS NULL OR added_on <= ?) ORDER BY id", String.class, person.getSheetId(), person.getId(), on));
        then.setAddedOn(person.getAddedOn());
        then.setStatements(0, null, null);
        jdbc.query("SELECT statement_count, stated_total, statement_date FROM person_statement_history"
                + " WHERE sheet_id = ? AND person_id = ? AND set_on <= ? ORDER BY id DESC LIMIT 1", (rs) -> {
                    then.setStatements(rs.getInt("statement_count"), rs.getString("stated_total"),
                            rs.getObject("statement_date", LocalDate.class));
                }, person.getSheetId(), person.getId(), on);
        // Each statement as it stood then (the last kept for its number), up to the count then.
        Map<Integer, Object[]> stated = new HashMap<>();
        jdbc.query("SELECT statement_count, stated_total, statement_date FROM person_statement_history"
                + " WHERE sheet_id = ? AND person_id = ? AND set_on <= ? AND statement_count > 0 ORDER BY id", (rs) -> {
                    stated.put(rs.getInt("statement_count"), new Object[] {
                            rs.getObject("statement_date", LocalDate.class), rs.getString("stated_total") });
                }, person.getSheetId(), person.getId(), on);
        stated.entrySet().stream()
                .filter((e) -> e.getKey() <= then.getStatementCount())
                .sorted(java.util.Comparator.comparing((Map.Entry<Integer, Object[]> e) -> (LocalDate) e.getValue()[0],
                        java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder()))
                        .thenComparing(Map.Entry::getKey))
                .forEach((e) -> then.getStatementLines().add(e.getValue()[0] + " " + e.getValue()[1]));
        dated.forEach((from, grade) -> then.setGradeFrom(grade, from));
        jdbc.query("SELECT from_date, to_date FROM person_hold WHERE sheet_id = ? AND person_id = ?"
                + " AND (made_on IS NULL OR made_on <= ?) ORDER BY id", (rs) -> {
                    then.getHolds().add(hold(rs));
                }, person.getSheetId(), person.getId(), on);
        List<LocalDate> retired = jdbc.query("SELECT from_date FROM person_retirement WHERE sheet_id = ? AND person_id = ?"
                + " AND set_on <= ? ORDER BY id DESC LIMIT 1", (rs, n) -> rs.getObject("from_date", LocalDate.class),
                person.getSheetId(), person.getId(), on);
        then.setRetiredFrom(retired.isEmpty() ? null : retired.get(0));
        then.getPastRetirements().addAll(person.getPastRetirements().stream().filter((r) -> !r.to().isAfter(day)).toList());
        then.getPatronIds().addAll(jdbc.queryForList("SELECT patron_id FROM person_patron WHERE sheet_id = ?"
                + " AND person_id = ? AND (added_on IS NULL OR added_on <= ?) ORDER BY named_seq NULLS FIRST, patron_id", Long.class,
                person.getSheetId(), person.getId(), on));
        return then;
    }
}

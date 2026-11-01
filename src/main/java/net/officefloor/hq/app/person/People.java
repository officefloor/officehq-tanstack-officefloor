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
import java.util.TreeMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The people of each sheet, kept in list order: by position (a new person goes to the end). */
@Repository
public class People {

    private static final String COLUMNS = "SELECT id, sheet_id, name, grade, terms, contact, tags, version, statement_count, stated_total, statement_date, retired_from, past_retirements, origin_sheet, origin_name, "
            + "(SELECT today FROM sheet WHERE sheet.id = person.sheet_id) AS sheet_today FROM person";

    /** The position after the last of the sheet (bound to the sheet id): a new person goes to the end. */
    static final String END_POSITION = "(SELECT COALESCE(MAX(position), 0) + 1 FROM person WHERE sheet_id = ?)";

    private final JdbcTemplate jdbc;

    public People(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Person> onSheet(String sheetId) {
        List<Person> people = jdbc.query(COLUMNS + " WHERE sheet_id = ? ORDER BY position, id",
                People::person,
                sheetId);
        Map<Long, List<Long>> patrons = new HashMap<>();
        jdbc.query("SELECT person_id, patron_id FROM person_patron WHERE sheet_id = ? ORDER BY id",
                (ResultSet rs) -> {
                    patrons.computeIfAbsent(rs.getLong("person_id"), k -> new ArrayList<>()).add(rs.getLong("patron_id"));
                }, sheetId);
        Map<Long, TreeMap<LocalDate, String>> grades = new HashMap<>();
        jdbc.query("SELECT person_id, grade, from_date FROM person_grade WHERE sheet_id = ?",
                (ResultSet rs) -> {
                    grades.computeIfAbsent(rs.getLong("person_id"), k -> new TreeMap<>())
                            .put(fromKey(rs.getDate("from_date")), rs.getString("grade"));
                }, sheetId);
        Map<Long, List<Hold>> holds = new HashMap<>();
        jdbc.query("SELECT person_id, from_date, to_date FROM person_hold WHERE sheet_id = ? ORDER BY id",
                (ResultSet rs) -> {
                    holds.computeIfAbsent(rs.getLong("person_id"), k -> new ArrayList<>()).add(hold(rs));
                }, sheetId);
        Map<Long, List<String>> snapshots = new HashMap<>();
        jdbc.query("SELECT person_id, left_day, due_total FROM person_snapshot WHERE sheet_id = ? ORDER BY id",
                (ResultSet rs) -> {
                    snapshots.computeIfAbsent(rs.getLong("person_id"), k -> new ArrayList<>()).add(snapshot(rs));
                }, sheetId);
        Map<Long, List<String>> aliases = new HashMap<>();
        jdbc.query("SELECT person_id, alias FROM person_alias WHERE sheet_id = ? ORDER BY alias_key, id",
                (ResultSet rs) -> {
                    aliases.computeIfAbsent(rs.getLong("person_id"), k -> new ArrayList<>()).add(rs.getString("alias"));
                }, sheetId);
        java.util.Set<String> used = people.isEmpty() ? java.util.Set.of() : usedInTree(sheetId);
        for (Person person : people) {
            person.setAliases(aliases.getOrDefault(person.getId(), new ArrayList<>()));
            person.setHolds(holds.getOrDefault(person.getId(), new ArrayList<>()));
            person.setSnapshots(snapshots.getOrDefault(person.getId(), new ArrayList<>()));
            person.setPatronIds(patrons.getOrDefault(person.getId(), new ArrayList<>()));
            TreeMap<LocalDate, String> dated = grades.get(person.getId());
            if (dated != null) person.setGrades(dated);
            person.setClashing(clashing(person, used));
        }
        return people;
    }

    /** The person with this id on this sheet, or null when there is none. */
    public Person find(String sheetId, long id) {
        return withPatrons(jdbc.query(COLUMNS + " WHERE sheet_id = ? AND id = ?",
                People::person,
                sheetId, id).stream().findFirst().orElse(null));
    }

    /** The person with this id on this sheet, read afresh and locked for the rest of the
     *  transaction, so two statements for one person at once happen one after the other. */
    public Person lock(String sheetId, long id) {
        return withPatrons(jdbc.query(COLUMNS + " WHERE sheet_id = ? AND id = ? FOR UPDATE", People::person, sheetId, id)
                .stream().findFirst().orElse(null));
    }

    /** The person on this sheet with this name or alias (matched as names are kept unique: without
     *  the spaces around it and in any case), or null when there is none. */
    public Person named(String sheetId, String name) {
        return withPatrons(jdbc.query(COLUMNS + " WHERE sheet_id = ? AND (name_key = LOWER(TRIM(?)) OR id IN "
                + "(SELECT person_id FROM person_alias WHERE sheet_id = ? AND alias_key = LOWER(TRIM(?))))",
                People::person, sheetId, name, sheetId, name).stream().findFirst().orElse(null));
    }

    /** Whether another person on this sheet (not `exceptId`, which may be null) already has this
     *  name, or anyone on the sheet has it as an alias. */
    public boolean nameTaken(String sheetId, String name, Long exceptId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND name_key = LOWER(TRIM(?)) AND id <> ?",
                Integer.class, sheetId, name, exceptId == null ? -1L : exceptId);
        return (n != null && n > 0) || aliasTaken(sheetId, name);
    }

    /** Whether this is taken on the sheet (see {@link #nameTaken}) or, as names are compared, used as a
     *  name or alias on any other sheet of its tree (see {@link #treeOf}). */
    public boolean nameUsedInTree(String sheetId, String name, Long exceptId) {
        return nameTaken(sheetId, name, exceptId) || usedInTree(sheetId).contains(name.trim().toLowerCase(java.util.Locale.ROOT));
    }

    /** Whether the person's name or any of their aliases is used on another sheet of their tree. */
    public boolean clashing(Person person) {
        return clashing(person, usedInTree(person.getSheetId()));
    }

    private static boolean clashing(Person person, java.util.Set<String> used) {
        if (used.isEmpty()) return false;
        if (used.contains(person.getName().trim().toLowerCase(java.util.Locale.ROOT))) return true;
        return person.getAliases().stream().anyMatch(a -> used.contains(a.trim().toLowerCase(java.util.Locale.ROOT)));
    }

    /** Every name and alias (in the form they are compared in) used on the other sheets of the sheet's tree. */
    private java.util.Set<String> usedInTree(String sheetId) {
        java.util.Set<String> used = new java.util.HashSet<>();
        for (String other : treeOf(sheetId)) {
            used.addAll(jdbc.queryForList("SELECT name_key FROM PUBLIC.person WHERE sheet_id = ?", String.class, other));
            used.addAll(jdbc.queryForList("SELECT alias_key FROM PUBLIC.person_alias WHERE sheet_id = ?", String.class, other));
        }
        return used;
    }

    /** The other sheets of the sheet's tree, among the sheets that exist: its ancestors (its parent, the
     *  parent's parent, and so on) and its descendants (the sheets naming it as parent, their children,
     *  and so on). Not dated: read from the live sheets. */
    public java.util.Set<String> treeOf(String sheetId) {
        java.util.Set<String> tree = new java.util.LinkedHashSet<>();
        java.util.Set<String> seen = new java.util.HashSet<>(List.of(sheetId));
        for (String sheet = parentOf(sheetId); sheet != null && seen.add(sheet); sheet = parentOf(sheet)) {
            tree.add(sheet);
        }
        java.util.ArrayDeque<String> todo = new java.util.ArrayDeque<>(List.of(sheetId));
        java.util.Set<String> visited = new java.util.HashSet<>(List.of(sheetId));
        while (!todo.isEmpty()) {
            for (String child : jdbc.queryForList("SELECT sheet_id FROM PUBLIC.sheet_setting WHERE parent_id = ?",
                    String.class, todo.poll())) {
                if (visited.add(child)) {
                    todo.add(child);
                    tree.add(child);
                }
            }
        }
        tree.remove(sheetId);
        tree.removeIf(sheet -> {
            Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM PUBLIC.sheet WHERE id = ?", Integer.class, sheet);
            return n == null || n == 0;
        });
        return tree;
    }

    private String parentOf(String sheetId) {
        return jdbc.queryForList("SELECT parent_id FROM PUBLIC.sheet_setting WHERE sheet_id = ?", String.class, sheetId)
                .stream().filter(p -> p != null && !p.isBlank()).findFirst().orElse(null);
    }

    /** Whether anyone on this sheet already has this as an alias. */
    private boolean aliasTaken(String sheetId, String alias) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person_alias WHERE sheet_id = ? AND alias_key = LOWER(TRIM(?))",
                Integer.class, sheetId, alias);
        return n != null && n > 0;
    }

    /** Whether this equals, as names are compared, any name or alias on this sheet. */
    public boolean nameOrAliasTaken(String sheetId, String alias) {
        return nameTaken(sheetId, alias, null);
    }

    /** Whether another person on this sheet (not `exceptId`, which may be null) already has this contact. */
    public boolean contactTaken(String sheetId, String contact, Long exceptId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND contact = ? AND id <> ?",
                Integer.class, sheetId, contact, exceptId == null ? -1L : exceptId);
        return n != null && n > 0;
    }

    /** Insert a new person (assigning its id) or update an existing one, if it is still at the
     *  version it was loaded at; false (nothing written) when someone has saved it since. */
    public boolean save(Person person) {
        if (person.getId() != null) {
            int n = jdbc.update("UPDATE person SET name = ?, terms = ?, contact = ?, tags = ?, version = version + 1 WHERE id = ? AND version = ?",
                    person.getName(), person.getTerms(), person.getContact(), person.getTags(), person.getId(), person.getVersion());
            if (n == 0) return false;
            person.setVersion(person.getVersion() + 1);
            Person.DatedGrade dated = person.getNewGrade();
            if (dated != null) saveGrade(person, dated);
            return true;
        }
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade, terms, contact, tags, position) VALUES (?, ?, ?, ?, ?, ?, "
                            + END_POSITION + ")", new String[] { "id" });
            ps.setString(1, person.getSheetId());
            ps.setString(2, person.getName());
            ps.setString(3, person.getFirstGrade());
            ps.setObject(4, person.getTerms(), java.sql.Types.INTEGER);
            ps.setString(5, person.getContact());
            ps.setString(6, person.getTags());
            ps.setString(7, person.getSheetId());
            return ps;
        }, keys);
        person.setId(keys.getKey().longValue());
        // The first grade a person is given applies from the beginning of time.
        saveGrade(person, new Person.DatedGrade(person.getFirstGrade(), null));
        return true;
    }

    /** Swap the person with their neighbour in the list (the one above when `up`, else below), both
     *  locked for the rest of the transaction; false (nothing written) when there is none, the person
     *  being first or last. Not an edit: neither person's version changes. */
    public boolean move(Person person, boolean up) {
        String sheetId = person.getSheetId();
        Long mine = jdbc.queryForObject("SELECT position FROM person WHERE id = ? FOR UPDATE", Long.class, person.getId());
        List<long[]> neighbour = jdbc.query("SELECT id, position FROM person WHERE sheet_id = ? AND "
                + (up ? "(position < ? OR (position = ? AND id < ?)) ORDER BY position DESC, id DESC"
                        : "(position > ? OR (position = ? AND id > ?)) ORDER BY position, id")
                + " LIMIT 1 FOR UPDATE",
                (rs, i) -> new long[] { rs.getLong(1), rs.getLong(2) }, sheetId, mine, mine, person.getId());
        if (neighbour.isEmpty()) return false;
        long otherId = neighbour.get(0)[0];
        long theirs = neighbour.get(0)[1];
        if (theirs == mine) {
            // Equal positions cannot be swapped: spread the sheet's positions out first.
            renumber(sheetId);
            mine = jdbc.queryForObject("SELECT position FROM person WHERE id = ?", Long.class, person.getId());
            theirs = jdbc.queryForObject("SELECT position FROM person WHERE id = ?", Long.class, otherId);
        }
        jdbc.update("UPDATE person SET position = ? WHERE id = ?", theirs, person.getId());
        jdbc.update("UPDATE person SET position = ? WHERE id = ?", mine, otherId);
        return true;
    }

    /** The person's place in their sheet's list, counting from 1. */
    public int positionOf(Person person) {
        List<Long> ids = jdbc.queryForList("SELECT id FROM person WHERE sheet_id = ? ORDER BY position, id",
                Long.class, person.getSheetId());
        return ids.indexOf(person.getId()) + 1;
    }

    private void renumber(String sheetId) {
        List<Long> ids = jdbc.queryForList("SELECT id FROM person WHERE sheet_id = ? ORDER BY position, id",
                Long.class, sheetId);
        for (int i = 0; i < ids.size(); i++) {
            jdbc.update("UPDATE person SET position = ? WHERE id = ?", i + 1, ids.get(i));
        }
    }

    /** Record a statement taken for the person (their count, last due-total and date). Not an edit:
     *  the person's version is untouched, so an editor open on them can still save. */
    public void saveStatement(Person person) {
        jdbc.update("UPDATE person SET statement_count = ?, stated_total = ?, statement_date = ? WHERE id = ?",
                person.getStatementCount(), person.getStatedTotal(), Date.valueOf(person.getStatementDate()),
                person.getId());
        jdbc.update("INSERT INTO person_statement (sheet_id, person_id, seq, stated_total, statement_date) VALUES (?, ?, ?, ?, ?)",
                person.getSheetId(), person.getId(), person.getStatementCount(), person.getStatedTotal(),
                Date.valueOf(person.getStatementDate()));
    }

    /** Cancel the person's last statement: their count goes down by one and they show the one
     *  before it (its due-total and date), or none. Not an edit: the person's version is untouched. */
    public void cancelStatement(Person person) {
        jdbc.update("DELETE FROM person_statement WHERE person_id = ? AND seq >= ?",
                person.getId(), person.getStatementCount());
        int count = person.getStatementCount() - 1;
        List<Object[]> before = jdbc.query(
                "SELECT stated_total, statement_date FROM person_statement WHERE person_id = ? AND seq = ?",
                (rs, n) -> new Object[] { rs.getBigDecimal(1), rs.getDate(2) }, person.getId(), count);
        person.setStatementCount(count);
        person.setStatedTotal(before.isEmpty() ? null : (java.math.BigDecimal) before.get(0)[0]);
        person.setStatementDate(before.isEmpty() ? null : ((Date) before.get(0)[1]).toLocalDate());
        jdbc.update("UPDATE person SET statement_count = ?, stated_total = ?, statement_date = ? WHERE id = ?",
                count, person.getStatedTotal(),
                person.getStatementDate() != null ? Date.valueOf(person.getStatementDate()) : null, person.getId());
    }

    /** Record the person's retirement date and past retirements. Not an edit: the person's version is untouched. */
    public void saveRetirement(Person person) {
        jdbc.update("UPDATE person SET retired_from = ?, past_retirements = ? WHERE id = ?",
                person.getRetiredFrom() != null ? Date.valueOf(person.getRetiredFrom()) : null,
                pastRetirements(person.getPastRetirements()), person.getId());
    }

    /** Past retirements as stored: `from:to` pairs separated by commas (null: none). */
    private static String pastRetirements(List<PastRetirement> past) {
        if (past.isEmpty()) return null;
        return String.join(",", past.stream().map(p -> p.from() + ":" + p.to()).toList());
    }

    private static List<PastRetirement> pastRetirements(String stored) {
        List<PastRetirement> past = new ArrayList<>();
        if (stored == null || stored.isEmpty()) return past;
        for (String pair : stored.split(",")) {
            String[] dates = pair.split(":");
            past.add(new PastRetirement(LocalDate.parse(dates[0]), LocalDate.parse(dates[1])));
        }
        return past;
    }

    /** Add a hold to the person. Not an edit: the person's version is untouched. */
    public void addHold(Person person, Hold hold) {
        jdbc.update("INSERT INTO person_hold (sheet_id, person_id, from_date, to_date) VALUES (?, ?, ?, ?)",
                person.getSheetId(), person.getId(), Date.valueOf(hold.from()),
                hold.to() != null ? Date.valueOf(hold.to()) : null);
        person.getHolds().add(hold);
    }

    /** Name `patron` as one of the person's patrons. Not an edit: the person's version is untouched. */
    public void addPatron(Person person, Person patron) {
        jdbc.update("INSERT INTO person_patron (sheet_id, person_id, patron_id) VALUES (?, ?, ?)",
                person.getSheetId(), person.getId(), patron.getId());
        person.getPatronIds().add(patron.getId());
    }

    /** Give the person an alias (kept trimmed). Not an edit: the person's version is untouched. */
    public void addAlias(Person person, String alias) {
        String kept = alias.trim();
        jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias, alias_key) VALUES (?, ?, ?, LOWER(?))",
                person.getSheetId(), person.getId(), kept, kept);
        person.setAliases(aliasesOf(person.getId()));
    }

    /** Merge `from` into `into`: from's claims move to into (numbers and dates kept, listed after
     *  into's own in the order they were added, each keeping the figure a statement bound it to);
     *  into's statement count becomes their own plus from's (see {@link #takeStatements}); from's name and
     *  aliases become into's aliases; from's patrons are added to into's (not into themselves, no
     *  repeats) and anyone naming from as patron names into instead (no repeats); from's pool
     *  memberships move to into (shares added where into is already a member) and their closed
     *  periods' totals are added to into's. From's holds, watchers, grades and contact go
     *  with them, and they leave the sheet. Not an edit of into: their version is untouched. */
    public void merge(Person from, Person into) {
        String sheetId = from.getSheetId();
        long fromId = from.getId();
        long intoId = into.getId();
        jdbc.query("SELECT id FROM claim WHERE person_id = ? ORDER BY listed", (ResultSet rs) -> {
            jdbc.update("UPDATE claim SET person_id = ?, "
                    + "listed = NEXT VALUE FOR claim_listed WHERE id = ?", intoId, rs.getLong(1));
        }, fromId);
        takeStatements(from, into);
        jdbc.update("UPDATE person_alias SET person_id = ? WHERE person_id = ?", intoId, fromId);
        String name = from.getName().trim();
        jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias, alias_key) VALUES (?, ?, ?, LOWER(?))",
                sheetId, intoId, name, name);
        for (Long patronId : from.getPatronIds()) {
            if (patronId != intoId && !into.getPatronIds().contains(patronId)) addPatron(into, find(sheetId, patronId));
        }
        jdbc.update("DELETE FROM person_patron WHERE person_id = ? OR (patron_id = ? AND (person_id = ? "
                + "OR person_id IN (SELECT person_id FROM person_patron WHERE patron_id = ?)))",
                fromId, fromId, intoId, intoId);
        jdbc.update("UPDATE person_patron SET patron_id = ? WHERE patron_id = ?", intoId, fromId);
        jdbc.update("UPDATE pool_member m SET share = share + (SELECT f.share FROM pool_member f "
                + "WHERE f.sheet_id = m.sheet_id AND f.pool = m.pool AND f.person_id = ?) WHERE m.person_id = ? "
                + "AND m.pool IN (SELECT pool FROM pool_member WHERE sheet_id = ? AND person_id = ?)",
                fromId, intoId, sheetId, fromId);
        jdbc.update("DELETE FROM pool_member WHERE person_id = ? AND pool IN "
                + "(SELECT pool FROM pool_member WHERE sheet_id = ? AND person_id = ?)", fromId, sheetId, intoId);
        jdbc.update("UPDATE pool_member SET person_id = ? WHERE person_id = ?", intoId, fromId);
        jdbc.update("UPDATE plan SET person_id = ? WHERE person_id = ?", intoId, fromId);
        jdbc.update("UPDATE period_person_total t SET total = total + (SELECT f.total FROM period_person_total f "
                + "WHERE f.sheet_id = t.sheet_id AND f.period_name = t.period_name AND f.person_id = ?) "
                + "WHERE t.sheet_id = ? AND t.person_id = ? AND t.period_name IN "
                + "(SELECT period_name FROM period_person_total WHERE sheet_id = ? AND person_id = ?)",
                fromId, sheetId, intoId, sheetId, fromId);
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND person_id = ? AND period_name IN "
                + "(SELECT period_name FROM period_person_total WHERE sheet_id = ? AND person_id = ?)",
                sheetId, fromId, sheetId, intoId);
        jdbc.update("UPDATE period_person_total SET person_id = ? WHERE sheet_id = ? AND person_id = ?",
                intoId, sheetId, fromId);
        for (String table : List.of("person_statement", "person_hold", "person_watch", "person_grade", "person_snapshot")) {
            jdbc.update("DELETE FROM " + table + " WHERE person_id = ?", fromId);
        }
        jdbc.update("DELETE FROM person WHERE id = ?", fromId);
    }

    /** `into` takes `from`'s statements: into's count becomes their own plus from's, and their last
     *  statement stays their own (or, having none, becomes from's). In into's statement history from's
     *  come first, before into's own, so cancelling into's last shows the one before it. Not an edit:
     *  into's version is untouched. */
    public void takeStatements(Person from, Person into) {
        int fromCount = from.getStatementCount();
        if (fromCount == 0) return;
        jdbc.update("UPDATE person_statement SET seq = seq + ? WHERE person_id = ?", fromCount, into.getId());
        jdbc.update("INSERT INTO person_statement (sheet_id, person_id, seq, stated_total, statement_date) "
                + "SELECT ?, ?, seq, stated_total, statement_date FROM person_statement WHERE person_id = ? ORDER BY seq",
                into.getSheetId(), into.getId(), from.getId());
        if (into.getStatementCount() == 0) {
            jdbc.update("UPDATE person SET statement_count = ?, stated_total = ?, statement_date = ? WHERE id = ?",
                    fromCount, from.getStatedTotal(),
                    from.getStatementDate() != null ? Date.valueOf(from.getStatementDate()) : null, into.getId());
        } else {
            jdbc.update("UPDATE person SET statement_count = statement_count + ? WHERE id = ?", fromCount, into.getId());
        }
    }

    private List<String> aliasesOf(long personId) {
        return new ArrayList<>(jdbc.queryForList(
                "SELECT alias FROM person_alias WHERE person_id = ? ORDER BY alias_key, id", String.class, personId));
    }

    /** Record the person's grade from its date, replacing the one they had from that date. */
    private void saveGrade(Person person, Person.DatedGrade dated) {
        Date from = dated.from() != null ? Date.valueOf(dated.from()) : null;
        jdbc.update("DELETE FROM person_grade WHERE person_id = ? AND from_date IS NOT DISTINCT FROM ?",
                person.getId(), from);
        jdbc.update("INSERT INTO person_grade (sheet_id, person_id, grade, from_date) VALUES (?, ?, ?, ?)",
                person.getSheetId(), person.getId(), dated.grade(), from);
    }

    private static LocalDate fromKey(Date from) {
        return from != null ? from.toLocalDate() : LocalDate.MIN;
    }

    /** The person (may be null) with their patrons' ids and dated grades read in. */
    private Person withPatrons(Person person) {
        if (person != null) {
            TreeMap<LocalDate, String> grades = new TreeMap<>();
            jdbc.query("SELECT grade, from_date FROM person_grade WHERE person_id = ?",
                    (ResultSet rs) -> {
                        grades.put(fromKey(rs.getDate("from_date")), rs.getString("grade"));
                    }, person.getId());
            if (!grades.isEmpty()) person.setGrades(grades);
            person.setHolds(new ArrayList<>(jdbc.query(
                    "SELECT from_date, to_date FROM person_hold WHERE person_id = ? ORDER BY id",
                    (rs, i) -> hold(rs), person.getId())));
            person.setSnapshots(new ArrayList<>(jdbc.query(
                    "SELECT left_day, due_total FROM person_snapshot WHERE person_id = ? ORDER BY id",
                    (rs, i) -> snapshot(rs), person.getId())));
            person.setAliases(aliasesOf(person.getId()));
            person.setPatronIds(new ArrayList<>(jdbc.queryForList(
                    "SELECT patron_id FROM person_patron WHERE person_id = ? ORDER BY id", Long.class, person.getId())));
            person.setClashing(clashing(person));
        }
        return person;
    }

    /** A snapshot line as shown: the day being left and the due-total then (n/a when it was). */
    private static String snapshot(ResultSet rs) throws SQLException {
        java.math.BigDecimal due = rs.getBigDecimal("due_total");
        return rs.getDate("left_day").toLocalDate() + " " + (due == null ? "n/a" : due.setScale(2).toPlainString());
    }

    /** Record a snapshot of the person: the day being left and their due-total then (null: n/a). Not an
     *  edit: the person's version is untouched. */
    public void addSnapshot(String sheetId, long personId, LocalDate leftDay, java.math.BigDecimal dueTotal) {
        jdbc.update("INSERT INTO person_snapshot (sheet_id, person_id, left_day, due_total) VALUES (?, ?, ?, ?)",
                sheetId, personId, Date.valueOf(leftDay), dueTotal == null ? null : dueTotal.setScale(2));
    }

    private static Hold hold(ResultSet rs) throws SQLException {
        Date to = rs.getDate("to_date");
        return new Hold(rs.getDate("from_date").toLocalDate(), to != null ? to.toLocalDate() : null);
    }

    private static Person person(ResultSet rs, int i) throws SQLException {
        Person person = new Person(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name"),
                rs.getString("grade"), rs.getInt("version"));
        person.setTerms(rs.getObject("terms", Integer.class));
        person.setContact(rs.getString("contact"));
        person.setTags(rs.getString("tags"));
        person.setOrigin(rs.getString("origin_sheet"), rs.getString("origin_name"));
        person.setStatementCount(rs.getInt("statement_count"));
        person.setStatedTotal(rs.getBigDecimal("stated_total"));
        Date date = rs.getDate("statement_date");
        person.setStatementDate(date != null ? date.toLocalDate() : null);
        Date retired = rs.getDate("retired_from");
        person.setRetiredFrom(retired != null ? retired.toLocalDate() : null);
        person.setPastRetirements(pastRetirements(rs.getString("past_retirements")));
        Date today = rs.getDate("sheet_today");
        person.setToday(today != null ? today.toLocalDate() : LocalDate.now());
        return person;
    }
}

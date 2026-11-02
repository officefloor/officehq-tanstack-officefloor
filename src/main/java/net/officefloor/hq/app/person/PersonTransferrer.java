package net.officefloor.hq.app.person;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.alias.Aliases;
import net.officefloor.hq.app.claim.ClaimStatusChanger;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.Roles;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetArchive;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

/**
 * Transfers a person to another sheet by id; only a steward may, a retired person may not be, nor to this sheet's own
 * id or an archived sheet, and a name, alias or contact of theirs already on the other sheet refuses it. They arrive as
 * the other sheet's newest person, with this sheet as their origin, every grade (and its from-date), their terms,
 * contact and aliases, and their claims with their status, dates, notes, frozen figures, logs and comments, listed in
 * the order they were. Each claim that had a number takes the other sheet's next one, in the order the claims were
 * opened (by the sheet's today when each was opened, then the order they were added); a correction or split names its
 * claim's new number when that claim came along (none otherwise). Statement bindings are lost (the statement count
 * starts at 0) and references, locks and settle batches are not carried. They leave this sheet entirely: patron links,
 * pool memberships, holds, closed periods' totals, statements and watches are dropped.
 */
@Service
public class PersonTransferrer {

    private final JdbcTemplate jdbc;
    private final People people;
    private final Aliases aliases;
    private final Claims claims;
    private final ClaimStatusChanger changer;
    private final Roles roles;
    private final SheetArchive archive;
    private final Audit audit;
    private final Rollup rollup;

    public PersonTransferrer(JdbcTemplate jdbc, People people, Aliases aliases, Claims claims,
            ClaimStatusChanger changer, Roles roles, SheetArchive archive, Audit audit, Rollup rollup) {
        this.jdbc = jdbc;
        this.people = people;
        this.aliases = aliases;
        this.claims = claims;
        this.changer = changer;
        this.roles = roles;
        this.archive = archive;
        this.audit = audit;
        this.rollup = rollup;
    }

    /** Transfers the person to the sheet {@code to}, writing nothing when it is refused. */
    public void transfer(String sheet, String user, Person person, String to) throws InvalidPersonException {
        if (!Role.STEWARD.equals(roles.of(sheet, user))) {
            throw new InvalidPersonException("Only a steward may transfer a person");
        }
        if (person.retiredFrom() != null) {
            throw new InvalidPersonException("A retired person cannot be transferred");
        }
        if (to == null || to.isBlank() || to.equals(sheet)) {
            throw new InvalidPersonException("A person is transferred to another sheet");
        }
        if (archive.archived(to)) {
            throw new InvalidPersonException("An archived sheet cannot change");
        }
        long f = person.id();
        List<String> theirAliases = jdbc.queryForList(
                "SELECT alias FROM person_alias WHERE sheet = ? AND person_id = ? ORDER BY alias_key", String.class,
                sheet, f);
        List<String> names = new ArrayList<>(theirAliases);
        names.add(0, person.name());
        for (String name : names) {
            if (people.nameTaken(to, name, null) || aliases.taken(to, name)) {
                throw new InvalidPersonException("The name " + name + " is already on sheet " + to);
            }
        }
        if (person.contact() != null && people.contactTaken(to, person.contact(), null)) {
            throw new InvalidPersonException("The contact " + person.contact() + " is already on sheet " + to);
        }

        Rollup.Snapshot beforeHere = rollup.take(sheet);
        Rollup.Snapshot beforeThere = rollup.take(to);

        // The person, as the other sheet's newest, with their grades, terms, contact and aliases.
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement("INSERT INTO person (sheet, name, terms, contact, list_position, origin) SELECT ?,"
                    + " name, terms, contact, " + People.NEXT_POSITION + ", ? FROM person WHERE sheet = ? AND id = ?",
                    new String[] { "id" });
            ps.setString(1, to);
            ps.setString(2, to);
            ps.setString(3, sheet);
            ps.setString(4, sheet);
            ps.setLong(5, f);
            return ps;
        }, keys);
        long t = keys.getKey().longValue();
        jdbc.update("INSERT INTO person_grade (person_id, grade, from_date) SELECT ?, grade, from_date FROM person_grade"
                + " WHERE person_id = ?", t, f);
        for (String alias : theirAliases) {
            aliases.add(to, t, alias);
        }

        // Claims, in the order they were listed, each with its log, comments and void reason.
        List<String> claimColumns = columns("CLAIM");
        Map<Long, Long> moved = new LinkedHashMap<>();
        for (Long claim : jdbc.queryForList("SELECT id FROM claim WHERE sheet = ? AND person = ? ORDER BY listed, id",
                Long.class, sheet, f)) {
            long copy = copyClaim(claimColumns, sheet, claim, to, t);
            moved.put(claim, copy);
            jdbc.update("INSERT INTO claim_log (sheet, claim, status, by_user, on_date, due) SELECT ?, ?, status, by_user,"
                    + " on_date, due FROM claim_log WHERE sheet = ? AND claim = ? ORDER BY id", to, copy, sheet, claim);
            jdbc.update("INSERT INTO claim_comment (sheet, claim, by_user, on_date, text) SELECT ?, ?, by_user, on_date,"
                    + " text FROM claim_comment WHERE sheet = ? AND claim = ? ORDER BY id", to, copy, sheet, claim);
            jdbc.update("INSERT INTO claim_void (sheet, claim, reason) SELECT ?, ?, reason FROM claim_void"
                    + " WHERE sheet = ? AND claim = ?", to, copy, sheet, claim);
        }

        // Numbers: the other sheet's next, in the order the claims were opened (its today then, then the order added).
        Map<String, String> renumbered = new HashMap<>();
        if (!moved.isEmpty()) {
            String in = moved.keySet().stream().map(String::valueOf).collect(Collectors.joining(","));
            List<Map<String, Object>> numbered = jdbc.queryForList("SELECT c.id,"
                    + " COALESCE(c.period_number, CAST(c.number_on_sheet AS VARCHAR)) AS shown,"
                    + " COALESCE((SELECT MIN(l.on_date) FROM claim_log l WHERE l.sheet = c.sheet AND l.claim = c.id"
                    + " AND l.status = 'OPEN'), c.claimed_on) AS opened_on FROM claim c WHERE c.sheet = ? AND c.id IN ("
                    + in + ") AND (c.period_number IS NOT NULL OR c.number_on_sheet IS NOT NULL) ORDER BY opened_on, c.id",
                    sheet);
            for (Map<String, Object> row : numbered) {
                long copy = moved.get(((Number) row.get("ID")).longValue());
                var claim = changer.number(to, claims.find(to, copy));
                renumbered.put((String) row.get("SHOWN"),
                        claim.periodNumber() != null ? claim.periodNumber() : String.valueOf(claim.number()));
            }
            // A correction or split names its claim's new number, or none when that claim stayed behind.
            for (Map.Entry<Long, Long> e : moved.entrySet()) {
                Map<String, Object> row = jdbc.queryForMap("SELECT corrects, split_from FROM claim WHERE id = ?",
                        e.getKey());
                String corrects = (String) row.get("CORRECTS");
                String splitFrom = (String) row.get("SPLIT_FROM");
                jdbc.update("UPDATE claim SET corrects = ?, split_from = ? WHERE sheet = ? AND id = ?",
                        corrects == null ? null : renumbered.get(corrects),
                        splitFrom == null ? null : renumbered.get(splitFrom), to, e.getValue());
            }
        }

        // They leave this sheet entirely.
        if (!moved.isEmpty()) {
            String in = moved.keySet().stream().map(String::valueOf).collect(Collectors.joining(","));
            jdbc.update("DELETE FROM claim_log WHERE sheet = ? AND claim IN (" + in + ")", sheet);
            jdbc.update("DELETE FROM claim_comment WHERE sheet = ? AND claim IN (" + in + ")", sheet);
            jdbc.update("DELETE FROM claim_void WHERE sheet = ? AND claim IN (" + in + ")", sheet);
            jdbc.update("DELETE FROM claim WHERE sheet = ? AND id IN (" + in + ")", sheet);
        }
        jdbc.update("DELETE FROM person_alias WHERE sheet = ? AND person_id = ?", sheet, f);
        jdbc.update("DELETE FROM patron WHERE sheet = ? AND (person_id = ? OR patron_id = ?)", sheet, f, f);
        jdbc.update("DELETE FROM pool_member WHERE sheet = ? AND person_id = ?", sheet, f);
        jdbc.update("DELETE FROM period_person_total WHERE sheet = ? AND person = ?", sheet, f);
        jdbc.update("DELETE FROM statement WHERE sheet = ? AND person = ?", sheet, f);
        jdbc.update("DELETE FROM person_hold WHERE person_id = ?", f);
        jdbc.update("DELETE FROM watch WHERE sheet = ? AND person_id = ?", sheet, f);
        jdbc.update("DELETE FROM person_grade WHERE person_id = ?", f);
        jdbc.update("DELETE FROM person WHERE sheet = ? AND id = ?", sheet, f);

        audit.record("PERSON_TRANSFERRED sheet=" + sheet + " by=" + user + " name=" + person.name() + " to=" + to);
        rollup.record(sheet, user, beforeHere);
        audit.record("PERSON_TRANSFERRED sheet=" + to + " by=" + user + " name=" + person.name() + " from=" + sheet);
        rollup.record(to, user, beforeThere);
    }

    /** The columns of the table a copy writes: all but its identity and generated ones. */
    private List<String> columns(String table) {
        return jdbc.queryForList("SELECT column_name FROM information_schema.columns WHERE table_schema = 'PUBLIC'"
                + " AND table_name = ? AND is_identity = 'NO' AND is_generated = 'NEVER' ORDER BY ordinal_position",
                String.class, table);
    }

    /**
     * Copies the claim to the person on the other sheet, listed as its newest, unbound from any statement, unnumbered
     * and with no reference, lock or settle batch; returns the copy's id.
     */
    private long copyClaim(List<String> columns, String sheet, long claim, String to, long person) {
        Map<String, String> set = Map.of("SHEET", "?", "PERSON", "?", "STATED", "FALSE", "STATED_DUE", "NULL",
                "NUMBER_ON_SHEET", "NULL", "PERIOD_NUMBER", "NULL", "REF", "NULL", "LOCKED_BY", "NULL", "BATCH", "NULL",
                "LISTED", "(SELECT COALESCE(MAX(l.listed), 0) + 1 FROM claim l WHERE l.sheet = ?)");
        String select = columns.stream().map((c) -> set.getOrDefault(c, "c." + c)).collect(Collectors.joining(", "));
        List<Object> args = new ArrayList<>();
        for (String c : columns) {
            switch (c) {
                case "SHEET", "LISTED" -> args.add(to);
                case "PERSON" -> args.add(person);
                default -> {
                }
            }
        }
        args.add(sheet);
        args.add(claim);
        String sql = "INSERT INTO claim (" + String.join(", ", columns) + ") SELECT " + select
                + " FROM claim c WHERE c.sheet = ? AND c.id = ?";
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement(sql, new String[] { "id" });
            for (int i = 0; i < args.size(); i++) {
                ps.setObject(i + 1, args.get(i));
            }
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }
}

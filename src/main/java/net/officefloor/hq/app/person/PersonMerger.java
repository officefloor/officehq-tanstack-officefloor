package net.officefloor.hq.app.person;

import java.math.BigDecimal;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.Roles;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.statement.Statements;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Merges a person into another on the sheet; only a steward may, neither may be retired, and a person is not merged
 * into themselves. The merged person's claims move to the other, keeping their numbers, dates and any figure a
 * statement bound them to, listed after the other's claims in the order they were added. Their statements count as
 * the other's too; the other's last statement stays theirs (or, having none, becomes the merged person's). Their
 * name and aliases become aliases of the other; their patrons are added to the other's (not the other themselves, no
 * repeats); anyone naming them as patron names the other instead (no repeats); their pool memberships move to the
 * other, the shares added when the other is already a member, and so do their closed periods' totals. Their holds,
 * watchers and contact are dropped, and they leave the sheet.
 */
@Service
public class PersonMerger {

    private final JdbcTemplate jdbc;
    private final People people;
    private final Roles roles;
    private final Audit audit;
    private final Rollup rollup;
    private final Statements statements;

    public PersonMerger(JdbcTemplate jdbc, People people, Roles roles, Audit audit, Rollup rollup,
            Statements statements) {
        this.jdbc = jdbc;
        this.people = people;
        this.roles = roles;
        this.audit = audit;
        this.rollup = rollup;
        this.statements = statements;
    }

    /** Merges the person into the one with id {@code intoId}, writing nothing when it is refused. */
    public void merge(String sheet, String user, Person from, Long intoId) throws InvalidPersonException {
        if (!Role.STEWARD.equals(roles.of(sheet, user))) {
            throw new InvalidPersonException("Only a steward may merge people");
        }
        Person into = intoId == null ? null : people.find(sheet, intoId);
        if (into == null || into.id() == from.id()) {
            throw new InvalidPersonException("A person is merged into another person on the sheet");
        }
        if (from.retiredFrom() != null || into.retiredFrom() != null) {
            throw new InvalidPersonException("A retired person cannot be merged");
        }
        Rollup.Snapshot before = rollup.take(sheet);
        long f = from.id();
        long t = into.id();

        // Claims, in the order they were added, each listed as the sheet's newest so they follow the other's.
        for (Long claim : jdbc.queryForList("SELECT id FROM claim WHERE sheet = ? AND person = ? ORDER BY id",
                Long.class, sheet, f)) {
            jdbc.update("UPDATE claim SET person = ?, listed ="
                    + " (SELECT COALESCE(MAX(l.listed), 0) + 1 FROM claim l WHERE l.sheet = ?) WHERE sheet = ? AND id = ?",
                    t, sheet, sheet, claim);
        }

        // Name and aliases become the other's aliases.
        jdbc.update("UPDATE person_alias SET person_id = ? WHERE sheet = ? AND person_id = ?", t, sheet, f);
        jdbc.update("INSERT INTO person_alias (sheet, person_id, alias) VALUES (?, ?, ?)", sheet, t, from.name());

        // Patrons: the merged person's own, then everyone naming them.
        for (Long patron : jdbc.queryForList("SELECT patron_id FROM patron WHERE sheet = ? AND person_id = ?",
                Long.class, sheet, f)) {
            addPatron(sheet, t, patron);
        }
        for (Long person : jdbc.queryForList("SELECT person_id FROM patron WHERE sheet = ? AND patron_id = ?",
                Long.class, sheet, f)) {
            addPatron(sheet, person, t);
        }
        jdbc.update("DELETE FROM patron WHERE sheet = ? AND (person_id = ? OR patron_id = ?)", sheet, f, f);

        // Pool memberships, shares added where the other is already a member.
        for (Map<String, Object> m : jdbc.queryForList(
                "SELECT pool, share FROM pool_member WHERE sheet = ? AND person_id = ?", sheet, f)) {
            String pool = (String) m.get("POOL");
            BigDecimal share = (BigDecimal) m.get("SHARE");
            if (jdbc.update("UPDATE pool_member SET share = share + ? WHERE sheet = ? AND pool = ? AND person_id = ?",
                    share, sheet, pool, t) == 0) {
                jdbc.update("UPDATE pool_member SET person_id = ? WHERE sheet = ? AND pool = ? AND person_id = ?", t,
                        sheet, pool, f);
            }
        }
        jdbc.update("DELETE FROM pool_member WHERE sheet = ? AND person_id = ?", sheet, f);

        // Closed periods' totals follow the claims.
        for (Map<String, Object> p : jdbc.queryForList(
                "SELECT period, total FROM period_person_total WHERE sheet = ? AND person = ?", sheet, f)) {
            String period = (String) p.get("PERIOD");
            BigDecimal total = (BigDecimal) p.get("TOTAL");
            if (jdbc.update("UPDATE period_person_total SET total = total + ? WHERE sheet = ? AND period = ? AND person = ?",
                    total, sheet, period, t) == 0) {
                jdbc.update("UPDATE period_person_total SET person = ? WHERE sheet = ? AND period = ? AND person = ?",
                        t, sheet, period, f);
            }
        }
        jdbc.update("DELETE FROM period_person_total WHERE sheet = ? AND person = ?", sheet, f);

        // Statements count as the other's, the other's last staying theirs.
        statements.move(sheet, f, sheet, t);

        // Dropped with the person: holds, watchers, grades and contact.
        jdbc.update("DELETE FROM person_hold WHERE person_id = ?", f);
        jdbc.update("DELETE FROM person_retirement WHERE person_id = ?", f);
        jdbc.update("DELETE FROM watch WHERE sheet = ? AND person_id = ?", sheet, f);
        jdbc.update("DELETE FROM person_grade WHERE person_id = ?", f);
        jdbc.update("DELETE FROM person WHERE sheet = ? AND id = ?", sheet, f);

        audit.record("PERSON_MERGED sheet=" + sheet + " by=" + user + " from=" + from.name() + " into=" + into.name());
        rollup.record(sheet, user, before);
    }

    /** Names the patron for the person, unless it is themselves or already named. */
    private void addPatron(String sheet, long person, long patron) {
        if (person == patron) {
            return;
        }
        Integer found = jdbc.queryForObject(
                "SELECT COUNT(*) FROM patron WHERE sheet = ? AND person_id = ? AND patron_id = ?", Integer.class, sheet,
                person, patron);
        if (found == null || found == 0) {
            jdbc.update("INSERT INTO patron (sheet, person_id, patron_id) VALUES (?, ?, ?)", sheet, person, patron);
        }
    }
}

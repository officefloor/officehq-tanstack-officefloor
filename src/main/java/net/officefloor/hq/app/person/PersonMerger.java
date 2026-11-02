package net.officefloor.hq.app.person;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.Roles;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.hq.app.statement.Statements;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

/**
 * Merges a person into another on the sheet; only a steward may, neither may be retired, and a person is not merged
 * into themselves. The merged person's claims move to the other, keeping their numbers, dates and any figure a
 * statement bound them to, listed after the other's claims in the order they were added. Their statements count as
 * the other's too; the other's last statement stays theirs (or, having none, becomes the merged person's). Their
 * name and aliases become aliases of the other; their patrons are added to the other's (not the other themselves, no
 * repeats); anyone naming them as patron names the other instead (no repeats); their pool and crew places move to the
 * other, the shares added when the other is already a member, and so do their closed periods' totals. Their holds,
 * watchers and contact are dropped, and they leave the sheet. The merge is kept so the steward who made it can
 * undo it on the same sheet-today (see {@link #undo}).
 */
@Service
public class PersonMerger {

    private final JdbcTemplate jdbc;
    private final People people;
    private final Roles roles;
    private final Audit audit;
    private final Rollup rollup;
    private final Statements statements;
    private final SheetClock clock;

    public PersonMerger(JdbcTemplate jdbc, People people, Roles roles, Audit audit, Rollup rollup,
            Statements statements, SheetClock clock) {
        this.clock = clock;
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
        long kept = keep(sheet, user, f, t);

        // Claims, in the order they were added, each listed as the sheet's newest so they follow the other's.
        for (Long claim : jdbc.queryForList("SELECT id FROM claim WHERE sheet = ? AND person = ? ORDER BY id",
                Long.class, sheet, f)) {
                jdbc.update("INSERT INTO person_merge_claim (merge_id, claim) VALUES (?, ?)", kept, claim);
            jdbc.update("UPDATE claim SET person = ?, listed ="
                    + " (SELECT COALESCE(MAX(l.listed), 0) + 1 FROM claim l WHERE l.sheet = ?) WHERE sheet = ? AND id = ?",
                    t, sheet, sheet, claim);
        }

        // Name and aliases become the other's aliases, their own kept so an undo takes them back.
        jdbc.update("INSERT INTO person_merge_alias (merge_id, alias) SELECT ?, alias FROM person_alias"
                + " WHERE sheet = ? AND person_id = ?", kept, sheet, f);
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

        // Crews: the crews they lead are led by the other (who leaves being a member of them); their crew
        // memberships move to the other, where the other is not already in the crew.
        jdbc.update("UPDATE crew SET lead_id = ? WHERE sheet = ? AND lead_id = ?", t, sheet, f);
        jdbc.update("DELETE FROM crew_member WHERE sheet = ? AND person_id = ? AND crew IN"
                + " (SELECT name FROM crew WHERE sheet = ? AND lead_id = ?)", sheet, t, sheet, t);
        jdbc.update("UPDATE crew_member m SET person_id = ? WHERE m.sheet = ? AND m.person_id = ?"
                + " AND NOT EXISTS (SELECT 1 FROM crew c WHERE c.sheet = m.sheet AND c.name = m.crew AND c.lead_id = ?)"
                + " AND NOT EXISTS (SELECT 1 FROM crew_member o WHERE o.sheet = m.sheet AND o.crew = m.crew AND o.person_id = ?)",
                t, sheet, f, t, t);
        jdbc.update("DELETE FROM crew_member WHERE sheet = ? AND person_id = ?", sheet, f);

        // Closed periods' totals follow the claims.
        for (Map<String, Object> p : jdbc.queryForList(
                "SELECT period, total, carried FROM period_person_total WHERE sheet = ? AND person = ?", sheet, f)) {
            String period = (String) p.get("PERIOD");
            BigDecimal total = (BigDecimal) p.get("TOTAL");
            BigDecimal carried = (BigDecimal) p.get("CARRIED");
            if (jdbc.update("UPDATE period_person_total SET total = total + ?, carried = carried + ? WHERE sheet = ?"
                    + " AND period = ? AND person = ?", total, carried, sheet, period, t) == 0) {
                jdbc.update("UPDATE period_person_total SET person = ? WHERE sheet = ? AND period = ? AND person = ?",
                        t, sheet, period, f);
            }
        }
        jdbc.update("DELETE FROM period_person_total WHERE sheet = ? AND person = ?", sheet, f);

        // Closed periods' fixed pool cuts follow the memberships, cuts added where the other was already a member.
        for (Map<String, Object> c : jdbc.queryForList(
                "SELECT period, pool, cut FROM period_pool_cut WHERE sheet = ? AND member = ?", sheet, String.valueOf(f))) {
            String period = (String) c.get("PERIOD");
            String pool = (String) c.get("POOL");
            String cut = (String) c.get("CUT");
            List<String> other = jdbc.queryForList("SELECT cut FROM period_pool_cut WHERE sheet = ? AND period = ?"
                    + " AND pool = ? AND member = ?", String.class, sheet, period, pool, String.valueOf(t));
            if (other.isEmpty()) {
                jdbc.update("UPDATE period_pool_cut SET member = ? WHERE sheet = ? AND period = ? AND pool = ? AND member = ?",
                        String.valueOf(t), sheet, period, pool, String.valueOf(f));
            } else if (!cut.isEmpty()) {
                String sum = other.get(0).isEmpty() ? cut
                        : new BigDecimal(other.get(0)).add(new BigDecimal(cut)).toPlainString();
                jdbc.update("UPDATE period_pool_cut SET cut = ? WHERE sheet = ? AND period = ? AND pool = ? AND member = ?",
                        sum, sheet, period, pool, String.valueOf(t));
            }
        }
        jdbc.update("DELETE FROM period_pool_cut WHERE sheet = ? AND member = ?", sheet, String.valueOf(f));

        // Opening balances add to the other's.
        for (Map<String, Object> o : jdbc.queryForList(
                "SELECT period, amount FROM period_opening WHERE sheet = ? AND person = ?", sheet, f)) {
            String period = (String) o.get("PERIOD");
            BigDecimal amount = (BigDecimal) o.get("AMOUNT");
            if (jdbc.update("UPDATE period_opening SET amount = amount + ? WHERE sheet = ? AND period = ? AND person = ?",
                    amount, sheet, period, t) == 0) {
                jdbc.update("UPDATE period_opening SET person = ? WHERE sheet = ? AND period = ? AND person = ?", t,
                        sheet, period, f);
            }
        }
        jdbc.update("DELETE FROM period_opening WHERE sheet = ? AND person = ?", sheet, f);

        // Statements count as the other's, the other's last staying theirs.
        statements.move(sheet, f, sheet, t);

        // Dropped with the person: holds, watchers, grades and contact.
        jdbc.update("DELETE FROM person_hold WHERE person_id = ?", f);
        jdbc.update("DELETE FROM person_retirement WHERE person_id = ?", f);
        jdbc.update("DELETE FROM watch WHERE sheet = ? AND person_id = ?", sheet, f);
        jdbc.update("DELETE FROM person_grade WHERE person_id = ?", f);
        jdbc.update("DELETE FROM person_terms WHERE person_id = ?", f);
        jdbc.update("DELETE FROM person WHERE sheet = ? AND id = ?", sheet, f);

        audit.record("PERSON_MERGED sheet=" + sheet + " by=" + user + " from=" + from.name() + " into=" + into.name());
        rollup.record(sheet, user, before);
    }

    /**
     * Undoes the last merge into the person: only the steward who made it may, only on the sheet's today it was made,
     * and only once. The merged person is back, under their own id, as the sheet's last person with the grades, terms
     * and contact they had, the claims that came from them (as they are now) and their own aliases, which leave the
     * other's along with their name. Patron, pool, crew and period changes stay.
     */
    public void undo(String sheet, String user, Person into) throws InvalidPersonException {
        if (!Role.STEWARD.equals(roles.of(sheet, user))) {
            throw new InvalidPersonException("Only a steward may undo a merge");
        }
        List<Map<String, Object>> last = jdbc.queryForList("SELECT id, from_id, made_by, made_on, undone, name"
                + " FROM person_merge WHERE sheet = ? AND into_id = ? ORDER BY id DESC LIMIT 1", sheet, into.id());
        if (last.isEmpty() || Boolean.TRUE.equals(last.get(0).get("UNDONE"))) {
            throw new InvalidPersonException(into.name() + " has no merge to undo");
        }
        Map<String, Object> merge = last.get(0);
        if (!user.equals(merge.get("MADE_BY"))) {
            throw new InvalidPersonException("Only the steward who made a merge may undo it");
        }
        if (!((java.sql.Date) merge.get("MADE_ON")).toLocalDate().equals(clock.today(sheet))) {
            throw new InvalidPersonException("A merge is undone only on the day it was made");
        }
        String name = (String) merge.get("NAME");
        if (people.nameTaken(sheet, name, null)) {
            throw new InvalidPersonException("The name " + name + " is now another person's");
        }
        long kept = ((Number) merge.get("ID")).longValue();
        long f = ((Number) merge.get("FROM_ID")).longValue();
        long t = into.id();
        Rollup.Snapshot before = rollup.take(sheet);

        // The person, under their own id, as the sheet's last, with their grades and terms.
        jdbc.update("INSERT INTO person (id, sheet, name, terms, contact, origin, tags, flag_reason, flag_until, approver,"
                + " arrived_on, list_position) SELECT ?, sheet, name, terms, contact, origin, tags, flag_reason, flag_until,"
                + " approver, arrived_on, " + People.NEXT_POSITION + " FROM person_merge WHERE id = ?", f, sheet, kept);
        jdbc.update("INSERT INTO person_grade (person_id, grade, from_date) SELECT ?, grade, from_date"
                + " FROM person_merge_grade WHERE merge_id = ?", f, kept);
        jdbc.update("INSERT INTO person_terms (person_id, terms, from_date) SELECT ?, terms, from_date"
                + " FROM person_merge_terms WHERE merge_id = ?", f, kept);

        // The claims that came from them, as they are now.
        jdbc.update("UPDATE claim SET person = ? WHERE sheet = ? AND person = ? AND id IN"
                + " (SELECT claim FROM person_merge_claim WHERE merge_id = ?)", f, sheet, t, kept);

        // Their name leaves the other's aliases, and their own aliases come back to them.
        jdbc.update("DELETE FROM person_alias WHERE sheet = ? AND person_id = ? AND alias = ?", sheet, t, name);
        for (String alias : jdbc.queryForList("SELECT alias FROM person_merge_alias WHERE merge_id = ? ORDER BY alias",
                String.class, kept)) {
            if (jdbc.update("UPDATE person_alias SET person_id = ? WHERE sheet = ? AND person_id = ? AND alias = ?", f,
                    sheet, t, alias) == 0 && !aliasOf(sheet, f, alias)) {
                jdbc.update("INSERT INTO person_alias (sheet, person_id, alias) VALUES (?, ?, ?)", sheet, f, alias);
            }
        }

        jdbc.update("UPDATE person_merge SET undone = TRUE WHERE id = ?", kept);
        audit.record("PERSON_MERGE_UNDONE sheet=" + sheet + " by=" + user + " name=" + name + " from=" + into.name());
        rollup.record(sheet, user, before);
    }

    /** Keeps the merge and the merged person as they are, so the merge can be undone; returns its id. */
    private long keep(String sheet, String user, long f, long t) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement("INSERT INTO person_merge (sheet, from_id, into_id, made_by, made_on, name,"
                    + " terms, contact, origin, tags, flag_reason, flag_until, approver, arrived_on) SELECT sheet, id, ?,"
                    + " ?, ?, name, terms, contact, origin, tags, flag_reason, flag_until, approver, arrived_on"
                    + " FROM person WHERE sheet = ? AND id = ?", new String[] { "id" });
            ps.setLong(1, t);
            ps.setString(2, user);
            ps.setDate(3, java.sql.Date.valueOf(clock.today(sheet)));
            ps.setString(4, sheet);
            ps.setLong(5, f);
            return ps;
        }, keys);
        long kept = keys.getKey().longValue();
        jdbc.update("INSERT INTO person_merge_grade (merge_id, grade, from_date) SELECT ?, grade, from_date"
                + " FROM person_grade WHERE person_id = ?", kept, f);
        jdbc.update("INSERT INTO person_merge_terms (merge_id, terms, from_date) SELECT ?, terms, from_date"
                + " FROM person_terms WHERE person_id = ?", kept, f);
        return kept;
    }

    /** Whether the alias is one of the person's. */
    private boolean aliasOf(String sheet, long person, String alias) {
        Integer found = jdbc.queryForObject("SELECT COUNT(*) FROM person_alias WHERE sheet = ? AND person_id = ?"
                + " AND alias_key = LOWER(?)", Integer.class, sheet, person, alias);
        return found != null && found > 0;
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

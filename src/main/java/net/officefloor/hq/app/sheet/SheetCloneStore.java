package net.officefloor.hq.app.sheet;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimLogStore;
import net.officefloor.hq.app.claim.ClaimStore;
import net.officefloor.hq.app.claim.Numbering;
import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.setting.SettingStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Copies a sheet to a sheet never used: its today, settings, roles, rates, holidays, void reasons, templates,
 * people (grades, terms, contacts, aliases, holds, retirements, patrons), pools, periods (all open) and claims
 * (status, frozen figures, dates, notes, tags, references, logs and comments). Each claim that had a number takes
 * the clone's next by its own numbering, in the order the claims were opened (by the sheet's today when each was
 * opened, then in list order of their people and the order they were added). Statements (and the figures they
 * bound), period figures, watches, delegations, locks and bulk settle batches are not copied.
 */
@Repository
public class SheetCloneStore {

    private final JdbcTemplate jdbc;
    private final SheetStore sheets;
    private final PersonStore people;
    private final ClaimStore claims;
    private final ClaimLogStore log;
    private final SettingStore settings;
    private final PeriodStore periods;

    public SheetCloneStore(JdbcTemplate jdbc, SheetStore sheets, PersonStore people, ClaimStore claims,
            ClaimLogStore log, SettingStore settings, PeriodStore periods) {
        this.jdbc = jdbc;
        this.sheets = sheets;
        this.people = people;
        this.claims = claims;
        this.log = log;
        this.settings = settings;
        this.periods = periods;
    }

    public void cloneTo(String from, String to) throws InvalidSheetException {
        if (sheets.exists(to)) {
            throw new InvalidSheetException("The sheet " + to + " is already in use");
        }
        sheets.markLoaded(to);
        sheets.setToday(to, sheets.today(from));

        jdbc.update("INSERT INTO sheet_setting (sheet_id, setting_key, setting_value) "
                + "SELECT ?, setting_key, setting_value FROM sheet_setting WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO sheet_setting_from (sheet_id, setting_key, from_date, setting_value) "
                + "SELECT ?, setting_key, from_date, setting_value FROM sheet_setting_from WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO sheet_role (sheet_id, user_name, role) "
                + "SELECT ?, user_name, role FROM sheet_role WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO rate (sheet_id, grade, pct, from_date, to_date) "
                + "SELECT ?, grade, pct, from_date, to_date FROM rate WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO holiday (sheet_id, holiday_date) "
                + "SELECT ?, holiday_date FROM holiday WHERE sheet_id = ? ORDER BY id", to, from);
        jdbc.update("INSERT INTO void_reason (sheet_id, reason) "
                + "SELECT ?, reason FROM void_reason WHERE sheet_id = ? ORDER BY id", to, from);
        jdbc.update("INSERT INTO claim_template (sheet_id, name, amount, note) "
                + "SELECT ?, name, amount, note FROM claim_template WHERE sheet_id = ? ORDER BY id", to, from);
        jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date, open_cap) "
                + "SELECT ?, name, from_date, to_date, open_cap FROM period WHERE sheet_id = ? ORDER BY id", to, from);

        // people, in list order
        List<Person> theirs = people.list(from);
        Map<Long, Long> personIds = new HashMap<>();
        Map<Long, Integer> place = new HashMap<>();
        for (Person person : theirs) {
            place.put(person.getId(), place.size());
            personIds.put(person.getId(), copyPerson(person, to));
        }
        jdbc.query("SELECT person_id, patron_id FROM patron WHERE sheet_id = ? ORDER BY named", rs -> {
            Long person = personIds.get(rs.getLong("person_id"));
            Long patron = personIds.get(rs.getLong("patron_id"));
            if (person != null && patron != null) {
                jdbc.update("INSERT INTO patron (sheet_id, person_id, patron_id) VALUES (?, ?, ?)", to, person, patron);
            }
        }, from);

        // pools, then their members (people or other pools)
        Map<Long, Long> poolIds = new HashMap<>();
        jdbc.query("SELECT id, name FROM pool WHERE sheet_id = ? ORDER BY id", rs -> {
            poolIds.put(rs.getLong("id"), insert("INSERT INTO pool (sheet_id, name) VALUES (?, ?)", to, rs.getString("name")));
        }, from);
        jdbc.query("SELECT * FROM pool_member WHERE sheet_id = ? ORDER BY id", rs -> {
            Long pool = poolIds.get(rs.getLong("pool_id"));
            Long person = rs.getObject("person_id") == null ? null : personIds.get(rs.getLong("person_id"));
            Long member = rs.getObject("member_pool_id") == null ? null : poolIds.get(rs.getLong("member_pool_id"));
            if (pool != null && (person != null || member != null)) {
                jdbc.update("INSERT INTO pool_member (sheet_id, pool_id, person_id, member_pool_id, share, from_date, to_date)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?)", to, pool, person, member, rs.getBigDecimal("share"),
                        rs.getObject("from_date", LocalDate.class), rs.getObject("to_date", LocalDate.class));
            }
        }, from);

        // claims: numbered afresh in the order they were opened
        List<Claim> all = new ArrayList<>(claims.list(from).stream()
                .filter(c -> personIds.containsKey(c.getPersonId())).toList());
        Map<Long, LocalDate> opened = log.openedOn(from);
        List<Claim> numbered = new ArrayList<>(all.stream().filter(c -> c.getNumber() != null).toList());
        numbered.sort(Comparator.comparing((Claim c) -> opened.getOrDefault(c.getId(), c.getDate()))
                .thenComparingInt(c -> place.get(c.getPersonId()))
                .thenComparingLong(Claim::getId));
        Map<String, String> numbers = new HashMap<>();
        Map<Long, String> numberOf = new HashMap<>();
        for (Claim claim : numbered) {
            String number = Numbering.next(to, claim.getDueBy(), claims, settings, sheets, periods);
            numbers.putIfAbsent(claim.getNumber(), number);
            numberOf.put(claim.getId(), number);
        }
        // listed newest first: copy the oldest first, so they keep their order
        for (int i = all.size() - 1; i >= 0; i--) {
            Claim claim = all.get(i);
            long id = claims.copyTo(claim, to, personIds.get(claim.getPersonId()), numberOf.get(claim.getId()),
                    renumbered(claim.getCorrects(), numbers), renumbered(claim.getSplitFrom(), numbers));
            if (claim.getRef() != null) {
                jdbc.update("UPDATE claim SET ref = ? WHERE sheet_id = ? AND id = ?", claim.getRef(), to, id);
            }
        }
    }

    /** Copies the person with their grades, terms, contact, tags, aliases, holds and retirements; the id of the copy. */
    private long copyPerson(Person person, String to) {
        String from = person.getSheetId();
        long id = insert("INSERT INTO person (sheet_id, name, retired_from, terms, contact, tags, list_position, origin) "
                + "SELECT ?, name, retired_from, terms, contact, tags, list_position, origin FROM person "
                + "WHERE sheet_id = ? AND id = ?", to, from, person.getId());
        jdbc.update("INSERT INTO person_grade (sheet_id, person_id, from_date, grade) "
                + "SELECT ?, ?, from_date, grade FROM person_grade WHERE sheet_id = ? AND person_id = ?",
                to, id, from, person.getId());
        jdbc.update("INSERT INTO person_hold (sheet_id, person_id, from_date, to_date) "
                + "SELECT ?, ?, from_date, to_date FROM person_hold WHERE sheet_id = ? AND person_id = ? ORDER BY id",
                to, id, from, person.getId());
        jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias) "
                + "SELECT ?, ?, alias FROM person_alias WHERE sheet_id = ? AND person_id = ? ORDER BY id",
                to, id, from, person.getId());
        jdbc.update("INSERT INTO person_retirement (sheet_id, person_id, from_date, to_date) "
                + "SELECT ?, ?, from_date, to_date FROM person_retirement WHERE sheet_id = ? AND person_id = ? ORDER BY id",
                to, id, from, person.getId());
        return id;
    }

    private long insert(String sql, Object... args) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, new String[] { "ID" });
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i] instanceof LocalDate d ? Date.valueOf(d) : args[i]);
            }
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }

    /** The number a claim's number became on the clone; as it was when no claim had it. */
    private static String renumbered(String number, Map<String, String> numbers) {
        return number == null ? null : numbers.getOrDefault(number, number);
    }
}

package net.officefloor.hq.app.sheet;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimStatusChanger;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.Roles;
import net.officefloor.hq.app.rollup.Rollup;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

/**
 * Clones the sheet to another id; only a steward may, and only to a sheet never used. The clone gets this sheet's
 * today, settings (its parent among them), roles, rates (with their older versions), holidays, void reasons, templates, people (with their grades,
 * terms, contacts, aliases, holds, retirements and patrons, listed as they are), pools, periods (all open) and claims
 * (status, frozen figures, dates, notes, logs, comments and lines, listed as they are). Each claim that had a number takes the
 * clone's next one by the clone's own numbering, in the order the claims were opened (by the sheet's today when each
 * was opened, then the order they were copied: by person, then as listed); a correction or split names its claim's new
 * number. Statements are not copied (no claim is bound to one), nor are locks, settle batches, watches, delegations,
 * closed periods' totals or the sheet's as-of history.
 */
@Service
public class SheetCloner {

    /** A column's value written as SQL as it stands, not bound. */
    private record Sql(String expression) {
    }

    private static final Sql NULL = new Sql("NULL");

    private static final Sql FALSE = new Sql("FALSE");

    /** The tables whose rows show a sheet has been used. */
    private static final List<String> USED = List.of("sheet", "setting", "user_role", "rate", "person", "claim",
            "period", "holiday", "void_reason", "claim_template", "pool", "statement", "delegation", "sheet_archive");

    private final JdbcTemplate jdbc;
    private final Roles roles;
    private final SheetRegistry registry;
    private final SheetClock clock;
    private final Claims claims;
    private final ClaimStatusChanger changer;
    private final Audit audit;
    private final Rollup rollup;

    public SheetCloner(JdbcTemplate jdbc, Roles roles, SheetRegistry registry, SheetClock clock, Claims claims,
            ClaimStatusChanger changer, Audit audit, Rollup rollup) {
        this.jdbc = jdbc;
        this.roles = roles;
        this.registry = registry;
        this.clock = clock;
        this.claims = claims;
        this.changer = changer;
        this.audit = audit;
        this.rollup = rollup;
    }

    /** Clones the sheet to the sheet {@code to}, writing nothing when it is refused. */
    public void cloneTo(String sheet, String user, String to) throws InvalidCloneException {
        if (!Role.STEWARD.equals(roles.of(sheet, user))) {
            throw new InvalidCloneException("Only a steward may clone the sheet");
        }
        if (to.equals(sheet) || used(to)) {
            throw new InvalidCloneException("The sheet " + to + " has been used already");
        }

        // The sheet's today, settings, roles, rates, holidays, void reasons and templates.
        jdbc.update("INSERT INTO sheet (id, today) VALUES (?, ?)", to, Date.valueOf(clock.today(sheet)));
        copy("SETTING", Map.of("SHEET", to), "sheet = ?", sheet);
        jdbc.update("INSERT INTO sheet_parent (child, parent) SELECT ?, parent FROM sheet_parent WHERE child = ?", to,
                sheet);
        copy("USER_ROLE", Map.of("SHEET", to), "sheet = ?", sheet);
        copy("RATE", Map.of("SHEET", to), "sheet = ?", sheet);
        copy("RATE_VERSION", Map.of("SHEET", to), "sheet = ?", sheet);
        copy("HOLIDAY", Map.of("SHEET", to), "sheet = ?", sheet);
        copy("VOID_REASON", Map.of("SHEET", to), "sheet = ?", sheet);
        copy("CLAIM_TEMPLATE", Map.of("SHEET", to), "sheet = ?", sheet);

        // The periods, all open.
        copy("PERIOD", Map.of("SHEET", to, "CLOSED", FALSE, "SOFT", FALSE, "CLOSED_TOTAL", NULL), "sheet = ?", sheet);

        // The people, listed as they are, with their grades, holds, retirements and aliases.
        Map<Long, Long> people = new LinkedHashMap<>();
        for (Long f : jdbc.queryForList("SELECT id FROM person WHERE sheet = ? ORDER BY list_position, id", Long.class,
                sheet)) {
            long t = copy("PERSON", Map.of("SHEET", to), "sheet = ? AND id = ?", sheet, f);
            people.put(f, t);
            copy("PERSON_GRADE", Map.of("PERSON_ID", t), "person_id = ?", f);
            copy("PERSON_TERMS", Map.of("PERSON_ID", t), "person_id = ? ORDER BY from_date", f);
            copy("PERSON_HOLD", Map.of("PERSON_ID", t), "person_id = ? ORDER BY id", f);
            copy("PERSON_RETIREMENT", Map.of("PERSON_ID", t), "person_id = ? ORDER BY id", f);
            copy("PERSON_ALIAS", Map.of("SHEET", to, "PERSON_ID", t), "sheet = ? AND person_id = ?", sheet, f);
        }
        for (Map<String, Object> row : jdbc.queryForList(
                "SELECT person_id, patron_id FROM patron WHERE sheet = ? ORDER BY named", sheet)) {
            Long person = people.get(((Number) row.get("PERSON_ID")).longValue());
            Long patron = people.get(((Number) row.get("PATRON_ID")).longValue());
            if (person != null && patron != null) {
                jdbc.update("INSERT INTO patron (sheet, person_id, patron_id) VALUES (?, ?, ?)", to, person, patron);
            }
        }

        // The pools, their members and sub-pools.
        copy("POOL", Map.of("SHEET", to), "sheet = ?", sheet);
        for (Map.Entry<Long, Long> p : people.entrySet()) {
            copy("POOL_MEMBER", Map.of("SHEET", to, "PERSON_ID", p.getValue()), "sheet = ? AND person_id = ?", sheet,
                    p.getKey());
        }
        copy("POOL_SUB", Map.of("SHEET", to), "sheet = ?", sheet);

        // The claims, by person then as listed, each with its log, comments, lines and void reason; unbound from any
        // statement, unnumbered, and with no lock or settle batch.
        Map<Long, Long> copied = new LinkedHashMap<>();
        for (Map.Entry<Long, Long> p : people.entrySet()) {
            for (Long claim : jdbc.queryForList("SELECT id FROM claim WHERE sheet = ? AND person = ? ORDER BY listed, id",
                    Long.class, sheet, p.getKey())) {
                Map<String, Object> set = new HashMap<>();
                set.put("SHEET", to);
                set.put("PERSON", p.getValue());
                set.put("STATED", FALSE);
                set.put("STATED_DUE", NULL);
                set.put("NUMBER_ON_SHEET", NULL);
                set.put("PERIOD_NUMBER", NULL);
                set.put("LOCKED_BY", NULL);
                set.put("BATCH", NULL);
                long copy = copy("CLAIM", set, "sheet = ? AND id = ?", sheet, claim);
                copied.put(claim, copy);
                copy("CLAIM_LOG", Map.of("SHEET", to, "CLAIM", copy), "sheet = ? AND claim = ? ORDER BY id", sheet, claim);
                copy("CLAIM_COMMENT", Map.of("SHEET", to, "CLAIM", copy), "sheet = ? AND claim = ? ORDER BY id", sheet,
                        claim);
                copy("CLAIM_LINE", Map.of("SHEET", to, "CLAIM", copy), "sheet = ? AND claim = ? ORDER BY id", sheet, claim);
                copy("CLAIM_VOID", Map.of("SHEET", to, "CLAIM", copy), "sheet = ? AND claim = ?", sheet, claim);
            }
        }

        // Numbers: the clone's own, in the order the claims were opened (its today then, then the order copied).
        if (!copied.isEmpty()) {
            String in = copied.keySet().stream().map(String::valueOf).collect(Collectors.joining(","));
            Map<Long, String> shown = new HashMap<>();
            for (Map<String, Object> row : jdbc.queryForList("SELECT id, COALESCE(period_number,"
                    + " CAST(number_on_sheet AS VARCHAR)) AS shown FROM claim WHERE sheet = ? AND id IN (" + in + ")"
                    + " AND (period_number IS NOT NULL OR number_on_sheet IS NOT NULL)", sheet)) {
                shown.put(copied.get(((Number) row.get("ID")).longValue()), (String) row.get("SHOWN"));
            }
            String copies = copied.values().stream().map(String::valueOf).collect(Collectors.joining(","));
            Map<String, String> renumbered = new HashMap<>();
            for (Long copy : jdbc.queryForList("SELECT c.id FROM claim c WHERE c.sheet = ? AND c.id IN (" + copies + ")"
                    + " ORDER BY COALESCE((SELECT MIN(l.on_date) FROM claim_log l WHERE l.sheet = c.sheet"
                    + " AND l.claim = c.id AND l.status = 'OPEN'), c.claimed_on), c.id", Long.class, to)) {
                if (!shown.containsKey(copy)) {
                    continue;
                }
                var claim = changer.number(to, claims.find(to, copy));
                renumbered.put(shown.get(copy),
                        claim.periodNumber() != null ? claim.periodNumber() : String.valueOf(claim.number()));
            }
            // A correction or split names its claim's new number (none when that claim had none).
            for (Long copy : copied.values()) {
                Map<String, Object> row = jdbc.queryForMap("SELECT corrects, split_from FROM claim WHERE id = ?", copy);
                String corrects = (String) row.get("CORRECTS");
                String splitFrom = (String) row.get("SPLIT_FROM");
                if (corrects != null || splitFrom != null) {
                    jdbc.update("UPDATE claim SET corrects = ?, split_from = ? WHERE sheet = ? AND id = ?",
                            corrects == null ? null : renumbered.get(corrects),
                            splitFrom == null ? null : renumbered.get(splitFrom), to, copy);
                }
            }
        }
        registry.loaded(to);

        audit.record("SHEET_CLONE_MADE sheet=" + sheet + " by=" + user + " to=" + to);
        audit.record("SHEET_CLONED sheet=" + to + " by=" + user + " from=" + sheet);
        rollup.record(to, user, new Rollup.Snapshot(Map.of(), null, null, null));
    }

    /** Whether the sheet has ever been loaded or holds anything. */
    private boolean used(String sheet) {
        if (registry.exists(sheet)) {
            return true;
        }
        for (String table : USED) {
            String column = "sheet".equals(table) ? "id" : "sheet";
            if (jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?", Integer.class,
                    sheet) > 0) {
                return true;
            }
        }
        return !audit.lines(sheet).isEmpty();
    }

    /**
     * Copies the table's rows the condition picks, in the order it gives, with the columns given set to their values
     * (an {@link Sql} as it stands, anything else bound); returns the last row's generated id, or -1 when none.
     */
    private long copy(String table, Map<String, Object> set, String where, Object... whereArgs) {
        List<String> columns = jdbc.queryForList("SELECT column_name FROM information_schema.columns WHERE"
                + " table_schema = 'PUBLIC' AND table_name = ? AND is_identity = 'NO' AND is_generated = 'NEVER'"
                + " ORDER BY ordinal_position", String.class, table);
        boolean identity = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE"
                + " table_schema = 'PUBLIC' AND table_name = ? AND column_name = 'ID' AND is_identity = 'YES'",
                Integer.class, table) > 0;
        List<Object> args = new ArrayList<>();
        List<String> select = new ArrayList<>();
        for (String c : columns) {
            Object value = set.get(c);
            if (value == null) {
                select.add("c." + c);
            } else if (value instanceof Sql sql) {
                select.add(sql.expression());
            } else {
                select.add("?");
                args.add(value);
            }
        }
        args.addAll(List.of(whereArgs));
        String sql = "INSERT INTO " + table + " (" + String.join(", ", columns) + ") SELECT " + String.join(", ", select)
                + " FROM " + table + " c WHERE " + where;
        if (!identity) {
            jdbc.update(sql, args.toArray());
            return -1;
        }
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement(sql, new String[] { "id" });
            for (int i = 0; i < args.size(); i++) {
                ps.setObject(i + 1, args.get(i));
            }
            return ps;
        }, keys);
        List<Map<String, Object>> generated = keys.getKeyList();
        return generated.isEmpty() ? -1 : ((Number) generated.get(generated.size() - 1).values().iterator().next())
                .longValue();
    }
}

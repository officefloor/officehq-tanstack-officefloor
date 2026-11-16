package net.officefloor.hq.app.sheet;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimNumbers;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Copies a sheet into a new, never used one. The clone gets the sheet's today, settings, roles, rates,
 * holidays, void reasons, templates, tags and retired grades, its people (with their grades, terms, contacts, aliases,
 * holds, retirements and patrons), pools with their members, crews with their members, periods (all open, their fixed totals
 * dropped) and claims (with their status, frozen figures, dates, notes, tags, logs and comments) and
 * the history the sheet keeps of them. Statements are not copied: every statement count starts at 0
 * and no figure a statement bound is kept. Watches, delegations, claim locks and settle batches are not
 * copied either. Each claim that had a number gets the clone's next, by the clone's own numbering, in
 * the order the originals were opened (by the sheet's today then, then the order they were added).
 */
@Repository
public class SheetCloner {

    private final JdbcTemplate jdbc;

    /** The columns of each table the store fills itself (generated or identity), by table. */
    private final Map<String, List<String>> filled = new HashMap<>();

    public SheetCloner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void cloneSheet(String from, String to, String user, ClaimRepository claims,
            PeriodRepository periods, SettingRepository settings, SheetRepository sheets) {
        jdbc.update("INSERT INTO sheet (id, created_by, today) SELECT ?, ?, today FROM sheet WHERE id = ?",
                to, user, from);
        jdbc.update("INSERT INTO sheet_source (sheet_id, source_id) VALUES (?, ?)", to, from);
        Map<String, Object> none = Map.of();
        for (String table : List.of("sheet_setting", "setting_history", "sheet_role", "rate", "rate_history", "rate_version",
                "holiday", "void_reason", "claim_template", "sheet_tag", "grade_retired")) {
            copy(table, from, to, Map.of(), none);
        }
        Map<String, Object> unstated = new HashMap<>();
        unstated.put("statement_count", 0);
        unstated.put("stated_total", null);
        unstated.put("statement_date", null);
        unstated.put("arrived_on", null); // a clone did not arrive by transfer: nothing there to undo
        unstated.put("version", 0);
        Map<Long, Long> people = copy("person", from, to, Map.of(), unstated);
        Map<String, Map<Long, Long>> byPerson = Map.of("person_id", people);
        for (String table : List.of("person_grade", "person_terms", "person_grade_history", "person_retired_history",
                "person_retirement", "person_hold", "person_alias")) {
            copy(table, from, to, byPerson, none);
        }
        copy("person_patron", from, to, Map.of("person_id", people, "patron_id", people), none);

        Map<Long, Long> pools = copy("pool", from, to, Map.of(), none);
        Map<String, Map<Long, Long>> members = new HashMap<>();
        members.put("pool_id", pools);
        members.put("person_id", people);
        members.put("member_pool_id", pools);
        copy("pool_member", from, to, members, none);

        Map<Long, Long> crews = copy("crew", from, to, Map.of("lead_id", people), none);
        copy("crew_member", from, to, Map.of("crew_id", crews, "person_id", people), none);

        Map<String, Object> open = new HashMap<>();
        open.put("closed", false);
        open.put("soft_closed", false);
        open.put("closed_total", null);
        open.put("closed_by", null);
        open.put("closed_at", null);
        copy("period", from, to, Map.of(), open);

        Map<String, Object> fresh = new HashMap<>();
        fresh.put("number", null);
        fresh.put("stated", false);
        fresh.put("stated_due", null);
        fresh.put("locked_by", null);
        fresh.put("settle_batch", null);
        Map<Long, Long> copied = copy("claim", from, to, byPerson, fresh);
        Map<String, Map<Long, Long>> byClaim = Map.of("claim_id", copied);
        copy("claim_log", from, to, byClaim, none);
        copy("claim_comment", from, to, byClaim, none);
        copy("claim_line", from, to, byClaim, none);
        copy("claim_share", from, to, Map.of("claim_id", copied, "person_id", people), none);
        Map<String, Object> unbound = new HashMap<>();
        unbound.put("stated", false);
        unbound.put("stated_due", null);
        copy("claim_history", from, to, Map.of("claim_id", copied, "person_id", people), unbound);

        // A forward-only rate applies to the claims added after it was saved: the copies of those now.
        for (String table : List.of("rate", "rate_version", "rate_history")) {
            for (Long after : jdbc.queryForList("SELECT DISTINCT claims_after FROM " + table
                    + " WHERE sheet_id = ? AND claims_after IS NOT NULL", Long.class, to)) {
                long now = copied.entrySet().stream().filter((e) -> e.getKey() <= after)
                        .mapToLong(Map.Entry::getValue).max().orElse(0);
                jdbc.update("UPDATE " + table + " SET claims_after = ? WHERE sheet_id = ? AND claims_after = ?",
                        -now - 1, to, after);
            }
            jdbc.update("UPDATE " + table + " SET claims_after = -claims_after - 1 WHERE sheet_id = ? "
                    + "AND claims_after < 0", to);
        }

        // Number afresh, in the order the originals were opened, by the clone's own numbering.
        Map<String, String> numbers = new HashMap<>();
        for (Map<String, Object> row : jdbc.queryForList("SELECT c.id, c.number FROM claim c "
                + "JOIN person p ON p.id = c.person_id WHERE c.sheet_id = ? AND c.number IS NOT NULL "
                + "ORDER BY COALESCE((SELECT MIN(l.on_date) FROM claim_log l WHERE l.claim_id = c.id AND "
                + "l.status = 'OPEN'), (SELECT MIN(l.on_date) FROM claim_log l WHERE l.claim_id = c.id AND "
                + "l.status <> 'DRAFT'), c.claim_date), p.list_pos, p.id, c.id", from)) {
            Claim claim = claims.find(to, copied.get(((Number) row.get("id")).longValue()));
            claims.renumber(claim, ClaimNumbers.next(claim, claims, periods, settings, sheets));
            numbers.put((String) row.get("number"), claim.getNumber());
        }
        // A correction or a split names the claim it came from by its number: by its new one now.
        for (String column : List.of("corrects", "split_from")) {
            for (Map<String, Object> row : jdbc.queryForList("SELECT id, " + column + " AS was FROM claim "
                    + "WHERE sheet_id = ? AND " + column + " IS NOT NULL", to)) {
                String now = numbers.get((String) row.get("was"));
                jdbc.update("UPDATE claim SET " + column + " = ? WHERE sheet_id = ? AND id = ?",
                        now, to, row.get("id"));
            }
        }
    }

    /** Copy the sheet's rows of the table to the clone, in the order they were added: each id column in
     *  {@code remap} given the clone's id for it (a row whose id has none is left out), each column in
     *  {@code set} that value. The clone's ids of the rows copied, by the original's, when they have one. */
    private Map<Long, Long> copy(String table, String from, String to, Map<String, Map<Long, Long>> remap,
            Map<String, Object> set) {
        List<String> skip = filled(table);
        boolean keyed = skip.contains("id");
        Map<Long, Long> ids = new LinkedHashMap<>();
        rows:
        for (Map<String, Object> row : jdbc.queryForList("SELECT * FROM " + table + " WHERE sheet_id = ?"
                + (keyed ? " ORDER BY id" : ""), from)) {
            List<String> columns = new ArrayList<>();
            List<Object> values = new ArrayList<>();
            for (Map.Entry<String, Object> column : row.entrySet()) {
                String name = column.getKey().toLowerCase(Locale.ROOT);
                if (skip.contains(name)) continue;
                Object value = column.getValue();
                if (name.equals("sheet_id")) {
                    value = to;
                } else if (set.containsKey(name)) {
                    value = set.get(name);
                } else if (remap.containsKey(name) && value != null) {
                    value = remap.get(name).get(((Number) value).longValue());
                    if (value == null) continue rows;
                }
                columns.add(name);
                values.add(value);
            }
            String sql = "INSERT INTO " + table + " (" + String.join(", ", columns) + ") VALUES ("
                    + String.join(", ", columns.stream().map((c) -> "?").toList()) + ")";
            GeneratedKeyHolder key = new GeneratedKeyHolder();
            jdbc.update(con -> {
                PreparedStatement ps = keyed ? con.prepareStatement(sql, new String[] { "id" }) : con.prepareStatement(sql);
                for (int i = 0; i < values.size(); i++) ps.setObject(i + 1, values.get(i));
                return ps;
            }, key);
            if (keyed) ids.put(((Number) row.get("id")).longValue(), key.getKey().longValue());
        }
        return ids;
    }

    /** The table's columns the store fills itself: generated from other columns, or its identity. */
    private List<String> filled(String table) {
        return filled.computeIfAbsent(table, (t) -> jdbc.queryForList("SELECT LOWER(COLUMN_NAME) FROM "
                + "INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = ? AND "
                + "(IS_GENERATED = 'ALWAYS' OR IS_IDENTITY = 'YES')", String.class, t.toUpperCase(Locale.ROOT)));
    }
}

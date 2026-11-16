package net.officefloor.hq.app.sheet;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimNumbering;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Copies a sheet to another, never used, id. The copy takes the sheet's today, settings, roles, rates, holidays, void
 * reasons, templates, people (with their grades, terms, contacts, aliases, holds, retirements and patrons), pools,
 * periods (all open, no figures fixed) and claims (status, frozen due, dates, notes and their logs, unbound from any
 * statement). Statements, watches, delegations, settle batches and archiving are not copied. The copy's claims are
 * numbered afresh by its own numbering, in the order the originals were opened: by the sheet's today when each was
 * opened, then by the order they were copied in (people in list order, each one's claims as added).
 */
@Repository
public class CloneRepository {

    /** The tables kept per sheet: a sheet with a row in any of them (or one loaded) has been used. */
    private static final List<String> SHEET_TABLES = List.of("person", "claim", "rate", "setting", "user_role", "period",
            "pool", "holiday", "void_reason", "claim_template", "delegation", "statement", "sheet_archived", "watch");

    private final JdbcTemplate jdbc;
    private final SheetRepository sheets;
    private final ClaimRepository claims;
    private final PeriodRepository periods;
    private final SettingRepository settings;

    public CloneRepository(JdbcTemplate jdbc, SheetRepository sheets, ClaimRepository claims, PeriodRepository periods,
            SettingRepository settings) {
        this.jdbc = jdbc;
        this.sheets = sheets;
        this.claims = claims;
        this.periods = periods;
        this.settings = settings;
    }

    /** Whether the sheet has never been used: never loaded, never changed, nothing kept for it. */
    public boolean unused(String id) {
        if (sheets.exists(id)) {
            return false;
        }
        for (String table : SHEET_TABLES) {
            Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE sheet_id = ?", Integer.class, id);
            if (n != null && n > 0) {
                return false;
            }
        }
        return true;
    }

    /** Copies sheet {@code from} to the unused sheet {@code to} (see the class comment); from then on {@code to} exists. */
    public void copy(String from, String to) {
        sheets.saveToday(to, sheets.today(from));
        sheets.markLoaded(to);
        for (String table : List.of("setting", "setting_log", "user_role", "rate", "rate_log", "rate_version", "holiday", "void_reason",
                "claim_template")) {
            copy(table, "WHERE sheet_id = ?" + (hasColumn(table, "id") ? " ORDER BY id" : ""), from, to, Map.of());
        }

        Map<Object, Object> people = copy("person", "WHERE sheet_id = ? ORDER BY id", from, to, Map.of());
        Map<String, Function<Map<String, Object>, Object>> ofPerson = Map.of("person_id", (r) -> people.get(r.get("person_id")));
        for (String table : List.of("person_grade", "person_grade_log", "person_terms_log", "person_alias", "person_hold",
                "person_retire_log", "person_retirement")) {
            copy(table, "WHERE sheet_id = ? ORDER BY " + (hasColumn(table, "id") ? "id" : "person_id"), from, to, ofPerson);
        }
        copy("patron", "WHERE sheet_id = ? ORDER BY named_seq", from, to, Map.of(
                "person_id", (r) -> people.get(r.get("person_id")),
                "patron_id", (r) -> people.get(r.get("patron_id"))));

        Map<Object, Object> pools = copy("pool", "WHERE sheet_id = ? ORDER BY id", from, to, Map.of());
        Map<String, Function<Map<String, Object>, Object>> member = new HashMap<>();
        member.put("pool_id", (r) -> pools.get(r.get("pool_id")));
        member.put("person_id", (r) -> r.get("person_id") == null ? null : people.get(r.get("person_id")));
        member.put("member_pool_id", (r) -> r.get("member_pool_id") == null ? null : pools.get(r.get("member_pool_id")));
        copy("pool_member", "WHERE pool_id IN (SELECT id FROM pool WHERE sheet_id = ?) ORDER BY id", from, null, member);

        Map<Object, Object> crews = copy("crew", "WHERE sheet_id = ? ORDER BY id", from, to,
                Map.of("lead_id", (r) -> people.get(r.get("lead_id"))));
        copy("crew_member", "WHERE crew_id IN (SELECT id FROM crew WHERE sheet_id = ?) ORDER BY id", from, null,
                Map.of("crew_id", (r) -> crews.get(r.get("crew_id")), "person_id", (r) -> people.get(r.get("person_id"))));

        Map<String, Function<Map<String, Object>, Object>> open = new HashMap<>();
        open.put("closed", (r) -> false);
        open.put("soft_closed", (r) -> false);
        open.put("closed_total", (r) -> null);
        copy("period", "WHERE sheet_id = ? ORDER BY id", from, to, open);

        Map<String, Function<Map<String, Object>, Object>> claim = new HashMap<>();
        claim.put("person_id", (r) -> people.get(r.get("person_id")));
        claim.put("claim_number", (r) -> null);
        claim.put("stated", (r) -> false);
        claim.put("stated_due", (r) -> null);
        claim.put("locked_by", (r) -> null);
        Map<Object, Object> copied = copy("claim", "c WHERE c.sheet_id = ? ORDER BY "
                + "(SELECT p.position FROM person p WHERE p.id = c.person_id), c.person_id, c.id", from, to, claim);
        Map<String, Function<Map<String, Object>, Object>> ofClaim = Map.of("claim_id", (r) -> copied.get(r.get("claim_id")));
        for (String table : List.of("claim_log", "claim_edit", "claim_comment", "claim_line", "claim_reminder")) {
            copy(table, "WHERE sheet_id = ? ORDER BY id", from, to, ofClaim);
        }
        copy("claim_void_reason", "WHERE sheet_id = ?", from, to, ofClaim);
        copy("claim_proof_log", "WHERE claim_id IN (SELECT id FROM claim WHERE sheet_id = ?) ORDER BY id", from, null, ofClaim);
        copy("claim_defer_log", "WHERE claim_id IN (SELECT id FROM claim WHERE sheet_id = ?) ORDER BY id", from, null, ofClaim);

        renumber(from, to, copied);
    }

    /** Gives the copied claims that had a number one afresh, by the copy's own numbering, in the order the originals
     *  were opened; a correction or split names the claim it corrects or was split from by its new number. */
    private void renumber(String from, String to, Map<Object, Object> copied) {
        record Numbered(long id, long copy, String number, LocalDate opened) {
        }
        List<Numbered> numbered = new ArrayList<>();
        jdbc.query("SELECT c.id, c.claim_number, c.claim_date, (SELECT MIN(l.log_date) FROM claim_log l WHERE l.sheet_id = c.sheet_id "
                + "AND l.claim_id = c.id AND l.status = 'OPEN') AS opened FROM claim c WHERE c.sheet_id = ? AND c.claim_number IS NOT NULL",
                (rs) -> {
                    LocalDate opened = rs.getObject("opened", LocalDate.class);
                    long id = rs.getLong("id");
                    numbered.add(new Numbered(id, ((Number) copied.get(id)).longValue(), rs.getString("claim_number"),
                            opened == null ? rs.getObject("claim_date", LocalDate.class) : opened));
                }, from);
        numbered.sort(Comparator.comparing(Numbered::opened).thenComparingLong(Numbered::copy));
        Map<String, String> renumbered = new HashMap<>();
        for (Numbered n : numbered) {
            Claim claim = claims.find(to, n.copy());
            String number = ClaimNumbering.next(claim, claims, periods, settings, sheets);
            claims.renumber(claim, number);
            renumbered.put(n.number(), number);
        }
        for (String column : List.of("corrects", "split_from")) {
            jdbc.query("SELECT id, " + column + " FROM claim WHERE sheet_id = ? AND " + column + " IS NOT NULL", (rs) -> {
                String was = rs.getString(2);
                if (renumbered.containsKey(was)) {
                    jdbc.update("UPDATE claim SET " + column + " = ? WHERE id = ?", renumbered.get(was), rs.getLong(1));
                }
            }, to);
        }
    }

    /**
     * Copies the rows of {@code table} picked by {@code where} (its one parameter: the sheet copied from) in that order,
     * each with every stored column as it was but its own id (given afresh), its sheet ({@code to}, when not null) and
     * the columns {@code overrides} works out from the row. Returns the new id of each row by its old id (tables with an
     * id of their own).
     */
    private Map<Object, Object> copy(String table, String where, String from, String to,
            Map<String, Function<Map<String, Object>, Object>> overrides) {
        List<String> columns = jdbc.queryForList("SELECT LOWER(COLUMN_NAME) FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = ? AND IS_GENERATED = 'NEVER' AND IS_IDENTITY = 'NO' "
                + "ORDER BY ORDINAL_POSITION", String.class, table.toUpperCase(Locale.ROOT));
        boolean ownId = hasColumn(table, "id") && !columns.contains("id");
        String insert = "INSERT INTO " + table + " (" + String.join(", ", columns) + ") VALUES ("
                + String.join(", ", columns.stream().map((c) -> "?").toList()) + ")";
        Map<Object, Object> ids = new HashMap<>();
        for (Map<String, Object> row : jdbc.queryForList("SELECT * FROM " + table + " " + where, from)) {
            Object[] values = new Object[columns.size()];
            for (int i = 0; i < values.length; i++) {
                String column = columns.get(i);
                Function<Map<String, Object>, Object> override = overrides.get(column);
                values[i] = override != null ? override.apply(row)
                        : column.equals("sheet_id") && to != null ? to
                        : row.get(column);
                if (values[i] instanceof LocalDate date) {
                    values[i] = Date.valueOf(date);
                }
            }
            if (!ownId) {
                jdbc.update(insert, values);
                continue;
            }
            GeneratedKeyHolder keys = new GeneratedKeyHolder();
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(insert, new String[] { "id" });
                for (int i = 0; i < values.length; i++) {
                    ps.setObject(i + 1, values[i]);
                }
                return ps;
            }, keys);
            ids.put(row.get("id"), keys.getKey().longValue());
        }
        return ids;
    }

    private boolean hasColumn(String table, String column) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'PUBLIC' "
                + "AND TABLE_NAME = ? AND COLUMN_NAME = ?", Integer.class, table.toUpperCase(Locale.ROOT),
                column.toUpperCase(Locale.ROOT));
        return n != null && n > 0;
    }
}

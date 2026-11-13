package net.officefloor.hq.app.sheet;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.RowCopy;
import net.officefloor.hq.app.claim.ClaimNumbers;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Clones a sheet to a new id: the clone gets its today, settings, roles, rates, holidays, void reasons, templates,
 * people (grades, terms, contacts, aliases, tags, holds, retirements, patrons), pools, periods (all open) and claims
 * (status, frozen figures, dates, notes, logs). Statements, watches, comments, locks, batches and as-of days are not
 * copied. Claims that had a number take the clone's own numbers afresh, in the order they were opened.
 */
@Repository
public class SheetCloner {

    /** The tables kept per sheet that are copied as they are, under the clone's id. */
    private static final List<String> SHEET_TABLES = List.of("user_role", "permission", "delegation", "setting_from",
            "grade_terms", "rate", "holiday", "void_reason", "claim_template", "claim_tag");

    /** The tables of a person's own entries, copied under the clone's id and the person's id there. */
    private static final List<String> PERSON_TABLES = List.of("person_grade", "person_hold", "person_alias",
            "person_retirement");

    private final JdbcTemplate jdbc;

    private final ClaimRepository claims;

    private final SheetRepository sheets;

    private final SettingRepository settings;

    private final PeriodRepository periods;

    public SheetCloner(JdbcTemplate jdbc, ClaimRepository claims, SheetRepository sheets, SettingRepository settings,
            PeriodRepository periods) {
        this.jdbc = jdbc;
        this.claims = claims;
        this.sheets = sheets;
        this.settings = settings;
        this.periods = periods;
    }

    /** Clone the sheet {@code from} to the never-used id {@code to}. */
    public void cloneSheet(String from, String to) {
        Map<String, Object> sheet = new HashMap<>();
        sheet.put("id", to);
        sheet.put("archived", false);
        sheet.put("last_claim_number", 0);
        sheet.put("last_batch", 0);
        RowCopy.copyAll(jdbc, "sheet", "id = ?", List.of(from), null, sheet);
        Map<String, Object> onClone = Map.of("sheet_id", to);
        for (String table : SHEET_TABLES) {
            RowCopy.copyAll(jdbc, table, "sheet_id = ?", List.of(from), null, onClone);
        }
        Map<String, Object> open = new HashMap<>(onClone);
        open.put("closed", false);
        open.put("soft_closed", false);
        open.put("closed_total", null);
        RowCopy.copyAll(jdbc, "period", "sheet_id = ?", List.of(from), "id", open);

        Map<Long, Long> people = copyPeople(from, to);
        copyPools(from, to, people);
        copyClaims(from, to, people);
    }

    /** Copy the people in their order in the list, with their own entries and patron links; their ids there by id here. */
    private Map<Long, Long> copyPeople(String from, String to) {
        Map<Long, Long> ids = new HashMap<>();
        for (long old : jdbc.queryForList("SELECT id FROM person WHERE sheet_id = ? ORDER BY position, id", Long.class,
                from)) {
            Map<String, Object> changed = new HashMap<>();
            changed.put("sheet_id", to);
            changed.put("statement_count", 0);
            changed.put("stated_total", null);
            changed.put("statement_date", null);
            long id = RowCopy.copy(jdbc, "person", old, changed);
            ids.put(old, id);
            for (String table : PERSON_TABLES) {
                RowCopy.copyAll(jdbc, table, "sheet_id = ? AND person_id = ?", List.of(from, old),
                        table.equals("person_grade") ? null : "id", Map.of("sheet_id", to, "person_id", id));
            }
        }
        for (Map<String, Object> link : jdbc.queryForList(
                "SELECT person_id, patron_id FROM person_patron WHERE sheet_id = ? ORDER BY seq", from)) {
            long person = ((Number) link.get("person_id")).longValue();
            long patron = ((Number) link.get("patron_id")).longValue();
            RowCopy.copyAll(jdbc, "person_patron", "sheet_id = ? AND person_id = ? AND patron_id = ?",
                    List.of(from, person, patron), null,
                    Map.of("sheet_id", to, "person_id", ids.get(person), "patron_id", ids.get(patron)));
        }
        return ids;
    }

    /** Copy the pools and their members (people and pools), as they were added. */
    private void copyPools(String from, String to, Map<Long, Long> people) {
        Map<Long, Long> pools = new HashMap<>();
        for (long old : jdbc.queryForList("SELECT id FROM pool WHERE sheet_id = ? ORDER BY id", Long.class, from)) {
            pools.put(old, RowCopy.copy(jdbc, "pool", old, Map.of("sheet_id", to)));
        }
        List<Map<String, Object>> members = jdbc.queryForList("SELECT m.id, m.pool_id, m.person_id, m.member_pool_id FROM"
                + " pool_member m JOIN pool p ON p.id = m.pool_id WHERE p.sheet_id = ? ORDER BY m.id", from);
        for (Map<String, Object> member : members) {
            Map<String, Object> changed = new HashMap<>();
            long id = ((Number) member.get("id")).longValue();
            changed.put("pool_id", pools.get(((Number) member.get("pool_id")).longValue()));
            Number person = (Number) member.get("person_id");
            Number pool = (Number) member.get("member_pool_id");
            changed.put("person_id", person == null ? null : people.get(person.longValue()));
            changed.put("member_pool_id", pool == null ? null : pools.get(pool.longValue()));
            RowCopy.copy(jdbc, "pool_member", id, changed);
        }
    }

    /**
     * Copy the claims, person by person in the order of the list and each person's in the order they were added, with
     * their logs and lines; figures a statement bound are lost. Each claim that had a number takes the clone's next one, in the
     * order they were opened (the sheet's today then, then the order copied); a correction or split naming one of them
     * names its new number.
     */
    private void copyClaims(String from, String to, Map<Long, Long> people) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT c.id, c.person_id, c.claim_number, c.claim_date,"
                + " (SELECT MIN(l.log_date) FROM claim_log l WHERE l.claim_id = c.id AND l.status = 'OPEN') AS opened"
                + " FROM claim c JOIN person p ON p.id = c.person_id WHERE c.sheet_id = ? ORDER BY p.position, p.id, c.id",
                from);
        List<ClaimRepository.Moved> copied = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            long old = ((Number) row.get("id")).longValue();
            Map<String, Object> changed = new HashMap<>();
            changed.put("sheet_id", to);
            changed.put("person_id", people.get(((Number) row.get("person_id")).longValue()));
            changed.put("stated", false);
            changed.put("stated_due", null);
            changed.put("batch", null);
            long id = RowCopy.copy(jdbc, "claim", old, changed);
            RowCopy.copyAll(jdbc, "claim_log", "claim_id = ?", List.of(old), "id",
                    Map.of("sheet_id", to, "claim_id", id));
            RowCopy.copyAll(jdbc, "claim_line", "claim_id = ?", List.of(old), "id",
                    Map.of("sheet_id", to, "claim_id", id));
            Object opened = row.get("opened") != null ? row.get("opened") : row.get("claim_date");
            copied.add(new ClaimRepository.Moved(id, (String) row.get("claim_number"),
                    opened instanceof Date d ? d.toLocalDate() : (LocalDate) opened));
        }
        Map<String, String> numbers = new HashMap<>();
        for (ClaimRepository.Moved m : copied.stream().filter((m) -> m.number() != null)
                .sorted(Comparator.comparing(ClaimRepository.Moved::opened).thenComparing(ClaimRepository.Moved::id))
                .toList()) {
            numbers.put(m.number(), ClaimNumbers.next(claims.find(to, m.id()), sheets, settings, periods));
        }
        claims.renumberTransferred(to, copied, numbers);
    }
}

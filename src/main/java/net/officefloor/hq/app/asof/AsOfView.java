package net.officefloor.hq.app.asof;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.TreeMap;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimResponse;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.period.PeriodResponse;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonResponse;
import net.officefloor.hq.app.rate.RateResponse;
import net.officefloor.hq.app.setting.DatedValue;
import net.officefloor.hq.app.setting.DelegationResponse;
import net.officefloor.hq.app.setting.RoleResponse;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.SettingsResponse;
import net.officefloor.hq.app.setting.ValidateSetting;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.hq.app.sheet.TodayResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

/**
 * The sheet as it stood at the end of a date, for a user's as-of view: every change made while the sheet's today was
 * on or before the date counts (the latest of each winning), every later one does not (see the history tables of V45
 * and V46), and the date stands in for the sheet's today. Each read gives the same shape the live read does, so the
 * screen works its figures from it unchanged.
 */
@Repository
public class AsOfView {

    /** A history row counts as of the date: made on or before it, or carried over from before history was kept. */
    private static final String UP_TO = " AND (set_on IS NULL OR set_on <= ?)";

    private static final Comparator<String> BY_NAME =
            String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder());

    private final JdbcTemplate jdbc;
    private final Sheets sheets;
    private final People people;
    private final Claims claims;
    private final Periods periods;

    public AsOfView(JdbcTemplate jdbc, Sheets sheets, People people, Claims claims, Periods periods) {
        this.jdbc = jdbc;
        this.sheets = sheets;
        this.people = people;
        this.claims = claims;
        this.periods = periods;
    }

    /** The as-of date asked for ("" or absent: none, the live view); null when none. */
    public static LocalDate parse(String asOf) {
        if (asOf == null || asOf.isBlank()) return null;
        try {
            return LocalDate.parse(asOf.trim());
        } catch (java.time.format.DateTimeParseException bad) {
            return null;
        }
    }

    /** The date standing for the sheet's today, and whether the sheet was archived by its end. */
    public TodayResponse today(String sheetId, LocalDate date) {
        sheets.today(sheetId);
        Date archivedOn = jdbc.queryForObject("SELECT archived_on FROM sheet WHERE id = ?", Date.class, sheetId);
        boolean archived = sheets.archived(sheetId) && (archivedOn == null || !archivedOn.toLocalDate().isAfter(date));
        return new TodayResponse(sheetId, date.toString(), archived);
    }

    /** The people on the sheet by the end of the date, each as they stood then, in list order (by place). */
    public List<PersonResponse> people(String sheetId, LocalDate date) {
        Date end = Date.valueOf(date);
        List<Person> found = new ArrayList<>();
        for (Person now : people.list(sheetId)) {
            if (now.getAddedOn() == null || !now.getAddedOn().isAfter(date)) found.add(now);
        }
        // Those merged away since still stood on the sheet.
        for (Person merged : people.mergedAfter(sheetId, date)) {
            if (merged.getAddedOn() == null || !merged.getAddedOn().isAfter(date)) found.add(merged);
        }
        found.sort(Comparator.comparingLong(Person::getPlace).thenComparingLong(Person::getId));
        Map<Long, Person> byId = new LinkedHashMap<>();
        for (Person now : found) {
            Person then = new Person(now.getId(), sheetId, now.getName(), now.getGrades().get(now.getGrades().firstKey()),
                    now.getVersion());
            then.setTerms(now.getTerms());
            then.setContact(now.getContact());
            // A person's tags are not kept in their history: they show as they are now.
            then.setTags(now.getTags());
            byId.put(now.getId(), then);
        }
        jdbc.query("SELECT person_id, name, terms, contact FROM person_history WHERE sheet_id = ?" + UP_TO + " ORDER BY seq",
                (RowCallbackHandler) rs -> {
                    Person then = byId.get(rs.getLong(1));
                    if (then == null) return;
                    then.setName(rs.getString(2));
                    int terms = rs.getInt(3);
                    then.setTerms(rs.wasNull() ? null : terms);
                    then.setContact(rs.getString(4));
                }, sheetId, end);
        Map<Long, Map<LocalDate, String>> grades = new HashMap<>();
        jdbc.query("SELECT person_id, from_date, grade FROM grade_history WHERE sheet_id = ?" + UP_TO + " ORDER BY seq",
                (RowCallbackHandler) rs -> {
                    LocalDate from = local(rs.getDate(2));
                    grades.computeIfAbsent(rs.getLong(1), k -> new LinkedHashMap<>())
                            .put(from == null ? Person.BEGINNING : from, rs.getString(3));
                }, sheetId, end);
        Map<Long, List<Person.Hold>> holds = new HashMap<>();
        jdbc.query("SELECT person_id, from_date, to_date FROM person_hold WHERE sheet_id = ?" + UP_TO + " ORDER BY seq",
                (RowCallbackHandler) rs -> holds.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>())
                        .add(new Person.Hold(rs.getDate(2).toLocalDate(), local(rs.getDate(3)))), sheetId, end);
        Map<Long, List<Person.Retirement>> retirements = new HashMap<>();
        jdbc.query("SELECT person_id, from_date, to_date FROM person_retirement WHERE sheet_id = ?" + UP_TO + " ORDER BY seq",
                (RowCallbackHandler) rs -> retirements.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>())
                        .add(new Person.Retirement(rs.getDate(2).toLocalDate(), rs.getDate(3).toLocalDate())), sheetId, end);
        // A snapshot is taken on the day the sheet moves to (dated the day it left): one taken after the date was not yet
        // taken at its end (a line kept without that day: taken once its own day was left).
        Map<Long, List<String>> snapshots = new HashMap<>();
        jdbc.query("SELECT person_id, set_on, due_total FROM person_snapshot WHERE sheet_id = ?"
                + " AND (taken_on <= ? OR (taken_on IS NULL AND set_on < ?)) ORDER BY seq",
                (RowCallbackHandler) rs -> snapshots.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>())
                        .add(rs.getDate(2).toLocalDate() + " " + rs.getString(3)), sheetId, end, end);
        Map<Long, LocalDate> retired = new HashMap<>();
        jdbc.query("SELECT person_id, retired_from FROM retire_history WHERE sheet_id = ?" + UP_TO + " ORDER BY seq",
                (RowCallbackHandler) rs -> retired.put(rs.getLong(1), local(rs.getDate(2))), sheetId, end);
        // The statements taken by the end of the date and not cancelled by then: how many, and the last one's figures.
        // (A statement a merge or a transfer brought over counts, and shows among the lines, but is never the last one.)
        Map<Long, Integer> statementCount = new HashMap<>();
        jdbc.query("SELECT person_id, stated_total, statement_date, brought FROM person_statement WHERE sheet_id = ?" + UP_TO
                + " AND NOT (cancelled AND cancelled_on <= ?) ORDER BY seq", (RowCallbackHandler) rs -> {
                    Person then = byId.get(rs.getLong(1));
                    if (then == null) return;
                    statementCount.merge(then.getId(), 1, Integer::sum);
                    if (rs.getBoolean(4)) return;
                    then.setStatedTotal(rs.getBigDecimal(2));
                    then.setStatementDate(local(rs.getDate(3)));
                }, sheetId, end, end);
        Map<Long, List<String>> statements = new HashMap<>();
        jdbc.query("SELECT person_id, statement_date, stated_total FROM person_statement WHERE sheet_id = ?" + UP_TO
                + " AND NOT (cancelled AND cancelled_on <= ?) ORDER BY statement_date NULLS FIRST, set_on NULLS FIRST, seq",
                (RowCallbackHandler) rs -> statements.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>())
                        .add(Person.statementLine(rs.getDate(2), rs.getBigDecimal(3))), sheetId, end, end);
        Map<Long, List<Long>> patrons = new HashMap<>();
        jdbc.query("SELECT person_id, patron_id FROM person_patron WHERE sheet_id = ?" + UP_TO + " ORDER BY seq",
                (RowCallbackHandler) rs -> patrons.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>()).add(rs.getLong(2)),
                sheetId, end);
        Map<Long, List<String>> aliases = new HashMap<>();
        jdbc.query("SELECT person_id, alias FROM person_alias WHERE sheet_id = ?" + UP_TO + " ORDER BY seq",
                (RowCallbackHandler) rs -> aliases.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>()).add(rs.getString(2)),
                sheetId, end);
        for (Person then : byId.values()) {
            List<String> aliasNames = new ArrayList<>(aliases.getOrDefault(then.getId(), List.of()));
            aliasNames.sort(BY_NAME);
            then.setAliases(aliasNames);
            then.setGrades(grades.getOrDefault(then.getId(), Map.of()));
            then.setToday(date);
            then.setHolds(new ArrayList<>(holds.getOrDefault(then.getId(), List.of())));
            then.setRetiredFrom(retired.get(then.getId()));
            then.setRetirements(new ArrayList<>(retirements.getOrDefault(then.getId(), List.of())));
            then.setSnapshots(new ArrayList<>(snapshots.getOrDefault(then.getId(), List.of())));
            then.setStatementCount(statementCount.getOrDefault(then.getId(), 0));
            then.setStatements(new ArrayList<>(statements.getOrDefault(then.getId(), List.of())));
            List<Long> ids = new ArrayList<>();
            List<String> names = new ArrayList<>();
            for (Long patronId : patrons.getOrDefault(then.getId(), List.of())) {
                Person patron = byId.get(patronId);
                if (patron == null) continue;
                ids.add(patronId);
                names.add(patron.getName());
            }
            names.sort(BY_NAME);
            then.setPatrons(ids, names);
        }
        return PersonResponse.from(new ArrayList<>(byId.values()));
    }

    /** The claims on the sheet by the end of the date, each as it stood then (its log as written by then), newest first. */
    public List<ClaimResponse> claims(String sheetId, LocalDate date) {
        Date end = Date.valueOf(date);
        Map<Long, Claim> latest = new HashMap<>();
        Map<Long, Claim> now = new LinkedHashMap<>();
        // Those transferred away since still stood on the sheet.
        claims.listWithTransferredAfter(sheetId, date).forEach(claim -> now.put(claim.getId(), claim));
        jdbc.query("SELECT * FROM claim_history WHERE sheet_id = ?" + UP_TO + " ORDER BY seq", (RowCallbackHandler) rs -> {
            Claim current = now.get(rs.getLong("claim_id"));
            if (current == null) return;
            String note = rs.getString("note");
            Claim then = new Claim(current.getId(), sheetId, rs.getLong("person_id"), rs.getBigDecimal("amount"),
                    rs.getDate("claim_date").toLocalDate(), note == null ? current.getNote() : note,
                    ClaimStatus.valueOf(rs.getString("status")), rs.getString("claim_number"),
                    rs.getBigDecimal("settled_due"), rs.getString("opened_by"));
            then.setStated(rs.getBoolean("stated"));
            then.setStatedDue(rs.getBigDecimal("stated_due"));
            LocalDate dueBy = local(rs.getDate("due_by"));
            then.setDueBy(dueBy == null ? then.getDate() : dueBy);
            then.setDeferredUntil(local(rs.getDate("deferred_until")));
            then.setCorrects(rs.getString("corrects"));
            int proofs = rs.getInt("proofs");
            then.setProofs(rs.wasNull() ? current.getProofs() : proofs);
            then.setSplitFrom(rs.getString("split_from"));
            then.setLateEntry(rs.getBoolean("late_entry"));
            then.setWaived(rs.getBoolean("waived"));
            int batch = rs.getInt("settle_batch");
            then.setSettleBatch(rs.wasNull() ? null : batch);
            // A claim's ref and tags are not kept in its history: they show as they are now.
            then.setRef(current.getRef());
            then.setTags(current.getTags());
            then.setRateMark(current.getRateMark());
            // Nor is its void reason: a claim void by then shows the reason it was voided with.
            if (then.getStatus() == ClaimStatus.VOID) then.setVoidReason(current.getVoidReason());
            latest.put(then.getId(), then);
        }, sheetId, end);
        List<ClaimResponse> shown = new ArrayList<>();
        for (Claim current : now.values()) {
            Claim then = latest.get(current.getId());
            if (then == null) continue;
            then.setCorrectedDate(claims.correctedDate(sheetId, then.getPersonId(), then.getCorrects()));
            List<String> log = new ArrayList<>();
            for (String line : current.getLog()) {
                java.util.regex.Matcher on = LOG_DATE.matcher(line);
                if (!on.find() || !LocalDate.parse(on.group(1)).isAfter(date)) log.add(line);
            }
            then.setLog(log);
            then.setComments(current.getComments().stream().filter(comment -> !comment.date().isAfter(date)).toList());
            shown.add(ClaimResponse.from(then));
        }
        return shown;
    }

    private static final java.util.regex.Pattern LOG_DATE = java.util.regex.Pattern.compile(" on (\\d{4}-\\d{2}-\\d{2}) due=[^ ]*$");

    /** The rates set by the end of the date, by grade then from-date (from the beginning of time first). */
    public List<RateResponse> rates(String sheetId, LocalDate date) {
        Map<String, TreeMap<LocalDate, RateResponse>> byGrade = new TreeMap<>();
        jdbc.query("SELECT grade, rate, from_date, to_date, after_mark FROM rate_history WHERE sheet_id = ?" + UP_TO + " ORDER BY seq",
                (RowCallbackHandler) rs -> {
                    LocalDate from = local(rs.getDate(3));
                    LocalDate to = local(rs.getDate(4));
                    String grade = rs.getString(1).trim();
                    TreeMap<LocalDate, RateResponse> byFrom = byGrade.computeIfAbsent(grade, g -> new TreeMap<>());
                    LocalDate key = from == null ? LocalDate.MIN : from;
                    RateResponse replaced = byFrom.get(key);
                    List<RateResponse.Earlier> earlier = new ArrayList<>();
                    if (replaced != null) {
                        earlier.add(new RateResponse.Earlier(replaced.rate(), replaced.to(), replaced.afterMark()));
                        earlier.addAll(replaced.earlier());
                    }
                    byFrom.put(key, new RateResponse(grade, rs.getBigDecimal(2).setScale(2).toPlainString(),
                            from == null ? "" : from.toString(), to == null ? "" : to.toString(),
                            replaced == null ? 1 : replaced.version() + 1, rs.getLong(5), earlier));
                }, sheetId, Date.valueOf(date));
        List<RateResponse> rates = new ArrayList<>();
        byGrade.values().forEach(byFrom -> rates.addAll(byFrom.values()));
        return rates;
    }

    /** The periods added by the end of the date, by from-date, each closed (or soft-closed) as it stood then. */
    public List<PeriodResponse> periods(String sheetId, LocalDate date) {
        Date end = Date.valueOf(date);
        Map<String, Date> addedOn = new HashMap<>();
        jdbc.query("SELECT name, added_on FROM period WHERE sheet_id = ?", (RowCallbackHandler) rs -> addedOn.put(
                rs.getString(1), rs.getDate(2)), sheetId);
        Map<String, PeriodResponse> byName = new LinkedHashMap<>();
        for (PeriodResponse now : periods.list(sheetId)) {
            Date added = addedOn.get(now.name());
            if (added != null && added.toLocalDate().isAfter(date)) continue;
            byName.put(now.name(), new PeriodResponse(now.name(), now.from(), now.to(), false, "", new LinkedHashMap<>(), false, now.cap(),
                    new LinkedHashMap<>()));
        }
        jdbc.query("SELECT name, closed, soft_closed, closed_total, person_totals, person_carried FROM period_history WHERE sheet_id = ?"
                + UP_TO + " ORDER BY seq", (RowCallbackHandler) rs -> {
                    PeriodResponse was = byName.get(rs.getString(1));
                    if (was == null) return;
                    BigDecimal total = rs.getBigDecimal(4);
                    byName.put(was.name(), new PeriodResponse(was.name(), was.from(), was.to(), rs.getBoolean(2),
                            total == null ? "" : total.setScale(2).toPlainString(), byPerson(rs.getString(5)), rs.getBoolean(3),
                            was.cap(), byPerson(rs.getString(6))));
                }, sheetId, end);
        return new ArrayList<>(byName.values());
    }

    /** The ids of the people the user watched at the end of the date, in the order they started watching. */
    public List<String> watches(String sheetId, String user, LocalDate date) {
        LinkedHashMap<Long, Boolean> watching = new LinkedHashMap<>();
        jdbc.query("SELECT person_id, watching FROM watch_history WHERE sheet_id = ? AND user_name = ?" + UP_TO
                + " ORDER BY seq", (RowCallbackHandler) rs -> {
                    watching.remove(rs.getLong(1));
                    if (rs.getBoolean(2)) watching.put(rs.getLong(1), true);
                }, sheetId, user, Date.valueOf(date));
        return watching.keySet().stream().map(String::valueOf).toList();
    }

    /** The settings as they stood at the end of the date, each dated one as in force on it (see {@link Settings#load}). */
    public SettingsResponse settings(String sheetId, LocalDate date) {
        sheets.today(sheetId);
        Date end = Date.valueOf(date);
        // The dated settings: the latest recorded for each key and from-date.
        Map<String, TreeMap<LocalDate, BigDecimal[]>> dated = new TreeMap<>();
        jdbc.query("SELECT setting_key, from_date, setting_value FROM setting_history WHERE sheet_id = ?" + UP_TO
                + " ORDER BY seq", (RowCallbackHandler) rs -> dated.computeIfAbsent(rs.getString(1), k -> new TreeMap<>())
                        .put(rs.getDate(2).toLocalDate(), new BigDecimal[] { rs.getBigDecimal(3) }), sheetId, end);
        java.util.function.Function<String, BigDecimal> on = key -> {
            Map.Entry<LocalDate, BigDecimal[]> found = dated.getOrDefault(key, new TreeMap<>()).floorEntry(date);
            return found == null ? null : found.getValue()[0];
        };
        Map<String, List<DatedValue>> datedShown = new TreeMap<>();
        dated.forEach((key, byFrom) -> byFrom.forEach((from, value) -> datedShown.computeIfAbsent(key, k -> new ArrayList<>())
                .add(new DatedValue(from.toString(), value[0] == null ? "" : ValidateSetting.ROUNDING.equals(key)
                        ? net.officefloor.hq.app.claim.Rounding.name(value[0]) : key.startsWith("terms")
                        ? String.valueOf(value[0].intValue()) : value[0].setScale(2).toPlainString()))));
        BigDecimal limit = on.apply(ValidateSetting.CLERK_LIMIT);
        BigDecimal cap = on.apply(ValidateSetting.CAP);
        BigDecimal threshold = on.apply(ValidateSetting.PROOF_THRESHOLD);
        BigDecimal noteThreshold = on.apply(ValidateSetting.NOTE_THRESHOLD);
        BigDecimal reviewThreshold = on.apply(ValidateSetting.REVIEW_THRESHOLD);
        BigDecimal floor = on.apply(ValidateSetting.FLOOR);
        BigDecimal terms = on.apply(ValidateSetting.TERMS);
        BigDecimal latePct = on.apply(ValidateSetting.LATE_PCT);
        Map<String, String> gradeTerms = new TreeMap<>();
        for (String grade : List.of("A", "B", "C")) {
            BigDecimal own = on.apply(ValidateSetting.GRADE_TERMS + grade);
            if (own != null) gradeTerms.put(grade, String.valueOf(own.intValue()));
        }
        // The policy date: the latest set (null: blank).
        List<LocalDate> policies = new ArrayList<>();
        jdbc.query("SELECT policy_date FROM policy_history WHERE sheet_id = ?" + UP_TO + " ORDER BY seq",
                (RowCallbackHandler) rs -> policies.add(local(rs.getDate(1))), sheetId, end);
        LocalDate policy = policies.isEmpty() ? null : policies.get(policies.size() - 1);
        // The undated settings: the latest set for each key.
        Map<String, String> plain = new HashMap<>();
        jdbc.query("SELECT setting_key, setting_value FROM plain_setting_history WHERE sheet_id = ?" + UP_TO + " ORDER BY seq",
                (RowCallbackHandler) rs -> plain.put(rs.getString(1), rs.getString(2)), sheetId, end);
        List<RoleResponse> roles = new ArrayList<>();
        new TreeMap<>(plain).forEach((key, value) -> {
            if (key.startsWith(Settings.ROLE)) roles.add(new RoleResponse(key.substring(Settings.ROLE.length()), value));
        });
        Map<String, String> perms = new LinkedHashMap<>();
        for (String action : ValidateSetting.ACTIONS) {
            perms.put(action, plain.getOrDefault(Settings.PERM + action, ""));
        }
        List<DelegationResponse> delegations = jdbc.query("SELECT user_name, from_date, to_date, delegated_by FROM sheet_delegation"
                + " WHERE sheet_id = ?" + UP_TO + " ORDER BY seq", (rs, i) -> new DelegationResponse(rs.getString(1),
                        rs.getDate(2).toLocalDate().toString(), rs.getDate(3).toLocalDate().toString(), rs.getString(4)),
                sheetId, end);
        return new SettingsResponse(limit == null ? "" : limit.setScale(2).toPlainString(),
                (floor == null ? Settings.START_FLOOR : floor).setScale(2).toPlainString(),
                String.valueOf(terms == null ? Settings.START_TERMS : terms.intValue()),
                (latePct == null ? Settings.START_LATE_PCT : latePct).setScale(2).toPlainString(),
                policy == null ? "" : policy.toString(), plain.getOrDefault(Settings.NUMBERING, ""), roles, gradeTerms,
                cap == null ? "" : cap.setScale(2).toPlainString(), datedShown,
                threshold == null ? "" : threshold.setScale(2).toPlainString(), perms, plain.getOrDefault(Settings.GRACE, ""),
                delegations, noteThreshold == null ? "" : noteThreshold.setScale(2).toPlainString(),
                plain.getOrDefault(Settings.ENTRY, ""), plain.getOrDefault(Settings.POT, ""),
                Objects.toString(sheets.parent(sheetId), ""), // the parent is not dated: as it is now
                plain.getOrDefault(Settings.FISCAL, "1"), plain.getOrDefault(Settings.ALERT, ""),
                net.officefloor.hq.app.claim.Rounding.name(on.apply(ValidateSetting.ROUNDING)),
                reviewThreshold == null ? "" : reviewThreshold.setScale(2).toPlainString(),
                plain.getOrDefault(Settings.ESCALATE, ""), plain.getOrDefault(Settings.REVIEW_SLA, ""));
    }

    /** Figures by person id from "<person id>=<figure>" joined by ";" (null or "": none). */
    private static Map<String, String> byPerson(String joined) {
        Map<String, String> figures = new LinkedHashMap<>();
        if (joined != null && !joined.isEmpty()) {
            for (String entry : joined.split(";")) {
                String[] pair = entry.split("=", 2);
                figures.put(pair[0], new BigDecimal(pair[1]).setScale(2).toPlainString());
            }
        }
        return figures;
    }

    private static LocalDate local(Date date) {
        return date == null ? null : date.toLocalDate();
    }
}

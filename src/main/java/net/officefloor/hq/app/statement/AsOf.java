package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ValidateSetting;
import net.officefloor.hq.app.sheet.Sheets;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

/**
 * What a statement's figures are worked from: as the store stands now, or as it stood at the end of a date — every
 * change made while the sheet's today was on or before that date, the latest of each winning (see the history tables
 * of V45), with that date standing in for the sheet's today.
 */
@Repository
public class AsOf {

    /** A person and their claims, with the date standing for the sheet's today and the figures in force then. */
    public record View(LocalDate today, LocalDate policyDate, BigDecimal cap, Person person, List<Claim> claims,
            BiFunction<String, LocalDate, BigDecimal> rate, Function<LocalDate, BigDecimal> floor,
            Function<LocalDate, BigDecimal> latePct) {
    }

    private record RateRow(BigDecimal rate, LocalDate from, LocalDate to) {
    }

    private final JdbcTemplate jdbc;
    private final Claims claims;
    private final Rates rates;
    private final Settings settings;
    private final Sheets sheets;

    public AsOf(JdbcTemplate jdbc, Claims claims, Rates rates, Settings settings, Sheets sheets) {
        this.jdbc = jdbc;
        this.claims = claims;
        this.rates = rates;
        this.settings = settings;
        this.sheets = sheets;
    }

    /** The person and their claims as they stand now. */
    public View now(Person person) {
        String sheetId = person.getSheetId();
        return new View(sheets.today(sheetId), settings.policyDate(sheetId), settings.cap(sheetId), person,
                claims.ofPerson(sheetId, person.getId()), (grade, on) -> rates.find(sheetId, grade, on),
                on -> settings.floor(sheetId, on), on -> settings.latePct(sheetId, on));
    }

    /** The person and the claims that were theirs as they stood at the end of the date. */
    public View at(Person person, LocalDate date) {
        String sheetId = person.getSheetId();
        Date end = Date.valueOf(date);
        String upTo = " AND (set_on IS NULL OR set_on <= ?) ORDER BY seq";

        // The rates: the latest set for each grade and from-date.
        Map<String, Map<LocalDate, RateRow>> rateRows = new HashMap<>();
        jdbc.query("SELECT grade, rate, from_date, to_date FROM rate_history WHERE sheet_id = ?" + upTo,
                (RowCallbackHandler) rs -> {
                    LocalDate from = local(rs.getDate("from_date"));
                    rateRows.computeIfAbsent(rs.getString("grade").trim(), g -> new HashMap<>())
                            .put(from == null ? LocalDate.MIN : from,
                                    new RateRow(rs.getBigDecimal("rate"), from, local(rs.getDate("to_date"))));
                }, sheetId, end);
        BiFunction<String, LocalDate, BigDecimal> rate = (grade, on) -> {
            TreeMap<LocalDate, RateRow> byFrom = new TreeMap<>(rateRows.getOrDefault(grade, Map.of()));
            Map.Entry<LocalDate, RateRow> found = byFrom.floorEntry(on);
            if (found == null) return null;
            RateRow row = found.getValue();
            return row.to() != null && row.to().isBefore(on) ? null : row.rate();
        };

        // The dated settings: the latest recorded for each key and from-date (a null value: none from then).
        Map<String, TreeMap<LocalDate, Object[]>> dated = new HashMap<>();
        jdbc.query("SELECT setting_key, from_date, setting_value FROM setting_history WHERE sheet_id = ?" + upTo,
                (RowCallbackHandler) rs -> dated.computeIfAbsent(rs.getString(1), k -> new TreeMap<>())
                        .put(rs.getDate(2).toLocalDate(), new Object[] { rs.getBigDecimal(3) }), sheetId, end);
        BiFunction<String, LocalDate, BigDecimal> setting = (key, on) -> {
            Map.Entry<LocalDate, Object[]> found = dated.getOrDefault(key, new TreeMap<>()).floorEntry(on);
            return found == null ? null : (BigDecimal) found.getValue()[0];
        };
        Function<LocalDate, BigDecimal> floor = on -> {
            BigDecimal value = setting.apply(ValidateSetting.FLOOR, on);
            return value == null ? Settings.START_FLOOR : value;
        };
        Function<LocalDate, BigDecimal> latePct = on -> {
            BigDecimal value = setting.apply(ValidateSetting.LATE_PCT, on);
            return value == null ? Settings.START_LATE_PCT : value;
        };
        BigDecimal cap = setting.apply(ValidateSetting.CAP, date);

        // The policy date: the latest set (blank: the date itself).
        List<LocalDate> policies = new ArrayList<>();
        jdbc.query("SELECT policy_date FROM policy_history WHERE sheet_id = ?" + upTo,
                (RowCallbackHandler) rs -> policies.add(local(rs.getDate(1))), sheetId, end);
        LocalDate policy = policies.isEmpty() || policies.get(policies.size() - 1) == null ? date
                : policies.get(policies.size() - 1);

        return new View(date, policy, cap, personAt(person, end), claimsAt(person, end), rate, floor, latePct);
    }

    /** The person with the grades, holds and retirement recorded by the end of the date. */
    private Person personAt(Person now, Date end) {
        String sheetId = now.getSheetId();
        Map<LocalDate, String> grades = new LinkedHashMap<>();
        jdbc.query("SELECT from_date, grade FROM grade_history WHERE sheet_id = ? AND person_id = ?"
                + " AND (set_on IS NULL OR set_on <= ?) ORDER BY seq", (RowCallbackHandler) rs -> {
                    LocalDate from = local(rs.getDate(1));
                    grades.put(from == null ? Person.BEGINNING : from, rs.getString(2));
                }, sheetId, now.getId(), end);
        Person person = new Person(now.getId(), sheetId, now.getName(), now.getGrades().get(now.getGrades().firstKey()));
        person.setGrades(grades);
        person.setToday(end.toLocalDate());
        List<Person.Hold> holds = new ArrayList<>();
        jdbc.query("SELECT from_date, to_date FROM person_hold WHERE sheet_id = ? AND person_id = ?"
                + " AND (set_on IS NULL OR set_on <= ?) ORDER BY seq", (RowCallbackHandler) rs -> holds.add(
                        new Person.Hold(rs.getDate(1).toLocalDate(), local(rs.getDate(2)))), sheetId, now.getId(), end);
        person.setHolds(holds);
        List<LocalDate> retired = new ArrayList<>();
        jdbc.query("SELECT retired_from FROM retire_history WHERE sheet_id = ? AND person_id = ?"
                + " AND (set_on IS NULL OR set_on <= ?) ORDER BY seq", (RowCallbackHandler) rs -> retired.add(
                        local(rs.getDate(1))), sheetId, now.getId(), end);
        person.setRetiredFrom(retired.isEmpty() ? null : retired.get(retired.size() - 1));
        return person;
    }

    /** The claims that were the person's at the end of the date, each as it stood then, newest first. */
    private List<Claim> claimsAt(Person person, Date end) {
        String sheetId = person.getSheetId();
        Map<Long, Claim> latest = new LinkedHashMap<>();
        jdbc.query("SELECT * FROM claim_history WHERE sheet_id = ? AND (set_on IS NULL OR set_on <= ?) ORDER BY seq",
                (RowCallbackHandler) rs -> {
                    Claim claim = new Claim(rs.getLong("claim_id"), sheetId, rs.getLong("person_id"),
                            rs.getBigDecimal("amount"), rs.getDate("claim_date").toLocalDate(), "",
                            ClaimStatus.valueOf(rs.getString("status")), null, rs.getBigDecimal("settled_due"), null);
                    claim.setStated(rs.getBoolean("stated"));
                    claim.setStatedDue(rs.getBigDecimal("stated_due"));
                    claim.setDueBy(local(rs.getDate("due_by")));
                    claim.setDeferredUntil(local(rs.getDate("deferred_until")));
                    claim.setCorrects(rs.getString("corrects"));
                    latest.put(claim.getId(), claim);
                }, sheetId, end);
        List<Claim> own = new ArrayList<>();
        for (Claim claim : latest.values()) {
            if (claim.getPersonId() == person.getId()) {
                if (claim.getDueBy() == null) claim.setDueBy(claim.getDate());
                claim.setCorrectedDate(claims.correctedDate(sheetId, person.getId(), claim.getCorrects()));
                own.add(0, claim);
            }
        }
        return own;
    }

    private static LocalDate local(Date date) {
        return date == null ? null : date.toLocalDate();
    }
}

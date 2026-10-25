package net.officefloor.hq.app.statement;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimFigures;
import net.officefloor.hq.app.claim.ClaimLog;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimShares;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonGrades;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateLogRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.sheet.SettingLogRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Takes a statement for the person: records their due-total now, on the sheet's today, and binds each of
 * their claims that counts to the due it shows now, its floored figure plus the late charge on it (a claim already bound keeps the due
 * it is bound to, and a bound due no longer follows the sheet's today); their claims that do not count (a retired person's unbound OPEN ones among
 * them; a hold takes none out) are unbound (live again).
 * <p>
 * Taken as of a date (on or before the sheet's today, and not before the person was added), it works the same out
 * as at the end of that date: every change made while the sheet's today was on or before it counts (a claim's status
 * and settled due, the rates, settings, grades, holds, retirement and the claims bound), every later change does not,
 * and that date stands in for the sheet's today. The claims that counted then are bound to their figures then; every
 * other claim of theirs is unbound.
 */
public class TakeStatement {
    public void take(@Val Sheet sheet, @Val Person person, ClaimRepository claims, RateRepository rates, PersonGradeRepository grades,
            StatementRepository statements, BindingLogRepository bindings, ClaimLogRepository logs,
            RateLogRepository rateLogs, SettingLogRepository settingLogs, PersonRepository people, EntityManager entityManager,
            @RequestParam(name = "asOf", required = false) String asOfParam, Out<Statement> statementOut)
            throws InvalidPersonException {
        LocalDate asOf = parseAsOf(asOfParam, sheet, person);
        // Lock the person so two statements (or a statement and an edit) are taken one after the other.
        entityManager.refresh(person, LockModeType.PESSIMISTIC_WRITE);
        List<Claim> mine = claims.findBySheetIdAndPersonIdOrderByIdDesc(sheet.getId(), person.getId());
        BindingLog binding = new BindingLog(sheet.getId(), person.getId(), sheet.getToday());
        String dueTotal;
        int counted = 0;
        if (asOf == null) {
            ClaimFigures figures = new ClaimFigures(rates.findInChain(sheet.getId()),
                    grades.gradesOf(person), sheet).withHolders(holders(sheet, people, grades));
            dueTotal = figures.dueTotal(mine, sheet.getToday(), person,
                    heldBy(person, sheet, claims, rates, grades, people));
            for (Claim claim : mine) {
                if (ClaimFigures.countsLive(claim, person, sheet.getToday())) {
                    BigDecimal figure = figures.due(claim);
                    claim.bind(true, figure);
                    binding.bind(claim.getId(), figure);
                    counted++;
                } else {
                    claim.bind(false, null);
                }
            }
        } else {
            Sheet then = settingLogs.sheetAsOf(sheet, asOf);
            Person personThen = person.asOf(asOf);
            PersonGrades gradesThen = grades.gradesOf(person).asOf(asOf);
            Function<Long, PersonGrades> holders = holders(sheet, people, grades);
            ClaimFigures figures = new ClaimFigures(rates.chainOf(sheet.getId()).stream()
                    .flatMap(id -> rateLogs.ratesAsOf(id, asOf).stream()).toList(), gradesThen, then)
                    .withHolders(id -> {
                        PersonGrades holder = holders.apply(id);
                        return holder == null ? null : holder.asOf(asOf);
                    });
            Map<Long, BigDecimal> boundThen = bindings.boundAsOf(person.getId(), asOf);
            Map<Long, List<ClaimLog>> logged = logs.findBySheetIdOrderByIdAsc(sheet.getId()).stream()
                    .collect(Collectors.groupingBy(ClaimLog::getClaimId));
            // Each claim as it stood at the end of the date, by its status log; one not yet added then is left out.
            List<Claim> claimsThen = new ArrayList<>();
            for (Claim claim : mine) {
                Claim claimThen = claimAsOf(claim, logged.getOrDefault(claim.getId(), List.of()), boundThen, asOf);
                if (claimThen != null) {
                    claimsThen.add(claimThen);
                }
            }
            dueTotal = figures.dueTotal(claimsThen, asOf, personThen);
            Map<Long, Claim> byId = mine.stream().collect(Collectors.toMap(Claim::getId, c -> c));
            mine.forEach(c -> c.bind(false, null));
            for (Claim claimThen : claimsThen) {
                if (ClaimFigures.countsLive(claimThen, personThen, asOf)) {
                    BigDecimal figure = figures.due(claimThen);
                    byId.get(claimThen.getId()).bind(true, figure);
                    binding.bind(claimThen.getId(), figure);
                    counted++;
                }
            }
        }
        claims.saveAllAndFlush(mine);
        bindings.saveAndFlush(binding);
        statementOut.set(statements.save(
                new Statement(sheet.getId(), person.getId(), dueTotal, sheet.getToday(), asOf, counted)));
    }

    /** The person's parts of the counted claims of others shared with them, each as its own person's figures give it
     *  (as the rollup counts them); null when they hold no part at a rate of a counted claim. */
    private static BigDecimal heldBy(Person person, Sheet sheet, ClaimRepository claims, RateRepository rates,
            PersonGradeRepository grades, PersonRepository people) {
        BigDecimal held = null;
        var sheetRates = rates.findInChain(sheet.getId());
        Function<Long, PersonGrades> holders = holders(sheet, people, grades);
        Map<Long, List<Claim>> shared = claims.findBySheetIdOrderByIdDesc(sheet.getId()).stream()
                .filter(c -> c.getShares() != null && ClaimShares.of(c).stream().anyMatch(sh -> person.getId().equals(sh.personId())))
                .collect(Collectors.groupingBy(Claim::getPersonId));
        for (Person owner : people.findListed(sheet.getId())) {
            List<Claim> owned = shared.getOrDefault(owner.getId(), List.of());
            if (owned.isEmpty()) {
                continue;
            }
            ClaimFigures ownerFigures = new ClaimFigures(sheetRates, grades.gradesOf(owner), sheet)
                    .withHolders(holders);
            for (Claim c : owned) {
                BigDecimal claimDue = ClaimFigures.countsLive(c, owner, sheet.getToday()) ? ownerFigures.due(c) : null;
                if (claimDue == null) {
                    continue;
                }
                for (var sh : ClaimShares.of(c)) {
                    if (person.getId().equals(sh.personId())) {
                        BigDecimal part = ownerFigures.part(c, claimDue, sh);
                        if (part != null) {
                            held = held == null ? part : held.add(part);
                        }
                    }
                }
            }
        }
        return held;
    }

    /** The grades over time of each person on the sheet, by person id (null: not on it), for the holders' parts. */
    private static Function<Long, PersonGrades> holders(Sheet sheet, PersonRepository people, PersonGradeRepository grades) {
        Map<Long, PersonGrades> byId = new HashMap<>();
        people.findListed(sheet.getId()).forEach(p -> byId.put(p.getId(), grades.gradesOf(p)));
        return byId::get;
    }

    /** The date the statement is taken as of: null when none is given; refuses one that is not a calendar date, one
     *  after the sheet's today, and one before the person was added. */
    private static LocalDate parseAsOf(String asOfParam, Sheet sheet, Person person) throws InvalidPersonException {
        if (asOfParam == null || asOfParam.isBlank()) {
            return null;
        }
        LocalDate asOf;
        try {
            asOf = LocalDate.parse(asOfParam.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidPersonException("The statement's date must be a calendar date.");
        }
        if (asOf.isAfter(sheet.getToday())) {
            throw new InvalidPersonException("The statement's date is after the sheet's today.");
        }
        if (person.getAddedOn() != null && asOf.isBefore(person.getAddedOn())) {
            throw new InvalidPersonException("The person was not on the sheet on the statement's date.");
        }
        return asOf;
    }

    /** The claim as it stood at the end of the date: the status its log had then and the due it was settled at (when
     *  SETTLED then), bound as the person's bindings then had it; null when it had not been added by then. A claim
     *  with no log at all is taken as it stands. */
    private static Claim claimAsOf(Claim claim, List<ClaimLog> log, Map<Long, BigDecimal> boundThen, LocalDate asOf) {
        boolean stated = boundThen.containsKey(claim.getId());
        BigDecimal statedDue = boundThen.get(claim.getId());
        if (log.isEmpty()) {
            return claim.asOf(claim.getStatus(), claim.getDue(), stated, statedDue);
        }
        ClaimLog last = log.stream().filter(l -> !l.getOnDate().isAfter(asOf)).reduce((a, b) -> b).orElse(null);
        if (last == null) {
            return null;
        }
        ClaimStatus status = ClaimStatus.valueOf(last.getStatus());
        return claim.asOf(status, status == ClaimStatus.SETTLED ? last.getDue() : null, stated, statedDue);
    }
}

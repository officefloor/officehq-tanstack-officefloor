package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimShare;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.claim.Late;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Dated;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.hq.app.sheet.SheetState;
import net.officefloor.hq.app.sheet.SheetStates;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Takes a statement of the person: records their due-total now (late charges included; their parts of claims shared
 * with them counted in, and the parts of theirs shared with others left out, as the rollup counts them), binds each of their
 * counting claims to the due it shows now (late charge included; a claim already bound keeps its figure) and
 * unbinds the rest. The due-total is n/a
 * when their grade has no rate on the sheet's today, unless a
 * counting claim has a figure apart from it (settled, bound, or at another grade in force on its date): then it is the figures there are.
 * <p>
 * Taken as of a date on or before the sheet's today, it is made from the sheet as it stood at the end of that date
 * (every change made while the sheet's today was on or before it, none after), with that date for the sheet's today
 * in every figure; it binds the claims of theirs that counted then to their figures then and unbinds the rest. A date
 * after the sheet's today is refused, and so is one on which the person was not on the sheet.
 */
public class TakeStatement {
    public void take(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @RequestBody StatementRequest request, @Val Person person, Claims claims,
            Statements statements, SheetClock clock, SheetStates states, Audit audit, Rollup rollup)
            throws InvalidPersonException {
        LocalDate today = clock.today(sheet);
        LocalDate asOf = asOf(request);
        if (asOf != null && asOf.isAfter(today)) {
            throw new InvalidPersonException("A statement cannot be taken as of a date after the sheet's today");
        }
        LocalDate on = asOf == null ? today : asOf;
        SheetState state = on.equals(today) ? states.now(sheet) : states.at(sheet, on);
        Person then = state == null ? null : state.person(person.id(), on);
        if (then == null) {
            throw new InvalidPersonException(person.name() + " was not on the sheet on " + on);
        }
        Rollup.Snapshot before = rollup.take(sheet);
        List<Rate> sheetRates = state.rates();
        Dated floor = state.floorDated();
        Late late = state.late(on);
        List<Claim> past = state.claimsOf(person.id(), on);
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        Map<Long, BigDecimal> bound = new HashMap<>();
        for (Claim claim : past) {
            if (then.counts(claim)) {
                // The statement binds the due as shown, late charge included: it no longer follows the sheet's today.
                BigDecimal due = claim.due(sheetRates, then.gradeOn(claim.date()), floor, late);
                bound.put(claim.id(), due);
                // The holders' parts of a shared claim count for them, not for its own person (as the rollup counts them).
                total = due == null ? total : total.add(due.subtract(claim.sharedPart(due)));
            }
        }
        // The person's parts of the claims shared with them, counted as their own persons count them.
        boolean holdsPart = false;
        for (Claim claim : state.claims()) {
            if (claim.person() == person.id() || claim.shares() == null) {
                continue;
            }
            Person owner = state.person(claim.person(), on);
            Claim shown = claim.on(on);
            if (owner == null || !owner.counts(shown)) {
                continue;
            }
            for (ClaimShare share : shown.shares()) {
                if (share.person() != person.id()) {
                    continue;
                }
                BigDecimal due = shown.due(sheetRates, owner.gradeOn(shown.date()), floor, late);
                if (due != null) {
                    holdsPart = true;
                    total = total.add(share.partOf(due));
                }
            }
        }
        boolean na = !holdsPart && Rate.on(sheetRates, then.grade(), on) == null
                && past.stream().noneMatch((c) -> then.counts(c) && then.figureApartFrom(c, on, sheetRates, floor));
        Map<Long, Claim> mine = claims.list(sheet).stream().filter((c) -> c.person() == person.id())
                .collect(Collectors.toMap(Claim::id, Function.identity()));
        for (Claim claim : mine.values()) {
            if (bound.containsKey(claim.id())) {
                claims.setStated(sheet, claim.id(), true, bound.get(claim.id()));
            } else if (claim.stated()) {
                claims.setStated(sheet, claim.id(), false, null);
            }
        }
        BigDecimal dueTotal = na ? null : Settings.capped(total, state.capOn(on));
        statements.add(sheet, person.id(), dueTotal, today, asOf);
        audit.record("STATEMENT sheet=" + sheet + " by=" + user + " name=" + person.name() + " due-total="
                + (dueTotal == null ? "n/a" : dueTotal.toPlainString()) + " claims=" + bound.size()
                + (asOf == null ? "" : " asof=" + asOf));
        rollup.record(sheet, user, before);
    }

    /** The as-of date the request gives, or null when it gives none. */
    private static LocalDate asOf(StatementRequest request) throws InvalidPersonException {
        String asof = request == null || request.asof() == null ? "" : request.asof().trim();
        try {
            return asof.isEmpty() ? null : LocalDate.parse(asof);
        } catch (DateTimeParseException e) {
            throw new InvalidPersonException("Not a valid as-of date: " + asof);
        }
    }
}

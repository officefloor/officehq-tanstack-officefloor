package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimFigure;
import net.officefloor.hq.app.claim.ClaimStore;
import net.officefloor.hq.app.claim.Lateness;
import net.officefloor.hq.app.claim.Rounding;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.rate.RateStore;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Takes a statement of the person on the sheet's today: their due-total (the sum of the figures of
 *  their claims that count; n/a when their grade in force today has no rate today and no counting claim
 *  keeps a fixed figure or a figure at another grade, the one in force on its date), binding each counting claim to its figure now (its rate figure, not floored, when not fixed) and unbinding the rest. A settled
 *  claim keeps its fixed figure. The due-total includes the late charges of overdue claims and the floor, but a
 *  claim is bound to its figure without its charge or floor, which keeps following the sheet's today. The
 *  due-total is at most the sheet's cap.
 *  <p>
 *  Taken as of a date (on or before the sheet's today), everything is as it stood at the end of that date
 *  (see {@link net.officefloor.hq.app.history.History}): their claims that counted then are bound to their
 *  figures then, and the date stands in for the sheet's today in every figure (grade and rate in force, floor,
 *  late charges, holds, retirement, cap); every other claim of theirs is unbound. The statement is dated that
 *  date. Refused when the date is after the sheet's today, or the person was not on the sheet then. */
public class TakeStatement {
    public void take(@Val StatementRequest request, @Val Person person, ClaimStore claims, RateStore rates,
            SheetStore sheets, SettingStore settings, PersonStore people, StatementStore statements,
            Out<Statement> statementOut) throws InvalidSheetException {
        String sheetId = person.getSheetId();
        LocalDate today = sheets.today(sheetId);
        LocalDate asOf = request.asOfDate();
        if (asOf != null && asOf.isAfter(today)) {
            throw new InvalidSheetException("The statement date is after the sheet's today");
        }
        // Locked, so a status change or another statement made at once waits for this one.
        List<Claim> bound = claims.listForUpdate(sheetId, person.getId());
        // What the figures follow: the store as it is now, or as it stood at the end of the date.
        LocalDate on = today;
        Person then = person;
        List<Claim> all;
        Map<Long, Claim> was = new HashMap<>();
        if (asOf == null) {
            all = claims.list(sheetId);
            bound.forEach(claim -> was.put(claim.getId(), claim));
        } else {
            on = asOf;
            then = people.findAsOf(sheetId, person.getId(), asOf);
            if (then == null) {
                throw new InvalidSheetException("The person was not on the sheet on " + asOf);
            }
            rates = rates.asOf(asOf);
            settings = settings.asOf(asOf);
            all = claims.listAsOf(sheetId, asOf);
            all.stream().filter(claim -> claim.getPersonId() == person.getId()).forEach(claim -> was.put(claim.getId(), claim));
        }
        LocalDate policy = settings.policyDate(sheetId);
        Rounding rounding = new Rounding(policy != null ? policy : on, all);
        Person whose = then;
        Lateness lateness = new Lateness(settings.latePcts(sheetId), settings.floors(sheetId), on, rounding,
                Lateness.graced(settings.graces(sheetId), id -> id == whose.getId() ? whose : null));
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        boolean fixed = false;
        int counted = 0;
        for (Claim live : bound) {
            // The claim as it stood then (itself when taken now); null when it was not theirs then.
            Claim claim = was.get(live.getId());
            // An OPEN (or PENDING) claim dated within a hold counts in no live total unless a statement bound
            // it, so a statement neither counts nor binds it; one it bound before stays counted. A deferred
            // claim counts in no total until the sheet's today reaches its date: a statement leaves it unbound.
            boolean counts = claim != null && RollupService.counts(claim, then, on);
            BigDecimal figure = null;
            boolean na = false;
            if (counts) {
                figure = ClaimFigure.of(claim, rates.find(sheetId, then.gradeOn(claim.getDate()), claim.getDate()), rounding);
                counted++;
                if (figure != null) {
                    total = total.add(lateness.dueOf(claim, figure));
                }
                // A figure at a grade other than the one in force today, or at a rate that ended before
                // today, does not hang on today's grade's rate.
                fixed |= claim.getSettledDue() != null || (claim.isBound() && claim.getStatedDue() != null)
                        || (figure != null && (!then.gradeOn(claim.getDate()).equals(then.gradeOn(on))
                                || rates.endsBefore(sheetId, then.gradeOn(claim.getDate()), claim.getDate(), on)));
                // A claim whose rate ended before its date is bound to n/a, so a later rate leaves it so.
                na = figure == null && (claim.isBound()
                        || (claim.getSettledDue() == null && rates.ended(sheetId, then.gradeOn(claim.getDate()), claim.getDate())));
            }
            live.setStated(counts);
            live.setStatedDue(figure);
            live.setStatedNa(na);
            claims.bind(live);
        }
        boolean na = rates.find(sheetId, then.gradeOn(on), on) == null && !fixed;
        Statement statement = new Statement(person.getId(), on,
                na ? null : SettingStore.capped(total, settings.cap(sheetId)), counted);
        statements.add(sheetId, statement, request.user());
        statementOut.set(statement);
    }
}

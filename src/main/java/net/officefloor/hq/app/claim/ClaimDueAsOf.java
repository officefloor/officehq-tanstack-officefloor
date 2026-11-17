package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.hq.app.statement.SheetAsOf;
import net.officefloor.hq.app.statement.Statement;
import net.officefloor.hq.app.statement.StatementRepository;

/**
 * A claim's due as it stood at the end of a date, as the as-of view works it out (see AsOf, RespondWithClaims): every
 * change made while the sheet's today was on or before the date counts (the claim's status its log shows then, its
 * values before any later edit, its deferral, its person's grades, the rates and settings, a statement binding it),
 * and the date stands in for the sheet's today, late charge and floor included.
 */
public final class ClaimDueAsOf {

    private ClaimDueAsOf() {
    }

    /** The claim's due at the end of the date; throws when the claim was not yet on the sheet then. The due is the
     *  figure (null: n/a) in the result. */
    public static Figure of(Claim claim, LocalDate date, ClaimRepository claims, ClaimLogRepository log,
            PersonRepository people, RateRepository rates, SettingRepository settings, StatementRepository statements)
            throws InvalidClaimStatusException {
        String sheetId = claim.sheetId();
        List<ClaimLogLine> lines = log.of(sheetId, claim.id());
        ClaimStatus status = lines.isEmpty() ? claim.status() : ClaimDue.statusAsOf(lines, date);
        if (status == null) {
            throw new InvalidClaimStatusException("The claim was not on the sheet on " + date);
        }
        Person current = people.find(sheetId, claim.personId());
        Person person = current == null ? null : people.asOf(current, date);
        SheetAsOf sheet = SheetAsOf.read(sheetId, date, rates, settings);
        LocalDate policy = sheet.policyDate();
        Rounding rounding = new Rounding(policy == null ? date : policy, claims,
                (on) -> sheet.value(SettingRepository.ROUNDING, on));
        LateCharge late = new LateCharge(date, sheet::latePct, rounding,
                (c) -> sheet.grace(person == null ? null : person.gradeOn(c.date()), c.date()),
                LateCharge.waivedIn(claims.waived(sheetId, date)));
        Claim values = claims.editedAfter(sheetId, date).getOrDefault(claim.id(), claim);
        var deferred = claims.deferredAsOf(sheetId, date);
        Statement last = claim.stated() ? statements.last(sheetId, claim.personId()) : null;
        boolean stated = last != null && AsOf.counts(last.madeOn(), date);
        Claim then = new Claim(claim.id(), sheetId, claim.personId(), values.amount(), values.date(), values.note(),
                status, claim.number(), status == ClaimStatus.SETTLED ? claim.due() : null, claim.openedBy(), stated,
                stated ? claim.statedDue() : null, values.corrects(), claim.dueBy(),
                deferred.containsKey(claim.id()) ? deferred.get(claim.id()) : claim.deferredUntil(), claim.proofs(),
                claim.splitFrom(), values.ref(), values.tags(), claim.held());
        if (person == null) {
            return new Figure(then.due());
        }
        return new Figure(late.add(then, figure(then, person.gradeOn(then.date()), sheet, rounding, rates),
                sheet.floor(then.date())));
    }

    /** The figure the claim had then (see ClaimDue#figure), at the rate in force then on its date. */
    private static BigDecimal figure(Claim c, String grade, SheetAsOf sheet, Rounding rounding, RateRepository rates) {
        if (c.due() != null) {
            return c.due();
        }
        if (ClaimDue.bound(c)) {
            return c.statedDue();
        }
        Rate rate = sheet.rate(grade, c.date(), rates.seqOf(c.id()));
        if (rate == null) {
            return null;
        }
        RoundingMode mode = rounding.mode(c);
        return c.amount().multiply(rate.rate()).movePointLeft(2).setScale(2, mode);
    }

    /** A due worked out as of a date; its value is null when it is n/a. */
    public record Figure(BigDecimal value) {
    }
}

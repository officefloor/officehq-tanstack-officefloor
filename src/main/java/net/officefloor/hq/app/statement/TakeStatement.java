package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimDue;
import net.officefloor.hq.app.claim.ClaimLogLine;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.LateCharge;
import net.officefloor.hq.app.claim.Rounding;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Takes the person's statement on the sheet's today: their due-total now (the figures of their claims that count
 * added up, each with its late charge; n/a when their grade has no rate today and no counting claim has a
 * figure, kept or at the grade in force on its date; at most the sheet's cap), and binds every claim of theirs that counts (a person's OPEN or PENDING claims dated within a hold of theirs, or any once their retirement date is reached, not bound by a statement, do not, nor do deferred claims) to its rate figure now
 * (never the floor nor its late charge, which keep following today); one that does not count is unbound (live again).
 *
 * Taken as of a date (on or before the sheet's today; a later one is refused, as is one before the person was added),
 * it is the same with the sheet as it stood at the end of that date (see SheetAsOf, PersonRepository#asOf): each claim
 * has the status its log shows then (one added later does not count), is bound only when the person's last statement
 * was taken while the sheet's today was on or before that date, and that date stands in for today in every figure.
 */
public class TakeStatement {
    public void take(@Val Person person, @Val StatementTaker taker, ClaimRepository claims, RateRepository rates,
            SheetRepository sheets, SettingRepository settings, StatementRepository statements, PersonRepository people,
            ClaimLogRepository log, Out<Statement> taken) throws InvalidPersonException {
        if (taker.asOf() != null) {
            takeAsOf(person, taker, claims, rates, sheets, settings, statements, people, log, taken);
            return;
        }
        String sheetId = person.sheetId();
        var today = sheets.today(sheetId);
        LateCharge late = LateCharge.of(sheetId, sheets, settings, claims);
        List<Claim> mine = claims.list(sheetId).stream().filter((c) -> c.personId() == person.id()).toList();
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        boolean figured = false;
        int counted = 0;
        for (Claim c : mine) {
            if (!ClaimDue.countsLive(c, person, today)) {
                claims.bind(c, false, null);
                continue;
            }
            BigDecimal figure = ClaimDue.figure(c, person.gradeOn(c.date()), rates, late.rounding());
            figured |= figure != null;
            if (figure != null) {
                total = total.add(late.add(c, figure, settings.floor(sheetId, c.date())));
            }
            counted++;
            claims.bind(c, true, figure);
        }
        String dueTotal = rates.find(sheetId, person.gradeOn(today), today) == null && !figured ? "n/a" : SettingRepository.capped(total, settings.cap(sheetId)).toPlainString();
        Statement statement = new Statement(sheetId, person.id(), today, dueTotal, counted, taker.by());
        statements.add(statement);
        taken.set(statement);
    }

    private static void takeAsOf(Person current, StatementTaker taker, ClaimRepository claims, RateRepository rates,
            SheetRepository sheets, SettingRepository settings, StatementRepository statements, PersonRepository people,
            ClaimLogRepository log, Out<Statement> taken) throws InvalidPersonException {
        String sheetId = current.sheetId();
        LocalDate today = sheets.today(sheetId);
        LocalDate date = taker.asOf();
        if (date.isAfter(today)) {
            throw new InvalidPersonException("A statement cannot be taken as of a date after the sheet's today");
        }
        LocalDate added = people.addedOn(sheetId, current.id());
        if (added != null && added.isAfter(date)) {
            throw new InvalidPersonException(current.name() + " was not on the sheet on " + date);
        }
        Person person = people.asOf(current, date);
        SheetAsOf sheet = SheetAsOf.read(sheetId, date, rates, settings);
        LocalDate policy = sheet.policyDate();
        Rounding rounding = new Rounding(policy == null ? date : policy, claims,
                (on) -> sheet.value(SettingRepository.ROUNDING, on));
        LateCharge late = new LateCharge(date, sheet::latePct, rounding,
                (c) -> sheet.grace(person.gradeOn(c.date()), c.date()));
        Statement last = statements.last(sheetId, person.id());
        boolean boundThen = last != null && last.madeOn() != null && !last.madeOn().isAfter(date);
        Map<Long, List<ClaimLogLine>> logs = log.bySheet(sheetId);
        List<Claim> mine = claims.list(sheetId).stream().filter((c) -> c.personId() == person.id()).toList();
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        boolean figured = false;
        int counted = 0;
        for (Claim c : mine) {
            List<ClaimLogLine> lines = logs.getOrDefault(c.id(), List.of());
            ClaimStatus status = lines.isEmpty() ? c.status() : ClaimDue.statusAsOf(lines, date);
            Claim then = status == null ? null : asItStood(c, status, boundThen);
            if (then == null || !ClaimDue.countsLive(then, person, date)) {
                claims.bind(c, false, null);
                continue;
            }
            BigDecimal figure = figure(then, person.gradeOn(c.date()), sheet, rounding);
            figured |= figure != null;
            if (figure != null) {
                total = total.add(late.add(then, figure, sheet.floor(c.date())));
            }
            counted++;
            claims.bind(c, true, figure);
        }
        String dueTotal = sheet.rate(person.gradeOn(date), date) == null && !figured ? "n/a"
                : SettingRepository.capped(total, sheet.cap()).toPlainString();
        Statement statement = new Statement(sheetId, person.id(), date, dueTotal, counted, taker.by(), date, today);
        statements.add(statement);
        taken.set(statement);
    }

    /** The claim as it stood: with the status it had then, its settled due only while SETTLED, and its statement
     *  binding only when that statement was taken by then. */
    private static Claim asItStood(Claim c, ClaimStatus status, boolean boundThen) {
        boolean stated = boundThen && c.stated();
        return new Claim(c.id(), c.sheetId(), c.personId(), c.amount(), c.date(), c.note(), status, c.number(),
                status == ClaimStatus.SETTLED ? c.due() : null, c.openedBy(), stated, stated ? c.statedDue() : null,
                c.corrects(), c.dueBy(), c.deferredUntil(), c.proofs(), c.splitFrom(), c.ref(), c.tags());
    }

    /** The figure the claim had then (see ClaimDue#figure), at the rate in force then on its date. */
    private static BigDecimal figure(Claim c, String grade, SheetAsOf sheet, Rounding rounding) {
        if (c.due() != null) {
            return c.due();
        }
        if (ClaimDue.bound(c)) {
            return c.statedDue();
        }
        Rate rate = sheet.rate(grade, c.date());
        if (rate == null) {
            return null;
        }
        RoundingMode mode = rounding.mode(c);
        return c.amount().multiply(rate.rate()).movePointLeft(2).setScale(2, mode);
    }
}

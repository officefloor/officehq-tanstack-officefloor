package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.asof.SheetDayRepository;
import net.officefloor.hq.app.asof.SheetState;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.Rounding;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.Cap;
import net.officefloor.hq.app.setting.DatedSettings;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Takes the person's statement: records their due-total now, binds each of their claims that counts to the
 * figure it has now (a live one at its rate alone, not raised to the floor), and unbinds (live again) each that does not. The due-total is their counting claims'
 * dues added up, at most the cap in force on the sheet's today (each the greater of the floor in force on its date and its figure plus any late charge on
 * it — the charge and floor are never bound, they keep following the sheet's today and settings); n/a when their grade on the sheet's today has no rate there and no counting claim has a figure (kept, or at the
 * rate for the grade in force on its date).
 * <p>
 * Taken as of a date (on or before the sheet's today), it works on the sheet as it stood at the end of that date — every
 * change made while the sheet's today was on or before it, none made later — with that date standing in for the
 * sheet's today: it binds the person's claims that counted then to their figures then, unbinds every other claim of
 * theirs, and records that day's due-total and that date. Refused (400) for a date after the sheet's today, or one
 * when the person was not yet on the sheet.
 */
public class TakeStatement {
    public void take(@Val Person person, @Val StatementAsOf asOf, ClaimRepository claims, PersonRepository people,
            RateRepository rates, SheetRepository sheets, SettingRepository settings, SheetDayRepository days,
            Out<Statement> statement, Out<Person> updated) throws InvalidStatementException {
        LocalDate today = sheets.today(person.sheetId());
        LocalDate on = asOf.date() == null ? today : asOf.date();
        if (on.isAfter(today)) {
            throw new InvalidStatementException("A statement cannot be taken as of a date after the sheet's today.");
        }
        List<Claim> personClaims = claims.findForPerson(person.sheetId(), person.id());
        SheetState then = asOf.date() == null ? null : days.asOf(person.sheetId(), on);
        Person thenPerson = person;
        List<Claim> thenClaims = personClaims;
        List<Rate> sheetRates;
        DatedSettings dated;
        LocalDate policyDate;
        if (then == null) {
            sheetRates = rates.findAll(person.sheetId());
            dated = settings.dated(person.sheetId());
            policyDate = settings.policyDate(person.sheetId());
        } else {
            thenPerson = then.person(person.id());
            if (thenPerson == null) {
                throw new InvalidStatementException(person.name() + " was not on the sheet on " + on + ".");
            }
            thenClaims = then.claimsOf(person.id());
            sheetRates = then.rates();
            dated = then.dated();
            policyDate = then.policyDate();
        }
        Rounding rounding = Rounding.of(policyDate == null ? on : policyDate, thenClaims);
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        boolean kept = false;
        int counted = 0;
        Map<Long, BigDecimal> figures = new HashMap<>();
        for (Claim claim : thenClaims) {
            if (!thenPerson.counts(claim, on)) {
                continue;
            }
            boolean fixed = claim.keepsFigure();
            BigDecimal rate = Rate.inForce(sheetRates, thenPerson.gradeOn(claim.date()), claim.date());
            BigDecimal figure = claim.figure(rate, rounding);
            figures.put(claim.id(), figure);
            counted++;
            kept |= fixed || figure != null;
            if (figure != null) {
                total = total.add(claim.due(rate, dated.floorOn(claim.date()), dated.lateCharge(on), rounding));
            }
        }
        for (Claim claim : personClaims) {
            boolean stated = figures.containsKey(claim.id());
            claims.bindStatement(claim, stated, stated ? figures.get(claim.id()) : null);
        }
        String dueTotal = Rate.inForce(sheetRates, thenPerson.gradeOn(on), on) == null && !kept ? "n/a"
                : Cap.apply(dated.capOn(on), total).setScale(2).toPlainString();
        statement.set(new Statement(person.name(), dueTotal, counted, asOf.date()));
        updated.set(people.recordStatement(person, dueTotal, on));
    }
}

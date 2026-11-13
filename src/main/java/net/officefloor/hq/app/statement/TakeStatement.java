package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
 * figure it has now (a live one raised to the floor in force on its date), and unbinds (live again) each that does not. The due-total is their counting claims'
 * dues added up, at most the cap in force on the sheet's today (each figure plus any late charge on it — the charge is never bound, it keeps following the
 * sheet's today); n/a when their grade on the sheet's today has no rate there and no counting claim has a figure (kept, or at the
 * rate for the grade in force on its date).
 */
public class TakeStatement {
    public void take(@Val Person person, ClaimRepository claims, PersonRepository people, RateRepository rates,
            SheetRepository sheets, SettingRepository settings, Out<Statement> statement, Out<Person> updated) {
        LocalDate today = sheets.today(person.sheetId());
        List<Rate> sheetRates = rates.findAll(person.sheetId());
        DatedSettings dated = settings.dated(person.sheetId());
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        boolean kept = false;
        int counted = 0;
        List<Claim> personClaims = claims.findForPerson(person.sheetId(), person.id());
        Rounding rounding = settings.rounding(person.sheetId(), today, personClaims);
        for (Claim claim : personClaims) {
            if (!person.counts(claim)) {
                claims.bindStatement(claim, false, null);
                continue;
            }
            boolean fixed = claim.keepsFigure();
            BigDecimal figure = claim.figure(Rate.inForce(sheetRates, person.gradeOn(claim.date()), claim.date()),
                    dated.floorOn(claim.date()), rounding);
            claims.bindStatement(claim, true, figure);
            counted++;
            kept |= fixed || figure != null;
            if (figure != null) {
                total = total.add(figure).add(dated.lateCharge(claim.date(), today).on(claim, figure, rounding.modeFor(claim)));
            }
        }
        String dueTotal = Rate.inForce(sheetRates, person.grade(), today) == null && !kept ? "n/a"
                : Cap.apply(dated.capOn(today), total).setScale(2).toPlainString();
        statement.set(new Statement(person.name(), dueTotal, counted));
        updated.set(people.recordStatement(person, dueTotal, today));
    }
}

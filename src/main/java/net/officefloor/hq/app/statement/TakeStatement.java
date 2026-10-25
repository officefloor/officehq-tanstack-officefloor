package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.access.LateCharge;
import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.Statements;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Takes the person's statement on the sheet's today: records their due-total, binds each of their counting claims
 * to the figure it has now, and unbinds the rest. A claim's figure is its settled due when it has one, else the
 * figure its last statement bound it to, else its amount times the rate in force on the claim's date of its
 * person's grade in force on that date (rounded as the claim rounds by the sheet's policy date, and at least the sheet's floor; none without a rate). The due-total is the counting claims' figures
 * added up, each with its late charge on top while it is overdue (the charge is never bound); n/a
 * (null) when the grade has no rate today and no counting claim keeps a settled or bound figure.
 */
public class TakeStatement {
    public void take(@Val Person person, ClaimRepository claims, RateRepository rates, SheetRepository sheets,
            SettingRepository settings, StatementRepository statements, Out<Statement> taken) {
        String sheetId = person.getSheetId();
        LocalDate today = sheets.today(sheetId);
        BigDecimal floor = settings.floor(sheetId);
        LateCharge late = settings.lateCharge(sheetId, today);
        List<Claim> mine = claims.listOf(sheetId, person.getId());
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        boolean kept = false;
        int counted = 0;
        for (Claim c : mine) {
            // OPEN and PENDING claims dated within a hold do not count, unless the last statement bound them.
            if (!c.getStatus().counts() || Rollup.held(c, person)) {
                claims.bind(c.getId(), false, null);
                continue;
            }
            counted++;
            kept |= c.getSettledDue() != null || (c.isBound() && c.getStatedDue() != null);
            BigDecimal figure = figure(c, person, rates, floor, late);
            // A figure at a grade other than today's does not hang on today's grade having a rate; nor does one
            // dated on or before today, whose rate may have ended since.
            kept |= figure != null && (!person.gradeOn(c.getDate()).equals(person.getGrade()) || !c.getDate().isAfter(today));
            if (figure != null) {
                sum = sum.add(late.due(c, figure));
            }
            claims.bind(c.getId(), true, figure);
        }
        BigDecimal dueTotal = kept || rates.find(sheetId, person.getGrade(), today) != null ? sum : null;
        statements.insert(sheetId, person.getId(), dueTotal, today);
        person.setStatements(new Statements(person.getStatements().count() + 1, dueTotal, today));
        taken.set(new Statement(dueTotal, counted));
    }

    private static BigDecimal figure(Claim claim, Person person, RateRepository rates, BigDecimal floor, LateCharge late) {
        if (claim.getSettledDue() != null) {
            return claim.getSettledDue();
        }
        if (claim.isBound()) {
            return claim.getStatedDue();
        }
        BigDecimal rate = rates.find(claim.getSheetId(), person.gradeOn(claim.getDate()), claim.getDate());
        return claim.figureAt(rate, floor, late.rounding(claim));
    }
}

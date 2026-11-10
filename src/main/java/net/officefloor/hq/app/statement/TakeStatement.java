package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.util.List;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimFigure;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Takes the person's statement: their due-total now (the figures of their claims that count, added
 * up, skipping n/a; n/a when their grade in force today has no rate today and no claim that counts keeps a fixed
 * figure, has one under another grade in force on its date, or has one dated on or before today (under a
 * rate that has since ended); held to the sheet's cap; each claim at its date's grade's rate; an overdue claim's late charge on top), as the screen shows it. Every claim of theirs that counts is bound to its figure now (a live
 * one at least the sheet's floor), never its late charge;
 * every other claim of theirs is left unbound (live). The person's statement count goes up by one,
 * and the due-total and the sheet's today are kept as the last statement's.
 */
public class TakeStatement {
    public void take(@Val Sheet sheet, @Val Person person, ClaimRepository claims, RateRepository rates,
            SettingsRepository settings, PersonRepository people, Out<Statement> statementOut) {
        List<Rate> all = rates.findAll(sheet.id());
        Settings sheetSettings = settings.find(sheet.id());
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        boolean fixed = false;
        int counted = 0;
        for (Claim claim : claims.findByPerson(sheet.id(), person.getId())) {
            boolean counts = claim.counts(person.heldOn(claim.getDate()));
            if (counts) {
                counted++;
                if (claim.getSettledDue() != null || claim.boundDue() != null) fixed = true;
                String grade = person.gradeOn(claim.getDate());
                BigDecimal figure = ClaimFigure.of(claim, all, grade, sheetSettings, sheet.today());
                // A figure found under a grade other than today's is kept, whatever today's rate.
                if (figure != null && !grade.equals(person.gradeOn(sheet.today()))) fixed = true;
                // So is one dated on or before today, found under a rate that has since ended.
                if (figure != null && !claim.getDate().isAfter(sheet.today())) fixed = true;
                // The due-total carries the late charge; the claim is bound to the figure under it.
                if (figure != null) total = total.add(figure).add(ClaimFigure.late(claim, figure, sheetSettings, sheet.today()));
                claim.bind(true, figure);
            } else {
                claim.bind(false, null);
            }
            claims.save(claim);
        }
        String dueTotal = Rate.inForce(all, person.gradeOn(sheet.today()), sheet.today()).isEmpty() && !fixed
                ? "n/a" : sheetSettings.capped(total, sheet.today()).toPlainString();
        person.setStatements(person.getStatementCount() + 1, dueTotal, sheet.today());
        people.saveStatements(person);
        statementOut.set(new Statement(dueTotal, counted));
    }
}

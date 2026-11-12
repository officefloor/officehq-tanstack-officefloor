package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimFigure;
import net.officefloor.hq.app.claim.ClaimStore;
import net.officefloor.hq.app.claim.Lateness;
import net.officefloor.hq.app.claim.Rounding;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.RateStore;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Takes a statement of the person on the sheet's today: their due-total (the sum of the figures of
 *  their claims that count; n/a when their grade in force today has no rate today and no counting claim
 *  keeps a fixed figure or a figure at another grade, the one in force on its date), binding each counting claim to its figure now (its floored live due when not fixed) and unbinding the rest. A settled
 *  claim keeps its fixed figure. The due-total includes the late charges of overdue claims, but a
 *  claim is bound to its figure without its charge, which keeps following the sheet's today. */
public class TakeStatement {
    public void take(@Val StatementRequest request, @Val Person person, ClaimStore claims, RateStore rates,
            SheetStore sheets, SettingStore settings, StatementStore statements, Out<Statement> statementOut) {
        String sheetId = person.getSheetId();
        LocalDate today = sheets.today(sheetId);
        BigDecimal floor = settings.floor(sheetId);
        Rounding rounding = Rounding.of(sheetId, settings, sheets, claims);
        Lateness lateness = new Lateness(settings.latePct(sheetId), today, rounding);
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        boolean fixed = false;
        int counted = 0;
        // Locked, so a status change or another statement made at once waits for this one.
        for (Claim claim : claims.listForUpdate(sheetId, person.getId())) {
            // An OPEN (or PENDING) claim dated within a hold counts in no live total unless a statement bound
            // it, so a statement neither counts nor binds it; one it bound before stays counted.
            boolean counts = RollupService.counts(claim, person);
            BigDecimal figure = null;
            if (counts) {
                figure = ClaimFigure.of(claim, rates.find(sheetId, person.gradeOn(claim.getDate()), claim.getDate()), floor, rounding);
                counted++;
                if (figure != null) {
                    total = total.add(claim.getSettledDue() != null ? figure : figure.add(lateness.charge(claim, figure)));
                }
                // A figure at a grade other than the one in force today does not hang on today's grade's rate.
                fixed |= claim.getSettledDue() != null || claim.isBound()
                        || (figure != null && !person.gradeOn(claim.getDate()).equals(person.gradeOn(today)));
            }
            claim.setStated(counts);
            claim.setStatedDue(figure);
            claims.bind(claim);
        }
        boolean na = rates.find(sheetId, person.gradeOn(today), today) == null && !fixed;
        Statement statement = new Statement(person.getId(), today, na ? null : total, counted);
        statements.add(sheetId, statement, request.user());
        statementOut.set(statement);
    }
}

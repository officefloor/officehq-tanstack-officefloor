package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimFigure;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.ClaimStore;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.RateStore;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Takes a statement of the person on the sheet's today: their due-total (the sum of the figures of
 *  their claims that count; n/a when their grade has no rate today and no counting claim keeps a
 *  fixed figure), binding each counting claim to its figure now and unbinding the rest. A settled
 *  claim keeps its fixed figure. */
public class TakeStatement {
    public void take(@Val StatementRequest request, @Val Person person, ClaimStore claims, RateStore rates,
            SheetStore sheets, StatementStore statements, Out<Statement> statementOut) {
        String sheetId = person.getSheetId();
        LocalDate today = sheets.today(sheetId);
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        boolean fixed = false;
        int counted = 0;
        // Locked, so a status change or another statement made at once waits for this one.
        for (Claim claim : claims.listForUpdate(sheetId, person.getId())) {
            boolean counts = claim.getStatus() == ClaimStatus.OPEN || claim.getStatus() == ClaimStatus.SETTLED;
            BigDecimal figure = null;
            if (counts) {
                figure = ClaimFigure.of(claim, rates.find(sheetId, person.getGrade(), claim.getDate()));
                counted++;
                if (figure != null) {
                    total = total.add(figure);
                }
                fixed |= claim.getSettledDue() != null || (claim.isStated() && claim.getStatedDue() != null);
            }
            claim.setStated(counts);
            claim.setStatedDue(figure);
            claims.bind(claim);
        }
        boolean na = rates.find(sheetId, person.getGrade(), today) == null && !fixed;
        Statement statement = new Statement(person.getId(), today, na ? null : total, counted);
        statements.add(sheetId, statement, request.user());
        statementOut.set(statement);
    }
}

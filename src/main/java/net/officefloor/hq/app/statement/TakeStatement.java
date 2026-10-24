package net.officefloor.hq.app.statement;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimFigures;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Takes a statement for the person: records their due-total now, on the sheet's today, and binds each of
 * their claims that counts to the figure it has now; their claims that do not count are unbound (live again).
 */
public class TakeStatement {
    public void take(@Val Sheet sheet, @Val Person person, ClaimRepository claims, RateRepository rates,
            StatementRepository statements, EntityManager entityManager, Out<Statement> statementOut) {
        // Lock the person so two statements (or a statement and an edit) are taken one after the other.
        entityManager.refresh(person, LockModeType.PESSIMISTIC_WRITE);
        List<Claim> mine = claims.findBySheetIdAndPersonIdOrderByIdDesc(sheet.getId(), person.getId());
        ClaimFigures figures = new ClaimFigures(rates.findBySheetIdAndGrade(sheet.getId(), person.getGrade()), sheet.getFloor());
        String dueTotal = figures.dueTotal(mine, sheet.getToday());
        int counted = 0;
        for (Claim claim : mine) {
            if (ClaimFigures.counts(claim)) {
                BigDecimal figure = figures.figure(claim);
                claim.bind(true, figure);
                counted++;
            } else {
                claim.bind(false, null);
            }
        }
        claims.saveAllAndFlush(mine);
        statementOut.set(statements.save(
                new Statement(sheet.getId(), person.getId(), dueTotal, sheet.getToday(), counted)));
    }
}

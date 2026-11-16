package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.util.List;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimDue;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Takes the person's statement on the sheet's today: their due-total now (the figures of their claims that count
 * added up; n/a when their grade has no rate today and no counting claim keeps a figure), and binds every claim of
 * theirs that counts to the figure it has now; one that does not count is unbound (live again).
 */
public class TakeStatement {
    public void take(@Val Person person, @Val StatementTaker taker, ClaimRepository claims, RateRepository rates,
            SheetRepository sheets, StatementRepository statements, Out<Statement> taken) {
        String sheetId = person.sheetId();
        var today = sheets.today(sheetId);
        List<Claim> mine = claims.list(sheetId).stream().filter((c) -> c.personId() == person.id()).toList();
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        boolean kept = false;
        int counted = 0;
        for (Claim c : mine) {
            if (!ClaimDue.counts(c)) {
                claims.bind(c, false, null);
                continue;
            }
            BigDecimal figure = ClaimDue.figure(c, person.grade(), rates);
            kept |= ClaimDue.kept(c);
            if (figure != null) {
                total = total.add(figure);
            }
            counted++;
            claims.bind(c, true, figure);
        }
        String dueTotal = rates.find(sheetId, person.grade(), today) == null && !kept ? "n/a" : total.toPlainString();
        Statement statement = new Statement(sheetId, person.id(), today, dueTotal, counted, taker.by());
        statements.add(statement);
        taken.set(statement);
    }
}

package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Takes a statement of the person: records their due-total now, binds each of their counting claims to the figure
 * it has now and unbinds the rest. The due-total is n/a when their grade has no rate on the sheet's today, unless a
 * counting claim keeps a figure (settled or bound): then it is the figures there are.
 */
public class TakeStatement {
    public void take(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Person person,
            Claims claims, Rates rates, Statements statements, SheetClock clock, Audit audit) {
        LocalDate today = clock.today(sheet);
        List<Rate> sheetRates = rates.list(sheet);
        List<Claim> mine = claims.list(sheet).stream().filter((c) -> c.person() == person.id()).toList();
        boolean na = Rate.on(sheetRates, person.grade(), today) == null
                && mine.stream().noneMatch((c) -> c.counts() && c.fixed());
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        int count = 0;
        for (Claim claim : mine) {
            if (claim.counts()) {
                BigDecimal due = claim.due(sheetRates, person.grade());
                claims.setStated(sheet, claim.id(), true, due);
                total = due == null ? total : total.add(due);
                count++;
            } else if (claim.stated()) {
                claims.setStated(sheet, claim.id(), false, null);
            }
        }
        BigDecimal dueTotal = na ? null : total;
        statements.add(sheet, person.id(), dueTotal, today);
        audit.record("STATEMENT sheet=" + sheet + " by=" + user + " name=" + person.name() + " due-total="
                + (dueTotal == null ? "n/a" : dueTotal.toPlainString()) + " claims=" + count);
    }
}

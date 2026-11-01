package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.claim.Late;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Takes a statement of the person: records their due-total now (late charges included), binds each of their
 * counting claims to the figure it has now (without its late charge) and unbinds the rest. The due-total is n/a
 * when their grade has no rate on the sheet's today, unless a
 * counting claim has a figure apart from it (settled, bound, or at another grade in force on its date): then it is the figures there are.
 */
public class TakeStatement {
    public void take(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Person person,
            Claims claims, Rates rates, Statements statements, SheetClock clock, Settings settings, Audit audit,
            Rollup rollup) {
        Rollup.Snapshot before = rollup.take(sheet);
        LocalDate today = clock.today(sheet);
        List<Rate> sheetRates = rates.list(sheet);
        BigDecimal floor = settings.floor(sheet);
        Late late = settings.late(sheet, today);
        List<Claim> mine = claims.list(sheet).stream().filter((c) -> c.person() == person.id()).toList();
        boolean na = Rate.on(sheetRates, person.grade(), today) == null
                && mine.stream().noneMatch((c) -> c.counts(person.held()) && person.figureApartFrom(c, today, sheetRates, floor));
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        int count = 0;
        for (Claim claim : mine) {
            if (claim.counts(person.held())) {
                // The statement binds the figure; the late charge keeps following the sheet's today.
                claims.setStated(sheet, claim.id(), true, claim.figure(sheetRates, person.gradeOn(claim.date()), floor));
                BigDecimal due = claim.due(sheetRates, person.gradeOn(claim.date()), floor, late);
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
        rollup.record(sheet, user, before);
    }
}

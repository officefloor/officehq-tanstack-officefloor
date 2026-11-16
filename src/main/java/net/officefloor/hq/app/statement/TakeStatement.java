package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.util.List;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimDue;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.LateCharge;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Takes the person's statement on the sheet's today: their due-total now (the figures of their claims that count
 * added up, each with its late charge; n/a when their grade has no rate today and no counting claim has a
 * figure, kept or at the grade in force on its date), and binds every claim of theirs that counts (a held person's OPEN or PENDING claims do not) to the figure it has now
 * (never its late charge, which keeps following today); one that does not count is unbound (live again).
 */
public class TakeStatement {
    public void take(@Val Person person, @Val StatementTaker taker, ClaimRepository claims, RateRepository rates,
            SheetRepository sheets, SettingRepository settings, StatementRepository statements, Out<Statement> taken) {
        String sheetId = person.sheetId();
        var today = sheets.today(sheetId);
        BigDecimal floor = settings.floor(sheetId);
        LateCharge late = LateCharge.of(sheetId, sheets, settings, claims);
        List<Claim> mine = claims.list(sheetId).stream().filter((c) -> c.personId() == person.id()).toList();
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        boolean figured = false;
        int counted = 0;
        for (Claim c : mine) {
            if (!ClaimDue.countsLive(c, person.held())) {
                claims.bind(c, false, null);
                continue;
            }
            BigDecimal figure = ClaimDue.figure(c, person.gradeOn(c.date()), rates, floor, late.rounding());
            figured |= figure != null;
            if (figure != null) {
                total = total.add(late.add(c, figure));
            }
            counted++;
            claims.bind(c, true, figure);
        }
        String dueTotal = rates.find(sheetId, person.gradeOn(today), today) == null && !figured ? "n/a" : total.toPlainString();
        Statement statement = new Statement(sheetId, person.id(), today, dueTotal, counted, taker.by());
        statements.add(statement);
        taken.set(statement);
    }
}

package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.access.LateCharge;
import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.Statements;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Takes the person's statement on the sheet's today: records their due-total, binds each of their counting claims
 * to the figure it has now, and unbinds the rest. A claim's figure is its settled due when it has one, else the
 * figure its last statement bound it to, else its amount times its person's grade's rate in force on the claim's
 * date (rounded half to even, and at least the sheet's floor; none without a rate). The due-total is the counting claims' figures
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
            // A held person's OPEN claims do not count; their SETTLED ones still do.
            if (!c.getStatus().counts() || (person.isHeld() && c.getStatus() == ClaimStatus.OPEN)) {
                claims.bind(c.getId(), false, null);
                continue;
            }
            counted++;
            kept |= c.getSettledDue() != null || (c.isStated() && c.getStatedDue() != null);
            BigDecimal figure = figure(c, person, rates, floor);
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

    private static BigDecimal figure(Claim claim, Person person, RateRepository rates, BigDecimal floor) {
        if (claim.getSettledDue() != null) {
            return claim.getSettledDue();
        }
        if (claim.isStated()) {
            return claim.getStatedDue();
        }
        BigDecimal rate = rates.find(claim.getSheetId(), person.getGrade(), claim.getDate());
        return rate == null ? null
                : claim.getAmount().multiply(rate).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN).max(floor);
    }
}

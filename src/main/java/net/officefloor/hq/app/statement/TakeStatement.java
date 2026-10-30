package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimResponse;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.claim.LateCharge;
import net.officefloor.hq.app.claim.Rounding;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Takes a statement: the person's due-total as the screen shows it now (the sum of their counting
 * claims' dues — each fixed or at the rate in force on its date, its late charge on top, raised to the floor in force on its date;
 * n/a when their grade has no rate today and no claim keeps a figure; held to the sheet's cap), and binds each counting claim to
 * its rate figure now — never the floor or its late charge, which keep being applied live.
 * A claim that does not count (an unbound OPEN claim dated within one of its person's holds, or a deferred claim, included) is left unbound.
 */
public class TakeStatement {
    public void take(@Val Person person, Claims claims, Rates rates, Sheets sheets, People people,
            Settings settings, Out<Statement> statement) {
        String sheetId = person.getSheetId();
        LocalDate today = sheets.today(sheetId);
        LocalDate policyDate = settings.policyDate(sheetId);
        List<Claim> own = claims.ofPerson(sheetId, person.getId());
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        boolean anyFigure = false;
        int counted = 0;
        for (Claim claim : own) {
            if (!person.countsLive(claim, today)) {
                claim.setStated(false);
                claim.setStatedDue(null);
            } else {
                RoundingMode rounding = Rounding.of(claim, policyDate);
                BigDecimal fixed = ClaimResponse.fixedDue(claim);
                // A bound claim keeps its figure (even n/a); a live one is worked at the rate on its date (unfloored).
                BigDecimal figure = fixed != null || claim.isBound() ? fixed
                        : Rates.due(claim.getAmount(), rates.find(sheetId, person.gradeOn(claim.getDate()), claim.getDate()), rounding);
                anyFigure |= fixed != null;
                if (figure != null) {
                    anyFigure = true;
                    total = total.add(LateCharge.due(claim, figure, settings.floor(sheetId, claim.getDate()),
                            on -> settings.latePct(sheetId, on), today, rounding));
                }
                claim.setStated(true);
                claim.setStatedDue(figure);
                counted++;
            }
            claims.bind(claim);
        }
        // n/a only when no claim keeps or gets a figure (each at the grade in force on its date).
        boolean na = !anyFigure && rates.find(sheetId, person.getGrade(), today) == null;
        BigDecimal dueTotal = na ? null : Settings.capped(total, settings.cap(sheetId));
        person.setStatementCount(person.getStatementCount() + 1);
        person.setStatedTotal(dueTotal);
        person.setStatementDate(today);
        people.saveStatement(person);
        people.addStatement(person);
        statement.set(new Statement(dueTotal, counted));
    }
}

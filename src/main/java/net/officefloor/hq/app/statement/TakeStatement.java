package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimResponse;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.claim.LateCharge;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Takes a statement: the person's due-total as the screen shows it now (the sum of their counting
 * claims' figures — fixed or at the rate in force on each claim's date, raised to the sheet's floor; n/a when their grade has no
 * rate today and no claim keeps a figure; an overdue claim's late charge included), and binds each
 * counting claim to its figure now — never its late charge, which keeps following the sheet's today.
 * A claim that does not count (a held person's OPEN claims included) is left unbound.
 */
public class TakeStatement {
    public void take(@Val Person person, Claims claims, Rates rates, Sheets sheets, People people,
            Settings settings, Out<Statement> statement) {
        String sheetId = person.getSheetId();
        LocalDate today = sheets.today(sheetId);
        BigDecimal floor = settings.floor(sheetId);
        BigDecimal latePct = settings.latePct(sheetId);
        int terms = settings.terms(sheetId);
        List<Claim> own = claims.ofPerson(sheetId, person.getId());
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        boolean anyFixed = false;
        int counted = 0;
        for (Claim claim : own) {
            if (!person.countsLive(claim)) {
                claim.setStated(false);
                claim.setStatedDue(null);
            } else {
                BigDecimal fixed = ClaimResponse.fixedDue(claim);
                // A bound claim keeps its figure (even n/a); a live one is worked at the rate on its date.
                BigDecimal figure = fixed != null || claim.isStated() ? fixed
                        : Rates.due(claim.getAmount(), rates.find(sheetId, person.getGrade(), claim.getDate()), floor);
                anyFixed |= fixed != null;
                if (figure != null) {
                    total = total.add(LateCharge.withCharge(claim, figure, latePct, terms, today));
                }
                claim.setStated(true);
                claim.setStatedDue(figure);
                counted++;
            }
            claims.bind(claim);
        }
        boolean na = !anyFixed && rates.find(sheetId, person.getGrade(), today) == null;
        BigDecimal dueTotal = na ? null : total;
        person.setStatementCount(person.getStatementCount() + 1);
        person.setStatedTotal(dueTotal);
        person.setStatementDate(today);
        people.saveStatement(person);
        statement.set(new Statement(dueTotal, counted));
    }
}

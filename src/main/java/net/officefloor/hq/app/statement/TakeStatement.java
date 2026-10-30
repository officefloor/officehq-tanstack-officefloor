package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimResponse;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.claim.LateCharge;
import net.officefloor.hq.app.claim.Rounding;
import net.officefloor.hq.app.person.InvalidPersonException;
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
 * <p>
 * Taken as of a date (on or before the sheet's today, and not before the person was added), the same is worked from the
 * store as it stood at the end of that date (see {@link AsOf}), that date standing in for the sheet's today: the claims
 * that counted then are bound to their figures then, the person's other claims are left unbound, and the statement is
 * dated that date.
 */
public class TakeStatement {
    public void take(@Val StatementRequest request, @Val Person person, Claims claims, AsOf asOf, Sheets sheets,
            People people, Out<Statement> statement) throws InvalidPersonException {
        String sheetId = person.getSheetId();
        LocalDate date = asOfDate(request, person, sheets.today(sheetId));
        AsOf.View view = date == null ? asOf.now(person) : asOf.at(person, date);
        LocalDate today = view.today();
        Person then = view.person();
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        boolean anyFigure = false;
        int counted = 0;
        // The figure each counting claim is bound to, by id (a null figure: n/a).
        Map<Long, BigDecimal> bound = new HashMap<>();
        for (Claim claim : view.claims()) {
            if (!then.countsLive(claim, today)) {
                continue;
            }
            RoundingMode rounding = Rounding.of(claim, view.policyDate(), view.rounding());
            BigDecimal fixed = ClaimResponse.fixedDue(claim);
            // A bound claim keeps its figure (even n/a); a live one is worked at the rate on its date (unfloored).
            BigDecimal figure = fixed != null || claim.isBound() ? fixed
                    : Rates.due(claim.getAmount(), view.rate().rate(then.gradeOn(claim.getDate()), claim.getDate(), claim.getRateMark()), rounding);
            if (figure != null) {
                anyFigure = true;
                total = total.add(LateCharge.due(claim, figure, view.floor().apply(claim.getDate()), view.latePct(),
                        today, rounding));
            }
            bound.put(claim.getId(), figure);
            counted++;
        }
        for (Claim claim : date == null ? view.claims() : claims.ofPerson(sheetId, person.getId())) {
            claim.setStated(bound.containsKey(claim.getId()));
            claim.setStatedDue(bound.get(claim.getId()));
            claims.bind(claim);
        }
        // n/a only when no claim keeps or gets a figure (each at the grade in force on its date).
        boolean na = !anyFigure && view.rate().rate(then.getGrade(), today, Rates.IN_FORCE) == null;
        BigDecimal dueTotal = na ? null : Settings.capped(total, view.cap());
        person.setStatementCount(person.getStatementCount() + 1);
        person.setStatedTotal(dueTotal);
        person.setStatementDate(today);
        people.saveStatement(person);
        people.addStatement(person);
        statement.set(new Statement(dueTotal, counted, date));
    }

    /** The date the statement is taken as of; null when it is taken now. Refused when it is not a date, is after the
     *  sheet's today, or is before the person was added. */
    private static LocalDate asOfDate(StatementRequest request, Person person, LocalDate today) throws InvalidPersonException {
        if (request.asOf() == null || request.asOf().isBlank()) {
            return null;
        }
        LocalDate date;
        try {
            date = LocalDate.parse(request.asOf().trim());
        } catch (DateTimeParseException e) {
            throw new InvalidPersonException("The statement date is not a date");
        }
        if (date.isAfter(today)) {
            throw new InvalidPersonException("A statement cannot be taken as of a date after the sheet's today");
        }
        if (person.getAddedOn() != null && date.isBefore(person.getAddedOn())) {
            throw new InvalidPersonException("The person was not on the sheet on that date");
        }
        return date;
    }
}

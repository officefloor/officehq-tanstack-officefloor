package net.officefloor.hq.app.person;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.claim.Figures;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Takes the person's statement: their due-total now, the sum of their counting claims' figures
 *  with any late charges, each at least the floor (n/a when there is no rate today and none has a figure), at most the sheet's cap, dated the sheet's today. Every claim
 *  of theirs that counts (not an OPEN one dated within one of their holds) is bound to its rate figure now, without its late charge or the floor; every other one is unbound.
 *  Taken as of a date (on or before the sheet's today), all of it is worked out on the sheet as it
 *  stood at the end of that date (see {@link AsOf}): the claims that counted then are bound to their
 *  figures then, and the statement is dated that date. Refused: a date after the sheet's today, or
 *  one the person was not yet on the sheet. */
public class TakeStatement {
    public void take(@Val StatementRequest request, @Val Person person, Claims claims, Figures figures,
            Rates rates, Sheets sheets, Settings settings, People people, AsOf asOf, Out<Statement> statement)
            throws InvalidSheetRequestException {
        LocalDate date = asOfDate(request);
        String sheetId = person.getSheetId();
        if (date != null && date.isAfter(sheets.today(sheetId))) {
            throw new InvalidSheetRequestException("A statement cannot be taken as of a date after the sheet's today");
        }
        Person locked = people.lock(sheetId, person.getId());
        person.setStatementCount(locked.getStatementCount());
        Taken taken = date == null ? work(person, claims, figures, rates, sheets, settings)
                : asOf.at(sheetId, date, () -> {
                    Person then = people.find(sheetId, person.getId());
                    return then == null ? null : work(then, claims, figures, rates, sheets, settings);
                });
        if (taken == null) {
            throw new InvalidSheetRequestException("This person was not on the sheet on " + date);
        }
        for (Claim claim : claims.ofPerson(sheetId, person.getId())) {
            boolean bound = taken.bound().containsKey(claim.getId());
            claim.setStated(bound);
            claim.setStatedDue(bound ? taken.bound().get(claim.getId()) : null);
            claims.save(claim);
        }
        person.setStatementCount(person.getStatementCount() + 1);
        person.setStatedTotal(taken.dueTotal());
        person.setStatementDate(taken.date());
        people.saveStatement(person);
        statement.set(new Statement(taken.dueTotal(), taken.bound().size(), request.user(), date));
    }

    /** The statement of the person as the sheet stands (live, or as of a date): the figure each
     *  counting claim is bound to, by id, and their due-total, dated the sheet's today. */
    private static Taken work(Person person, Claims claims, Figures figures, Rates rates, Sheets sheets,
            Settings settings) {
        String sheetId = person.getSheetId();
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        boolean any = false;
        Map<Long, BigDecimal> bound = new HashMap<>();
        for (Claim claim : claims.ofPerson(sheetId, person.getId())) {
            if (person.counts(claim)) {
                // The statement binds the rate figure; the late charge and floor on top keep following today.
                bound.put(claim.getId(), figures.figure(claim, person.gradeOn(claim.getDate())));
                BigDecimal figure = figures.due(claim, person.gradeOn(claim.getDate()));
                if (figure != null) {
                    total = total.add(figure);
                    any = true;
                }
            }
        }
        LocalDate today = sheets.today(sheetId);
        BigDecimal dueTotal = !any && rates.pct(sheetId, person.gradeOn(today), today) == null ? null
                : settings.capped(sheetId, total);
        return new Taken(bound, dueTotal, today);
    }

    /** The date the statement is taken as of; null when none was given. */
    private static LocalDate asOfDate(StatementRequest request) throws InvalidSheetRequestException {
        String text = request.asOf();
        if (text == null || text.isBlank()) return null;
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidSheetRequestException("Not a date: " + text);
        }
    }

    private record Taken(Map<Long, BigDecimal> bound, BigDecimal dueTotal, LocalDate date) {
    }
}

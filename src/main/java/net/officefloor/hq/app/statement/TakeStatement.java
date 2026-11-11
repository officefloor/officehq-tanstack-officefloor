package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimFigure;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimShare;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.pool.CutTotals;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Takes the person's statement: their due-total now (the figures of their claims that count, added
 * up, skipping n/a; n/a when their grade in force today has no rate today and no claim that counts keeps a fixed
 * figure, has one under another grade in force on its date, or has one dated on or before today (under a
 * rate that has since ended); held to the sheet's cap; each claim at its date's grade's rate; an overdue claim's late charge on top; a shared claim less its holders' parts, and the person's parts of claims shared with them added), as the screen shows it. Every claim of theirs that counts is bound to its rate figure now
 * (a live one floored) and the late charge on it as shown now (a figure and charge an earlier
 * statement bound stay as they are);
 * every other claim of theirs is left unbound (live). The person's statement count goes up by one,
 * and the due-total and the sheet's today are kept as the last statement's.
 * <p>
 * Taken as of a date (on or before the sheet's today, and not before the person was added), it is
 * taken as the sheet stood at the end of that date: every change made while the sheet's today was
 * on or before it counts (the rates, settings, the person's grades, holds and retirement, each
 * claim's status, settled due and statement binding), every later change does not, and the date
 * stands in for the sheet's today in every figure. That date is kept as the statement's date.
 */
public class TakeStatement {
    public void take(@Val Sheet sheet, @Val Person person, @Val StatementInput input, ClaimRepository claims,
            ClaimLogRepository logs, RateRepository rates, SettingsRepository settings, PersonRepository people,
            Out<Statement> statementOut) throws InvalidStatementException {
        LocalDate asOf = input.asOf();
        if (asOf != null && asOf.isAfter(sheet.today())) {
            throw new InvalidStatementException("A statement is taken as of a date on or before today");
        }
        if (asOf != null && !person.addedBy(asOf)) {
            throw new InvalidStatementException(person.getName() + " was not yet added on " + asOf);
        }
        LocalDate today = asOf != null ? asOf : sheet.today();
        List<Rate> all = asOf != null ? rates.findAsOf(sheet.id(), asOf) : rates.findAll(sheet.id());
        Settings sheetSettings = asOf != null ? settings.findAsOf(sheet.id(), asOf) : settings.find(sheet.id());
        Person then = asOf != null ? people.asOf(person, asOf) : person;
        Map<Long, ClaimLogRepository.Line> lines = asOf != null ? logs.linesOn(sheet.id(), asOf) : null;
        Map<Long, ClaimRepository.Binding> bindings = asOf != null ? claims.bindingsOn(sheet.id(), asOf) : null;
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        boolean fixed = false;
        int counted = 0;
        LocalDate latest = null;
        // Taken now, a shared claim counts less its holders' parts, and the parts of others' claims shared with them count.
        Map<Long, List<ClaimShare>> shares = asOf == null ? claims.shares(sheet.id()) : Map.of();
        List<Person> everyone = people.findAll(sheet.id());
        if (asOf == null) {
            BigDecimal held = Rollup.heldParts(everyone, claims.findAll(sheet.id()), shares, all,
                    sheetSettings, today).get(person.getId());
            if (held != null) {
                total = total.add(held);
                fixed = true;
            }
        }
        for (Claim live : claims.findByPerson(sheet.id(), person.getId())) {
            Claim claim = live;
            if (asOf != null) {
                ClaimLogRepository.Line line = lines.get(live.getId());
                ClaimRepository.Binding binding = bindings.get(live.getId());
                claim = line == null ? null : live.asThen(line.status(),
                        line.status() == ClaimStatus.SETTLED ? line.due() : null,
                        binding != null && binding.stated(), binding == null ? null : binding.statedDue(),
                        binding == null ? null : binding.statedLate());
            }
            boolean counts = claim != null && claim.counts(then.heldOn(claim.getDate(), today), today);
            if (counts) {
                counted++;
                if (latest == null || claim.getDate().isAfter(latest)) latest = claim.getDate();
                if (claim.getSettledDue() != null || claim.boundDue() != null) fixed = true;
                String grade = then.gradeOn(claim.getDate());
                BigDecimal figure = claim.getSettledDue() != null ? claim.getSettledDue()
                        : ClaimFigure.of(claim, all, grade, sheetSettings, today);
                // A figure found under a grade other than today's is kept, whatever today's rate.
                if (figure != null && !grade.equals(then.gradeOn(today))) fixed = true;
                // So is one dated on or before today, found under a rate that has since ended.
                if (figure != null && !claim.getDate().isAfter(today)) fixed = true;
                // The due-total carries the late charge; the claim is bound to the floored rate figure under it.
                BigDecimal due = ClaimFigure.withLate(claim, all, grade, sheetSettings, today);
                if (due != null) total = total.add(Rollup.ownPart(claim, due, shares.get(claim.getId()), everyone, all,
                        sheetSettings, today));
                // Bound with the late charge on it as shown (a bound one stays as it is).
                BigDecimal late = figure == null || claim.getSettledDue() != null ? null
                        : ClaimFigure.late(claim, figure, grade, sheetSettings, today);
                live.bind(true, figure, late);
            } else {
                live.bind(false, null);
            }
            claims.save(live);
        }
        String dueTotal = Rate.inForce(all, then.gradeOn(today), today).isEmpty() && !fixed
                ? "n/a" : sheetSettings.capped(total,
                        CutTotals.of(sheet.id(), today, asOf).getOrDefault(person.getId(), BigDecimal.ZERO), latest).toPlainString();
        person.setStatements(person.getStatementCount() + 1, dueTotal, today);
        people.saveStatements(person);
        statementOut.set(new Statement(dueTotal, counted, asOf));
    }
}

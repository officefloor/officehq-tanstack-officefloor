package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.LateCharge;
import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.history.SheetHistory;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.Statements;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Takes the person's statement on the sheet's today: records their due-total, binds each of their counting claims
 * to the figure it has now, and unbinds the rest. A claim's figure is its settled due when it has one, else the
 * figure its last statement bound it to, else its amount times the rate in force on the claim's date of its
 * person's grade in force on that date (rounded as the claim rounds by the sheet's policy date; none without a rate) — the rate figure, never floored. The due-total is the counting claims' dues
 * added up: each figure with its late charge on top while it is overdue, then at least the floor in force on the claim's date (neither is bound); n/a
 * (null) when the grade has no rate today and no counting claim keeps a settled or bound figure; at most the sheet's cap in force on its today.
 * <p>
 * Taken as of a date on or before the sheet's today, the statement reads the sheet as it stood at the end of that
 * date (every change made while the sheet's today was that date or earlier, none made later; see SheetHistory), that
 * date standing in for the sheet's today in every figure: the claims of theirs that counted then are bound to their
 * figures then, every other claim of theirs is unbound, and the statement is dated that date. A date after the
 * sheet's today is refused, as is one before the person was on the sheet.
 */
public class TakeStatement {
    public void take(@Val Person person, @Val StatementAsOf asOf, ClaimRepository claims, RateRepository rates,
            SheetRepository sheets, SettingRepository settings, StatementRepository statements, SheetHistory history,
            Out<Statement> taken) throws BadRequestException {
        String sheetId = person.getSheetId();
        LocalDate today = sheets.today(sheetId);
        LocalDate on = asOf.on() == null ? today : asOf.on();
        if (on.isAfter(today)) {
            throw new BadRequestException("A statement cannot be taken as of a date after the sheet's today (" + today + ")");
        }
        Figures figures;
        if (on.equals(today)) {
            figures = figures(person, claims, rates, settings, today);
        } else {
            Long snapshot = history.snapshotAsOf(sheetId, on);
            figures = snapshot == null ? null : history.read(sheetId, snapshot, on, jdbc -> {
                Person then = new PersonRepository(jdbc).find(sheetId, person.getId());
                return then == null ? null
                        : figures(then, new ClaimRepository(jdbc), new RateRepository(jdbc), new SettingRepository(jdbc), on);
            });
            if (figures == null) {
                throw new BadRequestException(person.getName() + " was not on the sheet on " + on);
            }
        }
        for (Claim c : claims.listOf(sheetId, person.getId())) {
            boolean counted = figures.bound().containsKey(c.getId());
            claims.bind(c.getId(), counted, counted ? figures.bound().get(c.getId()) : null);
        }
        statements.insert(sheetId, person.getId(), figures.dueTotal(), on);
        person.setStatements(new Statements(person.getStatements().count() + 1, figures.dueTotal(), on));
        taken.set(new Statement(figures.dueTotal(), figures.bound().size(), asOf.on()));
    }

    /** A statement's figures: its due-total (null: n/a) and the figure each counting claim is bound to (null: n/a), by id. */
    private record Figures(BigDecimal dueTotal, Map<Long, BigDecimal> bound) {
    }

    /** The person's statement figures as of {@code today}, read through the repositories given. */
    private static Figures figures(Person person, ClaimRepository claims, RateRepository rates,
            SettingRepository settings, LocalDate today) {
        String sheetId = person.getSheetId();
        LateCharge late = settings.lateCharge(sheetId, today);
        List<Claim> mine = claims.listOf(sheetId, person.getId());
        Map<Long, BigDecimal> bound = new HashMap<>();
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        boolean kept = false;
        for (Claim c : mine) {
            // OPEN and PENDING claims dated within a hold, or once the retirement date is reached, do not count, unless the last statement bound them; a claim
            // deferred past today does not count and is left unbound.
            if (!c.getStatus().counts() || Rollup.held(c, person, today) || c.isDeferredOn(today)) {
                continue;
            }
            kept |= c.getSettledDue() != null || (c.isBound() && c.getStatedDue() != null);
            BigDecimal figure = figure(c, person, rates, late);
            // A figure at a grade other than today's does not hang on today's grade having a rate; nor does one
            // dated on or before today, whose rate may have ended since.
            kept |= figure != null && (!person.gradeOn(c.getDate()).equals(person.getGrade()) || !c.getDate().isAfter(today));
            if (figure != null) {
                sum = sum.add(late.due(c, figure));
            }
            bound.put(c.getId(), figure);
        }
        BigDecimal dueTotal = SettingRepository.capped(
                kept || rates.find(sheetId, person.getGrade(), today) != null ? sum : null, late.settings().cap(today));
        return new Figures(dueTotal, bound);
    }

    private static BigDecimal figure(Claim claim, Person person, RateRepository rates, LateCharge late) {
        if (claim.getSettledDue() != null) {
            return claim.getSettledDue();
        }
        if (claim.isBound()) {
            return claim.getStatedDue();
        }
        BigDecimal rate = rates.find(claim.getSheetId(), person.gradeOn(claim.getDate()), claim.getDate(),
                claim.getAddedSeq());
        return claim.figureAt(rate, late.rounding(claim));
    }
}

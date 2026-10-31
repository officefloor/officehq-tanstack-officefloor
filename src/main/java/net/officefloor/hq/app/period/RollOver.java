package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.opening.Openings;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Rolls the year over: generates the new fiscal year's monthly periods as {@link GeneratePeriods} does (PERIOD_ADDED
 * each), then adds each person's open figure at the end of the old year's last period (see
 * {@link PeriodCloser#openAt}) to their opening for the new year's first period (OPENING_SET, a person with nothing
 * open is left out; not again when an earlier rollover of the year still stands), then ROLLOVER with the old year and how many periods were made,
 * then soft-closes every period of the old year still open, in start-date order (PERIOD_SOFT_CLOSED each, see
 * {@link PeriodCloser#softClose}).
 */
public class RollOver {
    public void rollover(@PathVariable("sheetId") String sheetId, @Val ValidRollover request, Periods periods,
            People people, Claims claims, Openings openings, PeriodCloser closer, Rollovers rollovers, Audit audit,
            ObjectResponse<GenerateResponse> response) {
        ValidGenerate next = request.next();
        long rollover = rollovers.start(sheetId, request.user(), request.fiscalYear());
        List<String> made = new ArrayList<>();
        YearMonth last = YearMonth.from(next.to());
        for (YearMonth month = YearMonth.from(next.from()); !month.isAfter(last); month = month.plusMonths(1)) {
            ValidPeriod period = new ValidPeriod(month.toString(), month.atDay(1), month.atEndOfMonth(), request.user());
            if (periods.addIfFree(sheetId, period)) {
                made.add(period.name());
                audit.recordOnCommit("PERIOD_ADDED sheet=" + sheetId + " by=" + period.user()
                        + " period=" + period.name() + " from=" + period.from() + " to=" + period.to());
            }
        }
        rollovers.generated(rollover, made);
        int generated = made.size();
        LocalDate end = request.periods().stream().map(p -> LocalDate.parse(p.to()))
                .max(Comparator.naturalOrder()).orElseThrow();
        PeriodResponse first = periods.list(sheetId).stream()
                .filter(p -> !LocalDate.parse(p.from()).isBefore(next.from()) && !LocalDate.parse(p.from()).isAfter(next.to()))
                .findFirst().orElse(null);
        // A year already rolled over (and not undone) has had its openings carried: they are not carried again.
        if (first != null && !rollovers.standsBefore(sheetId, request.fiscalYear(), rollover)) {
            List<Claim> all = claims.list(sheetId);
            for (Person person : people.list(sheetId)) {
                BigDecimal open = closer.openAt(sheetId, person, all, request.periods(), end);
                if (open.signum() == 0) {
                    continue;
                }
                openings.addByRollover(sheetId, person.getId(), first.name(), open, rollover);
                audit.recordOnCommit("OPENING_SET sheet=" + sheetId + " by=" + request.user() + " name=" + person.getName()
                        + " period=" + first.name() + " amount=" + open.toPlainString());
            }
        }
        audit.recordOnCommit("ROLLOVER sheet=" + sheetId + " by=" + request.user() + " fy=" + request.fiscalYear()
                + " periods=" + generated);
        // Then every period of the old year still open is soft-closed, in start-date order.
        List<PeriodResponse> old = request.periods().stream()
                .sorted(Comparator.comparing((PeriodResponse p) -> LocalDate.parse(p.from()))).toList();
        for (PeriodResponse period : old) {
            PeriodResponse locked = periods.lock(sheetId, period.name());
            if (locked != null && !locked.closed() && !locked.softClosed()) {
                closer.softClose(sheetId, locked, request.user());
            }
        }
        response.send(new GenerateResponse(generated, periods.list(sheetId)));
    }
}

package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
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
 * open is left out), then ROLLOVER with the old year and how many periods were made.
 */
public class RollOver {
    public void rollover(@PathVariable("sheetId") String sheetId, @Val ValidRollover request, Periods periods,
            People people, Claims claims, Openings openings, PeriodCloser closer, Audit audit,
            ObjectResponse<GenerateResponse> response) {
        ValidGenerate next = request.next();
        int generated = 0;
        YearMonth last = YearMonth.from(next.to());
        for (YearMonth month = YearMonth.from(next.from()); !month.isAfter(last); month = month.plusMonths(1)) {
            ValidPeriod period = new ValidPeriod(month.toString(), month.atDay(1), month.atEndOfMonth(), request.user());
            if (periods.addIfFree(sheetId, period)) {
                generated++;
                audit.recordOnCommit("PERIOD_ADDED sheet=" + sheetId + " by=" + period.user()
                        + " period=" + period.name() + " from=" + period.from() + " to=" + period.to());
            }
        }
        LocalDate end = request.periods().stream().map(p -> LocalDate.parse(p.to()))
                .max(Comparator.naturalOrder()).orElseThrow();
        PeriodResponse first = periods.list(sheetId).stream()
                .filter(p -> !LocalDate.parse(p.from()).isBefore(next.from()) && !LocalDate.parse(p.from()).isAfter(next.to()))
                .findFirst().orElse(null);
        if (first != null) {
            List<Claim> all = claims.list(sheetId);
            for (Person person : people.list(sheetId)) {
                BigDecimal open = closer.openAt(sheetId, person, all, request.periods(), end);
                if (open.signum() == 0) {
                    continue;
                }
                openings.add(sheetId, person.getId(), first.name(), open);
                audit.recordOnCommit("OPENING_SET sheet=" + sheetId + " by=" + request.user() + " name=" + person.getName()
                        + " period=" + first.name() + " amount=" + open.toPlainString());
            }
        }
        audit.recordOnCommit("ROLLOVER sheet=" + sheetId + " by=" + request.user() + " fy=" + request.fiscalYear()
                + " periods=" + generated);
        response.send(new GenerateResponse(generated, periods.list(sheetId)));
    }
}

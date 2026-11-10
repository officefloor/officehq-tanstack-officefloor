package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Roll a fiscal year (FY<yyyy>) over, by a steward only: make the next fiscal year's twelve monthly periods exactly
 *  as the fiscal year generator does, then add each person's open figure at the end of the old year's last period
 *  (the dues now of their claims that count whose due-by falls in a period of the old year — one starting within it —
 *  and that were OPEN or PENDING at that period's end) to their opening balance for the new year's first period
 *  (OPENING_SET, only for a figure above 0.00, and only when this rollover generated that period — a repeated
 *  rollover adds nothing again), and write ROLLOVER; then soft-close, as {@link SoftClosePeriod} would, every period
 *  of the old year still open (neither closed nor soft-closed), by from-date. A year that is not yyyy, or one with no periods,
 *  is refused. The rollover is kept as the sheet's last one, so {@link UndoRollover} can take it back. */
public class RolloverYear {
    private static final Pattern YEAR = Pattern.compile("\\d{4}");

    public void rollover(@PathVariable("sheetId") String sheetId, @RequestBody GenerateFyRequest request,
            ServerHttpConnection connection, PeriodRepository repository, SettingsRepository settings,
            ClaimRepository claims, PersonRepository people, PeriodCloser closer, Audit audit, Out<GenerateResult> result)
            throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        String year = request == null || request.year() == null ? "" : request.year().trim();
        if (!YEAR.matcher(year).matches()) {
            throw new BadRequestException("fiscal year must be a year (yyyy)");
        }
        if (!repository.lockSheet(id)) {
            throw new BadRequestException("no such sheet");
        }
        String role = settings.role(id, by);
        if (!SettingsRepository.STEWARD.equals(role)) {
            throw new BadRequestException("a " + role + " cannot roll a year over");
        }
        YearMonth start = YearMonth.of(Integer.parseInt(year), settings.fiscalStartMonth(id));
        List<Period> old = within(repository.list(id), start);
        if (old.isEmpty()) {
            throw new BadRequestException("FY" + year + " has no periods");
        }
        LocalDate end = old.stream().map(p -> LocalDate.parse(p.to())).max(Comparator.naturalOrder()).orElseThrow();
        String today = repository.today(id);
        Map<Long, String> statusesThen = claims.statusesOn(id, end);
        Map<Long, BigDecimal> open = new LinkedHashMap<>();
        Map<Long, Person> byId = new LinkedHashMap<>();
        for (Person p : people.list(id)) {
            byId.put(p.id(), p);
            open.put(p.id(), BigDecimal.ZERO.setScale(2));
        }
        for (Claim c : claims.list(id)) {
            Person person = byId.get(c.personId());
            if (person == null) continue;
            String then = statusesThen.get(c.id());
            if (then == null || !(then.equals("OPEN") || then.equals("PENDING"))) continue;
            if (!c.countsAs(then, person.liveHolds(today), today)) continue;
            LocalDate on = LocalDate.parse(c.dueBy());
            boolean in = old.stream()
                    .anyMatch(q -> !on.isBefore(LocalDate.parse(q.from())) && !on.isAfter(LocalDate.parse(q.to())));
            if (!in) continue;
            BigDecimal now = claims.dueNow(id, c);
            if (now == null) continue;
            open.merge(c.personId(), now, BigDecimal::add);
        }

        YearMonth next = start.plusMonths(12);
        java.util.Set<Long> before = new java.util.HashSet<>();
        repository.list(id).forEach(p -> before.add(p.id()));
        int made = GeneratePeriods.months(id, by, next, next.plusMonths(11), repository, audit);
        List<Period> after = repository.list(id);
        List<Long> generated = after.stream().map(Period::id).filter(p -> !before.contains(p)).toList();
        List<Object[]> openings = new java.util.ArrayList<>();
        Period first = within(after, next).stream().findFirst().orElse(null);
        if (first != null && generated.contains(first.id())) {
            for (Map.Entry<Long, BigDecimal> e : open.entrySet()) {
                if (e.getValue().signum() <= 0) continue;
                repository.addOpening(id, first.id(), e.getKey(), e.getValue());
                openings.add(new Object[] { first.id(), e.getKey(), e.getValue() });
                audit.record("OPENING_SET sheet=" + id + " by=" + by + " name=" + byId.get(e.getKey()).name()
                        + " period=" + first.name() + " amount=" + e.getValue().setScale(2).toPlainString());
            }
        }
        repository.keepRollover(id, by, today, "FY" + year, generated, openings);
        audit.record("ROLLOVER sheet=" + id + " by=" + by + " fy=FY" + year + " periods=" + made);
        for (Period p : within(repository.list(id), start)) {
            if (!p.closed() && !p.softClosed()) closer.close(id, by, p, false, true);
        }
        result.set(new GenerateResult(made));
    }

    /** The periods starting within the fiscal year beginning in the given month, by from-date. */
    private static List<Period> within(List<Period> periods, YearMonth start) {
        LocalDate from = start.atDay(1);
        LocalDate to = start.plusMonths(11).atEndOfMonth();
        return periods.stream().filter(p -> {
            LocalDate f = LocalDate.parse(p.from());
            return !f.isBefore(from) && !f.isAfter(to);
        }).sorted(Comparator.comparing(Period::from)).toList();
    }
}

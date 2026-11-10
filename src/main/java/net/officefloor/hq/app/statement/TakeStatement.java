package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimShare;
import net.officefloor.hq.app.history.HistoryRepository;
import net.officefloor.hq.app.history.SheetAsOf;
import net.officefloor.hq.app.person.Hold;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.pool.PoolRepository;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Take a person's statement: record their due-total now, and bind each claim of theirs that counts (a
 *  retired person's open (OPEN or PENDING) claims do not, unless already bound; a hold no longer takes any out) to its due as shown now — its figure plus its late charge, at least the floor (a settled claim keeps its
 *  frozen due; a claim already bound keeps the due it was bound to): while bound, its late charge no longer follows the
 *  sheet's today; a claim with no figure because the rate that would apply to it has ended is bound to n/a; a
 *  claim that does not count is unbound (live again). The due-total is as shown: the sum of the counting claims' dues now (late charges
 *  included), skipping those with none; 'n/a' when no rate for the grade they have on the sheet's today
 *  is in force then and no counting claim has a fixed figure or a due (at the grade it was claimed under); at
 *  most the sheet's cap less their cut total, not below 0.00 (taken as of a date, at most that day's cap).
 *
 *  <p>Taken as of a date (on or before the sheet's today; a later one is refused, as is one before the person
 *  was added), it binds the claims of theirs that counted at the end of that date to their figures as they
 *  stood then: every change made while the sheet's today was on or before that date counts, every later one
 *  does not, and that date stands in for the sheet's today in every figure (see {@link SheetAsOf}). Every
 *  other claim of theirs is unbound. The statement's date is that date, its due-total that day's, and its
 *  record ends with asof=<date>. */
public class TakeStatement {
    public void take(@PathVariable("sheetId") String sheetId, @PathVariable("personId") String personId,
            @Val StatementInput input, ServerHttpConnection connection, PersonRepository people, ClaimRepository claims,
            SheetRepository sheets, SettingsRepository settings, HistoryRepository history, PoolRepository pools, Audit audit, Rollup rollup,
            Out<Person> person) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        long pid;
        try {
            pid = Long.parseLong(personId == null ? "" : personId.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("no such person");
        }
        Person found = people.find(id, pid);
        if (found == null) {
            throw new BadRequestException("no such person");
        }
        LocalDate today = sheets.today(id);
        LocalDate asof = input.asof();
        if (asof != null) {
            if (today != null && asof.isAfter(today)) {
                throw new BadRequestException("a statement cannot be taken as of a date after the sheet's today (" + today + ")");
            }
            if (!history.existed(id, pid, asof)) {
                throw new BadRequestException(found.name() + " was not on the sheet on " + asof);
            }
        }
        SheetAsOf then = asof == null ? null : history.asOf(id, asof);
        List<Hold> holds = then == null ? found.liveHolds(today == null ? null : today.toString()) : then.liveHolds(pid);
        Rollup.Snapshot before = rollup.take(id);
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        boolean fixed = false;
        int count = 0;
        // Taken now, a shared claim counts its due less its holders' parts, and their part of each counting claim
        // shared with them counts too (as in their due-total, see Rollup).
        Map<String, List<ClaimShare>> shares = then == null ? claims.shares(id) : Map.of();
        for (Claim live : claims.ofPerson(id, pid)) {
            // As of a date, the claim as it stood then: absent when it was added later, or was another's then.
            Claim c = then == null ? live : then.claims().get(live.id());
            if (c == null || c.personId() != pid || !c.counts(holds)) {
                claims.bind(id, live.id(), false, null, false);
                continue;
            }
            count++;
            fixed |= c.fixed();
            // The claim is bound to its due as shown, late charge and floor included.
            BigDecimal now = then == null ? claims.dueNow(id, c) : then.dueNow(c);
            BigDecimal figure = now;
            if (now != null) {
                total = total.add(now).subtract(Rollup.parts(now, shares.get(String.valueOf(c.id()))));
                fixed = true;
            }
            // With no figure (no rate applies, or it is bound to n/a already) it is bound to n/a: it stays
            // n/a even once a rate is in force on its date (a later rate, or a new grade for its person then).
            boolean na = figure == null;
            claims.bind(id, c.id(), true, figure, na);
        }
        if (then == null) {
            String on = today == null ? null : today.toString();
            for (Claim c : claims.list(id)) {
                List<ClaimShare> held = shares.get(String.valueOf(c.id()));
                if (held == null || c.personId() == pid) continue;
                ClaimShare mine = held.stream().filter(s -> s.personId() == pid).findFirst().orElse(null);
                Person owner = mine == null ? null : people.find(id, c.personId());
                if (owner == null || !c.counts(owner.liveHolds(on))) continue;
                BigDecimal now = claims.dueNow(id, c);
                if (now == null) continue;
                total = total.add(ClaimShare.part(now, mine.share()));
                fixed = true;
            }
        }
        boolean rated = then == null ? claims.rated(id, pid, today) : then.rated(pid);
        BigDecimal cap = then == null ? settings.cap(id) : then.cap();
        BigDecimal cut = then == null && cap != null ? pools.cutTotals(id).get(pid) : null;
        String dueTotal = fixed || rated ? SettingsRepository.capped(total, cap, cut).setScale(2).toPlainString() : "n/a";
        people.stated(id, pid, dueTotal, asof == null ? today : asof);
        audit.record("STATEMENT sheet=" + id + " by=" + by + " name=" + found.name() + " due-total=" + dueTotal
                + " claims=" + count + (asof == null ? "" : " asof=" + asof));
        rollup.record(id, by, before);
        person.set(people.find(id, pid));
    }
}

package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimFigure;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.ClaimStore;
import net.officefloor.hq.app.claim.Lateness;
import net.officefloor.hq.app.claim.Rounding;
import net.officefloor.hq.app.claim.Shares;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.rate.RateStore;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Takes a statement of the person on the sheet's today: their due-total (the sum of the figures of
 *  their claims that count; n/a when their grade in force today has no rate today and no counting claim
 *  keeps a fixed figure or a figure at another grade, the one in force on its date), binding each counting claim to its due as shown now, late charge and floor included (a claim already bound
 *  keeps the due it was bound to) and unbinding the rest. A settled claim keeps its fixed figure. The due-total is
 *  at most the sheet's cap. As in the sheet's figures (see {@link RollupService}), the parts of their counting claims
 *  shared with other people are left out of it, and their parts of other people's counting claims shared with them are
 *  added to it, at their own grade's rate (frozen once settled; see {@link Shares}) (those claims are not theirs to
 *  bind, nor counted among their claims).
 *  <p>
 *  Taken as of a date (on or before the sheet's today), everything is as it stood at the end of that date
 *  (see {@link net.officefloor.hq.app.history.History}): their claims that counted then are bound to their
 *  figures then, and the date stands in for the sheet's today in every figure (grade and rate in force, floor,
 *  late charges, holds, retirement, cap); every other claim of theirs is unbound. The statement is dated that
 *  date. Refused when the date is after the sheet's today, or the person was not on the sheet then. */
public class TakeStatement {
    public void take(@Val StatementRequest request, @Val Person person, ClaimStore claims, RateStore rates,
            SheetStore sheets, SettingStore settings, PersonStore people, StatementStore statements,
            Out<Statement> statementOut) throws InvalidSheetException {
        String sheetId = person.getSheetId();
        LocalDate today = sheets.today(sheetId);
        LocalDate asOf = request.asOfDate();
        if (asOf != null && asOf.isAfter(today)) {
            throw new InvalidSheetException("The statement date is after the sheet's today");
        }
        // Locked, so a status change or another statement made at once waits for this one.
        List<Claim> bound = claims.listForUpdate(sheetId, person.getId());
        // What the figures follow: the store as it is now, or as it stood at the end of the date.
        LocalDate on = today;
        Person then = person;
        List<Claim> all;
        Map<Long, Claim> was = new HashMap<>();
        if (asOf == null) {
            all = claims.list(sheetId);
            bound.forEach(claim -> was.put(claim.getId(), claim));
        } else {
            on = asOf;
            then = people.findAsOf(sheetId, person.getId(), asOf);
            if (then == null) {
                throw new InvalidSheetException("The person was not on the sheet on " + asOf);
            }
            rates = rates.asOf(asOf);
            settings = settings.asOf(asOf);
            all = claims.listAsOf(sheetId, asOf);
            all.stream().filter(claim -> claim.getPersonId() == person.getId()).forEach(claim -> was.put(claim.getId(), claim));
        }
        LocalDate policy = settings.policyDate(sheetId);
        Rounding rounding = new Rounding(policy != null ? policy : on, settings.roundings(sheetId), all);
        Person whose = then;
        Lateness lateness = new Lateness(settings.latePcts(sheetId), settings.floors(sheetId), on, rounding,
                Lateness.graced(settings.graces(sheetId), id -> id == whose.getId() ? whose : null));
        // The parts of the shared claims that count: each claim's parts added up, and theirs of others' claims.
        Map<Long, BigDecimal> shared = new HashMap<>();
        BigDecimal held = BigDecimal.ZERO.setScale(2);
        boolean holds = false;
        for (Claim claim : all) {
            List<Shares.Share> holders = Shares.list(claim.getShares());
            if (holders.isEmpty()) {
                continue;
            }
            Person owner = claim.getPersonId() == then.getId() ? then
                    : asOf == null ? people.find(sheetId, claim.getPersonId()) : people.findAsOf(sheetId, claim.getPersonId(), asOf);
            if (owner == null || !RollupService.countsClaimed(claim, owner, on) || !RollupService.counts(claim, owner, on)) {
                continue;
            }
            Lateness owners = new Lateness(settings.latePcts(sheetId), settings.floors(sheetId), on, rounding,
                    Lateness.graced(settings.graces(sheetId), id -> id == owner.getId() ? owner : null));
            BigDecimal figure = owners.due(claim, rates.forClaim(claim).find(sheetId, owner.gradeOn(claim.getDate()), claim.getDate()));
            if (figure == null) {
                continue;
            }
            for (Shares.Share holder : holders) {
                Person whom = holder.personId() == then.getId() ? then
                        : asOf == null ? people.find(sheetId, holder.personId()) : people.findAsOf(sheetId, holder.personId(), asOf);
                String grade = whom == null ? null : whom.gradeOn(claim.getDate());
                BigDecimal pct = grade == null ? null : rates.forClaim(claim).find(sheetId, grade, claim.getDate());
                BigDecimal part = Shares.part(claim, figure, holder.share(), pct, rounding);
                shared.merge(claim.getId(), part, BigDecimal::add);
                if (holder.personId() == then.getId()) {
                    held = held.add(part);
                    // A frozen part, or one at a grade other than theirs on the date or at a rate that ended
                    // before it, does not hang on that grade's rate.
                    holds |= claim.getStatus() == ClaimStatus.SETTLED || (pct != null && (!grade.equals(then.gradeOn(on))
                            || rates.forClaim(claim).endsBefore(sheetId, grade, claim.getDate(), on)));
                }
            }
        }
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        boolean fixed = holds;
        int counted = 0;
        // The date of their latest claim that counts: their cap is the one in force on it (else on the date).
        LocalDate latest = null;
        for (Claim live : bound) {
            // The claim as it stood then (itself when taken now); null when it was not theirs then.
            Claim claim = was.get(live.getId());
            // An OPEN (or PENDING) claim of a retired person counts in no live total unless a statement bound
            // it, so a statement neither counts nor binds it; one it bound before stays counted. A deferred
            // claim counts in no total until the sheet's today reaches its date: a statement leaves it unbound.
            boolean counts = claim != null && RollupService.counts(claim, then, on);
            BigDecimal figure = null;
            boolean na = false;
            if (counts) {
                if (latest == null || claim.getDate().isAfter(latest)) {
                    latest = claim.getDate();
                }
                figure = ClaimFigure.of(claim, rates.forClaim(claim).find(sheetId, then.gradeOn(claim.getDate()), claim.getDate()), rounding);
                counted++;
                if (figure != null) {
                    // Bound as shown: its charge and floor stop following the sheet's today.
                    figure = lateness.dueOf(claim, figure);
                    // The holders' parts of a shared claim are theirs, not its person's.
                    total = total.add(figure.subtract(shared.getOrDefault(claim.getId(), BigDecimal.ZERO)));
                }
                // A figure at a grade other than the one in force today, or at a rate that ended before
                // today, does not hang on today's grade's rate.
                fixed |= claim.getSettledDue() != null || (claim.isBound() && claim.getStatedDue() != null)
                        || (figure != null && (!then.gradeOn(claim.getDate()).equals(then.gradeOn(on))
                                || rates.forClaim(claim).endsBefore(sheetId, then.gradeOn(claim.getDate()), claim.getDate(), on)));
                // A claim whose rate ended before its date is bound to n/a, so a later rate leaves it so.
                na = figure == null && (claim.isBound()
                        || (claim.getSettledDue() == null && rates.forClaim(claim).ended(sheetId, then.gradeOn(claim.getDate()), claim.getDate())));
            }
            live.setStated(counts);
            live.setStatedDue(figure);
            live.setStatedNa(na);
            claims.bind(live);
        }
        total = total.add(held);
        boolean na = rates.find(sheetId, then.gradeOn(on), on) == null && !fixed;
        Statement statement = new Statement(person.getId(), on,
                na ? null : SettingStore.capped(total, settings.caps(sheetId).apply(latest != null ? latest : on)), counted);
        statements.add(sheetId, statement, request.user());
        statementOut.set(statement);
    }
}

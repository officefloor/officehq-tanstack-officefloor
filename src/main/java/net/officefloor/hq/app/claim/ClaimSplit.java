package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.period.PeriodGuard;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.settings.SettingsRepository;

/** Split an amount (more than 0.00, less than the claim's amount) off an OPEN claim not deferred nor in a closed
 *  period (judged by the original claim's due-by alone), by a user whose role may open a claim: the claim keeps the rest and is unbound from its last statement;
 *  a new OPEN claim with the split-off amount, the same date and note and no number (it is numbered when settled), opened by the user, with
 *  its due-by fixed now and the original's number as split-from, is listed under the person. Every check is made
 *  before anything is written, so a refusal writes nothing to the store or the audit. Answers the new claim. */
final class ClaimSplit {
    private ClaimSplit() {
    }

    static Claim apply(String id, long cid, String amount, String by, ClaimRepository repository,
            PeriodRepository periods, SettingsRepository settings, Audit audit, Rollup rollup) throws BadRequestException {
        return apply(id, cid, amount, by, false, repository, periods, settings, audit, rollup);
    }

    /** Make every check of the split, writing nothing: throws the refusal the split would give. */
    static void check(String id, long cid, String amount, String by, ClaimRepository repository,
            PeriodRepository periods, SettingsRepository settings) throws BadRequestException {
        apply(id, cid, amount, by, true, repository, periods, settings, null, null);
    }

    private static Claim apply(String id, long cid, String amount, String by, boolean checkOnly, ClaimRepository repository,
            PeriodRepository periods, SettingsRepository settings, Audit audit, Rollup rollup) throws BadRequestException {
        Claim found = repository.find(id, cid);
        if (found == null) {
            throw new BadRequestException("no such claim");
        }
        if (!found.status().equals("OPEN")) {
            throw new BadRequestException("a " + found.status() + " claim cannot be split");
        }
        if (found.deferred()) {
            throw new BadRequestException("a deferred claim cannot be split");
        }
        BigDecimal part = ClaimInput.amount(amount == null ? "" : amount.trim());
        if (part.signum() <= 0 || part.compareTo(new BigDecimal(found.amount())) >= 0) {
            throw new BadRequestException("the amount split off must be more than 0.00 and less than the claim's amount");
        }
        PeriodGuard.open(periods, id, LocalDate.parse(found.dueBy()));
        if (!settings.allows(id, "open", by)) {
            throw new BadRequestException("a " + settings.role(id, by) + " cannot open a claim, so cannot split one");
        }
        if (checkOnly) {
            return null;
        }
        Rollup.Snapshot before = rollup.take(id);
        Long nid = repository.split(id, found, part, by);
        if (nid == null) {
            throw new BadRequestException("a " + found.status() + " claim cannot be split");
        }
        // The claim split off is unnumbered until it is settled.
        String number = null;
        repository.log(id, nid, by);
        Claim kept = repository.find(id, cid);
        Claim added = repository.find(id, nid);
        String person = repository.personName(id, found.personId());
        audit.record("CLAIM_SPLIT sheet=" + id + " by=" + by + " person=" + person + " amount=" + kept.amount()
                + " date=" + kept.date() + Claim.auditNumber(kept.number()) + " into=");
        audit.record("CLAIM_OPENED sheet=" + id + " by=" + by + " person=" + person + " amount=" + added.amount()
                + " date=" + added.date() + Claim.auditNumber(number));
        rollup.record(id, by, before);
        return added;
    }
}

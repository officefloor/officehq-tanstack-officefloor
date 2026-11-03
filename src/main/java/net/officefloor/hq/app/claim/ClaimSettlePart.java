package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.period.PeriodGuard;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.WatchRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.settings.SettingsRepository;

/** Part-settle an OPEN claim: split the amount off exactly as the split button does (see {@link ClaimSplit}), then
 *  settle the new claim at once exactly as the settle button does (PENDING when the clerk limit says so), with the
 *  four-eyes rules and the proof rule judged on the original claim (the new claim's opener is the user, who may still
 *  part-settle). Every check of both is made before anything is written, so a refusal of either refuses the whole
 *  action and writes nothing to the store or the audit. Answers the new claim. */
final class ClaimSettlePart {
    private ClaimSettlePart() {
    }

    /** Carries a refusal out of the held audit, which drops its records. */
    private static final class Refused extends RuntimeException {
        final BadRequestException refusal;

        Refused(BadRequestException refusal) {
            super(refusal.getMessage(), null, false, false);
            this.refusal = refusal;
        }
    }

    static Claim apply(String id, long cid, String amount, String by, ClaimRepository repository, PeriodRepository periods,
            SettingsRepository settings, Audit audit, Rollup rollup, WatchRepository watches) throws BadRequestException {
        Claim original = repository.find(id, cid);
        if (original == null) {
            throw new BadRequestException("no such claim");
        }
        ClaimSplit.check(id, cid, amount, by, repository, periods, settings);
        // The settle's checks, made on the original (whose status, permission and rules the new claim shares) ...
        ClaimStatusChange.settlePart(id, cid, original, by, true, repository, periods, settings, null, null, null);
        // ... and on the new claim's own due-by.
        PeriodGuard.open(periods, id, repository.dueBy(id, original.personId(), LocalDate.parse(original.date())));
        try {
            return audit.held(() -> {
                try {
                    Claim added = ClaimSplit.apply(id, cid, amount, by, repository, periods, settings, audit, rollup);
                    return ClaimStatusChange.settlePart(id, added.id(), original, by, false, repository, periods, settings,
                            audit, rollup, watches);
                } catch (BadRequestException e) {
                    throw new Refused(e);
                }
            });
        } catch (Refused e) {
            throw e.refusal;
        }
    }
}

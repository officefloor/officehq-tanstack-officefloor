package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.period.PeriodGuard;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.settings.SettingsRepository;

/** A steward re-dates an OPEN or PENDING claim: neither its old nor its new date may be in a closed period, nor the
 *  new date after its person's retirement. Its due-by stays as it was fixed and a figure its statement bound stays
 *  bound; its period follows the new date, and under period numbering a claim whose period changes takes the next
 *  number of its new period's sequence (its old one is never reused). Every check is made before anything is
 *  written, so a refusal writes nothing to the store or the audit. */
final class ClaimRedate {
    private ClaimRedate() {
    }

    static Claim apply(String id, long cid, String newDate, String by, ClaimRepository repository,
            PeriodRepository periods, SettingsRepository settings, PersonRepository people, Audit audit, Rollup rollup)
            throws BadRequestException {
        if (settings.archived(id)) {
            throw new BadRequestException("the sheet is archived");
        }
        Claim found = repository.find(id, cid);
        if (found == null) {
            throw new BadRequestException("no such claim");
        }
        if (!settings.role(id, by).equals(SettingsRepository.STEWARD)) {
            throw new BadRequestException("only a steward can re-date a claim");
        }
        if (!found.open()) {
            throw new BadRequestException("a " + found.status() + " claim cannot be re-dated");
        }
        String raw = newDate == null ? "" : newDate.trim();
        if (raw.isEmpty()) {
            throw new BadRequestException("give the claim's new date");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("the new date must be yyyy-mm-dd");
        }
        LocalDate old = LocalDate.parse(found.date());
        PeriodGuard.open(periods, id, old);
        PeriodGuard.open(periods, id, date);
        people.claimAllowed(id, found.personId(), date);
        Rollup.Snapshot before = rollup.take(id);
        boolean moved = !repository.sequenceKey(id, old).equals(repository.sequenceKey(id, date));
        if (!repository.redate(id, cid, date)) {
            throw new BadRequestException("a " + found.status() + " claim cannot be re-dated");
        }
        String number = found.number();
        if (moved && repository.numbersPerPeriod(id)) {
            number = repository.numberInPeriod(id, cid, date);
        }
        audit.record("CLAIM_REDATED sheet=" + id + " by=" + by + " person=" + repository.personName(id, found.personId())
                + " amount=" + found.amount() + " date=" + date + Claim.auditNumber(number)
                + " old=" + old + " new=" + date);
        rollup.record(id, by, before);
        return repository.find(id, cid);
    }
}

package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import java.math.BigDecimal;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.RetiredPersonException;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;

/** The claim form's rules (see ValidateClaim) for steps outside this package that add a claim, or keep values a claim
 *  will be added with, by them. Not a step itself. */
public final class ClaimFormRules {
    private ClaimFormRules() {
    }

    /** The claim form's add, checked as ValidateClaim checks it. */
    public static ClaimInput check(ClaimRequest request) throws InvalidClaimException {
        return ValidateClaim.check(request);
    }

    /** Store one checked claim as the claim form does, and write its CLAIM_ADDED line (the caller records how the
     *  sheet's figures moved). */
    public static Claim add(ClaimInput input, Person person, SheetRepository sheets, SettingRepository settings,
            HolidayRepository holidays, PeriodRepository periods, ClaimRepository repository, ClaimLogRepository log,
            Audit audit) {
        Claim added = SaveNewClaim.insert(input, person, sheets, settings, holidays, periods, repository, log);
        AuditClaimAdded.record(input, person, added, audit);
        return added;
    }

    /** Edit the claim's amount as the claim form would (its date, note, reference and tags kept), and write its
     *  CLAIM_EDITED line (the caller records how the sheet's figures moved): false, writing nothing, when the claim
     *  form would refuse it (not a DRAFT, its editor locked by another user, lines totalling another amount, dated
     *  after its person's retirement date, its due-by inside a closed period). */
    public static boolean editAmount(Claim loaded, String user, BigDecimal amount, SheetRepository sheets,
            PersonRepository people, PeriodRepository periods, ClaimRepository repository, ClaimLineRepository lines,
            Audit audit) {
        if (loaded.status() != ClaimStatus.DRAFT) {
            return false;
        }
        String holder = repository.lockedBy(loaded);
        if (holder != null && !holder.equals(user)) {
            return false;
        }
        BigDecimal total = lines.total(loaded);
        if (total != null && total.compareTo(amount) != 0) {
            return false;
        }
        Person person = people.find(loaded.sheetId(), loaded.personId());
        if (person == null) {
            return false;
        }
        try {
            RetiredPersonException.refuse(person, loaded.date() != null ? loaded.date() : sheets.today(loaded.sheetId()));
        } catch (RetiredPersonException e) {
            return false;
        }
        periods.lockSheet(loaded.sheetId());
        if (periods.closedOn(loaded.sheetId(), loaded.dueBy()) != null) {
            return false;
        }
        Claim claim = new Claim(loaded.id(), loaded.sheetId(), loaded.personId(), amount, loaded.date(), loaded.note(),
                loaded.status(), loaded.number(), loaded.settledDue(), loaded.openedBy(),
                loaded.stated(), loaded.statedDue(), loaded.corrects(), loaded.dueBy(), loaded.deferredUntil(),
                loaded.proofs(), loaded.splitFrom(), loaded.lateEntry(), loaded.reminded(), loaded.ref(),
                loaded.tags(), loaded.waived(), loaded.photos(), loaded.held());
        repository.update(claim, user);
        AuditClaimEdited.record(user, loaded, person, ClaimResponse.from(claim), audit);
        return true;
    }
}

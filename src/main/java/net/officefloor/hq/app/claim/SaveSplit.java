package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Stores the claim's remaining amount, but only while it is still OPEN with the amount the split started from (a
 * change another user made first is refused), then adds the new claim: the split-off amount, the same person, date
 * and note, its due-by fixed now from the person's own terms, else those of their grade in force on its date, OPEN with the sheet's next
 * number (see ClaimNumbering), opened by the user who split, and the original's number as split-from. The new claim's
 * status log starts with its OPEN line (see ClaimLog).
 */
public class SaveSplit {
    public void save(@Val Claim claim, @Val ClaimSplit split, ClaimRepository claims, PersonRepository people,
            PeriodRepository periods, SettingRepository settings, SheetRepository sheets, ClaimLog log,
            Out<SplitOff> made) throws InvalidClaimStatusException {
        if (!claims.split(claim, claim.amount().add(split.amount()))) {
            throw new InvalidClaimStatusException("The claim was changed by someone else");
        }
        Person person = people.find(claim.sheetId(), claim.personId());
        String grade = person == null ? null : person.gradeOn(claim.date());
        Claim added = claims.add(claim.sheetId(), claim.personId(), split.amount(), claim.date(), claim.note(), null,
                claim.date().plusDays(settings.terms(claim.sheetId(), person == null ? null : person.terms(), grade, claim.date())));
        String number = ClaimNumbering.next(added, claims, periods, settings, sheets);
        Claim opened = new Claim(added.id(), added.sheetId(), added.personId(), added.amount(), added.date(),
                added.note(), ClaimStatus.OPEN, number, null, split.by(), false, null, null, added.dueBy(), null, 0,
                claim.number());
        claims.updateStatus(opened, ClaimStatus.DRAFT);
        claims.splitFrom(opened);
        log.write(opened, split.by());
        made.set(new SplitOff(opened));
    }
}

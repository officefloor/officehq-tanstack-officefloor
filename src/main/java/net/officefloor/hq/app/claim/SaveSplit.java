package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Saves the split claim and adds the new OPEN claim with the split-off amount, the same date and note, the next
 *  number, opened by the user, its due-by fixed now from the terms in force on the date, split from the claim, and a
 *  late entry when the claim is one. */
public class SaveSplit {
    public void save(@Val ValidSplit request, @Val Claim claim, Claims claims, People people, Settings settings,
            ClaimNumbering numbering, ClaimLog log, Out<ClaimSplit> split) {
        claims.save(claim);
        claims.bind(claim);
        String sheetId = claim.getSheetId();
        Person person = people.find(sheetId, claim.getPersonId());
        Claim into = claims.add(sheetId, person.getId(), request.amount(), claim.getDate(), claim.getNote(),
                claim.getDate().plusDays(settings.termsFor(sheetId, person.getTerms(), person.gradeOn(claim.getDate()), claim.getDate())));
        into.setStatus(ClaimStatus.OPEN);
        into.setNumber(numbering.next(sheetId, into.getPeriodDate()));
        into.setOpenedBy(request.user());
        into.setSplitFrom(claim.getNumber());
        claims.save(into);
        claims.splitFrom(into);
        if (claim.isLateEntry()) {
            // A split is judged by the original claim alone: the new claim is a late entry as it is.
            claims.markLateEntry(into);
        }
        log.record(into, person, request.user());
        split.set(new ClaimSplit(into));
    }
}

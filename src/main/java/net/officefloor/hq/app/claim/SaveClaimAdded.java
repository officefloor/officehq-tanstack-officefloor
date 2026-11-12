package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the claim, due by its date plus the terms of its person's grade in force on that date (else the
 *  sheet's terms), fixed now. */
public class SaveClaimAdded {
    public void save(@Val ClaimInput input, @Val Person person, ClaimStore store, SettingStore settings,
            Out<Claim> claim) {
        int terms = settings.terms(person.getSheetId(), person.gradeOn(input.date()));
        claim.set(store.add(person.getSheetId(), person.getId(), input.amount(), input.date(), input.note(),
                input.corrects(), input.date().plusDays(terms)));
    }
}

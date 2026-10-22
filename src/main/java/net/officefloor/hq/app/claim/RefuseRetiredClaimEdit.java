package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.RetiredPersonException;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Refuse an edit that would date the claim (a blank date being the sheet's today) after its person's
 *  retirement date. */
public class RefuseRetiredClaimEdit {
    public void refuse(@Val ClaimInput input, @Val Claim loaded, SheetRepository sheets, PersonRepository people)
            throws RetiredPersonException {
        Person person = people.find(loaded.sheetId(), loaded.personId());
        if (person != null) {
            RetiredPersonException.refuse(person, input.date() != null ? input.date() : sheets.today(loaded.sheetId()));
        }
    }
}

package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.RetiredPersonException;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Refuse a new claim dated (a blank date being the sheet's today) after its person's retirement date. */
public class RefuseRetiredNewClaim {
    public void refuse(@Val ClaimInput input, @Val Person person, SheetRepository sheets) throws RetiredPersonException {
        RetiredPersonException.refuse(person, input.date() != null ? input.date() : sheets.today(person.sheetId()));
    }
}

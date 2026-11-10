package net.officefloor.hq.app.person;

import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Refuses a claim dated after its person's retirement date, before anything is written. */
public class CheckClaimRetired {
    public void check(@Val Sheet sheet, @Val Claim claim, PersonRepository people) throws RetiredPersonException {
        Person person = people.find(sheet.id(), claim.getPersonId());
        if (person != null && person.retiredBefore(claim.getDate())) throw new RetiredPersonException(person.getName());
    }
}

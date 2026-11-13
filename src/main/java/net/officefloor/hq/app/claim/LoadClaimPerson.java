package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** The person whose claim it is. */
public class LoadClaimPerson {
    public void load(@Val Claim claim, PersonRepository people, Out<Person> person) {
        person.set(people.find(claim.sheetId(), claim.personId()));
    }
}

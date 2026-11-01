package net.officefloor.hq.app.quickadd;

import java.util.List;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.person.Person;

/** The sheet's people and claims after a quick-add, which may have changed both. */
public record QuickAddResponse(List<Person> people, List<Claim> claims) {
}

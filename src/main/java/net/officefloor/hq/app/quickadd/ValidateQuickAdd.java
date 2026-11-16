package net.officefloor.hq.app.quickadd;

import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.ClaimValues;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.hq.app.person.PersonValues;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body by the person form's rules (a new person takes grade A) and the claim form's rules (no date, so the
 *  sheet's entry date, else its today; an empty note). */
public class ValidateQuickAdd {
    public void validate(@RequestBody QuickAddRequest request, Out<PersonValues> person, Out<ClaimValues> claim)
            throws InvalidPersonException, InvalidClaimException {
        String name = request == null ? null : request.name();
        String amount = request == null ? null : request.amount();
        String by = request == null ? null : request.by();
        person.set(PersonValues.from(new PersonRequest(name, "A", by, null, null, null, null, null)));
        claim.set(ClaimValues.from(new ClaimRequest(amount, "", "", "", by, "", null)));
    }
}

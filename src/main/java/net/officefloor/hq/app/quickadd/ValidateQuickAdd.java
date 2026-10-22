package net.officefloor.hq.app.quickadd;

import java.math.BigDecimal;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written, by the rules of the person and claim
 *  forms: a name is required (at most 200 characters); the amount is required, a number 0.00 or more with up
 *  to two decimals. Publishes the person as a grade A add (used only when the name is new to the sheet) and
 *  the claim with no date (the sheet's today) and an empty note. */
public class ValidateQuickAdd {
    public void validate(@RequestBody QuickAddRequest request, Out<PersonRequest> person, Out<ClaimInput> claim)
            throws InvalidPersonException, InvalidClaimException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPersonException("Sign in before changing people.");
        }
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidPersonException("A name is required.");
        }
        if (name.length() > 200) {
            throw new InvalidPersonException("A name is at most 200 characters.");
        }
        String amount = request.amount() == null ? "" : request.amount().trim();
        if (amount.isEmpty()) {
            throw new InvalidClaimException("An amount is required.");
        }
        if (!amount.matches("\\d{1,12}(\\.\\d{1,2})?")) {
            throw new InvalidClaimException("The amount must be a number, 0.00 or more, with up to two decimals.");
        }
        String user = request.user().trim();
        person.set(new PersonRequest(user, name, "A", null, null));
        claim.set(new ClaimInput(user, new BigDecimal(amount).setScale(2), null, "", null));
    }
}

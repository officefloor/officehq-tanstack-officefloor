package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the quick-add body, refusing an empty name or an amount that is not 0.00 or more with up
 *  to two decimals. Publishes the person (grade A, should they be new) and the claim (no date, so
 *  the sheet's today, and an empty note). */
public class ValidateQuickClaim {

    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@RequestBody QuickClaimRequest request, Out<PersonRequest> personOut,
            Out<ClaimInput> claimOut) throws InvalidClaimException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        if (name.isEmpty()) {
            throw new InvalidClaimException("A person needs a name.");
        }
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidClaimException("The amount must be a number of 0.00 or more, with up to two decimals.");
        }
        personOut.set(new PersonRequest(name, "A", null));
        claimOut.set(new ClaimInput(new BigDecimal(amount).setScale(2), null, ""));
    }
}

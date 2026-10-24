package net.officefloor.hq.app.quickadd;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Reads the quick-add bar from the body, trimming the name; refuses an empty name, and an amount the claim
 * form would refuse. Publishes the claim as the claim form would: the sheet's today, with an empty note.
 */
public class ValidateQuickAdd {

    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@RequestBody QuickAddRequest request, Out<QuickAddName> nameOut, Out<ClaimInput> claimOut)
            throws InvalidPersonException, InvalidClaimException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        if (name.isEmpty()) {
            throw new InvalidPersonException("A name is required.");
        }
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidClaimException("The amount must be a number, 0.00 or more, with up to two decimals.");
        }
        nameOut.set(new QuickAddName(name));
        claimOut.set(new ClaimInput(new BigDecimal(amount).setScale(2), null, "", null));
    }
}

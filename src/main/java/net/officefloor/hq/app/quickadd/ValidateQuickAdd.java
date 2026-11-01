package net.officefloor.hq.app.quickadd;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Checks every rule of the person and claim forms before anything is written, so a refused quick-add
 * leaves no new person behind and writes nothing to the audit file. The claim is a draft dated the
 * sheet's today with an empty note.
 */
public class ValidateQuickAdd {
    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@PathVariable("sheet") String sheet, @RequestBody QuickAddRequest request, SheetClock clock,
            Periods periods, Out<QuickAddRequest> body, Out<ClaimInput> claim)
            throws InvalidPersonException, InvalidClaimException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        if (name.isBlank()) {
            throw new InvalidPersonException("A name is required");
        }
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidClaimException("The amount must be a number of 0.00 or more with up to two decimals");
        }
        ClaimInput input = new ClaimInput(new BigDecimal(amount), clock.today(sheet), "");
        if (periods.closedOn(sheet, input.date())) {
            throw new InvalidClaimException("A claim may not be dated inside a closed period");
        }
        body.set(new QuickAddRequest(name, amount));
        claim.set(input);
    }
}

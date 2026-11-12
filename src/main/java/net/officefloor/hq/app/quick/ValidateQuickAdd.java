package net.officefloor.hq.app.quick;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Applies the person form's and the claim form's rules to a quick add, before anything is stored:
 * the name is required (and trimmed), the amount is 0.00 or more with up to two decimals. Publishes
 * the person as it would be added (grade A) and the draft claim (the sheet's today, an empty note).
 */
public class ValidateQuickAdd {
    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody QuickAddRequest request,
            SheetStore sheets, Out<PersonRequest> person, Out<ClaimInput> claim, RoleStore roles) throws InvalidSheetException {
        roles.refuseChange(sheetId.trim(), request == null ? null : request.user());
        if (request == null || request.name() == null || request.name().trim().isEmpty()) {
            throw new InvalidSheetException("A name is required");
        }
        String amount = request.amount() == null ? "" : request.amount().trim();
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidSheetException("The amount must be a number of 0.00 or more with up to two decimals");
        }
        person.set(new PersonRequest(request.name().trim(), "A", null, request.user(), null));
        claim.set(new ClaimInput(new BigDecimal(amount).setScale(2), sheets.today(sheetId.trim()), "",
                request.user()));
    }
}

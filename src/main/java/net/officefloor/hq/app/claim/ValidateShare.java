package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a claim share by a user who may not change the sheet, with no person chosen, or a share that is not a
 *  number with up to two decimals more than 0.00. */
public class ValidateShare {
    /** A number with up to two decimals. */
    private static final Pattern SHARE = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody ShareRequest request, Out<ShareInput> body,
            RoleStore roles) throws InvalidSheetException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        roles.refuseChange(sheetId.trim(), user);
        long personId;
        try {
            personId = Long.parseLong(request.person() == null ? "" : request.person().trim());
        } catch (NumberFormatException e) {
            throw new InvalidSheetException("Choose a person to share the claim with");
        }
        String share = request.share() == null ? "" : request.share().trim();
        if (!SHARE.matcher(share).matches()) {
            throw new InvalidSheetException("The share must be a number with up to two decimals");
        }
        BigDecimal value = new BigDecimal(share).setScale(2);
        if (value.signum() <= 0) {
            throw new InvalidSheetException("The share must be more than 0.00");
        }
        body.set(new ShareInput(personId, value, user));
    }
}

package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;

/** The person form's and the claim form's rules, applied to the quick-add bar. */
public class ValidateQuickClaim {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody QuickClaimRequest request, Out<ValidQuickClaim> body, Settings settings)
            throws InvalidPersonException, InvalidClaimException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidPersonException("A name is required");
        }
        body.set(new ValidQuickClaim(name, ValidateClaim.amount(request.amount()), request.user()));
    }
}

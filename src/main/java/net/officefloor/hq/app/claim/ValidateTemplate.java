package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.hq.app.template.InvalidTemplateException;
import net.officefloor.hq.app.template.TemplateRequest;
import net.officefloor.hq.app.template.ValidTemplate;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** A template's name must not be blank, and its amount and note must be ones the claim form takes for a new claim. */
public class ValidateTemplate {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody TemplateRequest request,
            Out<ValidTemplate> body, Settings settings) throws InvalidTemplateException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidTemplateException("A template's name cannot be blank");
        }
        ValidClaim claim;
        try {
            claim = ValidateClaim.check(new ClaimRequest(request.amount(), "", request.note(), request.user(), "", "", ""));
        } catch (InvalidClaimException e) {
            throw new InvalidTemplateException(e.getMessage());
        }
        body.set(new ValidTemplate(name, claim.amount(), claim.note(), request.user()));
    }
}

package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.template.Template;
import net.officefloor.hq.app.template.TemplateClaimRequest;
import net.officefloor.hq.app.template.TemplateStore;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** The claim a template makes: its amount and note as the template holds them now, with no date, put
 *  through the claim form's rules ({@link ValidateClaim}). Refuses a template the sheet does not have. */
public class ValidateTemplateClaim {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody TemplateClaimRequest request,
            TemplateStore templates, SettingStore settings, Out<ClaimInput> body, RoleStore roles)
            throws InvalidSheetException {
        String user = request == null ? null : request.user();
        roles.refuseChange(sheetId.trim(), user);
        String name = request == null || request.template() == null ? "" : request.template().trim();
        Template template = name.isEmpty() ? null : templates.find(sheetId.trim(), name);
        if (template == null) {
            throw new InvalidSheetException("No such template: " + name);
        }
        body.set(ValidateClaim.check(sheetId, new ClaimRequest(template.amount().toPlainString(), "",
                template.note(), user, "", ""), settings));
    }
}

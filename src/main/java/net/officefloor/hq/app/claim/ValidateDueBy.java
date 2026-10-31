package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;

public class ValidateDueBy {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody DueByRequest request, Out<ValidDueBy> body, Settings settings)
            throws InvalidClaimException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String dueBy = request.dueBy() == null ? "" : request.dueBy().trim();
        try {
            body.set(new ValidDueBy(request.user(), LocalDate.parse(dueBy)));
        } catch (DateTimeParseException e) {
            throw new InvalidClaimException("Setting a due-by needs the claim's new due-by");
        }
    }
}

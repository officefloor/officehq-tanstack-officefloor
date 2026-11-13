package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body of a claim's due-by setting ({date, user}, as a re-dating sends it). */
public class ValidateDueBy {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody RedateRequest request, Out<DueByInput> body, RoleStore roles) throws InvalidSheetException {
        roles.refuseChange(sheetId.trim(), request == null ? null : request.user());
        String text = request == null || request.date() == null ? "" : request.date().trim();
        try {
            body.set(new DueByInput(LocalDate.parse(text), request.user()));
        } catch (DateTimeParseException e) {
            throw new InvalidSheetException("Not a date: " + text);
        }
    }
}

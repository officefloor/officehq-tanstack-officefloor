package net.officefloor.hq.app.person;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses (400) a flag with a blank reason or an until date that is not yyyy-mm-dd; a blank one is the sheet's today. */
public class ValidateFlagRequest {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody @Valid FlagRequest request,
            SheetRepository sheets, Out<Flag> flag) throws InvalidPersonException {
        String reason = request.reason() == null ? "" : request.reason().trim();
        if (reason.isEmpty()) {
            throw new InvalidPersonException("A flag needs a reason.");
        }
        String until = request.until() == null ? "" : request.until().trim();
        if (until.isEmpty()) {
            flag.set(new Flag(reason, sheets.today(sheetId)));
            return;
        }
        try {
            flag.set(new Flag(reason, LocalDate.parse(until)));
        } catch (DateTimeParseException e) {
            throw new InvalidPersonException("The flag's until date must be yyyy-mm-dd.");
        }
    }
}

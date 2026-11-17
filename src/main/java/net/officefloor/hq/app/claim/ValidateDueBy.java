package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: setting a due-by needs a real date (yyyy-MM-dd), the claim's new due-by. */
public class ValidateDueBy {
    public void validate(@RequestBody DueByRequest request, Out<ClaimDueBySetting> setting)
            throws InvalidClaimStatusException {
        String date = request == null || request.dueBy() == null ? "" : request.dueBy().trim();
        LocalDate parsed;
        try {
            parsed = LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new InvalidClaimStatusException("Setting a due-by needs the claim's new due-by");
        }
        setting.set(new ClaimDueBySetting(parsed, request.by() == null ? "" : request.by().trim(), null));
    }
}

package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateBulkRedate {
    public void validate(@RequestBody BulkRedateRequest request, Out<BulkRedate> body) throws InvalidSheetRequestException {
        if (request == null || request.claimIds() == null || request.claimIds().isEmpty()) {
            throw new InvalidSheetRequestException("Select the claims to re-date");
        }
        String raw = request.date() == null ? "" : request.date().trim();
        try {
            body.set(new BulkRedate(new LinkedHashSet<>(request.claimIds()), LocalDate.parse(raw), request.user()));
        } catch (DateTimeParseException e) {
            throw new InvalidSheetRequestException("Not a date (yyyy-mm-dd): " + raw);
        }
    }
}

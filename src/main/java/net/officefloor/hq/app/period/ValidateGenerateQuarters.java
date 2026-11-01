package net.officefloor.hq.app.period;

import java.util.regex.Pattern;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A quarters generate needs a year (yyyy). */
public class ValidateGenerateQuarters {
    private static final Pattern YEAR = Pattern.compile("\\d{4}");

    public void validate(@RequestBody GenerateQuartersRequest request, Out<GenerateQuartersRequest> body)
            throws InvalidSheetRequestException {
        String year = request == null || request.year() == null ? "" : request.year().trim();
        if (!YEAR.matcher(year).matches()) {
            throw new InvalidSheetRequestException("A year must be yyyy");
        }
        body.set(new GenerateQuartersRequest(year, request.user()));
    }
}

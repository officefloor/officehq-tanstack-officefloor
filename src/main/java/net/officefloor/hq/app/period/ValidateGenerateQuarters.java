package net.officefloor.hq.app.period;

import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateGenerateQuarters {

    private static final Pattern YEAR = Pattern.compile("\\d{4}");

    // A year (yyyy): its four calendar quarters become periods named <yyyy>-Q1 to <yyyy>-Q4, each as the period
    // form would add it.
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody GenerateQuartersRequest body,
            Periods periods, Out<GeneratedPeriods> generatedOut) throws InvalidPeriodException {
        String year = body == null || body.year() == null ? "" : body.year().trim();
        if (!YEAR.matcher(year).matches()) {
            throw new InvalidPeriodException("The year must be a year (yyyy).");
        }
        String sheet = sheetId.trim();
        generatedOut.set(GeneratedPeriods.quarters(sheet, periods.list(sheet), Integer.parseInt(year)));
    }
}

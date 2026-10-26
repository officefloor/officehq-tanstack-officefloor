package net.officefloor.hq.app.period;

import java.util.regex.Pattern;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** The fiscal year to generate the periods of: a year of four digits (required). */
public class ValidateGenerateFiscalYear {

    private static final Pattern YEAR = Pattern.compile("\\d{4}");

    public void validate(@RequestBody GenerateFiscalYearRequest request, Out<FiscalYear> fiscalYear,
            Out<ChangedBy> changedBy) throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String year = request == null || request.year() == null ? "" : request.year().trim();
        if (!YEAR.matcher(year).matches() || Integer.parseInt(year) < 1) {
            throw new BadRequestException("Enter a fiscal year as YYYY");
        }
        fiscalYear.set(new FiscalYear(Integer.parseInt(year)));
        changedBy.set(by);
    }
}

package net.officefloor.hq.app.period;

import java.util.regex.Pattern;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** The calendar year to generate the quarters of: a year of four digits (required). */
public class ValidateGenerateQuarters {

    private static final Pattern YEAR = Pattern.compile("\\d{4}");

    public void validate(@RequestBody GenerateQuartersRequest request, Out<QuarterYear> quarterYear,
            Out<ChangedBy> changedBy) throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String year = request == null || request.year() == null ? "" : request.year().trim();
        if (!YEAR.matcher(year).matches() || Integer.parseInt(year) < 1) {
            throw new BadRequestException("Enter a year as YYYY");
        }
        quarterYear.set(new QuarterYear(Integer.parseInt(year)));
        changedBy.set(by);
    }
}

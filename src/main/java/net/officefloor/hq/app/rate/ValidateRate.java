package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.util.Set;
import java.util.regex.Pattern;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the rate form: the grade must be A, B or C; the percent is required, 0.00 or more with up
 *  to two decimals (below 100000). */
public class ValidateRate {
    private static final Set<String> GRADES = Set.of("A", "B", "C");
    private static final Pattern PCT = Pattern.compile("\\d{1,5}(\\.\\d{1,2})?|\\.\\d{1,2}");

    public void validate(@RequestBody RateRequest request, Out<RateInput> rate) throws BadRequestException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        String pct = request == null || request.pct() == null ? "" : request.pct().trim();
        if (!GRADES.contains(grade)) {
            throw new BadRequestException("grade must be A, B or C");
        }
        if (pct.isEmpty()) {
            throw new BadRequestException("rate is required");
        }
        if (!PCT.matcher(pct).matches()) {
            throw new BadRequestException("rate must be a percent of 0.00 or more with up to two decimals");
        }
        rate.set(new RateInput(grade, new BigDecimal(pct).setScale(2)));
    }
}

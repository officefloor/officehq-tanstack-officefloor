package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRate {

    private static final Set<String> GRADES = Set.of("A", "B", "C");
    private static final Pattern PERCENT = Pattern.compile("\\d{1,7}(\\.\\d{1,2})?");

    // The rate is kept with two decimals; a grade other than A, B or C, or a rate that is not a
    // percent of 0.00 or more with up to two decimals, is refused.
    public void validate(@PathVariable("sheetId") String sheetId, @PathVariable("grade") String grade,
            @RequestBody RateRequest body, Out<Rate> rateOut) throws InvalidRateException {
        String g = grade == null ? "" : grade.trim();
        String typed = body == null || body.rate() == null ? "" : body.rate().trim();
        if (!GRADES.contains(g)) {
            throw new InvalidRateException("The grade must be A, B or C.");
        }
        if (!PERCENT.matcher(typed).matches()) {
            throw new InvalidRateException("The rate must be a percent of 0.00 or more, with up to two decimals.");
        }
        rateOut.set(new Rate(sheetId.trim(), g, new BigDecimal(typed).setScale(2, RoundingMode.UNNECESSARY)));
    }
}

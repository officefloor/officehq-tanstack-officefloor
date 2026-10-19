package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.util.Set;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRate {

    private static final Set<String> GRADES = Set.of("A", "B", "C");

    /** 0 or more, with up to two decimals. */
    private static final Pattern PCT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@PathVariable("sheetId") String sheetId, @PathVariable("grade") String grade,
            @RequestBody RateRequest request, Out<Rate> rate) throws InvalidRateException {
        String pct = request == null || request.pct() == null ? "" : request.pct().strip();
        if (!GRADES.contains(grade)) {
            throw new InvalidRateException("The grade must be A, B or C.");
        }
        if (pct.isEmpty()) {
            throw new InvalidRateException("A rate is required.");
        }
        if (!PCT.matcher(pct).matches() || new BigDecimal(pct).compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new InvalidRateException("The rate must be a percent from 0.00 to 100.00, with up to two decimals.");
        }
        rate.set(new Rate(sheetId, grade, new BigDecimal(pct).setScale(2)));
    }
}

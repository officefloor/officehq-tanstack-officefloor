package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.util.Set;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestParam;

/** Reads the rate preview from the query; refuses a grade other than A, B or C and a candidate percent that is
 *  missing, negative or has more than two decimals. */
public class ValidateRatePreview {

    private static final Set<String> GRADES = Set.of("A", "B", "C");

    private static final Pattern PCT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@RequestParam(name = "grade", required = false) String gradeParam,
            @RequestParam(name = "pct", required = false) String pctParam, Out<RatePreviewInput> input)
            throws InvalidRateException {
        String grade = gradeParam == null ? "" : gradeParam.trim();
        String pct = pctParam == null ? "" : pctParam.trim();
        if (!GRADES.contains(grade)) {
            throw new InvalidRateException("The grade must be A, B or C.");
        }
        if (!PCT.matcher(pct).matches()) {
            throw new InvalidRateException("The rate must be a percent, 0.00 or more, with up to two decimals.");
        }
        input.set(new RatePreviewInput(grade, new BigDecimal(pct).setScale(2)));
    }
}

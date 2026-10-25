package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestParam;

/** Reads the rate preview from the query; refuses a grade not shaped as one (one to three capital letters) and a candidate percent that is
 *  missing, negative or has more than two decimals. */
public class ValidateRatePreview {

    private static final Pattern PCT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@RequestParam(name = "grade", required = false) String gradeParam,
            @RequestParam(name = "pct", required = false) String pctParam, Out<RatePreviewInput> input)
            throws InvalidRateException {
        String grade = gradeParam == null ? "" : gradeParam.trim();
        String pct = pctParam == null ? "" : pctParam.trim();
        if (!net.officefloor.hq.app.grade.RetiredGrades.isCode(grade)) {
            throw new InvalidRateException("The grade must be one of the sheet's grades.");
        }
        if (!PCT.matcher(pct).matches()) {
            throw new InvalidRateException("The rate must be a percent, 0.00 or more, with up to two decimals.");
        }
        input.set(new RatePreviewInput(grade, new BigDecimal(pct).setScale(2)));
    }
}

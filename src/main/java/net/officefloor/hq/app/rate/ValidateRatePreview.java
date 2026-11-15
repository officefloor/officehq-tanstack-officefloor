package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.util.Set;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestParam;

/** A preview needs a grade A, B or C and a candidate percent of 0.00 or more with up to two decimals. */
public class ValidateRatePreview {
    private static final Set<String> GRADES = Set.of("A", "B", "C");

    /** 0 or more, with up to two decimals. */
    private static final Pattern PCT = Pattern.compile("(\\d+(\\.\\d{0,2})?|\\.\\d{1,2})");

    public void validate(@RequestParam(name = "grade", required = false) String grade,
            @RequestParam(name = "pct", required = false) String pct, Out<ValidRate> candidate)
            throws InvalidRateException {
        String g = grade == null ? "" : grade.trim();
        String p = pct == null ? "" : pct.trim();
        if (!GRADES.contains(g)) {
            throw new InvalidRateException("Grade must be A, B or C");
        }
        if (!PCT.matcher(p).matches()) {
            throw new InvalidRateException("A percent of 0.00 or more with up to two decimals is required");
        }
        candidate.set(new ValidRate(g, new BigDecimal(p).setScale(2), null, null));
    }
}

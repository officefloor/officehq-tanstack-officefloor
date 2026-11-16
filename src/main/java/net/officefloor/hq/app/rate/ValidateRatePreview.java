package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.grade.GradeRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** A preview needs a grade of the sheet and a candidate percent of 0.00 or more with up to two decimals. */
public class ValidateRatePreview {
    /** 0 or more, with up to two decimals. */
    private static final Pattern PCT = Pattern.compile("(\\d+(\\.\\d{0,2})?|\\.\\d{1,2})");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestParam(name = "grade", required = false) String grade,
            @RequestParam(name = "pct", required = false) String pct, GradeRepository grades,
            Out<ValidRate> candidate)
            throws InvalidRateException {
        String g = grade == null ? "" : grade.trim();
        String p = pct == null ? "" : pct.trim();
        if (!grades.isGrade(sheetId.trim(), g)) {
            throw new InvalidRateException("Not a grade of the sheet: " + g);
        }
        if (!PCT.matcher(p).matches()) {
            throw new InvalidRateException("A percent of 0.00 or more with up to two decimals is required");
        }
        candidate.set(new ValidRate(g, new BigDecimal(p).setScale(2), null, null));
    }
}

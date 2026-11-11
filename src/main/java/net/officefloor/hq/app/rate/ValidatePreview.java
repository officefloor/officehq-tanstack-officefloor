package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePreview {
    private static final Pattern PCT = Pattern.compile("\\d{1,15}(\\.\\d{1,2})?");

    public void validate(@RequestBody PreviewRequest request, Out<PreviewInput> body)
            throws InvalidRateException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (!net.officefloor.hq.app.setting.Settings.isGradeCode(grade)) throw new InvalidRateException("A grade is one to three capital letters");
        String pct = request.pct() == null ? "" : request.pct().trim();
        if (pct.isEmpty()) throw new InvalidRateException("A rate is required");
        if (!PCT.matcher(pct).matches()) {
            throw new InvalidRateException("A rate is a percent of 0.00 or more, with up to two decimals");
        }
        body.set(new PreviewInput(grade, new BigDecimal(pct).setScale(2)));
    }
}

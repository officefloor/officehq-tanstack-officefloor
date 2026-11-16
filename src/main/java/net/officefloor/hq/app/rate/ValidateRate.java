package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.grade.GradeRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRate {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody RateRequest request,
            GradeRepository grades, Out<ValidRate> body)
            throws InvalidRateException {
        body.set(RateForm.check(request, grades.codes(sheetId.trim())));
    }
}

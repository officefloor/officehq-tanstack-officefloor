package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.grade.GradeRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class SaveRate {
    public void save(@Val RateInput input, @Val Sheet sheet, RateRepository repository,
            GradeRepository grades, Out<Rate> rateOut) throws InvalidRateException {
        if (!grades.exists(sheet.id(), input.grade())) {
            throw new InvalidRateException("Grade " + input.grade() + " is not a grade on the sheet");
        }
        Rate rate = new Rate(sheet.id(), input.grade(), input.pct(), input.from(), input.to());
        rateOut.set(repository.save(rate));
    }
}

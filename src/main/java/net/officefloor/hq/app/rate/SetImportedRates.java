package net.officefloor.hq.app.rate;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.grade.GradeRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Sets a rate per imported line, in order, as the rate form would: each replaces the grade's rate for
 * the same from-date, and is audited (RATE_SET), then the figures it moved. Every line was checked
 * first, so a refused import writes nothing.
 */
public class SetImportedRates {
    public void set(@Val Sheet sheet, @Val ImportRatesLines lines, RateRepository rates, PersonRepository people,
            SheetRepository sheets, ClaimRepository claims, SettingsRepository settings, HttpServletRequest request,
            GradeRepository grades, Audit audit, Out<ImportRatesResponse> result) throws InvalidRateException {
        for (RateInput input : lines.lines()) {
            if (!grades.exists(sheet.id(), input.grade())) {
                throw new InvalidRateException("Grade " + input.grade() + " is not a grade on the sheet");
            }
        }
        String user = Audit.user(request);
        Rollup before = Rollup.of(sheet.id(), sheet.today(), people, claims, rates, settings.find(sheet.id()));
        for (RateInput input : lines.lines()) {
            Rate rate = new Rate(sheet.id(), input.grade(), input.pct(), input.from(), input.to());
            rates.save(rate);
            new AuditRateSet().audit(sheet, rate, request, audit);
            Rollup after = Rollup.of(sheet.id(), sheets.today(sheet.id()), people, claims, rates,
                    settings.find(sheet.id()));
            after.changesSince(before, sheet.id(), user).forEach(audit::record);
            before = after;
        }
        result.set(new ImportRatesResponse(lines.lines().size()));
    }
}

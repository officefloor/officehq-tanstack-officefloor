package net.officefloor.hq.app.importrates;

import net.officefloor.hq.app.history.AsOf;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.grade.AddedGradeRepository;
import net.officefloor.hq.app.rate.AuditRateSet;
import net.officefloor.hq.app.rate.InvalidRateException;
import net.officefloor.hq.app.rate.RateInput;
import net.officefloor.hq.app.rate.RateLogRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rate.RateRules;
import net.officefloor.hq.app.rate.RateVersionRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Sets the checked lines in order, each exactly as the rate form would: keeping the grade's rate for the same
 * from-date as an older version, recorded as RATE_SET, followed by the figures it moved.
 */
public class SetImportedRates {
    public void set(@Val ImportedRates lines, @Val Sheet sheet, RateRepository rates, RateLogRepository log,
            RateVersionRepository versions, SheetRepository sheets, PersonRepository people, ClaimRepository claims, PersonGradeRepository grades, AsOf asOf,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit, AddedGradeRepository added, Out<RatesSet> setOut) throws InvalidRateException {
        // Every line's grade is checked against the sheet's grades before any is set, so one refused line sets nothing.
        for (RateInput line : lines.rates()) {
            RateRules.refuseUnknown(line, sheet, added);
        }
        AuditRateSet auditRate = new AuditRateSet();
        String by = AuditUser.decode(user);
        for (RateInput line : lines.rates()) {
            Rollup before = Rollup.of(sheet.getId(), sheets, people, claims, rates, grades, asOf);
            auditRate.audit(RateRules.set(line, sheet, rates, log, versions), user, audit);
            Rollup.of(sheet.getId(), sheets, people, claims, rates, grades, asOf).changesSince(before, sheet.getId(), by)
                    .forEach(audit::record);
        }
        setOut.set(new RatesSet(lines.rates().size()));
    }
}

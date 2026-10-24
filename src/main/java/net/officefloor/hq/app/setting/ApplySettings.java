package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Sets the sheet's clerk limit, floor, terms and late percent and policy date and period numbering from, and the grades' own terms, and the cap, those the form sets. */
public class ApplySettings {
    public void apply(@Val SettingsInput input, @Val Sheet sheet, SheetRepository repository) {
        if (input.setClerkLimit()) {
            sheet.setClerkLimit(input.clerkLimit());
        }
        if (input.setFloor()) {
            sheet.setFloor(input.floor());
        }
        if (input.setTerms()) {
            sheet.setTerms(input.terms());
        }
        if (input.setLatePct()) {
            sheet.setLatePct(input.latePct());
        }
        if (input.setPolicyDate()) {
            sheet.setPolicyDate(input.policyDate());
        }
        if (input.setPeriodNumberingFrom()) {
            sheet.setPeriodNumberingFrom(input.periodNumberingFrom());
        }
        input.gradeTerms().forEach(sheet::setGradeTerms);
        if (input.setCap()) {
            sheet.setCap(input.cap());
        }
        repository.saveAndFlush(sheet);
    }
}

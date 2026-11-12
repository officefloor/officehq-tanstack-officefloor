package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Val;

/** Applies an edit: the name, and the form's grade recorded from its from-date (the sheet's today when
 *  blank), keeping the grades already recorded — except that a grade left as the row showed it (the one
 *  in force on the sheet's today) with the from-date blank records nothing. Their own terms are recorded from the terms
 *  from-date, keeping those already recorded (from the beginning of time when blank). Their contact and tags
 *  become the form's (none when blank). */
public class ApplyPerson {
    public void apply(@Val PersonRequest request, @Val Person person, SheetStore sheets) {
        person.setName(request.name());
        person.setContact(request.contact());
        person.setTags(request.tags());
        PersonRules.applyTerms(person, request.terms(), request.termsFrom());
        PersonRules.applyGrade(person, request.grade(), request.gradeFrom(), sheets.today(person.getSheetId()));
    }
}

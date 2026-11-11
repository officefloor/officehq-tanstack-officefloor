package net.officefloor.hq.app.person;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.grade.GradeRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Adds or updates a person per imported line, in order. A line whose name is a person's name or alias on
 * the sheet (as names are compared) updates that person as the edit form would: their grade from the
 * sheet's today, and their terms and contact when the line gives them. Every other line adds a person.
 * Every line is checked first, as the person form checks a person (their name unused on the sheet and
 * its tree, their contact unused on the sheet), and a name, person or contact repeated within the import
 * is refused too, so a refused import writes nothing; then each person is saved and audited as the form
 * does (PERSON_ADDED or PERSON_EDITED), then the figures it moved. A line choosing a retired grade refuses the import.
 */
public class AddImportedPeople {
    public void add(@Val Sheet sheet, @Val ImportPeopleLines lines, PersonRepository people, SheetRepository sheets,
            ClaimRepository claims, RateRepository rates, SettingsRepository settings, GradeRepository grades,
            HttpServletRequest request, Audit audit, Out<ImportPeopleResponse> result)
            throws DuplicatePersonNameException, DuplicatePersonContactException, StalePersonException,
            InvalidPersonException {
        Set<String> retired = grades.retired(sheet.id());
        record Change(Person person, TermsEdit termsEdit, ContactEdit contactEdit) {}
        List<Change> built = new ArrayList<>();
        Set<String> names = new HashSet<>();
        Set<Long> updatedIds = new HashSet<>();
        Set<String> contacts = new HashSet<>();
        int added = 0;
        int updated = 0;
        for (PersonRequest line : lines.lines()) {
            if (!names.add(line.name().toLowerCase(Locale.ROOT))) {
                throw new DuplicatePersonNameException(line.name());
            }
            Person person = people.findByName(sheet.id(), line.name());
            TermsEdit termsEdit = null;
            ContactEdit contactEdit = null;
            boolean chosen = person == null || !line.grade().equals(person.gradeOn(sheet.today()));
            if (chosen && retired.contains(line.grade())) throw RetiredGrades.refused(line.grade());
            if (person == null) {
                person = new Person(null, sheet.id(), line.name(), line.grade());
                person.setTerms(line.termsDays());
                person.setContact(line.contactValue());
                added++;
            } else {
                if (!updatedIds.add(person.getId())) throw new DuplicatePersonNameException(line.name());
                Integer terms = line.termsDays();
                termsEdit = new TermsEdit(terms != null && !terms.equals(person.termsOn(sheet.today())));
                if (termsEdit.changed()) person.setTerms(terms);
                String contact = line.contactValue();
                contactEdit = new ContactEdit(contact != null && !contact.equals(person.getContact()));
                if (contact != null) person.setContact(contact);
                if (!line.grade().equals(person.gradeOn(sheet.today()))) {
                    person.setGradeFrom(line.grade(), sheet.today());
                }
                updated++;
            }
            if (person.getContact() != null && !contacts.add(person.getContact())) {
                throw new DuplicatePersonContactException(person.getContact());
            }
            new CheckPersonName().check(person, people, sheets);
            new CheckPersonContact().check(person, people);
            built.add(new Change(person, termsEdit, contactEdit));
        }
        String user = Audit.user(request);
        Rollup before = Rollup.of(sheet.id(), sheet.today(), people, claims, rates, settings.find(sheet.id()));
        for (Change change : built) {
            Person person = change.person();
            if (person.getId() == null) {
                people.save(person);
                new AuditPersonAdded().audit(sheet, person, request, audit);
            } else {
                if (!people.save(person)) throw new StalePersonException(String.valueOf(person.getId()));
                new AuditPersonEdited().audit(sheet, person, change.termsEdit(), change.contactEdit(), request, audit);
            }
            Rollup after = Rollup.of(sheet.id(), sheets.today(sheet.id()), people, claims, rates,
                    settings.find(sheet.id()));
            after.changesSince(before, sheet.id(), user).forEach(audit::record);
            before = after;
        }
        result.set(new ImportPeopleResponse(added, updated));
    }
}

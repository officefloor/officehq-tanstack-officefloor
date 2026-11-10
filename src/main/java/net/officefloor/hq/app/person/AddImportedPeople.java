package net.officefloor.hq.app.person;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Adds a person per imported line, in order. Every line is checked first, as the person form checks a
 * person (their name unused on the sheet and its tree, their contact unused on the sheet), and a name or
 * contact repeated within the import is refused too, so a refused import writes nothing; then each
 * person is saved and audited as one added by the form: PERSON_ADDED, then the figures it moved.
 */
public class AddImportedPeople {
    public void add(@Val Sheet sheet, @Val ImportPeopleLines lines, PersonRepository people, SheetRepository sheets,
            ClaimRepository claims, RateRepository rates, SettingsRepository settings, HttpServletRequest request,
            Audit audit, Out<ImportPeopleResponse> result)
            throws DuplicatePersonNameException, DuplicatePersonContactException {
        List<Person> built = new ArrayList<>();
        Set<String> names = new HashSet<>();
        Set<String> contacts = new HashSet<>();
        for (PersonRequest line : lines.lines()) {
            Person person = new Person(null, sheet.id(), line.name(), line.grade());
            person.setTerms(line.termsDays());
            person.setContact(line.contactValue());
            if (!names.add(person.getName().toLowerCase(Locale.ROOT))) {
                throw new DuplicatePersonNameException(person.getName());
            }
            if (person.getContact() != null && !contacts.add(person.getContact())) {
                throw new DuplicatePersonContactException(person.getContact());
            }
            new CheckPersonName().check(person, people, sheets);
            new CheckPersonContact().check(person, people);
            built.add(person);
        }
        String user = Audit.user(request);
        Rollup before = Rollup.of(sheet.id(), sheet.today(), people, claims, rates, settings.find(sheet.id()));
        for (Person person : built) {
            people.save(person);
            new AuditPersonAdded().audit(sheet, person, request, audit);
            Rollup after = Rollup.of(sheet.id(), sheets.today(sheet.id()), people, claims, rates,
                    settings.find(sheet.id()));
            after.changesSince(before, sheet.id(), user).forEach(audit::record);
            before = after;
        }
        result.set(new ImportPeopleResponse(built.size()));
    }
}

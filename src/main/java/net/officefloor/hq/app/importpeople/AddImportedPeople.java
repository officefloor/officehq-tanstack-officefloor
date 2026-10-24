package net.officefloor.hq.app.importpeople;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonAliasRepository;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.hq.app.person.PersonRules;
import net.officefloor.hq.app.person.TreeNames;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Makes each line's person by the person form's rules — all of them before any is stored, so a refused line adds
 * nothing. Then adds the people in order, each recorded as the person form records them, followed by the figures
 * that adding them moved.
 */
public class AddImportedPeople {
    public void add(@Val ImportedLines lines, @Val Sheet sheet, PersonRepository people, PersonAliasRepository aliases,
            TreeNames tree, SheetRepository sheets, ClaimRepository claims, RateRepository rates,
            PersonGradeRepository grades, @RequestHeader(name = "X-User", required = false) String user, Audit audit,
            Out<ImportedPeople> addedOut) throws InvalidPersonException {
        List<Person> made = new ArrayList<>();
        int at = 0;
        for (PersonRequest line : lines.people()) {
            at++;
            try {
                made.add(PersonRules.make(line, sheet, people, aliases, tree));
            } catch (InvalidPersonException e) {
                throw new InvalidPersonException("Line " + at + ": " + e.getMessage());
            }
        }
        String by = AuditUser.decode(user);
        for (Person person : made) {
            Rollup before = Rollup.of(sheet.getId(), sheets, people, claims, rates, grades);
            Person saved = people.saveAndFlush(person);
            audit.record(PersonRules.addedLine(saved, by));
            Rollup.of(sheet.getId(), sheets, people, claims, rates, grades).changesSince(before, sheet.getId(), by)
                    .forEach(audit::record);
        }
        addedOut.set(new ImportedPeople(made.size()));
    }
}

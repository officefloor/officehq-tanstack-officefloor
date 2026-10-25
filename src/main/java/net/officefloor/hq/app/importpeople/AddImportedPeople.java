package net.officefloor.hq.app.importpeople;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonAliasRepository;
import net.officefloor.hq.app.person.PersonGrade;
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
 * A line with a blank grade takes the sheet's default grade. Checks every line by the person form's rules before any is stored, so a refused line imports nothing. A line whose
 * name is a person's name or alias on the sheet, ignoring case, updates that person as the person form's edit would:
 * their grade is recorded from the sheet's today, and their terms and contact are set when the line gives them. Any
 * other line adds a person. The lines are then applied in order, each recorded as the person form records it
 * (PERSON_ADDED or PERSON_EDITED), followed by the figures it moved.
 */
public class AddImportedPeople {

    /** A line to apply: the person to add, or the person to update and the line updating them. */
    private record Planned(Person person, PersonRequest update) {
    }

    public void add(@Val ImportedLines lines, @Val Sheet sheet, PersonRepository people, PersonAliasRepository aliases,
            TreeNames tree, SheetRepository sheets, ClaimRepository claims, RateRepository rates,
            PersonGradeRepository grades, EntityManager entityManager,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit,
            Out<ImportedPeople> addedOut) throws InvalidPersonException {
        List<Person> onSheet = people.findListed(sheet.getId());
        List<Planned> planned = new ArrayList<>();
        Set<Long> updating = new HashSet<>();
        int at = 0;
        for (PersonRequest given : lines.people()) {
            at++;
            PersonRequest line = given.grade().isEmpty() ? given.withGrade(sheet.getDefaultGrade()) : given;
            try {
                Optional<Person> match = PersonAliasRepository.findByNameOrAlias(onSheet, aliases, sheet.getId(),
                        line.name());
                if (match.isPresent()) {
                    planned.add(new Planned(checkUpdate(match.get(), line, people, aliases, tree, updating), line));
                } else {
                    planned.add(new Planned(PersonRules.make(line, sheet, people, aliases, tree), null));
                }
            } catch (InvalidPersonException e) {
                throw new InvalidPersonException("Line " + at + ": " + e.getMessage());
            }
        }
        String by = AuditUser.decode(user);
        int added = 0;
        int updated = 0;
        for (Planned line : planned) {
            Rollup before = Rollup.of(sheet.getId(), sheets, people, claims, rates, grades);
            if (line.update() == null) {
                Person saved = people.saveAndFlush(line.person());
                audit.record(PersonRules.addedLine(saved, by));
                added++;
            } else {
                Person person = line.person();
                PersonRequest request = line.update();
                if (request.termsDays() != null) {
                    person.setTerms(request.termsDays());
                }
                if (request.contactOrNull() != null) {
                    person.setContact(request.contactOrNull());
                }
                // Every save raises the version, as the person form's edit does.
                entityManager.lock(person, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
                people.saveAndFlush(person);
                PersonGrade dated = grades.findByPersonIdAndFromDate(person.getId(), sheet.getToday())
                        .orElseGet(() -> new PersonGrade(person.getSheetId(), person.getId(), request.grade(),
                                sheet.getToday()));
                dated.setGrade(request.grade());
                dated.setMadeOn(sheet.getToday());
                grades.saveAndFlush(dated);
                audit.record(PersonRules.editedLine(person, grades.gradesOf(person).on(sheet.getToday()), by));
                updated++;
            }
            Rollup.of(sheet.getId(), sheets, people, claims, rates, grades).changesSince(before, sheet.getId(), by)
                    .forEach(audit::record);
        }
        addedOut.set(new ImportedPeople(added, updated));
    }

    /**
     * Checks a line updating the person as the person form's edit would: refused when another line of the import
     * already updates them, the person's name or an alias is in use on another sheet of the tree, or the line's
     * contact is another person's on the sheet.
     */
    private static Person checkUpdate(Person person, PersonRequest line, PersonRepository people,
            PersonAliasRepository aliases, TreeNames tree, Set<Long> updating) throws InvalidPersonException {
        if (!updating.add(person.getId())) {
            throw new InvalidPersonException("The person " + person.getName() + " is already updated by this import.");
        }
        List<String> names = new ArrayList<>(List.of(person.getName()));
        aliases.findByPersonIdOrderByIdAsc(person.getId()).forEach(a -> names.add(a.getAlias()));
        if (tree.usedAround(person.getSheetId(), names)) {
            throw new InvalidPersonException("The name or an alias is already in use on a sheet of this sheet's tree.");
        }
        if (line.contactOrNull() != null && people.existsBySheetIdAndContactAndIdNot(person.getSheetId(),
                line.contactOrNull(), person.getId())) {
            throw new InvalidPersonException("The contact is already in use on this sheet.");
        }
        return person;
    }
}

package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.pool.PoolRepository;
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
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Imports each line, in order: a line whose name matches a person on the sheet (by name or alias, as
 *  names are compared) updates them as {@link ApplyPerson} edits them with a blank from-date (their
 *  grade from the sheet's today; their terms and contact when the line gives them): PERSON_EDITED,
 *  then the ROLLUP lines of the figures that moved. Any other line adds a person as {@link AddPerson}
 *  does: PERSON_ADDED, then the ROLLUP lines of their figures. Every line is first checked as the form
 *  checks it (a person matched by two lines is refused too), so a refused line imports nothing. */
public class ImportPeople {
    public void importPeople(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by, @Val ImportLines lines,
            PersonRepository people, SheetRepository sheets, ClaimRepository claims, RateRepository rates,
            SettingRepository settings, PoolRepository pools, GradeRepository grades, Audit audit, Out<ImportResult> result) throws InvalidPersonException {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        List<Person> matched = new ArrayList<>();
        Set<Long> matchedIds = new HashSet<>();
        Set<String> tree = people.treeNames(id);
        for (PersonRequest request : lines.people()) {
            Person person = people.findByName(id, request.name());
            matched.add(person);
            // A retired grade cannot be chosen: for a new person, or as a change of a person's grade.
            if (person == null || !request.grade().equals(person.getGrade())) {
                grades.requireChoosable(id, request.grade());
            }
            if (person == null) {
                if (people.nameTakenInTree(id, request.name())) {
                    throw new InvalidPersonException("The name is already in use: " + request.name());
                }
                if (request.contact() != null && people.contactTaken(id, request.contact(), null)) {
                    throw new InvalidPersonException("The contact is already in use: " + request.contact());
                }
                continue;
            }
            if (!matchedIds.add(person.getId())) {
                throw new InvalidPersonException("The person is repeated in the import: " + person.getName());
            }
            if (tree.contains(person.getName().trim().toLowerCase(Locale.ROOT))
                    || person.getAliases().stream().anyMatch((a) -> tree.contains(a.trim().toLowerCase(Locale.ROOT)))) {
                throw new InvalidPersonException("The name is already in use on the sheet's tree: " + person.getName());
            }
            if (request.contact() != null && people.contactTaken(id, request.contact(), person.getId())) {
                throw new InvalidPersonException("The contact is already in use: " + request.contact());
            }
        }
        RecordPersonAdded added = new RecordPersonAdded();
        RecordPersonEdited edited = new RecordPersonEdited();
        int addedCount = 0;
        int updatedCount = 0;
        for (int i = 0; i < lines.people().size(); i++) {
            PersonRequest request = lines.people().get(i);
            Person person = matched.get(i);
            Rollup before = Rollup.of(id, people, claims, rates, sheets, settings, pools);
            sheets.open(id, user);
            if (person == null) {
                person = people.add(id, request.name(), request.grade(),
                        request.terms() == null ? null : Integer.valueOf(request.terms()), request.contact(), user);
                added.record(by, person, audit);
                addedCount++;
            } else {
                if (request.terms() != null) {
                    person.giveTerms(Integer.valueOf(request.terms()));
                }
                if (request.contact() != null) {
                    person.giveContact(request.contact());
                }
                if (!request.grade().equals(person.getGrade())) {
                    LocalDate today = sheets.today(id);
                    person.giveGrade(request.grade(), today.toString(), today);
                }
                if (!people.update(person)) {
                    throw new InvalidPersonException("The person was saved by someone else meanwhile: " + person.getName());
                }
                if (person.getRecorded() != null) {
                    people.saveGrade(person, person.getRecorded(), user);
                }
                edited.record(by, person, audit);
                updatedCount++;
            }
            for (String line : Rollup.of(id, people, claims, rates, sheets, settings, pools).changesSince(before, id, user)) {
                audit.record(line);
            }
        }
        result.set(new ImportResult(addedCount, updatedCount));
    }
}

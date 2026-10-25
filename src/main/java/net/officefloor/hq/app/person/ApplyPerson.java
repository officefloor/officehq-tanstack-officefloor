package net.officefloor.hq.app.person;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import net.officefloor.hq.app.grade.AddedGradeRepository;
import net.officefloor.hq.app.grade.RetiredGradeRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/** Refuses a grade retired on the sheet, even one the person already has. Saves the person's name, terms (from the form's terms from-date, replacing those recorded for that date and keeping
 *  the others; from the beginning of time when blank), contact and tags, and records their grade from the form's from-date (the sheet's today when blank),
 *  replacing the grade recorded for that date and keeping the others, even when it is the same as the one in force
 *  (a grade with no from-date the same as the one in force on the sheet's today is left unrecorded, so a grade later
 *  recorded from an earlier date still reaches past it); terms with no from-date the same as those already in force are left unrecorded, so terms later recorded from an
 *  earlier date still reach past them. */
public class ApplyPerson {
    public void apply(@Val PersonRequest request, @Val Person person, @Val Sheet sheet, PersonRepository repository,
            PersonAliasRepository aliases, TreeNames tree,
            PersonGradeRepository grades, RetiredGradeRepository retired, AddedGradeRepository added, EntityManager entityManager)
            throws InvalidPersonException, PersonConflictException {
        PersonRules.refuseRetired(person.getSheetId(), request.grade(), retired, added);
        if (request.version() == null || request.version() != person.getVersion()) {
            throw new PersonConflictException();
        }
        if (repository.existsBySheetIdAndNameIgnoreCaseAndIdNot(person.getSheetId(), request.name(), person.getId())
                || aliases.existsBySheetIdAndAliasIgnoreCase(person.getSheetId(), request.name())) {
            throw new InvalidPersonException("The name is already in use on this sheet.");
        }
        // A person clashing with the tree may be edited only to a name, with their aliases, that no longer clashes.
        java.util.List<String> names = new java.util.ArrayList<>(java.util.List.of(request.name()));
        aliases.findByPersonIdOrderByIdAsc(person.getId()).forEach(a -> names.add(a.getAlias()));
        if (tree.usedAround(person.getSheetId(), names)) {
            throw new InvalidPersonException("The name or an alias is already in use on a sheet of this sheet's tree.");
        }
        if (request.contactOrNull() != null && repository.existsBySheetIdAndContactAndIdNot(person.getSheetId(),
                request.contactOrNull(), person.getId())) {
            throw new InvalidPersonException("The contact is already in use on this sheet.");
        }
        boolean undated = request.gradeFrom() == null || request.gradeFrom().isEmpty();
        LocalDate from = undated ? sheet.getToday() : LocalDate.parse(request.gradeFrom());
        boolean gradeKept = undated && request.grade().equals(grades.gradesOf(person).on(sheet.getToday()));
        person.setName(request.name());
        if (request.termsFromDate() == null) {
            // Terms left as they are in force on the sheet's today change nothing (their dated history is kept).
            if (!java.util.Objects.equals(request.termsDays(), person.termsOn(sheet.getToday()))) {
                person.setTerms(request.termsDays());
            }
        } else {
            person.setTermsFrom(request.termsFromDate(), request.termsDays());
        }
        person.setContact(request.contactOrNull());
        person.setTags(request.tags());
        try {
            // Every save raises the version, even one that changes only a dated grade.
            entityManager.lock(person, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
            repository.saveAndFlush(person);
        } catch (ObjectOptimisticLockingFailureException | jakarta.persistence.OptimisticLockException e) {
            // saved by someone else between the load and this save
            throw new PersonConflictException();
        }
        // The grade is recorded from the from-date (the sheet's today when none is given), even one the same as the grade in
        // force on that date, so a grade later recorded from an earlier date does not reach past it; a grade with no
        // from-date left as it is in force on the sheet's today changes nothing.
        if (gradeKept) {
            return;
        }
        PersonGrade dated = grades.findByPersonIdAndFromDate(person.getId(), from)
                .orElseGet(() -> new PersonGrade(person.getSheetId(), person.getId(), request.grade(), from));
        dated.setGrade(request.grade());
        dated.setMadeOn(sheet.getToday());
        grades.saveAndFlush(dated);
    }
}

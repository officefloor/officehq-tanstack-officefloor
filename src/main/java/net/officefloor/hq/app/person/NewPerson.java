package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.regex.Pattern;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.rollup.Rollup;
import org.springframework.dao.DuplicateKeyException;

/** Adding a person as the person form does: shared by the form and the people import. */
public final class NewPerson {
    private static final Set<String> GRADES = Set.of("A", "B", "C");
    private static final Pattern TERMS = Pattern.compile("\\d{1,4}");

    private NewPerson() {
    }

    /** The form's rules: the request trimmed and normalised, or refused. */
    public static PersonRequest check(PersonRequest request) throws BadRequestException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (name.isEmpty()) {
            throw new BadRequestException("name is required");
        }
        if (!GRADES.contains(grade)) {
            throw new BadRequestException("grade must be A, B or C");
        }
        String from = request.gradeFrom() == null ? "" : request.gradeFrom().trim();
        if (!from.isEmpty()) {
            try {
                from = LocalDate.parse(from).toString();
            } catch (DateTimeParseException e) {
                throw new BadRequestException("from-date must be yyyy-mm-dd");
            }
        }
        String terms = request.terms() == null ? "" : request.terms().trim();
        if (!terms.isEmpty() && (!TERMS.matcher(terms).matches() || Integer.parseInt(terms) > 3650)) {
            throw new BadRequestException("terms must be a whole number of days from 0 to 3650");
        }
        String termsFrom = request.termsFrom() == null ? "" : request.termsFrom().trim();
        if (!termsFrom.isEmpty()) {
            try {
                termsFrom = LocalDate.parse(termsFrom).toString();
            } catch (DateTimeParseException e) {
                throw new BadRequestException("terms from-date must be yyyy-mm-dd");
            }
        }
        String contact = request.contact() == null ? "" : request.contact().trim().toLowerCase(java.util.Locale.ROOT);
        return new PersonRequest(name, grade, request.version(), from,
                terms.isEmpty() ? "" : String.valueOf(Integer.parseInt(terms)), contact,
                net.officefloor.hq.app.claim.Claim.tags(request.tags()), termsFrom);
    }

    /** Refuse choosing a grade retired on the sheet. */
    public static void checkNotRetired(net.officefloor.hq.app.settings.SettingsRepository settings, String id, String grade)
            throws BadRequestException {
        if (settings.retired(id, grade)) {
            throw new BadRequestException("grade " + grade + " is retired");
        }
    }

    /** Refuse a validated person whose name or contact is already in use on the sheet. */
    public static void checkFree(PersonRepository repository, String id, PersonRequest request) throws BadRequestException {
        if (repository.nameTaken(id, request.name(), 0)) {
            throw new BadRequestException("name is already in use");
        }
        if (!request.contact().isEmpty() && repository.contactTaken(id, request.contact(), 0)) {
            throw new BadRequestException("contact is already in use");
        }
    }

    /** Add a validated, checked person, writing PERSON_ADDED and the rollup. */
    public static Person add(PersonRepository repository, Audit audit, Rollup rollup, String id, String by,
            PersonRequest request) throws BadRequestException {
        String contact = request.contact().isEmpty() ? null : request.contact();
        Rollup.Snapshot before = rollup.take(id);
        Person added;
        try {
            Integer terms = request.terms().isEmpty() ? null : Integer.valueOf(request.terms());
            boolean dated = request.termsFrom() != null && !request.termsFrom().isEmpty();
            added = repository.add(id, request.name(), request.grade(), dated ? null : terms, contact);
            if (dated) {
                repository.terms(id, added.id(), terms, LocalDate.parse(request.termsFrom()));
                added = repository.find(id, added.id());
            }
        } catch (DuplicateKeyException e) {
            throw new BadRequestException("name is already in use");
        }
        if (request.tags() != null) {
            repository.tags(id, added.id(), request.tags());
            added = repository.find(id, added.id());
        }
        audit.record("PERSON_ADDED sheet=" + id + " by=" + by + " name=" + added.name()
                + " grade=" + added.grade() + (added.terms() == null ? "" : " terms=" + added.terms())
                + (added.contact() == null ? "" : " contact=" + added.contact()));
        rollup.record(id, by, before);
        return added;
    }
}

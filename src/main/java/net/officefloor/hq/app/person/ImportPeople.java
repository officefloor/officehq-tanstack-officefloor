package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Adds a person per imported line, in order. Every line is first checked as {@link AddPerson} checks
 *  a person (its name and contact not in use), so a refused line adds nothing; then each is added as
 *  the form adds them: PERSON_ADDED, then the ROLLUP lines of their figures. */
public class ImportPeople {
    public void importPeople(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by, @Val ImportLines lines,
            PersonRepository people, SheetRepository sheets, ClaimRepository claims, RateRepository rates,
            SettingRepository settings, Audit audit, Out<ImportResult> result) throws InvalidPersonException {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        for (PersonRequest request : lines.people()) {
            if (people.nameTaken(id, request.name(), null) || people.nameTakenInTree(id, request.name())) {
                throw new InvalidPersonException("The name is already in use: " + request.name());
            }
            if (request.contact() != null && people.contactTaken(id, request.contact(), null)) {
                throw new InvalidPersonException("The contact is already in use: " + request.contact());
            }
        }
        RecordPersonAdded record = new RecordPersonAdded();
        for (PersonRequest request : lines.people()) {
            Rollup before = Rollup.of(id, people, claims, rates, sheets, settings);
            sheets.open(id, user);
            Person person = people.add(id, request.name(), request.grade(),
                    request.terms() == null ? null : Integer.valueOf(request.terms()), request.contact(), user);
            record.record(by, person, audit);
            for (String line : Rollup.of(id, people, claims, rates, sheets, settings).changesSince(before, id, user)) {
                audit.record(line);
            }
        }
        result.set(new ImportResult(lines.people().size()));
    }
}

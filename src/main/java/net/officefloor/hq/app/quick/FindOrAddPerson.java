package net.officefloor.hq.app.quick;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The person named (ignoring case and surrounding spaces), or a new one with grade A, recorded as
 *  the person form records an add. The claim's own rules are checked first, so a claim that would
 *  be refused adds no one. */
public class FindOrAddPerson {
    public void find(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by, @Val QuickAddName name,
            @Val Rollup before, SheetRepository sheets, PersonRepository people,
            ClaimRepository claims, RateRepository rates, PeriodRepository periods,
            SettingRepository settings, HolidayRepository holidays, Audit audit,
            Out<Person> person) throws PeriodClosedException, InvalidPersonException {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        Person found = people.findByName(id, name.name());
        if (found != null) {
            person.set(found);
            return;
        }
        if (people.nameTakenInTree(id, name.name())) {
            throw new InvalidPersonException("The name is already in use on the sheet's tree");
        }
        // The claim would be refused when its due-by (grade A's terms in business days after its date) falls in a closed period.
        java.time.LocalDate date = settings.entryDate(id);
        periods.requireOpen(id, holidays.dueBy(id, date, settings.terms(id, "A", sheets.today(id).toString())).toString());
        sheets.open(id, user);
        Person added = people.add(id, name.name(), "A", user);
        audit.record("PERSON_ADDED sheet=" + id + " by=" + user + " name=" + added.getName()
                + " grade=" + added.getGrade());
        for (String line : Rollup.of(id, people, claims, rates, sheets, settings).changesSince(before, id, user)) {
            audit.record(line);
        }
        person.set(added);
    }
}

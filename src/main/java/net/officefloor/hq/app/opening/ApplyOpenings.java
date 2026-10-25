package net.officefloor.hq.app.opening;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonAliasRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Finds each line's person on the sheet by name or alias and its period on the sheet by name, both ignoring case,
 * checking every line before anything is stored; then, in order, adds each line's amount to the person's opening
 * balance for the period (so to their carried figure for it) and records OPENING_SET with the person's and period's names.
 * A closed (or soft-closed) period keeps the carried figures it fixed; the opening counts once it is reopened.
 */
public class ApplyOpenings {
    public void apply(@Val OpeningLines pasted, @Val Sheet sheet, PersonRepository people, PersonAliasRepository aliases,
            PeriodRepository periods, @RequestHeader(name = "X-User", required = false) String user, Audit audit,
            Out<ImportedOpenings> addedOut) throws InvalidOpeningException {
        String by = AuditUser.decode(user);
        List<Person> onSheet = people.findListed(sheet.getId());
        List<Period> sheetPeriods = periods.findBySheetIdOrderByFromDateAscIdAsc(sheet.getId());
        List<Person> persons = new ArrayList<>();
        List<Period> targets = new ArrayList<>();
        int at = 0;
        for (OpeningLine line : pasted.lines()) {
            at++;
            Person person = PersonAliasRepository.findByNameOrAlias(onSheet, aliases, sheet.getId(), line.name())
                    .orElseThrow(() -> new InvalidOpeningException(line.name() + " is not on the sheet."));
            Period period = sheetPeriods.stream().filter(p -> p.getName().equalsIgnoreCase(line.period())).findFirst()
                    .orElseThrow(() -> new InvalidOpeningException("The sheet has no period " + line.period() + "."));
            persons.add(person);
            targets.add(period);
        }
        List<String> records = new ArrayList<>();
        for (int i = 0; i < at; i++) {
            OpeningLine line = pasted.lines().get(i);
            Person person = persons.get(i);
            Period period = targets.get(i);
            period.addOpening(person.getId(), line.amount());
            records.add("OPENING_SET sheet=" + sheet.getId() + " by=" + by + " name=" + person.getName()
                    + " period=" + period.getName() + " amount=" + line.amount().toPlainString());
        }
        periods.saveAllAndFlush(targets.stream().distinct().toList());
        records.forEach(audit::record);
        addedOut.set(new ImportedOpenings(at));
    }
}

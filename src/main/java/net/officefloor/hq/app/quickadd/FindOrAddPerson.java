package net.officefloor.hq.app.quickadd;

import java.util.Optional;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.TreeNames;
import net.officefloor.hq.app.person.PersonAliasRepository;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Finds the person on the sheet going by the name or an alias, ignoring case; when there is none, adds them with grade A (refused when the name is in use on another sheet of the sheet's tree) and records
 * it as the person form would (the person, then how the figures moved), noting the figures afterwards so the
 * claim's own rollup counts only what the claim moved.
 */
public class FindOrAddPerson {
    public void find(@Val QuickAddName name, @Val Sheet sheet, @Val Rollup before, PersonRepository people, PersonAliasRepository aliases,
            SheetRepository sheets, ClaimRepository claims, RateRepository rates, PersonGradeRepository grades,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit, TreeNames tree, Out<Person> personOut,
            Out<Rollup> rollupOut) throws InvalidClaimException {
        Optional<Person> found = PersonAliasRepository.findByNameOrAlias(
                people.findListed(sheet.getId()), aliases, sheet.getId(), name.name());
        if (found.isPresent()) {
            personOut.set(found.get());
            return;
        }
        if (tree.usedAround(sheet.getId(), name.name())) {
            throw new InvalidClaimException("The name " + name.name() + " is already in use on a sheet of this sheet's tree.");
        }
        Person added = new Person(sheet.getId(), name.name(), "A");
        added.setAddedOn(sheet.getToday());
        Person person = people.saveAndFlush(added);
        String by = AuditUser.decode(user);
        audit.record("PERSON_ADDED sheet=" + person.getSheetId() + " by=" + by + " name=" + person.getName()
                + " grade=" + person.getGrade());
        Rollup now = Rollup.of(sheet.getId(), sheets, people, claims, rates, grades);
        now.changesSince(before, sheet.getId(), by).forEach(audit::record);
        personOut.set(person);
        rollupOut.set(now);
    }
}

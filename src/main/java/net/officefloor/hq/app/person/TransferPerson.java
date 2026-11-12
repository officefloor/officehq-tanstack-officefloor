package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimLogStore;
import net.officefloor.hq.app.claim.ClaimStore;
import net.officefloor.hq.app.claim.Numbering;
import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.pool.PoolStore;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Transfers the person to another sheet, refusing a retired person, an archived sheet, or a name, alias or
 *  contact of theirs already on it (a name or alias also when on another sheet of its tree). They arrive with their grades, terms, contact, aliases and claims; each claim
 *  that had a number takes the other sheet's next, in the order they were opened (by the sheet's today when each
 *  was opened, then the order they were added). They leave this sheet entirely. When the other sheet already has someone with their name or an alias
 *  of theirs, they merge into that person instead, as a merge does. */
public class TransferPerson {
    public void transfer(@Val TransferRequest request, @Val Person person, PersonStore people, ClaimStore claims,
            ClaimLogStore log, PatronStore patrons, PoolStore pools, SheetStore sheets, SettingStore settings,
            PeriodStore periods, RollupService rollups, Out<Transferred> transferred) throws InvalidSheetException {
        String from = person.getSheetId();
        String to = request.to();
        if (person.isRetired()) {
            throw new InvalidSheetException("A retired person cannot be transferred");
        }
        sheets.refuseArchived(to);
        // the sheet they leave is not counted: they take their names with them
        List<String> tree = settings.tree(to).stream().filter(s -> !s.equals(from)).toList();
        Person into = mergeTarget(to, person, people);
        if (people.usedOn(tree, person.getName())
                || (into == null && people.nameTaken(to, person.getName(), null))) {
            throw new InvalidSheetException("The other sheet already has " + person.getName());
        }
        for (String alias : person.getAliases()) {
            if (people.usedOn(tree, alias) || (into == null && people.nameTaken(to, alias, null))) {
                throw new InvalidSheetException("The other sheet already has " + alias);
            }
        }
        // merging drops their contact, so it cannot clash
        if (into == null && person.getContact() != null && people.contactTaken(to, person.getContact(), null)) {
            throw new InvalidSheetException("The other sheet already has the contact " + person.getContact());
        }

        Map<Long, LocalDate> opened = log.openedOn(from);
        List<Claim> theirs = new ArrayList<>(claims.list(from).stream()
                .filter(c -> c.getPersonId() == person.getId()).toList());
        List<Claim> numbered = new ArrayList<>(theirs.stream().filter(c -> c.getNumber() != null).toList());
        numbered.sort(Comparator.comparing((Claim c) -> opened.getOrDefault(c.getId(), c.getDate()))
                .thenComparingLong(Claim::getId));

        var before = rollups.snapshot(to);
        sheets.markLoaded(to);
        Map<String, String> numbers = new HashMap<>();
        Map<Long, String> numberOf = new HashMap<>();
        for (Claim claim : numbered) {
            String number = Numbering.next(to, claim.getDueBy(), claims, settings, sheets, periods);
            numbers.putIfAbsent(claim.getNumber(), number);
            numberOf.put(claim.getId(), number);
        }
        Person copy = into == null ? people.copyTo(person, to) : into;
        // listed newest first: copy the oldest first, so they keep their order
        for (int i = theirs.size() - 1; i >= 0; i--) {
            Claim claim = theirs.get(i);
            claims.copyTo(claim, to, copy.getId(), numberOf.get(claim.getId()), renumbered(claim.getCorrects(), numbers),
                    renumbered(claim.getSplitFrom(), numbers));
        }

        if (into != null) {
            // merged as a merge does: their claims after the other's (copied without bound figures above), their
            // name and aliases the other's aliases, the rest of them dropped
            for (String alias : person.getAliases()) {
                people.addAlias(into, alias);
            }
            people.addAlias(into, person.getName());
            copy = people.find(to, into.getId());
        }

        claims.removeAll(from, person.getId());
        patrons.drop(from, person.getId());
        pools.drop(from, person.getId());
        people.remove(person);
        transferred.set(new Transferred(to, copy, before, into != null));
    }

    /** The person on the other sheet the arriving person merges into: the one with their name or an alias of
     *  theirs (as names are compared); null when none. Refused when that is more than one person or a retired one. */
    private static Person mergeTarget(String to, Person person, PersonStore people) throws InvalidSheetException {
        Person into = null;
        List<String> names = new ArrayList<>(person.getAliases());
        names.add(0, person.getName());
        for (String name : names) {
            Person found = people.findByName(to, name);
            if (found == null) {
                continue;
            }
            if (into != null && into.getId() != found.getId()) {
                throw new InvalidSheetException("The other sheet already has " + name + " as someone else");
            }
            into = found;
        }
        if (into != null && into.isRetired()) {
            throw new InvalidSheetException("A retired person cannot be merged");
        }
        return into;
    }

    /** The number a claim's number became on the other sheet; as it was when that claim did not go with it. */
    private static String renumbered(String number, Map<String, String> numbers) {
        return number == null ? null : numbers.getOrDefault(number, number);
    }
}

package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimNumbers;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.role.StewardOnlyException;
import net.officefloor.hq.app.role.ViewerException;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetArchivedException;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Transfers the person to another sheet (a steward), not retired, and only when their name, aliases
 * and contact are free there (their name and aliases on the other sheets of its tree too). They arrive with their grades (and from-dates), terms, contact and
 * aliases, and their claims with their status, dates, notes, tags, frozen figures, logs and comments,
 * listed in the order they were. Each claim that had a number takes the other sheet's next, in the
 * order the claims were opened (by the sheet's today then, then the order added); a correction or
 * split names the new numbers. Figures a statement bound are kept, as are their statements (a
 * merge on arrival adds their count to the other's), and references and settle batches are not carried. They leave this sheet entirely: patron links
 * either way, pool memberships, watches and holds are dropped. When one person on the other sheet
 * already has their name or an alias (as names are compared, and not retired), they are then merged into
 * that person there, as a merge does (their contact dropped, so it need not be free there).
 */
public class TransferPerson {
    public void transfer(@Val TransferInput input, @Val Sheet sheet, @Val Person person, PersonRepository people,
            ClaimRepository claims, SheetRepository sheets, RoleRepository roles, SettingsRepository settings,
            PeriodRepository periods, RateRepository rates, HttpServletRequest request, Out<Transfer> transferOut)
            throws StewardOnlyException, InvalidPersonException, ViewerException, SheetArchivedException {
        String user = Audit.user(request);
        if (!Role.STEWARD.equals(roles.roleOf(sheet.id(), user))) {
            throw new StewardOnlyException("Only a steward may transfer people");
        }
        if (person.isRetired()) throw new InvalidPersonException("A retired person cannot be transferred");
        Sheet to = sheets.loadOrCreate(input.toSheet());
        if (Role.VIEWER.equals(roles.roleOf(to.id(), user))) {
            throw new ViewerException("A viewer may not change the sheet transferred to");
        }
        if (to.archived()) throw new SheetArchivedException(to.id());
        java.util.Set<String> toTree = sheets.tree(to.id());
        // One already there by their name or an alias: they are merged into that person on arrival.
        Person into = null;
        List<String> names = new ArrayList<>(List.of(person.getName()));
        names.addAll(person.getAliases());
        for (String name : names) {
            Person found = people.findByName(to.id(), name);
            if (found == null) continue;
            if (into != null && !into.getId().equals(found.getId())) {
                throw new InvalidPersonException("Their names are on more than one person on the sheet transferred to");
            }
            into = found;
        }
        if (into != null && into.isRetired()) throw new InvalidPersonException("A retired person cannot be merged");
        if (people.nameTakenIn(toTree, person.getName(), person.getId())) {
            throw new InvalidPersonException("That name is already on the sheet transferred to");
        }
        for (String alias : person.getAliases()) {
            if (people.nameTakenIn(toTree, alias, person.getId())) {
                throw new InvalidPersonException("An alias is already on the sheet transferred to");
            }
        }
        if (into == null && person.getContact() != null && people.contactTaken(to.id(), person.getContact(), null)) {
            throw new InvalidPersonException("That contact is already on the sheet transferred to");
        }
        Settings toSettings = settings.find(to.id());
        Rollup toBefore = Rollup.of(to.id(), to.today(), people, claims, rates, toSettings);

        // Oldest listed first, so they are listed on the other sheet as they were here.
        List<Claim> moving = new ArrayList<>(claims.findByPerson(sheet.id(), person.getId()));
        Collections.reverse(moving);
        Map<Long, LocalDate> openedOn = new HashMap<>();
        List<Claim> numbered = new ArrayList<>();
        for (Claim claim : moving) {
            if (claim.getNumber() == null) continue;
            LocalDate opened = claims.openedOn(claim);
            openedOn.put(claim.getId(), opened == null ? claim.getDate() : opened);
            numbered.add(claim);
        }
        numbered.sort(Comparator.comparing((Claim c) -> openedOn.get(c.getId()))
                .thenComparing(Claim::getId));
        Map<String, String> renumbered = new HashMap<>();
        Map<Long, String> numbers = new HashMap<>();
        for (Claim claim : numbered) {
            String number = ClaimNumbers.next(to, claim, toSettings, periods, claims);
            renumbered.put(claim.getNumber(), number);
            numbers.put(claim.getId(), number);
        }
        for (Claim claim : moving) {
            claims.transfer(claim, to.id(), numbers.get(claim.getId()),
                    claim.getCorrects() == null ? null : renumbered.getOrDefault(claim.getCorrects(), claim.getCorrects()),
                    claim.getSplitFrom() == null ? null : renumbered.getOrDefault(claim.getSplitFrom(), claim.getSplitFrom()));
        }
        people.transfer(person, to.id());
        if (into != null) {
            Person arrived = people.find(to.id(), person.getId());
            List<Claim> merging = new ArrayList<>(claims.findByPerson(to.id(), arrived.getId()));
            Collections.reverse(merging);
            long mergeId = people.recordMerge(arrived, into);
            for (Claim claim : merging) {
                claim.mergeTo(into.getId());
                claims.save(claim);
                people.recordMergedClaim(mergeId, claim.getId());
            }
            people.merge(arrived, into, mergeId);
        }
        transferOut.set(new Transfer(person, to, toBefore, into));
    }
}

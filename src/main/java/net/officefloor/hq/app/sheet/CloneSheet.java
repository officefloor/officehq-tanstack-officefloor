package net.officefloor.hq.app.sheet;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimNumbers;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.role.StewardOnlyException;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Clones the sheet to another id (a steward), only one never used. The clone gets this sheet's today,
 * settings, roles, rates, holidays, void reasons, templates, people, pools and periods (all open; see
 * {@link SheetCloneRepository}), then each person's claims, oldest listed first, with their status,
 * frozen figures, dates, notes and logs. Each claim that had a number takes the clone's next, in the
 * order the claims were opened (by the sheet's today then, then the order added to the clone); a
 * correction or split names the new numbers. Statements are not copied.
 */
public class CloneSheet {
    public void cloneSheet(@Val CloneInput input, @Val Sheet sheet, SheetRepository sheets, SheetCloneRepository copier,
            PersonRepository people, ClaimRepository claims, RoleRepository roles, SettingsRepository settings,
            PeriodRepository periods, HttpServletRequest request, Out<Clone> cloneOut)
            throws StewardOnlyException, InvalidSheetIdException {
        if (!Role.STEWARD.equals(roles.roleOf(sheet.id(), Audit.user(request)))) {
            throw new StewardOnlyException("Only a steward may clone the sheet");
        }
        if (sheets.find(input.toSheet()) != null) {
            throw new InvalidSheetIdException("A sheet can only be cloned to an id never used");
        }
        Map<Long, Long> copies = copier.copy(sheet.id(), input.toSheet());
        Sheet to = sheets.find(input.toSheet());
        Settings toSettings = settings.find(to.id());

        List<Claim> copied = new ArrayList<>();
        Map<Long, Long> copyOf = new HashMap<>();
        for (Person person : people.findAll(sheet.id())) {
            List<Claim> theirs = new ArrayList<>(claims.findByPerson(sheet.id(), person.getId()));
            Collections.reverse(theirs);
            for (Claim claim : theirs) {
                copyOf.put(claim.getId(), claims.copy(claim, to.id(), copies.get(person.getId())));
                copied.add(claim);
            }
        }
        Map<Long, LocalDate> openedOn = new HashMap<>();
        List<Claim> numbered = new ArrayList<>();
        for (Claim claim : copied) {
            if (claim.getNumber() == null) continue;
            LocalDate opened = claims.openedOn(claim);
            openedOn.put(claim.getId(), opened == null ? claim.getDate() : opened);
            numbered.add(claim);
        }
        numbered.sort(Comparator.comparing((Claim c) -> openedOn.get(c.getId()))
                .thenComparing((Claim c) -> copyOf.get(c.getId())));
        Map<String, String> renumbered = new HashMap<>();
        Map<Long, String> numbers = new HashMap<>();
        for (Claim claim : numbered) {
            String number = ClaimNumbers.next(to, claim, toSettings, periods, claims);
            renumbered.put(claim.getNumber(), number);
            numbers.put(claim.getId(), number);
        }
        for (Claim claim : copied) {
            claims.numberCopy(to.id(), copyOf.get(claim.getId()), numbers.get(claim.getId()),
                    claim.getCorrects() == null ? null : renumbered.getOrDefault(claim.getCorrects(), claim.getCorrects()),
                    claim.getSplitFrom() == null ? null : renumbered.getOrDefault(claim.getSplitFrom(), claim.getSplitFrom()));
        }
        cloneOut.set(new Clone(to));
    }
}

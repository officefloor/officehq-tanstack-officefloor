package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.Role;
import net.officefloor.hq.app.access.RoleRepository;
import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimNumbering;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Clones the sheet to another sheet id: only a steward may, and only to a sheet never used (not stored). See
 * {@link SheetCloneRepository} for what is copied. Each claim that had a number takes the clone's next one, in the
 * order the originals were opened (by the sheet's today when each was opened, then by the order they are added to the
 * clone: by person in list order, then by claim).
 */
public class CloneSheet {
    public void clone(@Val SheetResponse sheet, @Val CloneInput input, @Val ChangedBy by, RoleRepository roles,
            SheetRepository sheets, SheetCloneRepository cloner, PersonRepository people, ClaimRepository claims,
            ClaimLogRepository log, SettingRepository settings, PeriodRepository periods, Rollup rollup,
            Out<RollupBefore> before) throws BadRequestException {
        String from = sheet.id();
        String to = input.to();
        if (!Role.STEWARD.equals(roles.roleOf(from, by.name()))) {
            throw new BadRequestException("Only a steward may clone the sheet");
        }
        if (to.equals(from) || sheets.exists(to)) {
            throw new BadRequestException("That sheet is already in use: clone to a sheet never used");
        }
        before.set(new RollupBefore(rollup.snapshot(to)));
        Map<Long, Long> personIds = cloner.copySheet(from, to);

        List<Claim> ordered = new ArrayList<>();
        List<Claim> all = claims.list(from);
        for (Person person : people.list(from)) {
            all.stream().filter(c -> c.getPersonId() == person.getId())
                    .sorted(Comparator.comparingLong(Claim::getId)).forEach(ordered::add);
        }
        Map<Long, LocalDate> opened = new HashMap<>();
        for (Claim claim : ordered) {
            if (claim.getNumber() != null) {
                LocalDate on = log.firstOn(from, claim.getId(), ClaimStatus.OPEN);
                opened.put(claim.getId(), on != null ? on : claim.getDate());
            }
        }
        Map<Long, String> numbers = new LinkedHashMap<>();
        ordered.stream().filter(c -> opened.containsKey(c.getId()))
                .sorted(Comparator.comparing(c -> opened.get(c.getId())))
                .forEach(c -> numbers.put(c.getId(),
                        ClaimNumbering.number(to, c.getDueBy(), claims, settings, sheets, periods)));
        cloner.copyClaims(from, to, personIds, numbers);
    }
}

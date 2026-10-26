package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.Role;
import net.officefloor.hq.app.access.RoleRepository;
import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.claim.ClaimNumbering;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Transfers the person to another sheet by id: only a steward may, never a retired person, never to this sheet itself
 * nor to an archived one, and not while their name, one of their aliases or their contact is already on the other
 * sheet. Each claim of theirs that had a number takes the other sheet's next one, in the order the claims were opened
 * (by the sheet's today when each was opened, then by the order they were added). See
 * {@link PersonRepository#transfer} for what moves and what is dropped.
 */
public class TransferPerson {
    public void transfer(@Val TransferInput input, @Val Person person, @Val ChangedBy by, PersonRepository repository,
            RoleRepository roles, SheetRepository sheets, ClaimRepository claims, ClaimLogRepository log,
            SettingRepository settings, PeriodRepository periods, Rollup rollup, Out<PersonTransferred> transferred)
            throws BadRequestException {
        String sheetId = person.getSheetId();
        String to = input.to();
        if (!Role.STEWARD.equals(roles.roleOf(sheetId, by.name()))) {
            throw new BadRequestException("Only a steward may transfer people");
        }
        if (to.equals(sheetId)) {
            throw new BadRequestException("A person cannot be transferred to their own sheet");
        }
        if (person.isRetired()) {
            throw new BadRequestException("A retired person cannot be transferred");
        }
        if (sheets.isArchived(to)) {
            throw new BadRequestException("That sheet is archived");
        }
        if (repository.nameTaken(to, person.getName(), null)) {
            throw new BadRequestException("That name is already in use on the other sheet");
        }
        for (String alias : person.getAliases()) {
            if (repository.nameTaken(to, alias, null)) {
                throw new BadRequestException("The alias " + alias + " is already in use on the other sheet");
            }
        }
        if (repository.contactTaken(to, person.getContact(), null)) {
            throw new BadRequestException("That contact is already another person's on the other sheet");
        }
        if (!repository.touch(person)) {
            throw new BadRequestException("Someone else has changed this person; reload the sheet");
        }
        sheets.ensure(to);
        var before = rollup.snapshot(to);
        List<Claim> own = claims.listOf(sheetId, person.getId());
        Map<Long, LocalDate> opened = new LinkedHashMap<>();
        for (Claim claim : own) {
            if (claim.getNumber() != null) {
                LocalDate on = log.firstOn(sheetId, claim.getId(), ClaimStatus.OPEN);
                opened.put(claim.getId(), on != null ? on : claim.getDate());
            }
        }
        List<Claim> numbered = own.stream()
                .filter(c -> opened.containsKey(c.getId()))
                .sorted(Comparator.<Claim, LocalDate>comparing(c -> opened.get(c.getId())).thenComparing(Claim::getId))
                .toList();
        Map<Long, String> numbers = new LinkedHashMap<>();
        for (Claim claim : numbered) {
            numbers.put(claim.getId(), ClaimNumbering.number(to, claim.getDueBy(), claims, settings, sheets, periods));
        }
        repository.transfer(person, to, numbers);
        transferred.set(new PersonTransferred(person.getName(), to, before));
    }
}

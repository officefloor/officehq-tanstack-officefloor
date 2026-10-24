package net.officefloor.hq.app.claim;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Re-dates an OPEN or PENDING claim; only a steward may. A claim in any other status, a claim whose due-by is in a
 * closed period, or a new date after the person's retirement date is refused. The due-by stays as it was fixed and a
 * figure bound by a statement stays bound; so the claim's period (the one its due-by falls in) and its number stay as
 * they were. Returns the old and new date, for the audit.
 */
public class ApplyRedate {
    public Redated apply(@Val Claim claim, @Val Sheet sheet, ClaimRepository repository, EntityManager entityManager,
            PeriodRepository periods, PersonRepository people, RoleRepository roles,
            @RequestHeader(name = "X-User", required = false) String userHeader,
            @RequestParam(name = "date", required = false) String dateParam) throws ClaimStatusException {
        String user = AuditUser.decode(userHeader);
        if (!Role.STEWARD.equals(roles.roleOf(claim.getSheetId(), user))) {
            throw new ClaimStatusException("Only a steward can re-date a claim.");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(dateParam == null ? "" : dateParam.trim());
        } catch (DateTimeParseException e) {
            throw new ClaimStatusException("Not a date: " + dateParam);
        }
        boolean perPeriod = sheet.periodNumberingOn();
        if (perPeriod) {
            // Lock the sheet first (taking a number locks it), then re-read the claim so a concurrent change is seen.
            repository.lockSheet(claim.getSheetId());
            entityManager.refresh(claim);
        }
        if (claim.getStatus() != ClaimStatus.OPEN && claim.getStatus() != ClaimStatus.PENDING) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim cannot be re-dated.");
        }
        if (periods.isClosedOn(claim.getSheetId(), claim.getDueBy())) {
            throw new ClaimStatusException("A claim cannot be re-dated while its due-by is in a closed period.");
        }
        if (people.findById(claim.getPersonId()).map(p -> p.retiredOn(date)).orElse(false)) {
            throw new ClaimStatusException("No claim can be dated after the person's retirement date.");
        }
        LocalDate old = claim.getDate();
        claim.setDate(date);
        repository.saveAndFlush(claim);
        return new Redated(old, date);
    }

}

package net.officefloor.hq.app.claim;

import jakarta.persistence.EntityManager;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Moves a DRAFT, OPEN or PENDING claim to another person of the sheet; only a role the sheet's permission matrix
 * allows to reassign may (a steward, by the standing rule); a claim whose due-by is in a closed period, or one dated after the new person's retirement date, is refused. The claim keeps its
 * date and takes the next number in the sequence it would be opened in now (the sheet's, or its period's while period
 * numbering is on; its old number is never used again; a DRAFT, having none, gets none), loses any figure a statement bound it to (it is live until its new person's next statement) and is listed
 * as its new person's newest claim (moving it to the person it is already under relists it there, as the bulk move
 * does for a selected claim already that person's). Returns the name of the person it moved from, for the audit.
 */
public class ApplyReassign {
    public String apply(@Val Claim claim, @Val Person to, ClaimRepository repository, PersonRepository people,
            RoleRepository roles, EntityManager entityManager, SheetRepository sheets, PeriodRepository periods,
            @RequestHeader(name = "X-User", required = false) String userHeader)
            throws ClaimStatusException, ClerkLimitException {
        String user = AuditUser.decode(userHeader);
        Sheet sheet = sheets.findById(claim.getSheetId()).orElseGet(() -> new Sheet(claim.getSheetId()));
        // Lock the sheet first (taking a number needs it locked), then re-read the claim so a concurrent change is seen.
        repository.lockSheet(claim.getSheetId());
        entityManager.refresh(claim);
        String role = roles.roleOf(claim.getSheetId(), user);
        if (!sheet.allows("reassign", role)) {
            throw new ClerkLimitException("A " + role + " cannot move a claim to another person.");
        }
        if (periods.isClosedOn(claim.getSheetId(), claim.getDueBy())) {
            throw new ClaimStatusException("A claim in a closed period cannot move to another person.");
        }
        if (claim.getStatus() != ClaimStatus.DRAFT && !claim.getStatus().isOpen()) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim cannot move to another person.");
        }
        if (to.retiredOn(claim.getDate())) {
            throw new ClaimStatusException("A claim cannot move to a person retired before its date.");
        }
        String from = people.findById(claim.getPersonId()).map(Person::getName).orElse("");
        claim.setPersonId(to.getId());
        if (claim.getNumber() != null) {
            if (sheet.periodNumberingOn()) {
                String prefix = periods.findBySheetIdOrderByFromDateAscIdAsc(claim.getSheetId()).stream()
                        .filter(p -> p.contains(claim.getDueBy())).map(Period::getName).findFirst().orElse(null);
                String seq = prefix == null ? "X" : "P:" + prefix;
                claim.setNumber((prefix == null ? "X" : prefix) + "-" + repository.takeNextInSequence(claim.getSheetId(), seq));
            } else {
                repository.takeNextNumber(claim.getSheetId());
                claim.setNumber(String.valueOf(repository.lastNumber(claim.getSheetId())));
            }
        }
        claim.bind(false, null);
        repository.saveAndFlush(claim);
        repository.relist(claim.getId());
        entityManager.refresh(claim);
        return from;
    }
}

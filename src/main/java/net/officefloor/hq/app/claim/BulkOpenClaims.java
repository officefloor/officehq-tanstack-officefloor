package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Permissions;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Opens the selected claims in list order (people in the order they were added, newest claim first
 *  within a person), each numbered in turn as {@link OpenClaim} numbers one and recorded as it would
 *  record it (its CLAIM_OPENED line, or CLAIM_REVIEW for one gone to REVIEW, which counts as opened,
 *  then the rollup lines it moved). A claim that cannot be opened
 *  (its status does not allow it, its date is in a closed period, it has an empty note and a due
 *  above the note threshold, its person's flag is in force and the user is not a steward, its period caps its person's open claims and they
 *  already have that many, or it is not on the sheet) is left
 *  as it is and counted as refused; every one is, for a role the permission matrix does not let open
 *  or on an archived sheet. */
public class BulkOpenClaims {
    public void open(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by,
            @RequestBody BulkOpenRequest request, ClaimRepository claims, PeriodRepository periods,
            PersonRepository people, RateRepository rates, SheetRepository sheets,
            SettingRepository settings, RoleRepository roles, ClaimLogRepository log, Audit audit, Out<BulkOpenResult> result) {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        Set<Long> selected = new HashSet<>(request == null || request.claimIds() == null
                ? List.of() : request.claimIds());
        selected.remove(null);

        // The selected claims of the sheet, in list order.
        List<Person> everyone = people.list(id);
        List<Claim> all = claims.list(id);
        List<Claim> ordered = new ArrayList<>();
        for (Person p : everyone) {
            for (Claim c : all) {
                if (c.getPersonId() == p.getId() && selected.contains(c.getId())) ordered.add(c);
            }
        }

        boolean allowed = !sheets.isArchived(id)
                && Permissions.allows(id, user, Permissions.OPEN, roles, settings);
        int opened = 0;
        Rollup before = Rollup.of(id, people, claims, rates, sheets, settings);
        for (Claim claim : ordered) {
            if (!allowed) continue;
            try {
                OpenClaim.requireFlagSteward(claim, user, people, roles);
                periods.requireOpen(id, claim.getDueBy());
                ClaimStatus status = OpenClaim.opensTo(claim, people, rates, settings, sheets);
                OpenClaim.requireNote(claim, people, rates, settings, sheets);
                OpenClaim.requireUnderCap(claim, claims, periods);
                claim.setStatus(status.name());
            } catch (ClaimStatusException | PeriodClosedException refused) {
                continue;
            }
            claim.setOpenedBy(user);
            claims.update(claim);
            log.record(claim, user);
            opened++;
            Person person = everyone.stream().filter((p) -> p.getId() == claim.getPersonId())
                    .findFirst().orElse(null);
            audit.record(ClaimStatus.valueOf(claim.getStatus()).event() + " sheet=" + id + " by=" + user
                    + " person=" + (person == null ? "" : person.getName())
                    + " amount=" + claim.getAmount() + " date=" + claim.getDate()
                    + claim.getAuditNumber());
            Rollup after = Rollup.of(id, people, claims, rates, sheets, settings);
            for (String line : after.changesSince(before, id, user)) {
                audit.record(line);
            }
            before = after;
        }
        result.set(new BulkOpenResult(opened, selected.size() - opened));
    }
}

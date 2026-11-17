package net.officefloor.hq.app.claim;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Opens each selected claim that can be opened, in list order (people in list order, see PersonRepository#list; a person's
 * claims newest first): as the single open does, it must be a DRAFT whose due-by is not inside a closed period, nor one with an empty note whose due is above the note threshold
 * (see ApplyClaimStatus#needsNote), nor one that would take its person past its period's cap (see ClaimRules#overCap; those opened
 * before it in the same press count), takes its
 * number (see ClaimNumbering) and records who opened it (or, its due above the review threshold, goes to REVIEW
 * unnumbered, writing CLAIM_REVIEW, and counts as opened; see ApplyClaimStatus#needsReview), adds its OPEN line to its status log (see ClaimLog), then writes CLAIM_OPENED and the figures it moved (see Rollup).
 * Only a steward opens a claim of a person flagged on the sheet's today (see Person#flaggedOn).
 * A user whose role the sheet does not permit to open opens none. Each that cannot, or is not on the sheet, is left as it is and counted refused.
 */
public class OpenClaims {
    public void open(@PathVariable("sheetId") String sheetId, @Val BulkOpenRequest request, ClaimRepository claims,
            PersonRepository people, RateRepository rates, PeriodRepository periods, SettingRepository settings, RoleRepository roles, SheetRepository sheets, Rollup rollup, Audit audit, ClaimLog log,
            Out<BulkOpenResult> result) {
        String sheet = sheetId.trim();
        Set<Long> selected = new HashSet<>(request.claimIds());
        List<Person> listed = people.list(sheet);
        Map<Long, Integer> personOrder = new HashMap<>();
        for (int i = 0; i < listed.size(); i++) {
            personOrder.put(listed.get(i).id(), i);
        }
        Map<Long, Person> byId = listed.stream().collect(Collectors.toMap(Person::id, (p) -> p));
        List<Claim> chosen = claims.list(sheet).stream().filter((c) -> selected.contains(c.id()))
                .sorted((a, b) -> Integer.compare(personOrder.getOrDefault(a.personId(), Integer.MAX_VALUE),
                        personOrder.getOrDefault(b.personId(), Integer.MAX_VALUE)))
                .toList();
        LateCharge late = LateCharge.of(sheet, sheets, settings, claims);
        int opened = 0;
        String role = roles.roleOf(sheet, request.by());
        boolean steward = RoleRepository.STEWARD.equals(role);
        java.time.LocalDate today = sheets.today(sheet);
        if (!settings.permits(sheet, "open", role)) {
            chosen = List.of();
        }
        for (Claim claim : chosen) {
            if (claim.status() != ClaimStatus.DRAFT || periods.closedOn(sheet, claim.dueBy())
                    || ApplyClaimStatus.needsNote(claim, people, rates, settings, late)
                    || ClaimRules.overCap(claim, periods, claims)
                    || (!steward && byId.containsKey(claim.personId()) && byId.get(claim.personId()).flaggedOn(today))) {
                continue;
            }
            RollupBefore before = rollup.take(sheet);
            boolean review = ApplyClaimStatus.needsReview(claim, people, rates, settings, late);
            String number = review ? null : ClaimNumbering.next(claim, claims, periods, settings, sheets);
            Claim toSave = new Claim(claim.id(), claim.sheetId(), claim.personId(), claim.amount(), claim.date(),
                    claim.note(), review ? ClaimStatus.REVIEW : ClaimStatus.OPEN, number, claim.due(), request.by(), claim.stated(),
                    claim.statedDue(), claim.corrects(), claim.dueBy(), claim.deferredUntil(), claim.proofs(), claim.splitFrom(), claim.ref(), claim.tags(), claim.held());
            if (!claims.updateStatus(toSave, ClaimStatus.DRAFT)) {
                continue;
            }
            opened++;
            log.write(toSave, request.by());
            Person person = byId.get(claim.personId());
            audit.record((review ? "CLAIM_REVIEW" : "CLAIM_OPENED") + " sheet=" + sheet + " by=" + request.by() + " person="
                    + (person == null ? "" : person.name()) + " amount=" + claim.amount().toPlainString()
                    + " date=" + claim.date() + (review ? "" : " number=" + number));
            rollup.record(sheet, request.by(), before);
        }
        result.set(new BulkOpenResult(opened, selected.size() - opened));
    }
}

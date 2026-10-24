package net.officefloor.hq.app.paste;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.claim.OpenClaim;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimAudit;
import net.officefloor.hq.app.claim.ClaimLineRepository;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRules;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonAliasRepository;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.voidreason.VoidReasonRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Finds each line's person on the sheet by name or alias, ignoring case. A line whose person, date (blank: the
 * sheet's entry date) and note match one of that person's claims as they stood before the paste updates that claim:
 * a DRAFT is edited to the line's amount (CLAIM_EDITED when it changes; refused while another user edits it or when
 * its lines total otherwise) and opened; a match in any other status refuses the paste. Any other line makes its
 * claim by the claim form's rules (a reference repeating one of an earlier line, ignoring case, is refused too),
 * which is added and opened. Lines go in order; each is opened exactly as the open button opens a claim, then its
 * rollup lines follow. Any refusal refuses the whole paste: nothing is stored and no audit record is written, as the
 * records are written only once every line has gone through.
 */
public class AddPastedClaims {
    public void add(@Val PastedLines pasted, @Val Sheet sheet, PersonRepository people, PersonAliasRepository aliases,
            ClaimRepository claims, PeriodRepository periods, PersonGradeRepository grades, RateRepository rates,
            ClaimLogRepository log, HolidayRepository holidays, ClaimLineRepository claimLines, SheetRepository sheets,
            RoleRepository roles, VoidReasonRepository voidReasons, EntityManager entityManager,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit,
            Out<PastedClaims> addedOut) throws InvalidClaimException {
        String by = AuditUser.decode(user);
        List<Person> onSheet = people.findListed(sheet.getId());
        Map<Long, List<Claim>> before = new HashMap<>();
        Set<String> refs = new HashSet<>();
        Set<Long> matched = new HashSet<>();
        List<String> records = new ArrayList<>();
        Rollup rollup = Rollup.of(sheet.getId(), sheets, people, claims, rates, grades);
        int at = 0;
        for (PastedLine line : pasted.lines()) {
            at++;
            String prefix = "Line " + at + ": ";
            Person person = PersonAliasRepository.findByNameOrAlias(onSheet, aliases, sheet.getId(), line.name())
                    .orElse(null);
            if (person == null) {
                throw new InvalidClaimException(prefix + line.name() + " is not on the sheet.");
            }
            if (!before.containsKey(person.getId())) {
                // The person's claims as they stood before the paste: claims this paste adds never match.
                List<Claim> theirs = claims.findBySheetIdAndPersonIdOrderByIdDesc(sheet.getId(), person.getId());
                before.put(person.getId(), theirs);
            }
            LocalDate date = line.claim().date() != null ? line.claim().date() : sheet.entryDateInForce();
            String note = noteOf(line.claim().note());
            Claim claim = before.get(person.getId()).stream()
                    .filter(c -> date.equals(c.getDate()) && note.equals(noteOf(c.getNote())))
                    .filter(c -> !matched.contains(c.getId()))
                    .min((a, b) -> Boolean.compare(a.getStatus() != ClaimStatus.DRAFT, b.getStatus() != ClaimStatus.DRAFT))
                    .orElse(null);
            if (claim != null) {
                if (claim.getStatus() != ClaimStatus.DRAFT) {
                    throw new InvalidClaimException(prefix + "it matches a " + claim.getStatus()
                            + " claim of " + person.getName() + "; only a draft can be updated by a paste.");
                }
                if (claim.isLockedFor(by)) {
                    throw new InvalidClaimException(prefix + "the claim is being edited by " + claim.getLockedBy() + ".");
                }
                matched.add(claim.getId());
                if (claim.getAmount().compareTo(line.claim().amount()) != 0) {
                    if (periods.isClosedOn(sheet.getId(), claim.getDueBy())) {
                        throw new InvalidClaimException(prefix + "no claim can be edited in a closed period.");
                    }
                    BigDecimal lined = claimLines.totalOf(claim);
                    if (lined != null && line.claim().amount().compareTo(lined) != 0) {
                        throw new InvalidClaimException(prefix + "a claim with lines has their total as its amount ("
                                + lined.toPlainString() + ").");
                    }
                    claim.setAmount(line.claim().amount());
                    claim.setEditedBy(by);
                    claim = claims.saveAndFlush(claim);
                    records.add(ClaimAudit.line("CLAIM_EDITED", claim, person.getName(), by));
                }
            } else {
                try {
                    claim = ClaimRules.make(line.claim(), sheet, person, claims, periods, grades, holidays);
                } catch (InvalidClaimException e) {
                    throw new InvalidClaimException(prefix + e.getMessage());
                }
                if (line.claim().ref() != null && !refs.add(line.claim().ref().toLowerCase(Locale.ROOT))) {
                    throw new InvalidClaimException(prefix + "the reference " + line.claim().ref()
                            + " is already used in this paste.");
                }
                claim = claims.saveAndFlush(claim);
                records.add(ClaimAudit.line("CLAIM_ADDED", claim, person.getName(), by)
                        + (claim.getRef() == null ? "" : " ref=" + claim.getRef())
                        + (claim.getTags() == null ? "" : " tags=" + claim.getTags()));
                if (claim.isLateEntry()) {
                    records.add(ClaimAudit.line("LATE_ENTRY", claim, person.getName(), by));
                }
                ClaimRules.log(claim, sheet, log, people, rates, grades, by);
            }
            try {
                OpenClaim.open(claim, claims, entityManager, people, rates, grades, periods, sheet, roles, log,
                        voidReasons, user);
            } catch (Exception e) {
                throw new InvalidClaimException(prefix + e.getMessage());
            }
            records.add(ClaimAudit.line(claim.getStatus().event(), claim, person.getName(), by));
            ClaimRules.log(claim, sheet, log, people, rates, grades, by);
            Rollup now = Rollup.of(sheet.getId(), sheets, people, claims, rates, grades);
            records.addAll(now.changesSince(rollup, sheet.getId(), by));
            rollup = now;
        }
        records.forEach(audit::record);
        addedOut.set(new PastedClaims(at));
    }

    private static String noteOf(String note) {
        return Objects.requireNonNullElse(note, "").trim();
    }
}

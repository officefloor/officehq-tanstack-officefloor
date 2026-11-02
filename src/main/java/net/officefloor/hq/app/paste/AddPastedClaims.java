package net.officefloor.hq.app.paste;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import net.officefloor.hq.app.holiday.Holidays;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimLog;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.ClaimStatusChange;
import net.officefloor.hq.app.claim.ClaimStatusChanger;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.claim.InvalidClaimStatusChangeException;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Takes each checked line, in order, and opens its claim as the open button would, so they are numbered in paste
 * order. A line whose person, date and note match one of the person's claims as they stood before the paste (not
 * VOID or DELETED) updates that claim: the newest matching DRAFT is edited to the line's amount (when it differs) and
 * opened; a line whose matches hold no DRAFT is refused. Any other line adds a draft claim, as the claim form's
 * BuildClaim adds one, then opens it. Any refusal refuses the whole paste: its savepoint rolls back and the audit
 * records held meanwhile are dropped.
 */
public class AddPastedClaims {
    public void add(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @Val PastedClaims pasted, Claims claims, Periods periods, Settings settings, Holidays holidays, ClaimLog log, Audit audit, Rollup rollup,
            ClaimStatusChanger changer, PlatformTransactionManager transactions, ObjectResponse<PasteResponse> response)
            throws InvalidClaimException {
        TransactionTemplate transaction = new TransactionTemplate(transactions);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        // Newest first, as the sheet lists them: matched against as they stood before the paste.
        List<Claim> was = claims.list(sheet);
        audit.hold();
        try {
            // Within a savepoint, so a refusal part way through undoes the lines before it.
            transaction.execute((status) -> {
                try {
                    take(sheet, user, pasted, was, claims, periods, settings, holidays, log, audit, rollup, changer);
                } catch (InvalidClaimException refused) {
                    throw new Refused(refused);
                }
                return null;
            });
        } catch (Refused refused) {
            audit.discard();
            throw refused.cause;
        } catch (RuntimeException failed) {
            audit.discard();
            throw failed;
        }
        audit.release();
        response.send(new PasteResponse(pasted.lines().size(), claims.list(sheet)));
    }

    private static void take(String sheet, String user, PastedClaims pasted, List<Claim> was, Claims claims,
            Periods periods, Settings settings, Holidays holidays, ClaimLog log, Audit audit, Rollup rollup,
            ClaimStatusChanger changer) throws InvalidClaimException {
        for (PastedClaim line : pasted.lines()) {
            Person person = line.person();
            ClaimInput input = line.input();
            List<Claim> matches = was.stream()
                    .filter((c) -> c.person() == person.id() && c.status() != ClaimStatus.VOID
                            && c.status() != ClaimStatus.DELETED)
                    .filter((c) -> c.date().equals(input.date().toString())
                            && Objects.equals(c.note() == null ? "" : c.note(), input.note()))
                    .toList();
            long id;
            if (matches.isEmpty()) {
                id = add(sheet, user, person, input, claims, periods, settings, holidays, log, audit, rollup);
            } else {
                Claim draft = matches.stream().filter((c) -> c.status() == ClaimStatus.DRAFT).findFirst()
                        .orElseThrow(() -> new InvalidClaimException(
                                "Line " + line.at() + ": its claim is no longer a DRAFT"));
                edit(sheet, user, line, draft, claims, periods, audit, rollup);
                id = draft.id();
            }
            try {
                // Re-read so each claim is opened as the store holds it after the lines before it.
                changer.change(sheet, user, claims.find(sheet, id), ClaimStatusChange.OPEN);
            } catch (InvalidClaimStatusChangeException refusal) {
                throw new InvalidClaimException("Line " + line.at() + ": " + refusal.getMessage());
            }
        }
    }

    /** Carries a refusal out of the savepoint, rolling it back. */
    private static class Refused extends RuntimeException {
        final InvalidClaimException cause;

        Refused(InvalidClaimException cause) {
            super(cause);
            this.cause = cause;
        }
    }

    private static long add(String sheet, String user, Person person, ClaimInput input, Claims claims, Periods periods,
            Settings settings, Holidays holidays, ClaimLog log, Audit audit, Rollup rollup) {
        Rollup.Snapshot before = rollup.take(sheet);
        LocalDate dueBy = person.dueBy(sheet, input.date(), settings, holidays);
        long id = claims.add(sheet, person.id(), input, dueBy, person.termsOn(sheet, input.date(), settings));
        log.record(sheet, user, id);
        audit.record("CLAIM_ADDED sheet=" + sheet + " by=" + user + " " + input.addedAuditFields(person.name()));
        if (periods.softClosedOn(sheet, dueBy)) {
            claims.setLateEntry(sheet, id);
            audit.record("LATE_ENTRY sheet=" + sheet + " by=" + user + " " + input.auditFields(person.name()));
        }
        rollup.record(sheet, user, before);
        return id;
    }

    /** Edits the matched DRAFT to the line's amount, as the claim's editor would save it; nothing when it is the same. */
    private static void edit(String sheet, String user, PastedClaim line, Claim draft, Claims claims, Periods periods,
            Audit audit, Rollup rollup) throws InvalidClaimException {
        BigDecimal amount = line.input().amount();
        if (amount.compareTo(new BigDecimal(draft.amount())) == 0) {
            return;
        }
        String refused = null;
        if (periods.closedOn(sheet, LocalDate.parse(draft.dueBy()))) {
            refused = "a claim due inside a closed period may not be edited";
        } else if (!draft.lines().isEmpty()) {
            refused = "a claim with lines has their total for its amount";
        } else if (draft.lockedBy() != null && !draft.lockedBy().equals(user)) {
            refused = "the claim is being edited by " + draft.lockedBy();
        }
        if (refused != null) {
            throw new InvalidClaimException("Line " + line.at() + ": " + refused);
        }
        ClaimInput input = new ClaimInput(amount, line.input().date(), line.input().note(), draft.corrects(),
                line.input().ref() != null ? line.input().ref() : draft.ref(), draft.tags());
        try {
            input.checkCorrects(sheet, draft.person(), draft.id(), claims);
        } catch (InvalidClaimException e) {
            throw new InvalidClaimException("Line " + line.at() + ": " + e.getMessage());
        }
        Rollup.Snapshot before = rollup.take(sheet);
        if (!claims.update(sheet, draft.id(), input, draft.tagsEver(), user)) {
            throw new InvalidClaimException("Line " + line.at() + ": the claim is no longer a DRAFT, or another user is editing it");
        }
        audit.record("CLAIM_EDITED sheet=" + sheet + " by=" + user + " " + input.auditFields(line.person().name()));
        rollup.record(sheet, user, before);
    }
}

package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.period.PeriodGuard;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveClaim {
    public void save(@PathVariable("sheetId") String sheetId, @PathVariable("claimId") String claimId,
            @Val ClaimInput input, ServerHttpConnection connection, ClaimRepository repository,
            SheetRepository sheets, PeriodRepository periods, PersonRepository people, Audit audit, Rollup rollup, Out<Claim> claim) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        long cid;
        try {
            cid = Long.parseLong(claimId == null ? "" : claimId.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("no such claim");
        }
        Claim found = repository.find(id, cid);
        if (found == null) {
            throw new BadRequestException("no such claim");
        }
        // Only a draft can be edited: to change an open claim, void it and add a new one.
        if (!found.status().equals("DRAFT")) {
            throw new BadRequestException("a " + found.status() + " claim cannot be edited");
        }
        String holder = repository.lockedBy(id, cid);
        if (holder != null && !holder.equals(by)) {
            throw new BadRequestException("the claim is being edited by " + holder);
        }
        LocalDate date = input.date() != null ? input.date() : sheets.today(id);
        PeriodGuard.open(periods, id, LocalDate.parse(found.dueBy()));
        people.claimAllowed(id, found.personId(), date);
        input.checkCorrection(repository, id, found.personId());
        input.checkRef(repository, id, cid);
        // A claim's due-by is fixed when it is created: editing its date leaves it as it is.
        String dueBy = found.dueBy();
        Rollup.Snapshot before = rollup.take(id);
        if (!repository.update(id, cid, input.amount(), date, LocalDate.parse(dueBy), input.note(), input.corrects(), input.ref(), input.tags())) {
            throw new BadRequestException("only a DRAFT claim can be edited");
        }
        repository.editedBy(id, cid, by);
        repository.unlock(id, cid, by);
        long pid = found.personId();
        Claim saved = new Claim(cid, pid, ClaimRepository.money(input.amount()), date.toString(), input.note(),
                found.status(), found.number(), found.due(), found.openedBy(), found.stated(), found.statedDue(), input.corrects(),
                dueBy, found.statedNa(), found.deferredUntil(), found.deferred(), found.proofs(), found.splitFrom(), found.lateEntry(), found.reminded(), input.ref(), input.tags(), found.voidReason());
        audit.record("CLAIM_EDITED sheet=" + id + " by=" + by + " person=" + repository.personName(id, pid)
                + " amount=" + saved.amount() + " date=" + saved.date() + Claim.auditNumber(saved.number()));
        rollup.record(id, by, before);
        claim.set(saved);
    }
}

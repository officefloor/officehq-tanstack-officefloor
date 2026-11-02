package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveClaim {
    public void save(@PathVariable("sheetId") String sheetId, @PathVariable("claimId") String claimId,
            @Val ClaimInput input, ServerHttpConnection connection, ClaimRepository repository,
            SheetRepository sheets, Audit audit, Out<Claim> claim) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        long cid;
        try {
            cid = Long.parseLong(claimId == null ? "" : claimId.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("no such claim");
        }
        Long pid = repository.personOf(id, cid);
        if (pid == null) {
            throw new BadRequestException("no such claim");
        }
        LocalDate date = input.date() != null ? input.date() : sheets.today(id);
        repository.update(id, cid, input.amount(), date, input.note());
        Claim saved = new Claim(cid, pid, ClaimRepository.money(input.amount()), date.toString(), input.note());
        audit.record("claim-edit sheet=" + id + " by=" + by + " claim=" + cid + " person=" + pid
                + " amount=" + saved.amount() + " date=" + saved.date() + " note=" + saved.note());
        claim.set(saved);
    }
}

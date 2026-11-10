package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.history.AsOfView;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadClaims {
    public void load(@PathVariable("sheetId") String sheetId, ServerHttpConnection connection,
            ClaimRepository repository, AsOfView view, Out<Claims> claims) throws BadRequestException {
        CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        claims.set(view.read(connection, id, () -> new Claims(repository.list(id), repository.logs(id), repository.locks(id), repository.tagsUsed(id), repository.comments(id), repository.batches(id), repository.lastBatch(id), repository.lines(id), repository.added(id), repository.reviewLate(id), repository.shares(id), repository.reviewAssigned(id), repository.pendingLate(id))));
    }
}

package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadClaims {
    public void load(@PathVariable("sheetId") String sheetId, ServerHttpConnection connection,
            ClaimRepository repository, Out<Claims> claims) throws BadRequestException {
        CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        claims.set(new Claims(repository.list(id)));
    }
}

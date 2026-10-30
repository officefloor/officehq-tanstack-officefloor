package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePool {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PoolRequest request,
            Out<ValidPool> body, Settings settings, Pools pools) throws InvalidPoolException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidPoolException("A pool name cannot be blank");
        }
        if (pools.contains(sheetId, name)) {
            throw new InvalidPoolException("There is already a pool of that name");
        }
        body.set(new ValidPool(name, request.user()));
    }
}

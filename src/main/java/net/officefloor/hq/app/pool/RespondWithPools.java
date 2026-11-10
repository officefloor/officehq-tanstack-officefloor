package net.officefloor.hq.app.pool;

import java.util.List;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPools {
    public void respond(@Val Sheet sheet, PoolRepository repository, ObjectResponse<PoolsResponse> response) {
        List<PoolMember> members = repository.members(sheet.id());
        response.send(new PoolsResponse(repository.findAll(sheet.id()).stream()
                .map(p -> PoolResponse.from(p, members.stream().filter(m -> m.pool().equals(p.name())).toList()))
                .toList()));
    }
}

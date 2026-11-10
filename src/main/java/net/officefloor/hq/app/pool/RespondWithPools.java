package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.util.List;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPools {
    public void respond(@Val Sheet sheet, PoolRepository repository, SettingsRepository settings,
            ObjectResponse<PoolsResponse> response) {
        List<PoolMember> members = repository.members(sheet.id());
        BigDecimal pot = settings.find(sheet.id()).pot();
        response.send(new PoolsResponse(repository.findAll(sheet.id()).stream()
                .map(p -> PoolResponse.from(p, members.stream().filter(m -> m.pool().equals(p.name())).toList(), pot))
                .toList()));
    }
}

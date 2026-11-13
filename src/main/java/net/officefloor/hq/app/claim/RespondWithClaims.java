package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithClaims {
    public void respond(@Val Sheet sheet, @Val List<Claim> claims, ClaimLogRepository logs, ClaimLockRepository locks,
            ClaimRepository repository, ObjectResponse<List<ClaimResponse>> response) {
        Map<Long, List<ClaimLogLine>> log = logs.findAll(sheet.id());
        Map<Long, String> locked = locks.findAll(sheet.id());
        Set<Long> late = repository.lateEntries(sheet.id());
        Map<Long, LocalDate> reminded = repository.remindedAll(sheet.id());
        Map<Long, String> refs = repository.refs(sheet.id());
        Map<Long, String> tags = repository.tagsAll(sheet.id());
        response.send(claims.stream()
                .map((c) -> ClaimResponse.from(c, log.getOrDefault(c.id(), List.of()), locked.get(c.id()),
                        late.contains(c.id()), reminded.get(c.id()), refs.get(c.id()),
                        tags.get(c.id()))).toList());
    }
}

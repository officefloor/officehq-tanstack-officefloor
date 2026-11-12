package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.history.History;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's pools with their members, total share and whether they are balanced. */
public class RespondWithPools {
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, PoolStore store,
            ObjectResponse<List<PoolResponse>> response) {
        String sheet = sheetId.trim();
        LocalDate on = History.date(asOf);
        Map<Long, List<PoolStore.Member>> members = store.members(sheet, on);
        response.send(store.list(sheet, on).stream().map(p -> {
            List<PoolStore.Member> ms = members.getOrDefault(p.id(), List.of());
            BigDecimal total = ms.stream().filter(m -> !m.retired()).map(PoolStore.Member::share)
                    .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
            return new PoolResponse(p.id(), p.name(),
                    ms.stream().map(m -> new PoolResponse.MemberResponse(m.personId(), m.name(),
                            m.share().setScale(2).toPlainString(), m.retired())).toList(),
                    total.toPlainString(), total.compareTo(HUNDRED) == 0);
        }).toList());
    }
}

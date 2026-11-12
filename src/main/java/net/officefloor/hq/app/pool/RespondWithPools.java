package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.history.History;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's pools with their members, total share and whether they are balanced, and each member's cut
 *  of the sheet's pot. */
public class RespondWithPools {
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, PoolStore store, SettingStore settings,
            ObjectResponse<List<PoolResponse>> response) {
        String sheet = sheetId.trim();
        LocalDate on = History.date(asOf);
        BigDecimal pot = (on == null ? settings : settings.asOf(on)).pot(sheet);
        Map<Long, List<PoolStore.Member>> members = store.members(sheet, on);
        response.send(store.list(sheet, on).stream().map(p -> {
            List<PoolStore.Member> ms = members.getOrDefault(p.id(), List.of());
            BigDecimal total = ms.stream().filter(m -> !m.retired()).map(PoolStore.Member::share)
                    .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
            boolean balanced = total.compareTo(HUNDRED) == 0;
            List<BigDecimal> cuts = balanced && pot != null ? cuts(ms, pot) : null;
            List<PoolResponse.MemberResponse> shown = new ArrayList<>();
            for (int i = 0; i < ms.size(); i++) {
                PoolStore.Member m = ms.get(i);
                shown.add(new PoolResponse.MemberResponse(m.personId(), m.name(),
                        m.share().setScale(2).toPlainString(), m.retired(),
                        cuts == null ? null : cuts.get(i).toPlainString()));
            }
            return new PoolResponse(p.id(), p.name(), shown, total.toPlainString(), balanced);
        }).toList());
    }

    /** Each member's cut, in the members' order: the pot times their share over 100, rounded half to even to
     *  two decimals (a retired member's is 0.00); the remainder goes to the member with the largest share, the
     *  earliest added on a tie, so the cuts total exactly the pot. */
    private static List<BigDecimal> cuts(List<PoolStore.Member> ms, BigDecimal pot) {
        List<BigDecimal> cuts = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        int largest = -1;
        for (int i = 0; i < ms.size(); i++) {
            PoolStore.Member m = ms.get(i);
            BigDecimal cut = m.retired() ? BigDecimal.ZERO.setScale(2)
                    : pot.multiply(m.share()).divide(HUNDRED).setScale(2, RoundingMode.HALF_EVEN);
            cuts.add(cut);
            sum = sum.add(cut);
            if (!m.retired() && (largest < 0 || m.share().compareTo(ms.get(largest).share()) > 0)) {
                largest = i;
            }
        }
        if (largest >= 0) {
            cuts.set(largest, cuts.get(largest).add(pot.subtract(sum)).setScale(2));
        }
        return cuts;
    }
}

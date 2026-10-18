package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.web.HttpHeaderParameter;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Opens each selected claim of the sheet that can be opened — a DRAFT not dated inside a closed period —
 * giving each the sheet's next number in list order (people in the order they were added, then each
 * person's claims in the order they were added) and recording who opened it. Any other selected claim,
 * or an id not on the sheet, is left as it is and counted refused.
 */
public class BulkOpenClaims {
    public void open(@PathVariable("sheetId") String sheetId, @RequestBody BulkOpenRequest request,
            @HttpHeaderParameter("X-User") String user, ClaimRepository repository, PeriodRepository periods,
            Out<BulkOpened> openedOut) {
        String sheet = sheetId.trim();
        Set<Long> selected = new HashSet<>(request == null || request.ids() == null ? List.of() : request.ids());
        selected.remove(null);
        List<Claim> claims = repository.findBySheetIdOrderByIdDesc(sheet).stream()
                .filter((c) -> selected.contains(c.getId()))
                .sorted(Comparator.comparing(Claim::getPersonId).thenComparing(Claim::getId))
                .toList();
        List<Period> closed = periods.findBySheetId(sheet).stream().filter(Period::isClosed).toList();
        List<Claim> opened = new ArrayList<>();
        for (Claim claim : claims) {
            if (claim.getStatus() != ClaimStatus.DRAFT || closed.stream().anyMatch((p) -> p.contains(claim.getDate()))) {
                continue;
            }
            // As for a single open: taking the number holds the sheet's numbers, then the stored status
            // is re-checked so a claim another user opened meanwhile is refused here.
            repository.takeClaimNumber(sheet);
            if (ClaimStatus.valueOf(repository.storedStatus(claim.getId())) != ClaimStatus.DRAFT) {
                continue;
            }
            claim.setStatus(ClaimStatus.OPEN);
            claim.setNumber(repository.lastClaimNumber(sheet));
            claim.setOpenedBy(user == null ? "" : user.trim());
            repository.save(claim);
            opened.add(claim);
        }
        openedOut.set(new BulkOpened(opened, selected.size() - opened.size()));
    }
}

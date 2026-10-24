package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimLogRepository extends JpaRepository<ClaimLog, Long> {

    /** The sheet's log lines, oldest first. */
    List<ClaimLog> findBySheetIdOrderByIdAsc(String sheetId);

    /** The claim's log lines, oldest first. */
    List<ClaimLog> findByClaimIdOrderByIdAsc(Long claimId);

    /** The sheet's today when the claim was last settled (its latest SETTLED log line); null when it never was. */
    default LocalDate settledOn(Claim claim) {
        return findByClaimIdOrderByIdAsc(claim.getId()).stream().filter(l -> ClaimStatus.SETTLED.name().equals(l.getStatus()))
                .map(ClaimLog::getOnDate).reduce((a, b) -> b).orElse(null);
    }

    /** The sheet's today when the claim was last deleted (its latest DELETED log line); null when it never was. */
    default LocalDate deletedOn(Claim claim) {
        return findByClaimIdOrderByIdAsc(claim.getId()).stream().filter(l -> ClaimStatus.DELETED.name().equals(l.getStatus()))
                .map(ClaimLog::getOnDate).reduce((a, b) -> b).orElse(null);
    }

    /** The claim's log as the screen shows it, oldest first. */
    default List<String> linesOf(Claim claim) {
        return findByClaimIdOrderByIdAsc(claim.getId()).stream().map(ClaimLog::line).toList();
    }

    /** The status each claim on the sheet had at the end of a day, by claim id; a claim not yet added then has none. */
    default Map<Long, ClaimStatus> statusesOn(String sheetId, LocalDate day) {
        Map<Long, ClaimStatus> statuses = new HashMap<>();
        findBySheetIdOrderByIdAsc(sheetId).stream().filter(l -> !l.getOnDate().isAfter(day))
                .forEach(l -> statuses.put(l.getClaimId(), ClaimStatus.valueOf(l.getStatus())));
        return statuses;
    }

    /** The log of each claim on the sheet as the screen shows it, oldest first, by claim id. */
    default Map<Long, List<String>> linesOnSheet(String sheetId) {
        return findBySheetIdOrderByIdAsc(sheetId).stream().collect(Collectors.groupingBy(ClaimLog::getClaimId,
                Collectors.mapping(ClaimLog::line, Collectors.toList())));
    }
}

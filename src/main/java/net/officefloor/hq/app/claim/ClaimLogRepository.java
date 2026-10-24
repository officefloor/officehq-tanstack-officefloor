package net.officefloor.hq.app.claim;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimLogRepository extends JpaRepository<ClaimLog, Long> {

    /** The sheet's log lines, oldest first. */
    List<ClaimLog> findBySheetIdOrderByIdAsc(String sheetId);

    /** The claim's log lines, oldest first. */
    List<ClaimLog> findByClaimIdOrderByIdAsc(Long claimId);

    /** The claim's log as the screen shows it, oldest first. */
    default List<String> linesOf(Claim claim) {
        return findByClaimIdOrderByIdAsc(claim.getId()).stream().map(ClaimLog::line).toList();
    }

    /** The log of each claim on the sheet as the screen shows it, oldest first, by claim id. */
    default Map<Long, List<String>> linesOnSheet(String sheetId) {
        return findBySheetIdOrderByIdAsc(sheetId).stream().collect(Collectors.groupingBy(ClaimLog::getClaimId,
                Collectors.mapping(ClaimLog::line, Collectors.toList())));
    }
}

package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimCommentRepository extends JpaRepository<ClaimComment, Long> {

    /** The sheet's comments, oldest first. */
    List<ClaimComment> findBySheetIdOrderByIdAsc(String sheetId);

    /** The claim's comments, oldest first. */
    List<ClaimComment> findByClaimIdOrderByIdAsc(Long claimId);

    /** The claim's comments as the screen shows them, oldest first. */
    default List<String> linesOf(Claim claim) {
        return findByClaimIdOrderByIdAsc(claim.getId()).stream().map(ClaimComment::line).toList();
    }

    /** The comments of each claim on the sheet as the screen shows them, oldest first, by claim id; as of a date
     *  (null: now), only those written while the sheet's today was on or before it. */
    default Map<Long, List<String>> linesOnSheet(String sheetId, LocalDate date) {
        return findBySheetIdOrderByIdAsc(sheetId).stream().filter(c -> date == null || !c.getOnDate().isAfter(date))
                .collect(Collectors.groupingBy(ClaimComment::getClaimId,
                        Collectors.mapping(ClaimComment::line, Collectors.toList())));
    }
}

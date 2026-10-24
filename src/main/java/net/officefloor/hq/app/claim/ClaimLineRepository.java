package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimLineRepository extends JpaRepository<ClaimLine, Long> {

    /** The sheet's lines, in the order added. */
    List<ClaimLine> findBySheetIdOrderByIdAsc(String sheetId);

    /** The claim's lines, in the order added. */
    List<ClaimLine> findByClaimIdOrderByIdAsc(Long claimId);

    /** The claim's lines as the screen shows them, in the order added. */
    default List<String> linesOf(Claim claim) {
        return findByClaimIdOrderByIdAsc(claim.getId()).stream().map(ClaimLine::line).toList();
    }

    /** The claim's lines' quantities times unit amounts, added up; null when it has no lines. */
    default BigDecimal totalOf(Claim claim) {
        List<ClaimLine> lines = findByClaimIdOrderByIdAsc(claim.getId());
        return lines.isEmpty() ? null
                : lines.stream().map(ClaimLine::total).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
    }

    /** The lines of each claim on the sheet as the screen shows them, in the order added, by claim id; as of a date
     *  (null: now), only those added while the sheet's today was on or before it. */
    default Map<Long, List<String>> linesOnSheet(String sheetId, LocalDate date) {
        return findBySheetIdOrderByIdAsc(sheetId).stream().filter(l -> date == null || !l.getOnDate().isAfter(date))
                .collect(Collectors.groupingBy(ClaimLine::getClaimId,
                        Collectors.mapping(ClaimLine::line, Collectors.toList())));
    }
}

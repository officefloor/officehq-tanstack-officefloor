package net.officefloor.hq.app.claim;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    /** The sheet's claims, newest first. */
    List<Claim> findBySheetIdOrderByIdDesc(String sheetId);

    /** The person's claims, newest first. */
    List<Claim> findBySheetIdAndPersonIdOrderByIdDesc(String sheetId, Long personId);

    Optional<Claim> findByIdAndSheetId(Long id, String sheetId);

    /** Takes the sheet's next claim number; the update locks the sheet's row until the transaction ends, so
     * two claims opened at once are numbered one after the other. */
    @Modifying
    @Query(value = "UPDATE sheet SET last_claim_number = last_claim_number + 1 WHERE id = :sheetId",
            nativeQuery = true)
    int takeNextNumber(@Param("sheetId") String sheetId);

    @Query(value = "SELECT last_claim_number FROM sheet WHERE id = :sheetId", nativeQuery = true)
    int lastNumber(@Param("sheetId") String sheetId);
}

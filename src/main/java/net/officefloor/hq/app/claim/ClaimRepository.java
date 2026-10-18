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

    /** The sheet's claim with this number (numbers are unique on a sheet once given). */
    Optional<Claim> findFirstBySheetIdAndNumber(String sheetId, Integer number);

    Optional<Claim> findByIdAndSheetId(Long id, String sheetId);

    /** Takes the sheet's next claim number. The update locks the sheet's row until the transaction
     *  ends, so claims opened at the same moment are numbered one after the other. */
    @Modifying
    @Query(value = "UPDATE sheet SET claim_numbers = claim_numbers + 1 WHERE id = :sheetId", nativeQuery = true)
    int takeClaimNumber(@Param("sheetId") String sheetId);

    @Query(value = "SELECT claim_numbers FROM sheet WHERE id = :sheetId", nativeQuery = true)
    int lastClaimNumber(@Param("sheetId") String sheetId);

    /** The claim's status as stored now, read past the loaded entity. */
    @Query(value = "SELECT status FROM claim WHERE id = :id", nativeQuery = true)
    String storedStatus(@Param("id") Long id);
}

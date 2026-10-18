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

    /** Holds the sheet's numbers (locks its row) without taking a sheet-wide number. */
    @Modifying
    @Query(value = "UPDATE sheet SET claim_numbers = claim_numbers WHERE id = :sheetId", nativeQuery = true)
    int holdClaimNumbers(@Param("sheetId") String sheetId);

    /** Takes the next number of one of the sheet's period sequences; 0 when the sequence has none yet. */
    @Modifying
    @Query(value = "UPDATE claim_sequence SET last_number = last_number + 1 WHERE sheet_id = :sheetId AND seq_key = :key",
            nativeQuery = true)
    int takeSequenceNumber(@Param("sheetId") String sheetId, @Param("key") String key);

    /** Starts one of the sheet's period sequences at 1. */
    @Modifying
    @Query(value = "INSERT INTO claim_sequence (sheet_id, seq_key, last_number) VALUES (:sheetId, :key, 1)",
            nativeQuery = true)
    int startSequence(@Param("sheetId") String sheetId, @Param("key") String key);

    @Query(value = "SELECT last_number FROM claim_sequence WHERE sheet_id = :sheetId AND seq_key = :key", nativeQuery = true)
    int lastSequenceNumber(@Param("sheetId") String sheetId, @Param("key") String key);

    /** The claim's status as stored now, read past the loaded entity. */
    @Query(value = "SELECT status FROM claim WHERE id = :id", nativeQuery = true)
    String storedStatus(@Param("id") Long id);
}

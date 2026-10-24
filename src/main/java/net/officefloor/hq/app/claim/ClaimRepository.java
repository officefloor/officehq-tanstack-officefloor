package net.officefloor.hq.app.claim;

import java.time.LocalDate;
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

    /** The person's claims given this number (at most one: numbers are never reused on a sheet). */
    List<Claim> findBySheetIdAndPersonIdAndNumber(String sheetId, Long personId, String number);

    /** Refuses a correction unless the claim it corrects is a SETTLED claim of the same person; gives that
     *  claim's date (null when it is not a correction). */
    default LocalDate checkCorrects(String sheetId, Long personId, String corrects) throws InvalidClaimException {
        if (corrects == null) {
            return null;
        }
        return findBySheetIdAndPersonIdAndNumber(sheetId, personId, corrects).stream()
                .filter(c -> c.getStatus() == ClaimStatus.SETTLED).map(Claim::getDate).findFirst()
                .orElseThrow(() -> new InvalidClaimException("A correction must name a settled claim of the same person."));
    }

    /** Takes the sheet's next claim number; the update locks the sheet's row until the transaction ends, so
     * two claims opened at once are numbered one after the other. */
    @Modifying
    @Query(value = "UPDATE sheet SET last_claim_number = last_claim_number + 1 WHERE id = :sheetId",
            nativeQuery = true)
    int takeNextNumber(@Param("sheetId") String sheetId);

    @Query(value = "SELECT last_claim_number FROM sheet WHERE id = :sheetId", nativeQuery = true)
    int lastNumber(@Param("sheetId") String sheetId);

    /** Locks the sheet's row until the transaction ends without taking a number, so two claims opened at once in
     *  one per-period sequence are numbered one after the other. */
    @Modifying
    @Query(value = "UPDATE sheet SET last_claim_number = last_claim_number WHERE id = :sheetId", nativeQuery = true)
    int lockSheet(@Param("sheetId") String sheetId);

    /** The last number given out in one of the sheet's per-period sequences; null when it has given none. */
    @Query(value = "SELECT last_number FROM claim_sequence WHERE sheet_id = :sheetId AND seq = :seq", nativeQuery = true)
    Integer lastInSequence(@Param("sheetId") String sheetId, @Param("seq") String seq);

    @Modifying
    @Query(value = "INSERT INTO claim_sequence (sheet_id, seq, last_number) VALUES (:sheetId, :seq, 1)", nativeQuery = true)
    int startSequence(@Param("sheetId") String sheetId, @Param("seq") String seq);

    @Modifying
    @Query(value = "UPDATE claim_sequence SET last_number = last_number + 1 WHERE sheet_id = :sheetId AND seq = :seq",
            nativeQuery = true)
    int advanceSequence(@Param("sheetId") String sheetId, @Param("seq") String seq);

    /** Takes the next number in one of the sheet's per-period sequences (from 1); call with the sheet locked. */
    default int takeNextInSequence(String sheetId, String seq) {
        Integer last = lastInSequence(sheetId, seq);
        if (last == null) {
            startSequence(sheetId, seq);
            return 1;
        }
        advanceSequence(sheetId, seq);
        return last + 1;
    }
}

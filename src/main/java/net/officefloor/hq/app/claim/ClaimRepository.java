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

    /** The sheet's claims in the order they are listed: newest first, a moved claim as its new person's newest. */
    List<Claim> findBySheetIdOrderByListedDescIdDesc(String sheetId);

    /** The sheet's claims some open editor locks. */
    List<Claim> findBySheetIdAndLockedByIsNotNull(String sheetId);

    /** Lists the claim as the newest: gives it the next listing key. */
    @Modifying
    @Query(value = "UPDATE claim SET listed = NEXT VALUE FOR claim_listed_seq WHERE id = :id", nativeQuery = true)
    int relist(@Param("id") Long id);

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

    /** Whether a claim on the sheet other than the given one (null: none) has the reference, ignoring case. */
    boolean existsBySheetIdAndRefIgnoreCaseAndIdNot(String sheetId, String ref, Long id);

    boolean existsBySheetIdAndRefIgnoreCase(String sheetId, String ref);

    /** Refuses a reference (null: none) another claim on the sheet already has, ignoring case. */
    default void checkRef(String sheetId, Long claimId, String ref) throws InvalidClaimException {
        if (ref == null) {
            return;
        }
        boolean used = claimId == null ? existsBySheetIdAndRefIgnoreCase(sheetId, ref)
                : existsBySheetIdAndRefIgnoreCaseAndIdNot(sheetId, ref, claimId);
        if (used) {
            throw new InvalidClaimException("The reference " + ref + " is already used on this sheet.");
        }
    }

    /** Takes the sheet's next claim number: the sheets of a tree (the sheet's top ancestor and every sheet under it)
     *  share one sequence, so it is the next after the last number given on any sheet of the tree, kept as the sheet's
     *  last number (read with {@link #lastNumber}). The tree's top sheet's row is locked, then the sheet's own, until the
     *  transaction ends, so two claims opened at once anywhere in the tree are numbered one after the other. */
    default int takeNextNumber(String sheetId) {
        java.util.Set<String> up = new java.util.LinkedHashSet<>();
        String top = sheetId;
        for (String id = sheetId; id != null && up.add(id); ) {
            top = id;
            id = parentOf(id).stream().filter(p -> p != null).findFirst().orElse(null);
        }
        lockSheet(top);
        lockSheet(sheetId);
        java.util.Set<String> tree = new java.util.LinkedHashSet<>();
        java.util.Deque<String> todo = new java.util.ArrayDeque<>(List.of(top));
        while (!todo.isEmpty()) {
            String id = todo.pop();
            if (tree.add(id)) {
                todo.addAll(childrenOf(id));
            }
        }
        int next = lastNumberIn(tree) + 1;
        setLastNumber(sheetId, next);
        return next;
    }

    @Query(value = "SELECT setting_value FROM sheet_setting WHERE sheet_id = :sheetId AND setting_key = 'parent'",
            nativeQuery = true)
    List<String> parentOf(@Param("sheetId") String sheetId);

    @Query(value = "SELECT DISTINCT sheet_id FROM sheet_setting WHERE setting_key = 'parent' AND setting_value = :sheetId",
            nativeQuery = true)
    List<String> childrenOf(@Param("sheetId") String sheetId);

    /** The last number given on any of the sheets. */
    @Query(value = "SELECT COALESCE(MAX(last_claim_number), 0) FROM sheet WHERE id IN (:sheetIds)", nativeQuery = true)
    int lastNumberIn(@Param("sheetIds") java.util.Collection<String> sheetIds);

    @Modifying
    @Query(value = "UPDATE sheet SET last_claim_number = :number WHERE id = :sheetId", nativeQuery = true)
    int setLastNumber(@Param("sheetId") String sheetId, @Param("number") int number);

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

    @Query(value = "SELECT batch_number FROM settle_batch WHERE sheet_id = :sheetId AND batch_key = :key", nativeQuery = true)
    Integer batchNumber(@Param("sheetId") String sheetId, @Param("key") String key);

    @Modifying
    @Query(value = "UPDATE sheet SET last_settle_batch = last_settle_batch + 1 WHERE id = :sheetId", nativeQuery = true)
    int takeNextBatch(@Param("sheetId") String sheetId);

    @Query(value = "SELECT last_settle_batch FROM sheet WHERE id = :sheetId", nativeQuery = true)
    int lastBatch(@Param("sheetId") String sheetId);

    @Modifying
    @Query(value = "INSERT INTO settle_batch (sheet_id, batch_key, batch_number) VALUES (:sheetId, :key, :number)",
            nativeQuery = true)
    int startBatch(@Param("sheetId") String sheetId, @Param("key") String key, @Param("number") int number);

    /** The number of the bulk settle batch the key names: the sheet's next one the first time the key is used (a
     *  batch is numbered when its first claim settles). Locks the sheet's row first, so two batches numbered at once
     *  are numbered one after the other. */
    default int batchFor(String sheetId, String key) {
        lockSheet(sheetId);
        Integer number = batchNumber(sheetId, key);
        if (number != null) {
            return number;
        }
        takeNextBatch(sheetId);
        int next = lastBatch(sheetId);
        startBatch(sheetId, key, next);
        return next;
    }
}

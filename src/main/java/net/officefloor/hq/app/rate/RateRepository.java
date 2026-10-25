package net.officefloor.hq.app.rate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RateRepository extends JpaRepository<Rate, Long> {

    /** The sheet's rates, by grade, then by from-date (a rate with none first). */
    List<Rate> findBySheetIdOrderByGradeAscFromDateAscIdAsc(String sheetId);

    List<Rate> findBySheetIdAndGrade(String sheetId, String grade);

    /** The grade's rate with this from-date (null: the one with none). */
    default Optional<Rate> findWithFrom(String sheetId, String grade, LocalDate from) {
        return findBySheetIdAndGrade(sheetId, grade).stream()
                .filter(r -> from == null ? r.getFromDate() == null : from.equals(r.getFromDate()))
                .findFirst();
    }

    /** The grade's rate in force on a date: the one whose from-date is the latest on or before it, a rate with
     *  no from-date applying from the beginning of time; empty when none applies, or when that one's to-date is before it. */
    default Optional<Rate> findInForce(String sheetId, String grade, LocalDate on) {
        return findBySheetIdAndGrade(sheetId, grade).stream()
                .filter(r -> r.getFromDate() == null || !r.getFromDate().isAfter(on))
                .max(Comparator.comparing(Rate::getFromDate, Comparator.nullsFirst(Comparator.naturalOrder())))
                .filter(r -> !r.endedBy(on));
    }

    /** How many sheets up a sheet's rates are looked for: its parent, its parent's parent, and one more. */
    int CHAIN_LEVELS = 3;

    /** The parent sheet a sheet names (its undated parent setting); empty (or a null value) when none. */
    @Query("select s.value from SheetSetting s where s.sheet.id = :sheetId and s.key = 'parent'")
    List<String> parentsOf(@Param("sheetId") String sheetId);

    /** The sheet a sheet was cloned from; empty (or a null value) when it is not a clone. */
    @Query("select s.clonedFrom from Sheet s where s.id = :sheetId")
    List<String> sourcesOf(@Param("sheetId") String sheetId);

    /** The sheet's id, then its parent's, then its parent's parent, up to three levels up; then, when the sheet was
     *  cloned, the chain of the sheet it was cloned from (its id, its parents, its own source's chain), and so on. */
    default List<String> chainOf(String sheetId) {
        List<String> chain = new ArrayList<>();
        for (String from = sheetId; from != null && !chain.contains(from); ) {
            chain.add(from);
            String id = from;
            for (int level = 0; level < CHAIN_LEVELS; level++) {
                String parent = parentsOf(id).stream().filter(p -> p != null).findFirst().orElse(null);
                if (parent == null || chain.contains(parent)) {
                    break;
                }
                chain.add(parent);
                id = parent;
            }
            from = sourcesOf(from).stream().filter(p -> p != null).findFirst().orElse(null);
        }
        return chain;
    }

    /** The sheet's rates set, in the order set. */
    @Query("select l from RateLog l where l.sheetId = :sheetId order by l.id")
    List<RateLog> logsOf(@Param("sheetId") String sheetId);

    /** The sheet's rates (as {@link #findBySheetIdOrderByGradeAscFromDateAscIdAsc}), then those of each sheet up its
     *  chain ({@link #chainOf}: its parents, then its clone source's) in turn, each with its log lines ({@link Rate#forClaim}): a grade with no rate in force on a date on a sheet takes the next one's. */
    default List<Rate> findInChain(String sheetId) {
        List<Rate> rates = new ArrayList<>();
        for (String id : chainOf(sheetId)) {
            List<Rate> own = findBySheetIdOrderByGradeAscFromDateAscIdAsc(id);
            List<RateLog> lines = logsOf(id);
            own.forEach(r -> r.setLayers(lines));
            rates.addAll(own);
        }
        return rates;
    }
}

package net.officefloor.hq.app.rate;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RateVersionRepository extends JpaRepository<RateVersion, Long> {

    List<RateVersion> findBySheetIdAndGradeOrderByIdDesc(String sheetId, String grade);

    /** The newest older version of the grade's rate with this from-date (null: the one with none); empty when none. */
    default Optional<RateVersion> findNewest(String sheetId, String grade, LocalDate from) {
        return findBySheetIdAndGradeOrderByIdDesc(sheetId, grade).stream()
                .filter(v -> Objects.equals(v.getFromDate(), from)).findFirst();
    }
}

package net.officefloor.hq.app.grade;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddedGradeRepository extends JpaRepository<AddedGrade, Long> {

    List<AddedGrade> findBySheetIdOrderById(String sheetId);

    boolean existsBySheetIdAndGrade(String sheetId, String grade);
}

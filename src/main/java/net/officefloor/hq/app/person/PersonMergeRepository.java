package net.officefloor.hq.app.person;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PersonMergeRepository extends JpaRepository<PersonMerge, Long> {

    /** The last merge into the person still standing. */
    Optional<PersonMerge> findFirstBySheetIdAndIntoIdOrderByIdDesc(String sheetId, Long intoId);

    /** Puts a merged person's row back as it stood, under their own id; their collections are added after. */
    @Modifying(flushAutomatically = true)
    @Query(value = "INSERT INTO person (id, sheet_id, name, grade, terms, tags, position, version, added_on,"
            + " origin_sheet, arrived_on, flag_reason, flag_until, approver) VALUES (:#{#p.id}, :#{#p.sheetId},"
            + " :#{#p.name}, :#{#p.grade}, :#{#p.terms}, :#{#p.tags}, NULL, :#{#p.version}, :#{#p.addedOn},"
            + " :#{#p.originSheet}, :#{#p.arrivedOn}, :#{#p.flagReason}, :#{#p.flagUntil}, :#{#p.approver})",
            nativeQuery = true)
    int restore(@Param("p") Person p);
}

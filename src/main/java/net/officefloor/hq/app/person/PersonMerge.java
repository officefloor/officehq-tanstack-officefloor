package net.officefloor.hq.app.person;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** A merge of a person into another, kept so the last merge into a person can be undone on the sheet's today it was
 *  made, by the user who made it: the merged person as they stood, the claims that moved from them, their own aliases
 *  and the aliases the merge gave the other. */
@Entity
@Table(name = "person_merge")
public class PersonMerge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sheet_id", nullable = false)
    private String sheetId;

    @Column(name = "into_id", nullable = false)
    private Long intoId;

    @Column(name = "person_id", nullable = false)
    private Long personId;

    @Column(name = "made_on", nullable = false)
    private LocalDate madeOn;

    @Column(name = "made_by", nullable = false)
    private String madeBy;

    /** The merged person's state as entity_history keeps it. */
    @Lob
    @Column(nullable = false)
    private String state;

    @Lob
    @Column(name = "claim_ids", nullable = false)
    private String claimIds;

    @Lob
    @Column(nullable = false)
    private String aliases;

    @Lob
    @Column(name = "alias_ids", nullable = false)
    private String aliasIds;

    protected PersonMerge() {
    }

    public PersonMerge(String sheetId, Long intoId, Long personId, LocalDate madeOn, String madeBy, String state,
            List<Long> claimIds, List<String> aliases, List<Long> aliasIds) {
        this.sheetId = sheetId;
        this.intoId = intoId;
        this.personId = personId;
        this.madeOn = madeOn;
        this.madeBy = madeBy;
        this.state = state;
        this.claimIds = join(claimIds);
        this.aliases = String.join("\n", aliases);
        this.aliasIds = join(aliasIds);
    }

    private static String join(List<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining("\n"));
    }

    private static List<Long> split(String ids) {
        return ids.isEmpty() ? List.of() : Arrays.stream(ids.split("\n")).map(Long::valueOf).toList();
    }

    public Long getId() {
        return id;
    }

    public String getSheetId() {
        return sheetId;
    }

    public Long getIntoId() {
        return intoId;
    }

    public Long getPersonId() {
        return personId;
    }

    public LocalDate getMadeOn() {
        return madeOn;
    }

    public String getMadeBy() {
        return madeBy;
    }

    public String getState() {
        return state;
    }

    /** The ids of the claims that moved from them, in the order they were added. */
    public List<Long> getClaimIds() {
        return split(claimIds);
    }

    /** Their own aliases, in the order they were added. */
    public List<String> getAliases() {
        return aliases.isEmpty() ? List.of() : List.of(aliases.split("\n"));
    }

    /** The ids of the aliases the merge gave the other: their name and their own aliases. */
    public List<Long> getAliasIds() {
        return split(aliasIds);
    }
}

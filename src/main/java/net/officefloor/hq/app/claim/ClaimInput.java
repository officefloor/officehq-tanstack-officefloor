package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * A validated claim body: a blank date already defaulted to the sheet's entry date (none set: its today); a negative amount (a correction) with
 * the number the claim it corrects shows (on the sheet, or within its period), which is null for any other claim; and its
 * external reference, trimmed (null when blank); and its tags, each trimmed and lower-cased, repeats dropped, sorted.
 */
public record ClaimInput(BigDecimal amount, LocalDate date, String note, String corrects, String ref, List<String> tags) {

    public ClaimInput(BigDecimal amount, LocalDate date, String note) {
        this(amount, date, note, null, null);
    }

    public ClaimInput(BigDecimal amount, LocalDate date, String note, String corrects, String ref) {
        this(amount, date, note, corrects, ref, List.of());
    }

    /** The tags of a comma separated text: each trimmed and lower-cased, blanks and repeats dropped, sorted. */
    public static List<String> tagsOf(String text) {
        return text == null ? List.of()
                : Arrays.stream(text.split(",")).map(t -> t.trim().toLowerCase(Locale.ROOT)).filter(t -> !t.isEmpty())
                        .distinct().sorted().toList();
    }

    /**
     * The audit fields of the claim as added: {@link #auditFields} then ref=<reference> when it has one, then
     * tags=<comma separated, sorted> when it has any.
     */
    public String addedAuditFields(String person) {
        return auditFields(person) + (ref == null ? "" : " ref=" + ref)
                + (tags.isEmpty() ? "" : " tags=" + String.join(",", tags));
    }

    /** Refuses a reference another claim on the sheet (any but the one excluded, null for none) already has, ignoring case. */
    public void checkRef(String sheet, Long excluded, Claims claims) throws InvalidClaimException {
        if (ref != null && claims.refUsed(sheet, ref, excluded)) {
            throw new InvalidClaimException("The reference " + ref + " is already used on the sheet");
        }
    }

    /** The audit fields of this claim for the named person: amount as the screen shows it, ISO date. */
    public String auditFields(String person) {
        return "person=" + person + " amount=" + amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString()
                + " date=" + date;
    }

    /**
     * Refuses a correction unless the claim it names is a SETTLED claim of the same person, and one larger than what
     * remains of that claim: its amount less the corrections already made against it (VOID and DELETED ones, and the
     * one excluded — the claim being edited, null for none — left out).
     */
    public void checkCorrects(String sheet, long person, Long excluded, Claims claims) throws InvalidClaimException {
        if (corrects == null) {
            return;
        }
        Claim corrected = claims.findByNumber(sheet, corrects);
        if (corrected == null || corrected.person() != person || corrected.status() != ClaimStatus.SETTLED) {
            throw new InvalidClaimException("A correction must name a SETTLED claim of the same person");
        }
        BigDecimal remains = new BigDecimal(corrected.amount()).subtract(claims.corrected(sheet, corrects, excluded));
        if (amount.negate().compareTo(remains) > 0) {
            throw new InvalidClaimException("A correction may not exceed what remains of the claim it corrects ("
                    + remains.setScale(2, RoundingMode.HALF_UP).toPlainString() + ")");
        }
    }
}

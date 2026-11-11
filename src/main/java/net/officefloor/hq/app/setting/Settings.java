package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

/**
 * A sheet's settings. The figure settings are dated ({@code dated}, by key, each value from a
 * from-date; a blank value is "none"): its clerk limit, the highest due a clerk may settle (none: no
 * limit), and its clerk limit per grade (none: the sheet's), its floor, the least a claim's live due may be, and its terms, the days after a claim's
 * date that it is due by, and its late percent, what an overdue claim's due gains per full 30 days
 * overdue, and its terms per grade (a grade with none uses the sheet's terms), and its cap (none:
 * no cap), the most a person's due-total may be, and its proof threshold (none: none), above which a
 * claim's due, on the claim's date, needs a proof before it is settled or approved, and its note
 * threshold (none: none), above which a claim's due, on the claim's date, needs a note before it is opened, and
 * its rounding mode (half-up or half-even; none: by the policy date), how a claim dated then rounds, and
 * its proof threshold per grade (none: the sheet's proof threshold), and its review threshold (none: none),
 * above which a claim's due, on the claim's date, sends it to REVIEW when it is opened. The value in force on a date is the one with the
 * latest from-date on or before it; before every from-date the starting value (floor 1.00, late
 * percent 0.00, terms 14; none for the others). A claim takes its floor and late percent on
 * its date, and its terms on the day it was made, and its clerk limit on its date; a person's cap is the one in force on the date of
 * their latest claim that counts. The policy settings (the rounding mode, the floor, the cap, the terms, the late percent
 * and the clerk limit) are versioned together: every change to one of them is a new policy version ({@code versions},
 * the from-date of each in the order made; null: none, counting always).
 * Not dated: its policy date (null: the sheet's today): a claim dated before it rounds half up, one
 * on or after it half to even, and its period numbering from (null: off): while the sheet's today
 * is on or after it, a claim opened is numbered within its period, and its permission matrix
 * ({@code perms}, by action, the roles that may take it as a comma separated list; blank or missing:
 * the standing rule, open, settle, void, close and changing the sheet's today ({@code today}) by clerk and
 * steward, reassign and reopen by steward),
 * and its grace days (null: off): when the sheet's today is changed, every open period whose to-date
 * plus the grace days is before the new today is closed, and its entry date (null: off): while it is
 * set, the date of a new claim left undated instead of the sheet's today, and its pot (null: none),
 * shared out as cuts among a balanced pool's members, and its fiscal year start month (null: 1), and its alert threshold (null: off),
 * and its escalation gap in days (null: off), and its review SLA in days (null: none): a REVIEW claim whose
 * review day plus it is before the sheet's today is late for review, and its proof kinds (null: none), a
 * comma separated list of kinds: while set, a claim that needs a proof needs at least one of each, and its
 * default grade (null: A), the grade a quick-added person and an imported line with a blank grade take, and
 * its score thresholds, mid and high (null: none): while both are set, each person shows a band by their score, and
 * its score log switch (false: off): while it is on, changing the sheet's today writes a SCORE line per person, and
 * its maximum of open claims per person (null: none): opening a claim is refused while its person already has that
 * many OPEN or PENDING claims.
 */
public record Settings(String sheetId, LocalDate today, Map<String, NavigableMap<LocalDate, String>> dated,
        LocalDate policyDate, LocalDate periodNumberingFrom, Map<String, String> perms, Integer graceDays,
        LocalDate entryDate, BigDecimal pot, Integer fiscalStartMonth, BigDecimal alertThreshold,
        Integer escalateAfter, Integer reviewSla, String proofKinds, String defaultGrade, Integer scoreMid,
        Integer scoreHigh, boolean scoreLog, Integer maxOpen, Integer pendingSla) {
    /** The grade a new person takes when none is given: the default grade, else A. */
    public String newPersonGrade() {
        return defaultGrade == null || defaultGrade.isBlank() ? "A" : defaultGrade;
    }

    /** The month, 1 to 12, a fiscal year starts in. */
    public int fiscalStart() {
        return fiscalStartMonth == null ? 1 : fiscalStartMonth;
    }

    /** The name, FY<yyyy>, of the fiscal year a date falls in: by the calendar year of its first day. */
    public String fiscalYearOf(LocalDate date) {
        return "FY" + (date.getMonthValue() >= fiscalStart() ? date.getYear() : date.getYear() - 1);
    }

    /** The grades every sheet has; a sheet may add more (see GradeRepository). */
    public static final List<String> GRADES = List.of("A", "B", "C");

    /** Whether a code is shaped as a grade: one to three capital letters. */
    public static boolean isGradeCode(String grade) {
        return grade != null && grade.matches("[A-Z]{1,3}");
    }

    public static final String CLERK_LIMIT = "clerk-limit";
    public static final String FLOOR = "floor";
    public static final String TERMS = "terms";
    public static final String LATE_PCT = "late-pct";
    public static final String CAP = "cap";
    public static final String PROOF_THRESHOLD = "proof-threshold";
    public static final String NOTE_THRESHOLD = "note-threshold";
    public static final String ALERT_THRESHOLD = "alert-threshold";
    public static final String ESCALATE_AFTER = "escalate-after";
    public static final String REVIEW_SLA = "review-sla";
    public static final String ROUNDING = "rounding";
    public static final String REVIEW_THRESHOLD = "review-threshold";
    public static final String PROOF_KINDS = "proof-kinds";
    public static final String DEFAULT_GRADE = "default-grade";
    public static final String SCORE_MID = "score-mid";
    public static final String SCORE_HIGH = "score-high";
    public static final String SCORE_LOG = "score-log";
    public static final String MAX_OPEN = "max-open";
    public static final String PENDING_SLA = "pending-sla";

    /** The policy settings, versioned together: every change to one of them is a new policy version. */
    public static final java.util.Set<String> POLICY_KEYS = java.util.Set.of(ROUNDING, FLOOR, CAP, TERMS, LATE_PCT, CLERK_LIMIT);

    /** The value the score log switch is recorded and shown as while it is on. */
    public static final String ON = "on";

    /** The kinds of proof the proof kinds list (none set: none). */
    public List<String> proofKindList() {
        if (proofKinds == null || proofKinds.isBlank()) return List.of();
        return java.util.Arrays.stream(proofKinds.split(",")).map(String::trim).filter((k) -> !k.isEmpty()).toList();
    }

    /** The rounding modes a sheet may set, as they are recorded. */
    public static final String HALF_UP = "half-up";
    public static final String HALF_EVEN = "half-even";

    /** The actions of the permission matrix, in the order they are shown and audited. */
    public static final List<String> ACTIONS = List.of("open", "settle", "void", "reassign", "close", "reopen", "today");

    /** The key of an action's entry in the permission matrix. */
    public static String permKey(String action) {
        return "perm-" + action;
    }

    /** The roles that may take an action by the standing rule. */
    public static List<String> standingRoles(String action) {
        return "reassign".equals(action) || "reopen".equals(action) || "lock".equals(action) ? List.of("steward") : List.of("clerk", "steward");
    }

    /** The roles that may take an action: its entry in the permission matrix, else the standing rule. */
    public List<String> rolesFor(String action) {
        String entry = perms == null ? null : perms.get(action);
        if (entry == null || entry.isBlank()) return standingRoles(action);
        return java.util.Arrays.stream(entry.split(",")).map(String::trim).filter((r) -> !r.isEmpty()).toList();
    }

    /** Whether a role may take an action. */
    public boolean allows(String action, String role) {
        if (net.officefloor.hq.app.role.Role.VIEWER.equals(role)) return false; // a viewer changes nothing
        if (net.officefloor.hq.app.role.Role.RESTRICTED.equals(role)) role = net.officefloor.hq.app.role.Role.CLERK; // acts as a clerk
        return rolesFor(action).contains(role);
    }

    /** The key of a grade's own terms. */
    public static String gradeTermsKey(String grade) {
        return "terms-" + grade;
    }

    /** The key of a grade's grace days. */
    public static String gradeGraceKey(String grade) {
        return "grace-" + grade;
    }

    /** The key of a grade's own proof threshold. */
    public static String gradeProofThresholdKey(String grade) {
        return "proof-threshold-" + grade;
    }

    /** A grade's own proof threshold in force on a date, or null when it has none. */
    public BigDecimal gradeProofThresholdOn(String grade, LocalDate date) {
        return figureOn(gradeProofThresholdKey(grade), date);
    }

    /** A grade's grace days in force on a date, or null when it has none. */
    public Integer gradeGraceOn(String grade, LocalDate date) {
        String value = valueOn(gradeGraceKey(grade), date);
        return value == null || value.isEmpty() ? null : Integer.valueOf(value);
    }

    /**
     * The day a claim's overdue count starts from: its due-by plus the grace days of its grade (the
     * one in force on its date) in force on its date, or its due-by when that grade has none. A
     * claim is overdue when this is before the sheet's today, and its late charge's 30-day
     * boundaries are counted from it.
     */
    public LocalDate overdueFrom(LocalDate dueBy, String grade, LocalDate date) {
        Integer grace = grade == null ? null : gradeGraceOn(grade, date);
        return grace == null ? dueBy : dueBy.plusDays(grace);
    }

    /** A sheet's floor until it is set. */
    public static final BigDecimal DEFAULT_FLOOR = new BigDecimal("1.00");

    /** A sheet's terms until they are set. */
    public static final int DEFAULT_TERMS = 14;

    /** A sheet's late percent until it is set. */
    public static final BigDecimal DEFAULT_LATE_PCT = new BigDecimal("0.00");

    /** The value of a key in force on a date: null when none is recorded on or before it, '' for none. */
    public String valueOn(String key, LocalDate date) {
        NavigableMap<LocalDate, String> values = dated.get(key);
        if (values == null || date == null) return null;
        Map.Entry<LocalDate, String> entry = values.floorEntry(date);
        return entry == null ? null : entry.getValue();
    }

    private BigDecimal figureOn(String key, LocalDate date) {
        String value = valueOn(key, date);
        return value == null || value.isEmpty() ? null : new BigDecimal(value).setScale(2);
    }

    /** The floor in force on a date. */
    public BigDecimal floorOn(LocalDate date) {
        BigDecimal floor = figureOn(FLOOR, date);
        return floor != null ? floor : DEFAULT_FLOOR;
    }

    /** The late percent in force on a date. */
    public BigDecimal latePctOn(LocalDate date) {
        BigDecimal pct = figureOn(LATE_PCT, date);
        return pct != null ? pct : DEFAULT_LATE_PCT;
    }

    /** The sheet's terms in force on a date. */
    public int termsOn(LocalDate date) {
        String value = valueOn(TERMS, date);
        return value == null || value.isEmpty() ? DEFAULT_TERMS : Integer.parseInt(value);
    }

    /** A grade's own terms in force on a date, or null when it takes the sheet's. */
    public Integer gradeTermsOn(String grade, LocalDate date) {
        String value = valueOn(gradeTermsKey(grade), date);
        return value == null || value.isEmpty() ? null : Integer.valueOf(value);
    }

    /** The terms of a person's claim made on a date (the sheet's today then): their own in force then, else their grade's then, else the sheet's. */
    public int termsFor(net.officefloor.hq.app.person.Person person, LocalDate date) {
        Integer own = person.termsOn(date);
        return own != null ? own : termsFor(person.gradeOn(date), date);
    }

    /** The terms of a grade on a date: its own, else the sheet's. */
    public int termsFor(String grade, LocalDate date) {
        Integer own = gradeTermsOn(grade, date);
        return own != null ? own : termsOn(date);
    }

    /** The key of a grade's own clerk limit. */
    public static String gradeClerkLimitKey(String grade) {
        return "clerk-limit-" + grade;
    }

    /** A grade's own clerk limit in force on a date, or null when it has none. */
    public BigDecimal gradeClerkLimitOn(String grade, LocalDate date) {
        return figureOn(gradeClerkLimitKey(grade), date);
    }

    /** The clerk limit of a grade in force on a date: the grade's own when it has one, else the sheet's (null: no limit). */
    public BigDecimal clerkLimitFor(String grade, LocalDate date) {
        BigDecimal own = grade == null ? null : gradeClerkLimitOn(grade, date);
        return own != null ? own : clerkLimitOn(date);
    }

    /** The clerk limit in force on a date (null: no limit). */
    public BigDecimal clerkLimitOn(LocalDate date) {
        return figureOn(CLERK_LIMIT, date);
    }

    /** The proof threshold in force on a date (null: none). */
    public BigDecimal proofThresholdOn(LocalDate date) {
        return figureOn(PROOF_THRESHOLD, date);
    }

    /**
     * The proof threshold of a claim of a grade dated {@code date}: the grade's own in force on that
     * date when it has one, else the sheet's (null: none).
     */
    public BigDecimal proofThresholdFor(String grade, LocalDate date) {
        BigDecimal own = grade == null ? null : gradeProofThresholdOn(grade, date);
        return own != null ? own : proofThresholdOn(date);
    }

    /**
     * Whether a claim of a grade dated {@code date} with a due (null: n/a) needs a proof before it is
     * settled or approved: its due is above its proof threshold and it has no proofs, or, while the
     * proof kinds are set, none of a kind they list. A due of n/a, or no threshold, needs none.
     */
    public boolean needsProof(BigDecimal due, String grade, LocalDate date, net.officefloor.hq.app.claim.Claim claim) {
        BigDecimal threshold = proofThresholdFor(grade, date);
        if (threshold == null || due == null || due.compareTo(threshold) <= 0) return false;
        if (claim.getProofs() == 0) return true;
        for (String kind : proofKindList()) {
            if (claim.proofsOf(kind) == 0) return true;
        }
        return false;
    }

    /** The note threshold in force on a date (null: none). */
    public BigDecimal noteThresholdOn(LocalDate date) {
        return figureOn(NOTE_THRESHOLD, date);
    }

    /**
     * Whether a claim dated {@code date} with a due (null: n/a) and a note needs a note before it is
     * opened: its due is above the note threshold in force on its date and its note is empty. A due
     * of n/a, or no threshold, needs none.
     */
    public boolean needsNote(BigDecimal due, LocalDate date, String note) {
        BigDecimal threshold = noteThresholdOn(date);
        return (note == null || note.isBlank()) && threshold != null && due != null && due.compareTo(threshold) > 0;
    }

    /** The review threshold in force on a date (null: none). */
    public BigDecimal reviewThresholdOn(LocalDate date) {
        return figureOn(REVIEW_THRESHOLD, date);
    }

    /**
     * Whether a claim dated {@code date} with a due (null: n/a) goes to REVIEW when it is opened:
     * its due is above the review threshold in force on its date. A due of n/a, or no threshold,
     * goes straight to OPEN.
     */
    public boolean needsReview(BigDecimal due, LocalDate date) {
        BigDecimal threshold = reviewThresholdOn(date);
        return threshold != null && due != null && due.compareTo(threshold) > 0;
    }

    /** The cap in force on a date (null: none). */
    public BigDecimal capOn(LocalDate date) {
        return figureOn(CAP, date);
    }

    /** The rounding mode in force on a date (null: none). */
    public RoundingMode roundingOn(LocalDate date) {
        String value = valueOn(ROUNDING, date);
        if (HALF_UP.equals(value)) return RoundingMode.HALF_UP;
        if (HALF_EVEN.equals(value)) return RoundingMode.HALF_EVEN;
        return null;
    }

    /**
     * How every figure of a claim that rounds by {@code date} is rounded: by the rounding mode in
     * force on that date when there is one, else half up before the policy date ({@code today} when
     * it is blank), half to even on or after it.
     */
    public RoundingMode rounding(LocalDate date, LocalDate today) {
        RoundingMode mode = roundingOn(date);
        if (mode != null) return mode;
        LocalDate policy = policyDate != null ? policyDate : today;
        return date.isBefore(policy) ? RoundingMode.HALF_UP : RoundingMode.HALF_EVEN;
    }

    /** The date of a new claim left undated: the entry date while it is set, else the sheet's today. */
    public LocalDate entryDay(LocalDate today) {
        return entryDate != null ? entryDate : today;
    }

    /** Whether a period ending {@code to} is past its grace days on {@code today}: never while they are off. */
    public boolean pastGrace(LocalDate to, LocalDate today) {
        return graceDays != null && to.plusDays(graceDays).isBefore(today);
    }

    /** Whether a claim opened on {@code today} is numbered within its period. */
    public boolean numbersByPeriod(LocalDate today) {
        return periodNumberingFrom != null && !today.isBefore(periodNumberingFrom);
    }

    /**
     * A person's due-total (null: n/a) held to the cap in force on {@code date}, the date of their latest claim
     * that counts (null: none, no cap), less their cut total (not below 0.00): the cap covers their due-total and
     * their cut total together. A due-total of n/a is not capped.
     */
    public BigDecimal capped(BigDecimal dueTotal, BigDecimal cutTotal, LocalDate date) {
        BigDecimal cap = capOn(date);
        if (cap == null || dueTotal == null) return dueTotal;
        BigDecimal room = cap.subtract(cutTotal).max(BigDecimal.ZERO).setScale(2);
        return dueTotal.compareTo(room) <= 0 ? dueTotal : room;
    }

    /**
     * Whether a due (null: n/a) of a claim of a grade is within the clerk limit in force on the claim's
     * {@code date}: the grade's own when it has one, else the sheet's. A due of n/a is within any limit.
     */
    public boolean withinClerkLimit(BigDecimal due, String grade, LocalDate date) {
        BigDecimal clerkLimit = clerkLimitFor(grade, date);
        return clerkLimit == null || due == null || due.compareTo(clerkLimit) <= 0;
    }
}

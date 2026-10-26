import { ClaimCells } from '../../slots/defs/claimRow';
import { useSettings } from '../access/useAccess';
import { useSheetToday } from '../today/today.slot';

/** Whether a claim is review-late: REVIEW, and its review day (the sheet's today when it went to REVIEW) plus the
 *  sheet's review SLA days before the sheet's today; never while the SLA is blank. */
export function useIsReviewLate(sheetId: string): (claim: { status: string; reviewDay?: string | null }) => boolean {
  const sla = useSettings(sheetId).data?.find((s) => s.key === 'review-sla')?.value;
  const today = useSheetToday(sheetId).data?.today;
  return (claim) => {
    if (claim.status !== 'REVIEW' || !claim.reviewDay || sla == null || sla === '' || today === undefined) return false;
    return plusDays(claim.reviewDay, Number(sla)) < today;
  };
}

/** The date (yyyy-mm-dd) the days after another. */
function plusDays(date: string, days: number): string {
  const d = new Date(`${date}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}

// Whether the REVIEW claim has waited past the sheet's review SLA.
function ClaimReviewLate({ sheetId, claim }: { sheetId: string; user: string; personId: number; claim: { status: string; reviewDay?: string | null } }) {
  const isReviewLate = useIsReviewLate(sheetId);
  return <>Review late <span data-testid="claim-review-late">{isReviewLate(claim) ? 'yes' : 'no'}</span>{' '}</>;
}

export const contributions = [
  ClaimCells.fill({ order: 13, Component: ClaimReviewLate }),
];

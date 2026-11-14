import { useSheetToday } from '../session/sheetToday';
import { useSettings } from '../settings/settings';
import type { Claim } from '../claims/claims';
import { reviewLate } from './reviewLate';

// Whether a REVIEW claim has stayed in review past the sheet's review SLA.
export function ClaimReviewLate({ claim }: { claim: Claim }) {
  const today = useSheetToday();
  const sla = useSettings().data?.reviewSla ?? '';
  return <span data-testid="claim-review-late">{reviewLate(claim, sla, today) ? 'yes' : 'no'}</span>;
}

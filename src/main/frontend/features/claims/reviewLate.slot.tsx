import { ClaimCells } from '../../slots/defs/claimColumns';
import { useReviewSla } from '../settings/settingsQuery';
import { useSheetToday } from '../sheet/useSheetToday';
import { isReviewLate, type Claim } from './claimsQuery';

/** Whether the REVIEW claim is past the sheet's review SLA: its review day plus the SLA's days is before the sheet's today. */
function ClaimReviewLate({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  const today = useSheetToday(sheetId);
  const sla = useReviewSla(sheetId);
  return <td data-testid="claim-review-late">{isReviewLate(claim, today, sla) ? 'yes' : 'no'}</td>;
}

export const contribution = ClaimCells.fill({ order: 17, Component: ClaimReviewLate });

import type { Claim } from '../claims/claims';
import { useSheetToday } from '../session/sheetToday';
import { overdue } from './overdue';

// Whether a claim is overdue: OPEN (or PENDING), with its due-by before the sheet's today.
export function ClaimOverdue({ claim }: { claim: Claim }) {
  const today = useSheetToday();
  return <span data-testid="claim-overdue">{overdue(claim, today) ? 'yes' : 'no'}</span>;
}

import type { Claim } from '../claims/claims';
import { useSheetToday } from '../session/sheetToday';
import { useTerms } from '../settings/settings';
import { overdue } from './overdue';

// Whether a claim is overdue: OPEN, with its due-by before the sheet's today.
export function ClaimOverdue({ claim }: { claim: Claim }) {
  const terms = useTerms();
  const today = useSheetToday();
  return <span data-testid="claim-overdue">{overdue(claim, terms, today) ? 'yes' : 'no'}</span>;
}

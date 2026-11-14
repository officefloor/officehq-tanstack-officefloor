import { useSheetToday } from '../session/sheetToday';
import { useSettings } from '../settings/settings';
import type { Claim } from '../claims/claims';
import { pendingLate } from './pendingLate';

// Whether a PENDING claim has stayed pending past the sheet's pending SLA.
export function ClaimPendingLate({ claim }: { claim: Claim }) {
  const today = useSheetToday();
  const sla = useSettings().data?.pendingSla ?? '';
  return <span data-testid="claim-pending-late">{pendingLate(claim, sla, today) ? 'yes' : 'no'}</span>;
}

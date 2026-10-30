import { ClaimCells } from '../../slots/defs/claimColumns';
import { usePendingSla } from '../settings/settingsQuery';
import { useSheetToday } from '../sheet/useSheetToday';
import { isPendingLate, type Claim } from './claimsQuery';

/** Whether the PENDING claim is past the sheet's pending SLA: its pending day plus the SLA's days is before the sheet's today. */
function ClaimPendingLate({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  const today = useSheetToday(sheetId);
  const sla = usePendingSla(sheetId);
  return <td data-testid="claim-pending-late">{isPendingLate(claim, today, sla) ? 'yes' : 'no'}</td>;
}

export const contribution = ClaimCells.fill({ order: 17.5, Component: ClaimPendingLate });

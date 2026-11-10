import { SheetBody } from '../../slots/defs/sheetBody';
import { liveHolds, usePeople } from '../people/people';
import { useSheetToday } from '../sheet/sheet';
import { sumAmounts, useClaims } from './claims';

// The sheet's claimed-total: everyone's claims added up.
function ClaimedTotal({ sheetId }: { sheetId: string }) {
  const claims = useClaims(sheetId);
  const today = useSheetToday(sheetId);
  const holds = new Map((usePeople(sheetId).data ?? []).map((p) => [p.id, liveHolds(p, today)]));
  return (
    <p>
      Claimed in all: <strong data-testid="rollup-claimed-total">{sumAmounts(claims.data ?? [], (id) => holds.get(id) ?? [], today)}</strong>
    </p>
  );
}

export const contribution = SheetBody.fill({ order: 10, Component: ClaimedTotal });

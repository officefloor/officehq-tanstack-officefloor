import { SheetBody } from '../../slots/defs/sheet';
import { usePeople } from '../people/people';
import { sheetTagSum, useRates } from '../rates/rates';
import { useFloor, useLate } from '../settings/settings';
import { useSheetToday } from '../sheet/sheet';
import { useClaims, useTagsUsed } from './claims';

/** The sheet's due-total per tag, for every tag ever used on it (on a claim or a person): the dues of the claims that count which
 *  carry it or whose person carries it
 *  (a claim with two tags counts in both; see sheetTagSum). */
function TagTotals({ sheetId }: { sheetId: string; user: string }) {
  const tags = useTagsUsed(sheetId).data ?? [];
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  if (tags.length === 0) return null;
  return (
    <p>
      Due by tag{' '}
      {[...tags].sort().map((tag) => (
        <span key={tag}>
          {tag} <strong data-testid={`tag-${tag}-due-total`}>{sheetTagSum(people, claims, rates, today, floor, late, tag)}</strong>{' '}
        </span>
      ))}
    </p>
  );
}

export const contribution = SheetBody.fill({ order: 21.6, Component: TagTotals });

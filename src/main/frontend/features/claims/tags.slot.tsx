import { HomeMain } from '../../slots/defs/homeMain';
import { usePeople } from '../people/people';
import { useCurrentSheet } from '../../session/session';
import { useSheetToday } from '../sheet/sheet';
import { floorOf, useSettings } from '../settings/settings';
import { lateOf } from '../overdue/overdue';
import { dueTotal, personTagged, useRates } from '../rates/rates';
import { live, useClaims, useTags } from './claims';

// For every tag ever used on the sheet, its due-total: the dues of the claims that count which carry it,
// everyone's added up, not capped or trimmed (a claim with two tags counts in both). A claim counts for a tag
// when it or its person carries it, once.
function TagTotals() {
  const [sheet] = useCurrentSheet();
  const tags = useTags(sheet);
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const settings = useSettings(sheet).data;
  const floor = floorOf(settings);
  const total = (tag: string) =>
    dueTotal(
      (people.data ?? []).map((p) =>
        personTagged(live((claims.data ?? []).filter((c) => c.personId === p.id), [p]), tag, p.tags, rates.data, p.grades, today, floor,
          lateOf(settings, today, [p])),
      ),
    );
  if ((tags.data ?? []).length === 0) return null;
  return (
    <p>
      Due by tag{' '}
      {(tags.data ?? []).map((tag) => (
        <span key={tag}>
          {tag} <strong data-testid={`tag-${tag}-due-total`}>{total(tag)}</strong>{' '}
        </span>
      ))}
    </p>
  );
}

export const contribution = HomeMain.fill({ order: 36.5, Component: TagTotals });

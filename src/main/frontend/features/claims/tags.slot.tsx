import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser, useSheetInfo, useSheetToday } from '../session/session';
import { gradeOn, usePeople } from '../people/people';
import { useSettings } from '../roles/roles';
import { chargedOf, sumDue, useRates } from '../rates/rates';
import { countsLive, holdsOf, useClaims } from './claims';

// Per tag ever used on the sheet (by any of its claims, including those since transferred away) or carried by one of its people now, sorted, its due-total: the
// dues (with any late charge, as shown now, each at the grade its person had on its date) of the claims that count
// live and carry it now, themselves or through their person, added up once each, skipping n/a. A claim with two tags
// counts in both.
function TagTotals() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople();
  const claims = useClaims();
  const rates = useRates();
  const settings = useSettings();
  const today = useSheetToday();
  const sheetInfo = useSheetInfo();
  if (!user || !sheet) return null;
  const allClaims = claims.data ?? [];
  const personOf = new Map((people.data ?? []).map((p) => [p.id, p]));
  const held = holdsOf(people.data);
  const tags = [...new Set([...allClaims.flatMap((c) => c.tagsUsed ?? []), ...(sheetInfo.data?.departedTags ?? []),
    ...(people.data ?? []).flatMap((p) => p.tags ?? [])])].sort();
  const carries = (c: (typeof allClaims)[number], t: string) =>
    (c.tags ?? []).includes(t) || (personOf.get(c.personId)?.tags ?? []).includes(t);
  return (
    <section data-testid="tag-totals">
      <h2>Tags</h2>
      <table>
        <thead>
          <tr>
            <th>Tag</th>
            <th>Due</th>
          </tr>
        </thead>
        <tbody>
          {tags.map((t) => (
            <tr key={t}>
              <td>{t}</td>
              <td data-testid={`tag-${t}-due-total`}>
                {sumDue(allClaims
                  .filter((c) => carries(c, t) && personOf.has(c.personId) && countsLive(c, held, today))
                  .map((c) => chargedOf(c, rates.data, gradeOn(personOf.get(c.personId)!, c.date), settings.data, today)))}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 34, Component: TagTotals });

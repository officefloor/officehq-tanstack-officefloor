import { useQueryClient } from '@tanstack/react-query';
import { figureToday } from '../../api/http';
import { HomeMain } from '../../slots/defs/homeMain';
import { sheetQueryKey, useCurrentSheet, useCurrentUser, type SheetInfo } from '../session/session';
import { gradeOn, peoplePath, peopleQueryKey, type Person } from '../people/people';
import { csvValue, readFresh, sheetInfoPath } from '../people/export.slot';
import { chargedOf, ratesPath, ratesQueryKey, type Rate } from '../rates/rates';
import { claimsPath, claimsQueryKey, countsLive, holdsOf, type Claim } from '../claims/claims';
import { policyVersionOn, settingsPath, settingsQueryKey, type Settings } from './roles';

/** An amount ("12.50") in whole cents, so sums are exact. */
const cents = (amount: string) => Math.round(Number(amount) * 100);

// The policy export button: downloads the policy versions as CSV — version, from (its from-date, or "start"), key,
// value, claims (the count of claims, DELETED ones left out, whose policy version it is, as the policy lines show)
// and due-total (the due of those of them that count toward the live totals, as their rows show it, added up) — one
// line per version, oldest first. Quoting and line ends as the people export; its inputs are re-read from the store
// when pressed, as the people export's.
function ExportPolicy() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const queryClient = useQueryClient();
  if (!user || !sheet) return null;
  const download = async () => {
    const [info, people, claims, rates, settings] = await Promise.all([
      readFresh<SheetInfo>(queryClient, sheetQueryKey(sheet), sheetInfoPath(sheet)),
      readFresh<Person[]>(queryClient, peopleQueryKey(sheet), peoplePath(sheet)),
      readFresh<Claim[]>(queryClient, claimsQueryKey(sheet), claimsPath(sheet)),
      readFresh<Rate[]>(queryClient, ratesQueryKey(sheet), ratesPath(sheet)),
      readFresh<Settings>(queryClient, settingsQueryKey(sheet), settingsPath(sheet)),
    ]);
    const today = figureToday(info.today);
    const holds = holdsOf(people);
    const byId = new Map(people.map((p) => [p.id, p]));
    const counted = claims.filter((c) => c.status !== 'DELETED');
    const lines = [['version', 'from', 'key', 'value', 'claims', 'due-total']];
    (settings.policyLog ?? []).forEach((v, i) => {
      const mine = counted.filter((c) => policyVersionOn(settings, c.date) === i + 1);
      const due = mine
        .filter((c) => countsLive(c, holds, today))
        .reduce((n, c) => {
          const person = byId.get(c.personId);
          const charged = person ? chargedOf(c, rates, gradeOn(person, c.date), settings, today) : 'n/a';
          return charged === 'n/a' ? n : n + cents(charged);
        }, 0);
      lines.push([String(i + 1), v.from ?? 'start', v.key, v.value ?? '', String(mine.length), (due / 100).toFixed(2)]);
    });
    const csv = lines.map((l) => l.map(csvValue).join(',') + '\n').join('');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `policy-${sheet}.csv`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  };
  return (
    <button type="button" data-testid="export-policy" onClick={() => { void download(); }}>
      Export policy versions (CSV)
    </button>
  );
}

export const contribution = HomeMain.fill({ order: 34.75, Component: ExportPolicy });

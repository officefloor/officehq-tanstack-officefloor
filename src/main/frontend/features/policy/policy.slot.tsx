import { useState } from 'react';
import { useIsFetching } from '@tanstack/react-query';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { claimDueCents, useClaims, useLate } from '../claims/claims';
import { gradeOn, usePeople } from '../people/people';
import { claimRateOf, showDue, useRates } from '../rates/rates';
import { useFloorCents, usePolicyVersionOn, useSettings } from '../settings/settings';

// A CSV value: wrapped in double quotes (any quote inside doubled) when it holds a comma, a quote or a line break.
const csvValue = (v: string) => (/[",\r\n]/.test(v) ? `"${v.replace(/"/g, '""')}"` : v);

// The sheet's policy versions, oldest first, one line each: its number, its from-date (or "start" when it has none),
// the key and value its change set, and how many claims have it as their policy version (the version in force on the
// claim's date, as each claim row shows it). A button downloads them as CSV, each version with its claims' count and
// the due of those claims added up.
function Policy({ sheet }: { sheet: string; user: string }) {
  const [shown, setShown] = useState(false);
  const { data: settings } = useSettings(sheet);
  const { data: claims } = useClaims(sheet);
  const { data: people = [] } = usePeople(sheet);
  const { data: rates = [] } = useRates(sheet);
  const floor = useFloorCents(sheet);
  const late = useLate(sheet);
  const policyOn = usePolicyVersionOn(sheet);

  // While the screen's reads are in flight the figures are not yet the ones shown, so the export waits for them.
  const reading = useIsFetching() > 0;

  const counts = new Map<number, number>();
  const dueCents = new Map<number, bigint>();
  for (const claim of claims ?? []) {
    const version = policyOn(claim.date);
    counts.set(version, (counts.get(version) ?? 0) + 1);
    const person = people.find((p) => p.id === claim.person);
    // A claim with no due figure (no rate for its grade) adds nothing.
    const due = person && claimDueCents(claim, claimRateOf(rates, gradeOn(person, claim.date), claim), floor, late);
    dueCents.set(version, (dueCents.get(version) ?? 0n) + (due ?? 0n));
  }

  const download = () => {
    const lines = [
      'version,from,key,value,claims,due-total',
      ...(settings?.policyChanges ?? []).map((v, i) =>
        [
          String(i + 1),
          v.from === '' ? 'start' : v.from,
          v.key,
          v.value,
          String(counts.get(i + 1) ?? 0),
          showDue(dueCents.get(i + 1) ?? 0n),
        ]
          .map(csvValue)
          .join(','),
      ),
    ];
    const url = URL.createObjectURL(new Blob([lines.map((l) => `${l}\n`).join('')], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `policy-${sheet}.csv`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  };

  return (
    <section>
      <h2>Policy versions</h2>
      <button type="button" data-testid="policy-show" onClick={() => setShown(true)}>
        Show
      </button>{' '}
      <button type="button" data-testid="export-policy" disabled={reading} onClick={download}>
        Export policy (CSV)
      </button>
      {shown && (
        <ul>
          {(settings?.policyChanges ?? []).map((v, i) => (
            <li key={i} data-testid="policy-line">
              <code>{`${i + 1} ${v.from === '' ? 'start' : v.from} ${v.key}=${v.value} ${counts.get(i + 1) ?? 0}`}</code>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 30, Component: Policy });

import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { getJsonLive } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser, type SheetInfo } from '../session/session';
import { claimsPath, type Claim } from '../claims/claims';
import { personDue } from '../rates/due.slot';
import { ratesPath, type Rate } from '../rates/rates';
import { settingsPath, type Settings } from '../roles/roles';
import { peoplePath, type Person } from './people';

// The snapshot comparison: one line per person, in list order — `<name> <then> <now>`: `then` their due-total in
// the SNAPSHOT written when the sheet's today was changed away from the from-date (blank: the sheet's today), `now`
// likewise for the to-date, or their due-total now when the to-date is blank. A figure with no snapshot is empty,
// and each line is trimmed. Read from the store as it is now.

/** A person's due-total in the latest snapshot taken on a date, or '' with none. */
function snapshotOn(person: Person, date: string): string {
  const found = person.snapshots.filter((s) => s.date === date);
  return found.length === 0 ? '' : found[found.length - 1].dueTotal;
}

async function compareSnapshots(sheetId: string, from: string, to: string): Promise<string[]> {
  const [info, people, claims, rates, settings] = await Promise.all([
    getJsonLive<SheetInfo>(`/api/sheets/${encodeURIComponent(sheetId)}`),
    getJsonLive<Person[]>(peoplePath(sheetId)),
    getJsonLive<Claim[]>(claimsPath(sheetId)),
    getJsonLive<Rate[]>(ratesPath(sheetId)),
    getJsonLive<Settings>(settingsPath(sheetId)),
  ]);
  const fromDate = from.trim() === '' ? info.today : from.trim();
  return people.map((p) => {
    const then = snapshotOn(p, fromDate);
    const now = to.trim() === '' ? personDue(p, claims, rates, info.today, settings, people) : snapshotOn(p, to.trim());
    return [p.name, then, now].filter((s) => s !== '').join(' ');
  });
}

function SnapshotCompare() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const compare = useMutation({
    mutationFn: (dates: { from: string; to: string }) => compareSnapshots(sheet ?? '', dates.from, dates.to),
  });
  const submit = useSubmitOnce(compare);
  if (!user || !sheet) return null;

  return (
    <div>
      <h2>Compare snapshots</h2>
      <input data-testid="snapshot-from" placeholder="from YYYY-MM-DD" value={from}
        onChange={(e) => { setFrom(e.target.value); compare.reset(); }} />
      <input data-testid="snapshot-to" placeholder="to YYYY-MM-DD (blank: now)" value={to}
        onChange={(e) => { setTo(e.target.value); compare.reset(); }} />
      <button type="button" data-testid="snapshot-compare" disabled={compare.isPending}
        onClick={() => submit({ from, to })}>
        Compare
      </button>
      {compare.isSuccess && (
        <ul>
          {compare.data.map((line, i) => (
            <li key={i} data-testid="snapshot-line">{line}</li>
          ))}
        </ul>
      )}
      {compare.isError && <span data-testid="snapshot-compare-error">The snapshots could not be compared.</span>}
    </div>
  );
}

export const contribution = HomeMain.fill({ order: 19.3, Component: SnapshotCompare });

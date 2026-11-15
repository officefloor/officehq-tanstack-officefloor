import { useEffect, useRef, useState } from 'react';
import { useIsFetching, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { claimsKey, listClaims } from '../claims/api';
import { GRADES, listPeople, peopleKey } from '../people/api';
import { listRates, ratesKey } from '../rates/api';
import { listSettings, settingsKey } from '../settings/api';
import { sheetKey, useSheetToday } from '../sheet/api';
import { useGradeDues } from './Summary';

/** Whether a sheet with the id was ever loaded; asking never makes it. */
function sheetExists(id: string): Promise<boolean> {
  return getJson<boolean>(`/api/sheets/${encodeURIComponent(id)}/exists`, true);
}

/** Works out another sheet's summary due-total per grade and hands it up once all of it is read; shows nothing. */
function OtherDues({ sheetId, user, report }: { sheetId: string; user: string; report: (dues: Record<string, string>) => void }) {
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const rates = useQuery({ queryKey: ratesKey(sheetId), queryFn: () => listRates(sheetId) });
  const settings = useQuery({ queryKey: settingsKey(sheetId), queryFn: () => listSettings(sheetId) });
  const today = useSheetToday(sheetId, user);
  const dues = useGradeDues(sheetId, user);
  const fetching = useIsFetching({ queryKey: sheetKey(sheetId) });
  const ready = people.isSuccess && claims.isSuccess && rates.isSuccess && settings.isSuccess && today !== '' && fetching === 0;
  const seen = JSON.stringify(dues);
  useEffect(() => {
    if (ready) report(dues);
  }, [ready, seen]);
  return null;
}

/** A sheet id entry and a button that compare this sheet with another: one line per grade,
 *  `<grade> <this sheet's summary due-total> <the other sheet's>` (0.00 for a grade the other sheet has
 *  no one on); a sheet never loaded gives no lines. The other sheet is read afresh, and never made. */
export function CompareSheet({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [other, setOther] = useState('');
  // The comparison running or done: the other sheet (null: it does not exist) and its dues once read.
  const [run, setRun] = useState<{ n: number; id: string | null } | null>(null);
  const [theirs, setTheirs] = useState<Record<string, string> | null>(null);
  const runs = useRef(0);
  const mine = useGradeDues(sheetId, user);

  useEffect(() => {
    runs.current++;
    setRun(null);
    setTheirs(null);
  }, [sheetId]);

  const compare = async () => {
    const n = ++runs.current;
    const id = other.trim();
    setRun(null);
    setTheirs(null);
    const exists = id !== '' && await sheetExists(id).catch(() => false);
    if (runs.current !== n) return;
    // Read afresh; this sheet's own reads are already live on the screen.
    if (exists && id !== sheetId) queryClient.removeQueries({ queryKey: sheetKey(id) });
    setRun({ n, id: exists ? id : null });
  };

  const lines = run !== null && run.id !== null && theirs !== null
    ? GRADES.map((g) => `${g} ${mine[g]} ${theirs[g] ?? '0.00'}`)
    : [];

  return (
    <div>
      {run?.id != null && (
        <OtherDues key={run.n} sheetId={run.id} user={user} report={(d) => {
          if (runs.current === run.n) setTheirs(d);
        }} />
      )}
      <label>
        Compare with sheet{' '}
        <input data-testid="compare-sheet" value={other} onChange={(e) => setOther(e.target.value)} />
      </label>{' '}
      <button type="button" data-testid="compare" onClick={() => void compare()}>
        Compare
      </button>
      <ul>
        {lines.map((l) => (
          <li key={l} data-testid="compare-line">{l}</li>
        ))}
      </ul>
    </div>
  );
}

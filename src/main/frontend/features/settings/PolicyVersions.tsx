import { useEffect, useState } from 'react';
import { useIsFetching, useQuery, useQueryClient } from '@tanstack/react-query';
import { claimsKey, counts, listClaims } from '../claims/api';
import { downloadCsv } from '../people/ExportPeople';
import { gradeOn, listPeople, peopleKey } from '../people/api';
import { claimDue, formatCents, hundredths, listRates, ratesKey } from '../rates/api';
import { useSheetToday } from '../sheet/api';
import { listPolicyVersions, policyVersionsKey, settingsKey, useFloor, useLate, usePolicyVersions } from './api';

/** The sheet's policy versions, oldest first, once shown: each one's number, its from-date (start:
 *  always), the setting and value its change set, and how many claims go by it (the version in force on
 *  their date). */
export function PolicyVersions({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [shown, setShown] = useState(false);
  const versions = useQuery({ queryKey: policyVersionsKey(sheetId), queryFn: () => listPolicyVersions(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const policy = usePolicyVersions(sheetId);
  const count = (version: number) => (claims.data ?? []).filter((c) => policy.on(c.date) === version).length;

  return (
    <section>
      <h3>Policy versions</h3>
      <button type="button" data-testid="policy-show" onClick={() => {
        setShown(true);
        void queryClient.invalidateQueries({ queryKey: policyVersionsKey(sheetId) });
        void queryClient.invalidateQueries({ queryKey: claimsKey(sheetId), exact: true });
      }}>
        Show policy versions
      </button>{' '}
      <ExportPolicy sheetId={sheetId} user={user} />
      {shown && (
        <ul>
          {(versions.data ?? []).map((v) => (
            <li key={v.version} data-testid="policy-line">
              {v.version} {v.from ?? 'start'} {v.key}={v.value} {count(v.version)}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

/** The "export policy" button: downloads the sheet's policy versions, oldest first, as a CSV file, one line
 *  each `version,from,key,value,claims,due-total` (from blank: always): how many claims go by it (as its
 *  policy line counts them) and the due of those of them that count (see `counts`), added up. The sheet's
 *  versions, claims and the figures their dues go by are read afresh for it. */
function ExportPolicy({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  // The export: its figures being read afresh, or read and ready to download.
  const [run, setRun] = useState<'reading' | 'ready' | null>(null);
  const versions = useQuery({ queryKey: policyVersionsKey(sheetId), queryFn: () => listPolicyVersions(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const rates = useQuery({ queryKey: ratesKey(sheetId), queryFn: () => listRates(sheetId) });
  const floor = useFloor(sheetId);
  const today = useSheetToday(sheetId, user);
  const charges = useLate(sheetId, today);
  const policy = usePolicyVersions(sheetId);
  const fetching = useIsFetching({ predicate: (q) => q.queryKey[0] === 'sheet' && q.queryKey[1] === sheetId });

  // Another sheet loaded: an export still running was for another sheet.
  useEffect(() => setRun(null), [sheetId]);

  const start = async () => {
    setRun('reading');
    await Promise.all([settingsKey(sheetId), claimsKey(sheetId), peopleKey(sheetId), ratesKey(sheetId)]
      .map((queryKey) => queryClient.refetchQueries({ queryKey })));
    setRun((r) => (r === 'reading' ? 'ready' : r));
  };

  useEffect(() => {
    if (run !== 'ready' || fetching > 0 || !versions.data || !claims.data || !people.data) return;
    const byId = new Map(people.data.map((p) => [p.id, p]));
    const lines = versions.data.map((v) => {
      const own = claims.data.filter((c) => policy.on(c.date) === v.version);
      let total = 0n;
      for (const c of own.filter(counts)) {
        const p = byId.get(c.personId);
        const due = p ? claimDue(c, rates.data, (d) => gradeOn(p, d), floor, charges) : null;
        if (due !== null) total += hundredths(due);
      }
      return [String(v.version), v.from ?? '', v.key, v.value, String(own.length), formatCents(total)];
    });
    downloadCsv('policy.csv', [['version', 'from', 'key', 'value', 'claims', 'due-total'], ...lines]);
    setRun(null);
  }, [run, fetching]);

  return (
    <button type="button" data-testid="export-policy" disabled={run !== null} onClick={() => void start()}>
      Export policy
    </button>
  );
}

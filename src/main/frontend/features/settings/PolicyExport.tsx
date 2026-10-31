import { useEffect, useRef, useState } from 'react';
import { useIsFetching, useQueryClient } from '@tanstack/react-query';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useSheetFigures } from '../people/useSheetFigures';
import { gradeOn } from '../people/usePeople';
import { useGrace } from '../claims/useGrace';
import { claimDue, formatCents } from '../rates/due';
import { claimRate } from '../rates/useRates';
import { downloadCsv } from '../people/csv';
import { useFloor, useLatePct, usePolicy, usePolicyVersions, useSettings } from './useSettings';

// Downloads the policy versions as CSV, oldest first: version, from, key, value, then the count of the
// counting claims whose policy version it is (the one in force on the claim's date) and their dues added up
// (n/a adds nothing). Each press re-reads the sheet first.
export function PolicyExport() {
  const [sheet] = useSearchParam('sheet', asString);
  const [run, setRun] = useState<number | null>(null);
  const queryClient = useQueryClient();

  const start = async () => {
    setRun(null);
    await queryClient.invalidateQueries({ queryKey: ['sheet', sheet] });
    setRun((was) => (was ?? 0) + 1);
  };

  return (
    <>
      <button type="button" data-testid="export-policy" onClick={() => void start()}>
        Export policy versions
      </button>
      {run !== null && <Collect key={run} sheet={sheet} onDone={() => setRun(null)} />}
    </>
  );
}

function Collect({ sheet, onDone }: { sheet: string; onDone: () => void }) {
  const { live, byId, rates, today } = useSheetFigures(sheet);
  const settings = useSettings(sheet);
  const versions = usePolicyVersions(sheet);
  const floor = useFloor(sheet);
  const late = { pct: useLatePct(sheet), today, grace: useGrace(sheet) };
  const policy = usePolicy(sheet);
  const fetching = useIsFetching({ queryKey: ['sheet', sheet] });
  const done = useRef(false);
  const ready = fetching === 0 && settings !== undefined && today !== undefined;

  useEffect(() => {
    if (!ready || done.current) return;
    done.current = true;
    const lines = settings.policyLines ?? [];
    const counts = lines.map(() => 0);
    const dues = lines.map(() => 0n);
    for (const c of live) {
      const v = versions.on(c.date);
      if (v < 1 || v > lines.length) continue;
      counts[v - 1] += 1;
      const person = byId.get(c.personId)!;
      dues[v - 1] += claimDue(c, claimRate(rates, gradeOn(person, c.date), c), floor, late, policy) ?? 0n;
    }
    downloadCsv('policy.csv', [
      ['version', 'from', 'key', 'value', 'claims', 'due-total'],
      ...lines.map((l, i) => [String(i + 1), l.from, l.key, l.value, String(counts[i]), formatCents(dues[i])]),
    ]);
    onDone();
  });
  return null;
}

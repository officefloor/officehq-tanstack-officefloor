import { useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { getJson } from '../../api/http';
import { useClaims } from '../claims/useClaims';
import { usePeople } from '../people/usePeople';
import { totalDue, useRates } from '../rates/useRates';
import { useSettings } from '../settings/useSettings';
import { useSheet } from '../sheet/useSheet';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { useGradeDues } from '../summary/useGradeDues';

type GradeTotals = Record<string, string>;

/** Per grade, the sheet's summary due-total, or null until everything it is worked out from has
 *  loaded. */
function useGradeTotals(sheet: string): GradeTotals | null {
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const settings = useSettings(sheet);
  const today = useSheet(sheet);
  const byGrade = useGradeDues(sheet);
  const ready = people.isSuccess && claims.isSuccess && rates.isSuccess && settings.isSuccess && today.isSuccess;
  return ready ? Object.fromEntries(Object.entries(byGrade).map(([g, on]) => [g, totalDue(on.map((d) => d.due))])) : null;
}

/** The other sheet's per-grade figures, reported up. Renders nothing. */
function OtherFigures({ sheet, report }: { sheet: string; report: (totals: string | null) => void }) {
  const totals = useGradeTotals(sheet);
  const json = totals === null ? null : JSON.stringify(totals);
  useEffect(() => report(json), [json, report]);
  useEffect(() => () => report(null), [report]);
  return null;
}

/**
 * Compare this sheet with another by id: one line per grade of this sheet, `<grade> <this> <other>`,
 * each the grade's summary due-total on that sheet (0.00 for a grade the other sheet has no one on).
 * A sheet that has never been loaded gives no lines, and is not started by asking.
 */
function SheetComparePanel({ sheet }: { user: string; sheet: string }) {
  const [entry, setEntry] = useState('');
  const [other, setOther] = useState<{ id: string; press: number } | null>(null);
  const [otherTotals, setOtherTotals] = useState<string | null>(null);
  const mine = useGradeTotals(sheet);
  const exists = useQuery({
    queryKey: ['sheet', other?.id ?? '', 'exists', other?.press ?? 0],
    queryFn: () => getJson<boolean>(`/api/sheets/${encodeURIComponent(other!.id)}/exists`),
    enabled: other !== null && other.id !== '',
    staleTime: 0,
    gcTime: 0,
  });
  const theirs: GradeTotals | null = otherTotals === null ? null : JSON.parse(otherTotals);
  const found = exists.isSuccess && exists.data === true;

  return (
    <section data-testid="sheet-compare-panel">
      <h2>Compare sheets</h2>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          setOther((was) => ({ id: entry.trim(), press: (was?.press ?? 0) + 1 }));
        }}
      >
        <input
          data-testid="compare-sheet"
          aria-label="Other sheet id"
          placeholder="other sheet id"
          value={entry}
          onChange={(e) => setEntry(e.target.value)}
        />{' '}
        <button type="submit" data-testid="compare">
          Compare
        </button>
      </form>
      {found && other && <OtherFigures key={other.id} sheet={other.id} report={setOtherTotals} />}
      {found && mine && theirs && (
        <ul>
          {Object.keys(mine).map((g) => (
            <li key={g} data-testid="compare-line">
              {g} {mine[g]} {theirs[g] ?? '0.00'}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 28.8, Component: SheetComparePanel });

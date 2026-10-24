import { useQueryClient, type QueryClient } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { HomeMain } from '../../slots/defs/homeMain';
import { sheetQueryKey, useCurrentSheet, useCurrentUser, type SheetInfo } from '../session/session';
import { claimsPath, claimsQueryKey, type Claim } from '../claims/claims';
import { settingsPath, settingsQueryKey, type Settings } from '../roles/roles';
import { ratesPath, ratesQueryKey, type Rate } from '../rates/rates';
import { personDue } from '../rates/due.slot';
import { gradeOn, peoplePath, peopleQueryKey, type Person } from './people';

// The export button: downloads every person on the sheet (retired people included, in list order) as CSV —
// name, grade in force on the sheet's today, retired-from date (blank when not retired) and due-total as shown.
// Its inputs are re-read from the store when pressed, so in an as-of view they are the as-of figures (the as-of
// date standing in for today) even while the screen is still re-reading them after the as-of date was changed.
export function csvValue(value: string): string {
  return /[",\r\n]/.test(value) ? `"${value.replace(/"/g, '""')}"` : value;
}

/** A sheet read made afresh from the store (as of the screen's as-of date, when one is set) for an export. */
export function readFresh<T>(queryClient: QueryClient, queryKey: readonly unknown[], path: string): Promise<T> {
  return queryClient.fetchQuery({ queryKey, queryFn: () => getJson<T>(path), staleTime: 0 });
}

export const sheetInfoPath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}`;

function ExportPeople() {
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
    const today = info.today;
    const lines = [['name', 'grade', 'retired-from', 'due-total']];
    for (const p of people) {
      lines.push([p.name, gradeOn(p, today), p.retiredFrom ?? '', personDue(p, claims, rates, today, settings)]);
    }
    const csv = lines.map((l) => l.map(csvValue).join(',') + '\n').join('');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `people-${sheet}.csv`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  };
  return (
    <button type="button" data-testid="export-people" onClick={() => { void download(); }}>
      Export people (CSV)
    </button>
  );
}

export const contribution = HomeMain.fill({ order: 19, Component: ExportPeople });

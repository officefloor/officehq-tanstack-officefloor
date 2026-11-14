import { useQueryClient } from '@tanstack/react-query';
import { getLiveJson } from '../../api/http';
import type { Claim } from '../claims/claims';
import { gradeOn, liveHolds } from '../people/people';
import type { Person } from '../people/people';
import type { Pool } from '../pools/pools';
import { personDue } from '../rates/rates';
import type { Rate } from '../rates/rates';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import type { SheetInfo } from '../session/session';
import { dueRules } from '../settings/settings';
import type { Settings } from '../settings/settings';
import { csvField } from './ExportPeople';
import { freshClaims, freshPeople, freshPools, freshRates, freshSettings, freshSheetInfo } from './fresh';

type SheetData = [SheetInfo, Person[], Claim[], Rate[], Settings, Pool[]];

/** Another sheet of the tree as the store holds it now. */
function liveSheet(sheet: string): Promise<SheetData> {
  const path = `/api/sheets/${encodeURIComponent(sheet)}`;
  return Promise.all([
    getLiveJson<SheetInfo>(path),
    getLiveJson<Person[]>(`${path}/people`),
    getLiveJson<Claim[]>(`${path}/claims`),
    getLiveJson<Rate[]>(`${path}/rates`),
    getLiveJson<Settings>(`${path}/settings`),
    getLiveJson<Pool[]>(`${path}/pools`),
  ]);
}

/** The lines `<sheet>,<name>,<grade>,<due-total>` for one sheet's people in list order, by the sheet's own today. */
function sheetLines(sheet: string, [info, people, claims, rates, settings, pools]: SheetData): string[][] {
  const today = info.today;
  const rules = dueRules(settings, today, claims, pools);
  return people.map((p) => {
    const due = personDue(claims.filter((c) => c.personId === p.id), rates, p.grades, liveHolds(p, rules.today), rules, {
      personId: p.id,
      claims,
      people,
    });
    return [sheet, p.name, gradeOn(p.grades, today) ?? p.grade, due];
  });
}

// The button that downloads the people of the whole tree as a CSV file: a sheet column, then the name, grade and
// due-total as the people export gives them — this sheet's people first in list order, then the other sheets of its
// tree (its ancestors and descendants) by sheet id. Every sheet is re-read from the store first. Quoted and ended as
// the people export.
export function ExportTree() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const queryClient = useQueryClient();

  if (!user || !sheet) return null;

  const download = async () => {
    const ids = await getLiveJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/tree`);
    const others = [...ids].sort((a, b) => (a < b ? -1 : a > b ? 1 : 0));
    const [own, ...data] = await Promise.all([
      Promise.all([
        freshSheetInfo(queryClient, sheet),
        freshPeople(queryClient, sheet),
        freshClaims(queryClient, sheet),
        freshRates(queryClient, sheet),
        freshSettings(queryClient, sheet),
        freshPools(queryClient, sheet),
      ]),
      ...others.map(liveSheet),
    ]);
    const lines = [['sheet', 'name', 'grade', 'due-total'], ...sheetLines(sheet, own), ...others.flatMap((id, i) => sheetLines(id, data[i]))];
    const csv = lines.map((l) => l.map(csvField).join(',') + '\n').join('');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `tree-${sheet}.csv`;
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  };

  return (
    <button type="button" data-testid="export-tree" onClick={download}>
      Export tree (CSV)
    </button>
  );
}

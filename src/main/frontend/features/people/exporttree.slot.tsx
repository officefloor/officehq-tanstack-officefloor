import { useState } from 'react';
import { getJsonNow } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheet';
import { useClaims, type Claim } from '../claims/claims';
import { capLessCut, usePersonCap, type Pool } from '../pools/pools';
import { personDueTotal, useRates, type Rate } from '../rates/rates';
import { floorOf, lateOf, useFloor, useLate, type Settings } from '../settings/settings';
import { useSheetToday, type Sheet } from '../sheet/sheet';
import { gradeOn, usePeople, type Person } from './people';

/** A CSV value: wrapped in double quotes (any quote inside doubled) when it holds a comma, a quote or a line
 *  break, else as it is. */
function csvValue(value: string): string {
  return /[",\r\n]/.test(value) ? `"${value.replace(/"/g, '""')}"` : value;
}

/** Another sheet's people, in list order, each as [sheet id, name, grade, due-total] with their grade in force on
 *  that sheet's today and their due-total as shown on their row there, read from the store as it is now. */
async function sheetLines(sheetId: string): Promise<string[][]> {
  const read = <T,>(part: string) =>
    getJsonNow<T>(`/api/sheets/${encodeURIComponent(sheetId)}${part === '' ? '' : `/${part}`}`);
  const [people, claims, rates, settings, pools, sheet] = await Promise.all([
    read<Person[]>('people'), read<Claim[]>('claims'), read<Rate[]>('rates'), read<Settings>('settings'),
    read<Pool[]>('pools'), read<Sheet>(''),
  ]);
  const today = sheet.today;
  const floor = floorOf(settings);
  const late = lateOf(settings, today, claims, people);
  return people.map((p) => [sheetId, p.name, gradeOn(p, today),
    personDueTotal(p, claims, rates, today, floor, late, capLessCut(settings.cap ?? '', pools, p.id), people)]);
}

/** The button that downloads the people of the whole tree (this sheet, its ancestors and its descendants) as a CSV
 *  file: a header line, then one line per person, retired people included — this sheet's people first in list order
 *  (as the people export has them), then the other sheets' by sheet id, each sheet's in its list order — with their
 *  sheet id, name, grade in force on their sheet's today and due-total as shown on their row; each line ends with a
 *  line feed. */
function ExportTree({ sheetId }: { sheetId: string; user: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const capOf = usePersonCap(sheetId);
  const [error, setError] = useState('');

  const download = async () => {
    setError('');
    try {
      const others = await getJsonNow<string[]>(`/api/sheets/${encodeURIComponent(sheetId)}/tree`);
      const otherLines = await Promise.all(others.map(sheetLines));
      const lines = [
        ['sheet', 'name', 'grade', 'due-total'],
        ...people.map((p) => [sheetId, p.name, gradeOn(p, today),
          personDueTotal(p, claims, rates, today, floor, late, capOf(p.id), people)]),
        ...otherLines.flat(),
      ];
      const csv = lines.map((values) => values.map(csvValue).join(',') + '\n').join('');
      const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
      const link = document.createElement('a');
      link.href = url;
      link.download = `tree-${sheetId}.csv`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      setTimeout(() => URL.revokeObjectURL(url), 0);
    } catch {
      setError('Could not export the tree.');
    }
  };

  return (
    <p>
      <button type="button" data-testid="export-tree" onClick={download}>
        Export tree people (CSV)
      </button>
      {error && <span data-testid="export-tree-error"> {error}</span>}
    </p>
  );
}

export const contribution = SheetBody.fill({ order: 11.05, Component: ExportTree });

import { asString, useSearchParam } from '../../url/useSearchParam';

export const COLUMNS = [
  'claimed',
  'due-total',
  'excess',
  'statement-count',
  'stated-total',
  'statement-date',
  'overdue-count',
  'overdue-total',
  'patrons',
  'backing',
  'hold',
  'retired',
  'terms',
  'contact',
  'aliases',
] as const;
export type Column = (typeof COLUMNS)[number];

// Which of the people list's columns this user's screen shows: every one when the screen is first
// opened; the switched-off ones live in the URL (`hideColumns`, comma separated), so the choice
// belongs to this user's screen and loading a sheet leaves it as it is. A column switched off has
// no cell at all; totals, the header, the summary and every other figure are unaffected.
export function useColumns(): [(column: Column) => boolean, (column: Column, on: boolean) => void] {
  const [raw, setRaw] = useSearchParam('hideColumns', asString);
  const hidden = raw.split(',').filter((c) => c !== '');
  const shown = (column: Column) => !hidden.includes(column);
  const set = (column: Column, on: boolean) => {
    const next = COLUMNS.filter((c) => (c === column ? !on : hidden.includes(c)));
    setRaw(next.join(','));
  };
  return [shown, set];
}

export function ColumnChooser() {
  const [shown, set] = useColumns();
  return (
    <fieldset>
      <legend>Columns</legend>
      {COLUMNS.map((c) => (
        <label key={c}>
          <input type="checkbox" data-testid={`column-${c}`} checked={shown(c)} onChange={(e) => set(c, e.target.checked)} /> {c}{' '}
        </label>
      ))}
    </fieldset>
  );
}

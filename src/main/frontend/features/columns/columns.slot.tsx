import { SheetBody } from '../../slots/defs/sheetBody';
import { COLUMNS, useColumnChooser } from './useColumns';

// The columns of the people list the signed-in user's screen shows: a box per column, all ticked until switched off.
// A column switched off has no cell on this user's screen; every total and figure elsewhere stays as it is. Saved per
// sheet and user, with no audit line.
function ColumnChooser({ sheetId }: { sheetId: string; user: string }) {
  const { shown, choose } = useColumnChooser(sheetId);
  return (
    <section data-testid="columns-panel">
      Columns:{' '}
      {COLUMNS.map((c) => (
        <label key={c}>
          <input type="checkbox" data-testid={`column-${c}`} checked={shown(c)} onChange={(e) => choose(c, e.target.checked)} />{' '}
          {c}{' '}
        </label>
      ))}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 9, Component: ColumnChooser });

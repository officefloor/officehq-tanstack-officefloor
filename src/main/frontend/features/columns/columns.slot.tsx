import { SheetPage } from '../../slots/defs/sheetPage';
import { COLUMNS, setColumnShown, useShownColumns } from './columns';

/** The column chooser: a checkbox per column of the people list the signed-in user's screen may hide. */
function ColumnChooser({ user }: { sheetId: string; user: string }) {
  const shown = useShownColumns(user);
  return (
    <fieldset>
      <legend>Columns</legend>
      {COLUMNS.map((c) => (
        <label key={c}>
          <input type="checkbox" data-testid={`column-${c}`} checked={shown(c)} onChange={(e) => setColumnShown(user, c, e.target.checked)} />{' '}
          {c}{' '}
        </label>
      ))}
    </fieldset>
  );
}

export const contribution = SheetPage.fill({ order: 9, Component: ColumnChooser });

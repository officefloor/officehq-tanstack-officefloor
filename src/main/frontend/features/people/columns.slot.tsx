import { SheetBody } from '../../slots/defs/sheet';
import { COLUMNS, useHiddenColumns } from './columns';

/** A checkbox per column of the people list: which of them this user's screen shows. */
function ColumnChooser(_: { sheetId: string; user: string }) {
  const [hidden, setShown] = useHiddenColumns();
  return (
    <p>
      Columns{' '}
      {COLUMNS.map((c) => (
        <label key={c}>
          <input
            type="checkbox"
            data-testid={`column-${c}`}
            checked={!hidden.has(c)}
            onChange={(e) => setShown(c, e.target.checked)}
          />{' '}
          {c}{' '}
        </label>
      ))}
    </p>
  );
}

export const contribution = SheetBody.fill({ order: 9.9, Component: ColumnChooser });

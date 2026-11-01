import { SheetPanels } from '../../slots/defs/sheetPanels';
import { COLUMNS, setColumnShown, useColumnShown, type Column } from './useColumns';

function ColumnBox({ user, column }: { user: string; column: Column }) {
  const shown = useColumnShown(user, column);
  return (
    <label>
      <input
        type="checkbox"
        data-testid={`column-${column}`}
        checked={shown}
        onChange={(e) => setColumnShown(user, column, e.target.checked)}
      />{' '}
      {column}
    </label>
  );
}

/** The column chooser: a checkbox per switchable column of the people list, for this user's screen. */
function ColumnChooser({ user }: { user: string; sheet: string }) {
  return (
    <section>
      <h3>Columns</h3>
      {COLUMNS.map((c) => (
        <span key={c}>
          <ColumnBox user={user} column={c} />{' '}
        </span>
      ))}
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 9.5, Component: ColumnChooser });

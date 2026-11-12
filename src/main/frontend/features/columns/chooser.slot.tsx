import { HomeMain } from '../../slots/defs/homeMain';
import { COLUMNS, useHiddenColumns } from './columns';

// A checkbox per column of the people list: unticked, this screen shows no cell for it.
function ColumnChooser() {
  const [hidden, setHidden] = useHiddenColumns();
  return (
    <fieldset>
      <legend>Columns</legend>
      {COLUMNS.map((c) => (
        <label key={c}>
          <input
            type="checkbox"
            data-testid={`column-${c}`}
            checked={!hidden.includes(c)}
            onChange={(e) => setHidden(e.target.checked ? hidden.filter((h) => h !== c) : [...hidden, c])}
          />
          {c}{' '}
        </label>
      ))}
    </fieldset>
  );
}

export const contribution = HomeMain.fill({ order: 19.9, Component: ColumnChooser });

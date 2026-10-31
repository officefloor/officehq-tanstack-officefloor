import { useState } from 'react';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { useAsOf } from './useAsOf';

/** Shows the sheet as of a date on this user's screen (or as it stands, when cleared), and the
 *  date it is shown as of. */
function AsOfPicker() {
  const [asOf, setAsOf] = useAsOf();
  const [date, setDate] = useState('');

  return (
    <form
      data-testid="asof-form"
      onSubmit={(e) => {
        e.preventDefault();
        const trimmed = date.trim();
        if (/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) setAsOf(trimmed);
      }}
    >
      As of: <strong data-testid="sheet-asof">{asOf}</strong>{' '}
      <label>
        Show as of{' '}
        <input
          data-testid="asof-date"
          placeholder="yyyy-mm-dd"
          value={date}
          onChange={(e) => setDate(e.target.value)}
        />
      </label>{' '}
      <button type="submit" data-testid="asof-set">
        Set
      </button>{' '}
      <button type="button" data-testid="asof-clear" onClick={() => setAsOf(undefined)}>
        Clear
      </button>
    </form>
  );
}

export const contribution = SheetPanels.fill({ order: 1, Component: AsOfPicker });

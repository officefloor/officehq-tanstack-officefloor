import { useState } from 'react';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useAsOf } from './useAsOf';

/** Show the sheet as of a date: every cell then shows the sheet as it stood at the end of that day (the
 *  date standing in for the sheet's today), and every change is refused, until it is cleared. */
function AsOfControl() {
  const [asOf, setAsOf] = useAsOf();
  const [date, setDate] = useState('');
  return (
    <form
      data-testid="asof-form"
      onSubmit={(e) => {
        e.preventDefault();
        const want = date.trim();
        if (/^\d{4}-\d{2}-\d{2}$/.test(want)) setAsOf(want);
      }}
    >
      As of: <strong data-testid="sheet-asof">{asOf}</strong>{' '}
      <label>
        Show as of{' '}
        <input
          type="text"
          placeholder="yyyy-mm-dd"
          data-testid="asof-date"
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

export const contribution = SheetBody.fill({ order: 1, Component: () => <AsOfControl /> });

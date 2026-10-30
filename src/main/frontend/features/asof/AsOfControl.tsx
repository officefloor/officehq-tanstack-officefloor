import { useState } from 'react';
import { useSearchParam, asString } from '../../url/useSearchParam';
import { AS_OF_PARAM } from './useAsOf';

// Sets or clears the signed-in user's as-of date, and shows it (nothing when none). While it is set the whole screen
// shows the sheet as it stood at the end of that date and every change is refused; it is this screen's alone.
export function AsOfControl() {
  const [asOf, setAsOf] = useSearchParam(AS_OF_PARAM, asString);
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
      As of: <span data-testid="sheet-asof">{asOf}</span>{' '}
      <input type="date" data-testid="asof-date" value={date} onChange={(e) => setDate(e.target.value)} />
      <button type="submit" data-testid="asof-set">Set as-of</button>
      <button type="button" data-testid="asof-clear" onClick={() => setAsOf(undefined)}>
        Clear as-of
      </button>
    </form>
  );
}

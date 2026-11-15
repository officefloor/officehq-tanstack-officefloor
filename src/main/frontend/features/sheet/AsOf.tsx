import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { asString, useSearchParam } from '../../url/useSearchParam';

/** The date this user's screen shows the sheet as of ('': as it is now). It lives in the URL (`asof`),
 *  so it belongs to this screen alone, and every request about the sheet carries it (api/http). */
export const useAsOf = () => useSearchParam('asof', asString);

/** Set or clear the as-of date. While it is set every figure on the screen is the sheet as it stood
 *  at the end of that date, and every change is refused. */
export function AsOf() {
  const queryClient = useQueryClient();
  const [asOf, setAsOf] = useAsOf();
  const [date, setDate] = useState('');

  // Everything on screen is re-read once the URL carries the new date.
  const show = async (value: string) => {
    await setAsOf(value);
    await queryClient.resetQueries();
  };

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        const value = date.trim();
        if (value !== '') void show(value);
      }}
    >
      As of <span data-testid="sheet-asof">{asOf}</span>{' '}
      <input
        type="date"
        data-testid="asof-date"
        value={date}
        onChange={(e) => setDate(e.target.value)}
      />{' '}
      <button type="submit" data-testid="asof-set">
        Show as of
      </button>{' '}
      <button type="button" data-testid="asof-clear" onClick={() => void show('')}>
        Show now
      </button>
    </form>
  );
}

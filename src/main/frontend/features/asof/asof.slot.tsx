import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { setViewAsOf } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { asString, useSearchParam } from '../../url/useSearchParam';

// The as-of date of this user's screen: while it is set, every panel shows the sheet as it stood at the end of that
// date (the date standing in for the sheet's today) and every change is refused. Setting or clearing it re-reads the
// whole sheet. It lives in the URL (search param `asof`), so it outlives loading another sheet; other users' screens
// are not affected.
function AsOfPanel({ sheetId }: { sheetId: string; user: string }) {
  const [asOf, setAsOf] = useSearchParam('asof', asString);
  const [input, setInput] = useState('');
  const [error, setError] = useState('');
  const queryClient = useQueryClient();

  const show = async (date: string) => {
    setViewAsOf(date);
    setAsOf(date);
    await queryClient.resetQueries({ queryKey: ['sheet', sheetId] });
  };

  return (
    <section data-testid="asof-panel">
      <form
        onSubmit={(e) => {
          e.preventDefault();
          const date = input.trim();
          if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) {
            setError('Enter a date');
            return;
          }
          setError('');
          void show(date);
        }}
      >
        As of: <strong data-testid="sheet-asof">{asOf}</strong>{' '}
        <input
          type="date"
          data-testid="asof-date"
          value={input}
          onChange={(e) => {
            setInput(e.target.value);
            setError('');
          }}
        />{' '}
        <button type="submit" data-testid="asof-set">
          Show as of
        </button>{' '}
        <button
          type="button"
          data-testid="asof-clear"
          onClick={() => {
            setError('');
            void show('');
          }}
        >
          Clear
        </button>
      </form>
      {error !== '' && (
        <p data-testid="asof-error" role="alert">
          {error}
        </p>
      )}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 1, Component: AsOfPanel });

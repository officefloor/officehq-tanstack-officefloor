import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { ASOF_PARAM } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { asString, useSearchParam } from '../../url/useSearchParam';

/** The as-of date the signed-in user's screen reads the sheet at ('' for now). */
export const useAsOf = () => useSearchParam(ASOF_PARAM, asString);

// The as-of view: an entry, a set and a clear button, and the as-of date shown (nothing while
// unset). While it is set every cell on the screen reads the sheet as it stood at the end of that
// date (the server answers each read so), and every change is refused. It lives in this user's URL,
// so other users' screens are unaffected. Setting or clearing it re-reads everything.
function AsOfView() {
  const queryClient = useQueryClient();
  const [asOf, setAsOf] = useAsOf();
  const [input, setInput] = useState('');
  const [error, setError] = useState('');

  const shown = useRef(asOf);
  useEffect(() => {
    if (shown.current === asOf) return;
    shown.current = asOf;
    void queryClient.resetQueries();
  }, [asOf, queryClient]);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const date = input.trim();
    if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) {
      setError('Choose a date to view the sheet as of.');
      return;
    }
    setAsOf(date);
  };

  return (
    <form onSubmit={submit} data-testid="asof-form">
      As of: <strong data-testid="sheet-asof">{asOf}</strong>{' '}
      <label>
        View as of{' '}
        <input
          type="date"
          data-testid="asof-date"
          value={input}
          onChange={(e) => {
            setInput(e.target.value);
            setError('');
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="asof-set">
        Set
      </button>{' '}
      <button
        type="button"
        data-testid="asof-clear"
        onClick={() => {
          setError('');
          setAsOf(undefined);
        }}
      >
        Clear
      </button>
      {error && (
        <p role="alert" data-testid="asof-error">
          {error}
        </p>
      )}
    </form>
  );
}

export const contribution = SheetBody.fill({ order: -99, Component: AsOfView });

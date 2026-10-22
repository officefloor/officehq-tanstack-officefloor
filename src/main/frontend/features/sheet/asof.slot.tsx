import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { setRequestAsOf } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheet';
import { useAsOf } from './sheet';

/** Whether the text is a real date as yyyy-mm-dd. */
function isDate(text: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(text)) return false;
  const d = new Date(`${text}T00:00:00Z`);
  return !Number.isNaN(d.getTime()) && d.toISOString().slice(0, 10) === text;
}

/** The as-of date of this user's screen: shown ('' when none), set and cleared. Setting or clearing it re-reads
 *  everything under the sheet, as it stood at the end of that day or as it is now. */
function SheetAsOf({ sheetId }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [asOf, setAsOf] = useAsOf();
  const [typed, setTyped] = useState('');
  const [invalid, setInvalid] = useState(false);

  useEffect(() => {
    setRequestAsOf(asOf);
  }, [asOf]);

  const show = async (date: string) => {
    setRequestAsOf(date);
    setAsOf(date === '' ? undefined : date);
    await queryClient.resetQueries({ queryKey: ['sheet', sheetId] });
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    const date = typed.trim();
    if (!isDate(date)) {
      setInvalid(true);
      return;
    }
    setInvalid(false);
    void show(date);
  };

  return (
    <section>
      <form data-testid="asof-form" onSubmit={submit}>
        <label>
          As of{' '}
          <input
            data-testid="asof-date"
            placeholder="yyyy-mm-dd"
            value={typed}
            onChange={(e) => {
              setTyped(e.target.value);
              setInvalid(false);
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="asof-set">
          Show as of
        </button>{' '}
        <button
          type="button"
          data-testid="asof-clear"
          onClick={() => {
            setInvalid(false);
            void show('');
          }}
        >
          Show now
        </button>
      </form>
      <p>
        Showing as of <strong data-testid="sheet-asof">{asOf}</strong>
      </p>
      {invalid && (
        <p data-testid="asof-error" role="alert">
          Enter the as-of date as a real date (yyyy-mm-dd).
        </p>
      )}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 0.5, Component: SheetAsOf });

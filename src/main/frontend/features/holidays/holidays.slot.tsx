import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { holidaysKey, holidaysPath, useHolidays } from './holidays';

// The sheet's holidays, and a form that adds a date to them. A claim's due-by, fixed when the claim is added, is
// moved past the holidays on the sheet then; adding a holiday works out again the due-by of every claim not SETTLED
// or VOID due on or after it, which may move the sheet's figures.
function Holidays({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const { data: holidays = [] } = useHolidays(sheet);
  const [date, setDate] = useState('');

  const add = useMutation({
    mutationFn: (body: { date: string }) => postJson<string[]>(byUser(holidaysPath(sheet), user), body),
    onSuccess: (updated) => {
      queryClient.setQueryData(holidaysKey(sheet), updated);
      setDate('');
      // Due-by dates, and everything figured from them, may have moved: re-read the rest of the sheet.
      void queryClient.invalidateQueries({
        predicate: (q) => q.queryKey.includes(sheet) && q.queryKey[0] !== 'holidays' && q.queryKey[0] !== 'sheet',
      });
    },
  });
  const send = useSubmitOnce(add);

  const change = (next: string) => {
    setDate(next);
    add.reset();
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    // The server refuses a date that is not valid or is already on the list.
    send({ date });
  };

  return (
    <section>
      <h2>Holidays</h2>
      <form onSubmit={submit}>
        <label>
          Date{' '}
          <input
            data-testid="holiday-date"
            placeholder="yyyy-mm-dd"
            value={date}
            onChange={(e) => change(e.target.value)}
          />
        </label>{' '}
        <button type="submit" data-testid="holiday-add" disabled={add.isPending}>
          Add holiday
        </button>
        {add.isError && (
          <span data-testid="holiday-error" role="alert">
            {' '}A holiday needs a valid date not already on the list.
          </span>
        )}
      </form>
      <ul>
        {holidays.map((d) => (
          <li key={d} data-testid={`holiday-row-${d}`}>
            {d}
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 9, Component: Holidays });

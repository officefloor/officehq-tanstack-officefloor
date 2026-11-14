import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useOneAtATime } from '../../query/useOneAtATime';
import { claimsKey } from '../claims/claims';
import { peopleKey } from '../people/people';
import type { Person } from '../people/people';
import { usePersonError } from '../people/personError';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { useColumnShown } from '../columns/columns';

const DATE = /^\d{4}-\d{2}-\d{2}$/;

// A person's statements: how many were taken, the last one's due-total and date, and the button that
// takes one — recording their due-total now and binding each counting claim to its figure now — and the
// button a steward cancels the last one with, making the claims it bound live again. A date beside the button takes the
// statement as of that day (on or before the sheet's today; blank: now), as the sheet stood at the end of it.
export function PersonStatement({ person }: { person: Person }) {
  const shown = {
    count: useColumnShown('statement-count'),
    stated: useColumnShown('stated-total'),
    date: useColumnShown('statement-date'),
  };
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [, setError] = usePersonError(person.id);
  const queryClient = useQueryClient();
  const [asof, setAsof] = useState('');
  const once = useOneAtATime();

  const take = useMutation({
    mutationFn: (date: string | null) =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/statement?user=${encodeURIComponent(user)}`,
        { asof: date },
      ),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
      ]);
    },
    onError: () => setError('The statement could not be taken.'),
    // The date was for this statement only: the next one is taken now unless a date is typed again.
    onSettled: () => setAsof(''),
  });

  const cancel = useMutation({
    mutationFn: () =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/statement-cancel?user=${encodeURIComponent(user)}`,
        {},
      ),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
      ]);
    },
    onError: () => setError('The statement could not be cancelled.'),
  });

  return (
    <td>
      {shown.count && <span data-testid="person-statement-count">{person.statementCount}</span>}{' '}
      {shown.stated && <span data-testid="person-stated-total">{person.statedTotal ?? ''}</span>}{' '}
      {shown.date && <span data-testid="person-statement-date">{person.statementDate ?? ''}</span>}{' '}
      <input
        data-testid="person-statement-asof"
        placeholder="As of yyyy-mm-dd (blank: now)"
        value={asof}
        onChange={(e) => {
          setAsof(e.target.value);
          setError('');
        }}
      />
      <button
        type="button"
        data-testid="person-statement"
        disabled={take.isPending}
        onClick={() => {
          setError('');
          const date = asof.trim();
          if (date && !DATE.test(date)) {
            setError('Type a date as yyyy-mm-dd.');
            return;
          }
          once(() => take.mutateAsync(date || null));
        }}
      >
        Statement
      </button>
      <button
        type="button"
        data-testid="person-unstate"
        disabled={cancel.isPending}
        onClick={() => {
          setError('');
          once(() => cancel.mutateAsync());
        }}
      >
        Cancel statement
      </button>
    </td>
  );
}

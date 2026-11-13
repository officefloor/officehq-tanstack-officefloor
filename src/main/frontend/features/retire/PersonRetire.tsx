import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useOneAtATime } from '../../query/useOneAtATime';
import { peopleKey } from '../people/people';
import type { Person } from '../people/people';
import { usePersonError } from '../people/personError';
import { useCurrentSheet, useCurrentUser } from '../session/session';

// Whether the person is retired and from when, and the date and button that retire them (a blank
// date is the sheet's today). From the day the date is reached, a retired person's open and pending claims leave
// the live totals (settled and statement-bound ones still count); one dated after the date is refused.
export function PersonRetire({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [from, setFrom] = useState('');
  const [, setError] = usePersonError(person.id);
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const retire = useMutation({
    mutationFn: (date: string) =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/retire?user=${encodeURIComponent(user)}`,
        { from: date },
      ),
    onSuccess: async () => {
      setFrom('');
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
    },
    onError: () => setError('The person could not be retired.'),
  });

  const submit = () => {
    setError('');
    const date = from.trim();
    if (date && !/^\d{4}-\d{2}-\d{2}$/.test(date)) {
      setError('Type a date as yyyy-mm-dd.');
      return;
    }
    once(() => retire.mutateAsync(date));
  };

  return (
    <td>
      <span data-testid="person-retired">{person.retired ? 'yes' : 'no'}</span>{' '}
      <span data-testid="person-retired-from">{person.retiredFrom ?? ''}</span>{' '}
      <input
        data-testid="person-retire-date"
        placeholder="yyyy-mm-dd"
        value={from}
        onChange={(e) => {
          setFrom(e.target.value);
          setError('');
        }}
      />
      <button type="button" data-testid="person-retire" disabled={retire.isPending} onClick={submit}>
        Retire
      </button>
    </td>
  );
}

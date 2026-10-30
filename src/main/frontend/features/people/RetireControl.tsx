import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { peopleKey, type Person } from './usePeople';

// Retires the person from a date (blank takes the sheet's today): from then on no claim of theirs
// may be dated after it, and the list leaves them out unless "show retired" is on. A refusal shows
// the person's error.
export function RetireControl({ personId }: { personId: string }) {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const [date, setDate] = useState('');
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const retire = useMutation({
    mutationFn: (from: string) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${encodeURIComponent(personId)}/retire`, {
        date: from,
        user,
      }),
    onSuccess: () => {
      setDate('');
      return queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
    },
  });

  return (
    <>
      <input
        type="date"
        data-testid="person-retire-date"
        value={date}
        onChange={(e) => {
          setDate(e.target.value);
          retire.reset();
        }}
      />{' '}
      <button
        type="button"
        data-testid="person-retire"
        onClick={() => {
          retire.reset();
          once(() => retire.mutateAsync(date.trim()));
        }}
      >
        Retire
      </button>
      {retire.isError && (
        <span data-testid="person-error" role="alert">
          {' '}
          The person could not be retired.
        </span>
      )}
    </>
  );
}

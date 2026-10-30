import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { peopleKey, type Person } from './usePeople';

// Names another person on the sheet as the person's patron, one at a time. The person is not
// offered as their own patron; naming the same patron twice is refused. A refusal shows the
// patron error.
export function PatronControl({ personId, people }: { personId: string; people: Person[] }) {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const [patronId, setPatronId] = useState('');
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const add = useMutation({
    mutationFn: (patron: string) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${encodeURIComponent(personId)}/patrons`, {
        patronId: patron,
        user,
      }),
    onSuccess: () => {
      setPatronId('');
      return queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
    },
  });

  return (
    <>
      <select
        data-testid="person-patron-select"
        value={patronId}
        onChange={(e) => {
          setPatronId(e.target.value);
          add.reset();
        }}
      >
        <option value="">Patron…</option>
        {people
          .filter((p) => p.id !== personId)
          .map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
            </option>
          ))}
      </select>{' '}
      <button
        type="button"
        data-testid="person-patron-add"
        onClick={() => {
          add.reset();
          once(() => add.mutateAsync(patronId));
        }}
      >
        Add patron
      </button>
      {add.isError && (
        <span data-testid="person-patron-error" role="alert">
          {' '}
          The patron could not be added.
        </span>
      )}
    </>
  );
}

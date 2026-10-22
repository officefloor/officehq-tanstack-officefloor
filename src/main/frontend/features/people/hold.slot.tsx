import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, putJson } from '../../api/http';
import { PersonRow } from '../../slots/defs/personRow';
import { peopleKey } from './people';
import type { Person } from './people';

/** Whether the person is on hold, and the button that puts them on hold or takes them off it. While held,
 *  their OPEN and PENDING claims count in no live total; their SETTLED claims still count. */
function PersonHold({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const [error, setError] = useState('');

  const toggle = useMutation({
    mutationFn: () =>
      putJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/hold`, { user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
    onError: (e) =>
      setError(e instanceof HttpError && e.body ? e.body : `Could not change the hold on ${person.name}.`),
  });

  return (
    <td>
      Hold <span data-testid="person-hold">{person.held ? 'yes' : 'no'}</span>{' '}
      <button
        type="button"
        data-testid="person-hold-toggle"
        disabled={toggle.isPending}
        onClick={() => {
          setError('');
          toggle.mutate();
        }}
      >
        {person.held ? 'Release' : 'Hold'}
      </button>
      {error && (
        <p data-testid="person-error" role="alert">
          {error}
        </p>
      )}
    </td>
  );
}

export const contribution = PersonRow.fill({ order: 45, Component: PersonHold });

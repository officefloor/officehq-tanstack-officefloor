import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonHeads } from '../../slots/defs/personRow';
import { peopleKey, usePeople, type Person } from '../people/people.slot';
import { personActionKey, useLastPersonAction } from '../people/personAction';
import { useUser } from '../session/useSession';
import { useColumnShown } from '../columns/useColumns';

function AliasHeads({ sheetId }: { sheetId: string; user: string }) {
  const shown = useColumnShown(sheetId);
  return (
    <>
      {shown('aliases') && <th>Aliases</th>}
      <th>Clashing</th>
      <th />
    </>
  );
}

// The person's aliases (sorted as names are compared: ignoring case) and the entry that adds one more. An alias
// shares the names' uniqueness on the sheet and across its tree; whether the person clashes (their name or an alias
// used on another sheet of the tree — the person form refuses their edits until a rename ends it); the quick-add bar and the paste box find the person by it. A refusal
// shows on the person until the entry is changed or the next action on their row.
function AliasCells({ sheetId, personId }: { sheetId: string; user: string; personId: number }) {
  const shown = useColumnShown(sheetId);
  const person = usePeople(sheetId).data?.find((p) => p.id === personId);
  const [alias, setAlias] = useState('');
  const [user] = useUser();
  const queryClient = useQueryClient();
  const add = useMutation({
    mutationKey: personActionKey(sheetId, personId),
    mutationFn: async (typed: string) => {
      if (typed.trim() === '') throw new Error('Enter an alias');
      return postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/aliases`, {
        alias: typed.trim(),
        by: user ?? '',
      });
    },
    onSuccess: async () => {
      setAlias('');
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
  });
  const last = useLastPersonAction(sheetId, personId, add.submittedAt);

  return (
    <>
      {shown('aliases') && <td data-testid="person-aliases">{(person?.aliases ?? []).join(', ')}</td>}
      <td data-testid="person-clashing">{person?.clashing ? 'yes' : 'no'}</td>
      <td>
        <input
          data-testid="person-alias-input"
          value={alias}
          onChange={(e) => {
            setAlias(e.target.value);
            add.reset();
          }}
        />{' '}
        <button type="button" data-testid="person-alias-add" disabled={add.isPending} onClick={() => add.mutate(alias)}>
          Add alias
        </button>
        {add.isError && last && (
          <span data-testid="person-error" role="alert">
            {add.error.message}
          </span>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonHeads.fill({ order: 55, Component: AliasHeads }),
  PersonCells.fill({ order: 55, Component: AliasCells }),
];

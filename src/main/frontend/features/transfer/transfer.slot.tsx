import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import type { Person } from '../people/people';
import { useUser } from '../session/session';
import { useRowError } from '../people/rowError';

function TransferHead() {
  return <th>Transfer</th>;
}

// An entry for another sheet's id and a button that transfers this person there (a steward): they
// arrive with their grades, terms, contact, aliases and claims, and leave this sheet's list.
function PersonTransfer({ sheetId, person }: { sheetId: string; person: Person }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [to, setTo] = useState('');
  const [error, setError] = useRowError(person.id, 'transfer');
  const once = useOnce();
  const transfer = useMutation({
    mutationFn: (toSheet: string) =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheetId)}/people/${encodeURIComponent(person.id)}/transfer`,
        { toSheet },
        user,
      ),
    onSuccess: async (_person, toSheet) => {
      await queryClient.invalidateQueries({ queryKey: ['sheet', toSheet.trim()] });
      await queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
    onError: () => setError('That person could not be transferred.'),
  });

  const submit = () => {
    setError('');
    once((done) => transfer.mutate(to, { onSettled: done }));
  };

  return (
    <td>
      <input
        data-testid="person-transfer-to"
        value={to}
        placeholder="sheet id"
        onChange={(e) => {
          setTo(e.target.value);
          setError('');
        }}
      />{' '}
      <button type="button" data-testid="person-transfer" onClick={submit} disabled={transfer.isPending}>
        Transfer
      </button>
      {error && (
        <p role="alert" data-testid="person-error">
          {error}
        </p>
      )}
    </td>
  );
}

export const contributions = [
  PersonHead.fill({ order: 66, Component: TransferHead }),
  PersonRow.fill({ order: 66, Component: PersonTransfer }),
];

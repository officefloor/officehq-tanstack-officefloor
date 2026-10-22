import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonRow } from '../../slots/defs/personRow';
import { usePersonError } from './rowError';
import type { Person } from './people';

/** The entry and button that transfer the person to another sheet by id (a steward only; not a retired person, not
 *  to this sheet). They arrive there with their grades, terms, contact, aliases and claims, and leave the list. */
function PersonTransfer({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const [toSheet, setToSheet] = useState('');
  const { error, setError, begin } = usePersonError(sheetId, person.id);
  const once = useSubmitOnce();

  const transfer = useMutation({
    mutationFn: (to: string) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/transfer`, { user, toSheet: to }),
    onSuccess: async (_, to) => {
      // the person and their claims leave this sheet for the other: everything shown of both follows
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] }),
        queryClient.invalidateQueries({ queryKey: ['sheet', to] }),
      ]);
    },
    onError: (e) =>
      setError(e instanceof HttpError && e.body ? e.body : `Could not transfer ${person.name}.`),
    onSettled: once.done,
  });

  return (
    <td>
      <input
        data-testid="person-transfer-to"
        placeholder="Sheet id"
        value={toSheet}
        onChange={(e) => {
          setToSheet(e.target.value);
          setError('');
        }}
      />{' '}
      <button
        type="button"
        data-testid="person-transfer"
        disabled={transfer.isPending}
        onClick={() => {
          begin();
          if (toSheet.trim() === '') {
            setError('Enter the id of the sheet to transfer to.');
            return;
          }
          if (!once.start()) return;
          transfer.mutate(toSheet.trim());
        }}
      >
        Transfer
      </button>
      {error && (
        <p data-testid="person-error" role="alert">
          {error}
        </p>
      )}
    </td>
  );
}

export const contribution = PersonRow.fill({ order: 49, Component: PersonTransfer });

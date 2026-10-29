import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { usePersonError } from './usePersonError';
import { useUser } from '../session/useUser';
import { sheetKey } from '../sheet/sheetQuery';

/**
 * The control that transfers the person to another sheet by id (a steward only; not a retired person, not this
 * sheet). The person leaves this sheet with their claims, so every figure of the sheet is re-read. Beside it, the
 * undo of the transfer that brought them here: a steward, on the sheet-today they arrived, sends them back.
 */
function PersonTransfer({ sheetId, personId }: { sheetId: string; personId: number }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [to, setTo] = useState('');
  const [, setError] = usePersonError(personId);
  const once = useSubmitOnce();
  const transfer = useMutation({
    mutationFn: (toSheetId: string) =>
      postJson<unknown>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/transfer`, { toSheetId }, user),
    onSuccess: async () => {
      setTo('');
      setError('');
      await queryClient.invalidateQueries({ queryKey: sheetKey(sheetId) });
    },
    onError: () => setError('The person could not be transferred.'),
  });
  const undo = useMutation({
    mutationFn: () =>
      postJson<unknown>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/transfer-undo`, {}, user),
    onSuccess: async () => {
      setError('');
      await queryClient.invalidateQueries({ queryKey: sheetKey(sheetId) });
    },
    onError: () => setError('The transfer could not be undone.'),
  });

  return (
    <td>
      <input
        data-testid="person-transfer-to"
        placeholder="sheet id"
        value={to}
        onChange={(e) => {
          setTo(e.target.value);
          setError('');
        }}
      />{' '}
      <button
        type="button"
        data-testid="person-transfer"
        disabled={transfer.isPending}
        onClick={() => {
          setError('');
          if (to.trim() === '') {
            setError('Give the id of the sheet to transfer to.');
            return;
          }
          once(() => transfer.mutateAsync(to.trim()));
        }}
      >
        Transfer
      </button>{' '}
      <button
        type="button"
        data-testid="person-transfer-undo"
        disabled={undo.isPending}
        onClick={() => {
          setError('');
          once(() => undo.mutateAsync());
        }}
      >
        Undo transfer
      </button>
    </td>
  );
}

// The row's `person-error` is shown by the hold cell, shared by every cell acting on the person.
export const contributions = [
  PersonHeaders.fill({ order: 23.5, Component: () => <th>Transfer</th> }),
  PersonCells.fill({ order: 23.5, Component: PersonTransfer }),
];

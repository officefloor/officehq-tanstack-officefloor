import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { sheetKey, useSheet } from './sheet';
import type { Sheet } from './sheet';

// The sheet's today: shown, and changeable. It starts as the real date.
function SheetToday({ sheet }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const { data } = useSheet(sheet);
  const [draft, setDraft] = useState('');
  const save = useMutation({
    mutationFn: (today: string) =>
      postJson<Sheet>(`/api/sheets/${encodeURIComponent(sheet)}/today`, { today }),
    onSuccess: (updated) => {
      queryClient.setQueryData(sheetKey(sheet), updated);
      // Everything on the sheet may depend on its today: re-read it all.
      void queryClient.invalidateQueries({
        predicate: (q) => q.queryKey.includes(sheet) && q.queryKey[0] !== 'sheet',
      });
    },
  });
  const send = useSubmitOnce(save);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    send(draft.trim());
  };

  return (
    <form onSubmit={submit}>
      Today: <strong data-testid="sheet-today">{data?.today ?? ''}</strong>{' '}
      <input
        data-testid="sheet-today-input"
        type="text"
        placeholder="yyyy-mm-dd"
        value={draft}
        onChange={(e) => {
          setDraft(e.target.value);
          save.reset();
        }}
      />
      <button type="submit" data-testid="sheet-today-save" disabled={save.isPending}>
        Set today
      </button>
      {save.isError && (
        <span data-testid="sheet-error" role="alert">
          {' '}That date was refused.
        </span>
      )}
    </form>
  );
}

export const contribution = SheetPanels.fill({ order: 0, Component: SheetToday });

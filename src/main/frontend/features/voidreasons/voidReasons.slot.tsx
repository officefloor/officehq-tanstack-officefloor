import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { useVoidReasons, voidReasonsKey, type VoidReason } from './useVoidReasons';

/**
 * The sheet's void reasons and a text entry to add one. While the list is not empty, a claim is
 * voided only with a reason chosen from it; a blank reason, or one already on the list, is refused.
 */
function VoidReasonsPanel({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const { data: reasons = [] } = useVoidReasons(sheet);
  const [text, setText] = useState('');
  const [invalid, setInvalid] = useState(false);

  const submitOnce = useSubmitOnce();
  const add = useMutation({
    mutationFn: (r: string) =>
      postJson<VoidReason>(`/api/sheets/${encodeURIComponent(sheet)}/void-reasons`, { reason: r, user }),
    onSuccess: async () => {
      setText('');
      await queryClient.invalidateQueries({ queryKey: voidReasonsKey(sheet) });
    },
  });

  // An error goes as soon as its form is changed or submitted again.
  const clearError = () => {
    add.reset();
    setInvalid(false);
  };

  return (
    <section data-testid="void-reasons-panel">
      <h2>Void reasons</h2>
      <form
        data-testid="void-reason-form"
        onSubmit={(e) => {
          e.preventDefault();
          const r = text.trim();
          if (r === '' || reasons.some((v) => v.reason === r)) {
            // Refused here, before anything is sent: the store and the audit file are untouched.
            add.reset();
            setInvalid(true);
            return;
          }
          setInvalid(false);
          submitOnce(() => add.mutateAsync(r));
        }}
      >
        <label>
          Reason{' '}
          <input
            data-testid="void-reason-text"
            value={text}
            onChange={(e) => {
              setText(e.target.value);
              clearError();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="void-reason-add" disabled={add.isPending}>
          Add void reason
        </button>
        {(invalid || add.isError) && (
          <span data-testid="void-reason-error" role="alert">
            {' '}
            {invalid ? 'Enter a reason not already on the list.' : 'Could not add this void reason.'}
          </span>
        )}
      </form>
      <ul data-testid="void-reason-list">
        {reasons.map((v) => (
          <li key={v.reason} data-testid={`void-reason-row-${v.reason}`}>
            {v.reason}
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 8, Component: VoidReasonsPanel });

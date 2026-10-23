import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';
import { useSheet } from '../sheet/sheet';

/** The button that generates the plans' claims: for every plan on the sheet, in the order they were added, a DRAFT
 *  claim for every plan date up to its end and the sheet's today that it has made none for yet. Shows how many were
 *  made, until the sheet's today changes (the count belongs to the today it was made under). */
function GeneratePlans({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const today = useSheet(sheetId).data?.today;
  const [last, setLast] = useState<{ count: number; today: string | undefined } | null>(null);
  const generated = last !== null && last.today === today ? last.count : null;
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const generate = useMutation({
    mutationFn: () =>
      postJson<{ generated: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/plans/generate`, { user }),
    onSuccess: async (r) => {
      // new claims move every figure on the sheet: re-read it all
      await queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
      setLast({ count: r.generated, today });
    },
    onError: (e) => setError(e instanceof HttpError && e.body ? e.body : 'Could not generate the plans’ claims.'),
    onSettled: once.done,
  });

  return (
    <div>
      <button
        type="button"
        data-testid="plan-generate"
        disabled={generate.isPending}
        onClick={() => {
          setError('');
          if (!once.start()) return;
          generate.mutate();
        }}
      >
        Generate plan claims
      </button>
      {generated !== null && (
        <>
          {' '}Generated <span data-testid="plan-generated-count">{generated}</span>
        </>
      )}
      {error && (
        <p data-testid="plan-generate-error" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}

export const contribution = SheetBody.fill({ order: 14.5, Component: GeneratePlans });

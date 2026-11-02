import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { asString, useSearchParam } from '../../url/useSearchParam';

// The date this screen previews the sheet at (search param `preview`, so it is this user's screen only). While it is
// set every read carries it (api/http.ts): every figure is made as if it were the sheet's today, over the sheet's
// current records. Changes never carry it, so they still use the sheet's real today.
function Preview() {
  const queryClient = useQueryClient();
  const [preview, setPreview] = useSearchParam('preview', asString);
  const [draft, setDraft] = useState('');
  const shown = useRef(preview);

  // Once the URL carries the new date, everything on screen is read again for it.
  useEffect(() => {
    if (shown.current === preview) return;
    shown.current = preview;
    void queryClient.resetQueries();
  }, [preview, queryClient]);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    const date = draft.trim();
    if (/^\d{4}-\d{2}-\d{2}$/.test(date) && !Number.isNaN(Date.parse(date))) setPreview(date);
  };

  return (
    <form onSubmit={submit}>
      Preview: <strong data-testid="sheet-preview">{preview}</strong>{' '}
      <input
        data-testid="preview-date"
        type="text"
        placeholder="yyyy-mm-dd"
        value={draft}
        onChange={(e) => setDraft(e.target.value)}
      />
      <button type="submit" data-testid="preview-set">
        Preview at
      </button>
      <button type="button" data-testid="preview-clear" onClick={() => setPreview(undefined)}>
        Clear preview
      </button>
    </form>
  );
}

export const contribution = SheetPanels.fill({ order: 2, Component: Preview });

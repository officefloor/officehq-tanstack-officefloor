import { useEffect, useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { AppHeader } from '../../slots/defs/appHeader';
import { useCurrentSheet, useCurrentUser, usePreview } from '../../session/session';
import { useSheet } from './sheet';

// The screen's preview date: while one is set, every figure shows the sheet's current records as if its today
// were that date. Changes still go through, on the sheet's real today. Only this screen's.
function SheetPreview() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [preview, setPreview] = usePreview();
  const loaded = useSheet(sheet);
  const [input, setInput] = useState('');
  const queryClient = useQueryClient();
  // A read not keyed by the preview date (plans, openings) is re-read when it is set or cleared.
  const was = useRef(preview);
  useEffect(() => {
    if (was.current === preview) return;
    was.current = preview;
    void queryClient.invalidateQueries({ queryKey: ['sheet'] });
  }, [preview, queryClient]);

  if (!user || sheet === '') return null;
  return (
    <form
      data-testid="preview-form"
      onSubmit={(e) => {
        e.preventDefault();
        const date = input.trim();
        if (/^\d{4}-\d{2}-\d{2}$/.test(date)) setPreview(date);
      }}
    >
      Preview <strong data-testid="sheet-preview">{loaded.data?.preview ?? ''}</strong>{' '}
      <input data-testid="preview-date" type="date" value={input} onChange={(e) => setInput(e.target.value)} />
      <button type="submit" data-testid="preview-set">
        Preview date
      </button>
      <button type="button" data-testid="preview-clear" onClick={() => setPreview(undefined)}>
        Clear preview
      </button>
    </form>
  );
}

export const contribution = AppHeader.fill({ order: 26, Component: SheetPreview });

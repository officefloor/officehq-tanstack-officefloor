import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useNavigate } from '@tanstack/react-router';
import { SheetPage } from '../../slots/defs/sheetPage';
import { usePreview } from './preview';

/** The preview date: an entry, set and clear, and the date shown (nothing while none is set). Setting or clearing it
 *  re-reads everything on the screen. */
function Preview() {
  const [preview] = usePreview();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [typed, setTyped] = useState('');
  const show = async (date: string) => {
    await navigate({
      to: '.',
      search: (prev: Record<string, unknown>) => {
        const next = { ...prev };
        if (date === '') delete next.preview;
        else next.preview = date;
        return next;
      },
      replace: true,
    });
    await queryClient.resetQueries();
  };
  return (
    <form
      data-testid="preview-form"
      onSubmit={(e) => {
        e.preventDefault();
        const date = typed.trim();
        if (/^\d{4}-\d{2}-\d{2}$/.test(date)) void show(date);
      }}
    >
      Preview today as <span data-testid="sheet-preview">{preview}</span>{' '}
      <input type="date" data-testid="preview-date" value={typed} onChange={(e) => setTyped(e.target.value)} />
      <button type="submit" data-testid="preview-set">
        Preview
      </button>{' '}
      <button type="button" data-testid="preview-clear" onClick={() => void show('')}>
        Clear
      </button>
    </form>
  );
}

export const contribution = SheetPage.fill({ order: 1.5, Component: Preview });

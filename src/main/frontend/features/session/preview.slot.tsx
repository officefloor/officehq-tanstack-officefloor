import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { AppSession } from '../../slots/defs/appSession';
import { useCurrentSheet, useCurrentUser, usePreview } from './session';

// The preview date of this screen (the `preview` search param; other users' screens are unaffected): while it is
// set, every figure on the screen is worked out as if the sheet's today were that date, over the sheet's records as
// they are now (api/http.ts, session.ts). Changes still go through on the sheet's real today, and nothing a change
// of today does (snapshots, scores, auto-closing) happens. Setting or clearing it re-reads the sheet.
const isDate = (raw: string) => /^\d{4}-\d{2}-\d{2}$/.test(raw) && !Number.isNaN(Date.parse(`${raw}T00:00:00Z`));

function PreviewDate() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [preview, setPreview] = usePreview();
  const [date, setDate] = useState('');
  const queryClient = useQueryClient();

  const change = async (next: string | undefined) => {
    await setPreview(next);
    await queryClient.resetQueries({ queryKey: ['sheet'] });
  };

  if (!user || !sheet) return null;
  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        if (isDate(date.trim())) void change(date.trim());
      }}
    >
      <span>
        Preview <strong data-testid="sheet-preview">{preview}</strong>
      </span>
      <input data-testid="preview-date" placeholder="YYYY-MM-DD" value={date} onChange={(e) => setDate(e.target.value)} />
      <button type="submit" data-testid="preview-set">
        Set preview
      </button>
      <button type="button" data-testid="preview-clear" onClick={() => void change(undefined)}>
        Clear preview
      </button>
    </form>
  );
}

export const contribution = AppSession.fill({ order: 26, Component: PreviewDate });

import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { ASOF_PARAM } from '../../api/http';
import { AppSession } from '../../slots/defs/appSession';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useCurrentSheet, useCurrentUser, useSheetInfo } from './session';

// The as-of view of this screen (the `asof` search param; other users' screens are unaffected): while
// it is set, every read of the sheet gives it as it stood at the end of that date, the date standing in
// for its today, and every change is refused (api/http.ts). Setting or clearing it re-reads the sheet.
const isDate = (raw: string) => /^\d{4}-\d{2}-\d{2}$/.test(raw) && !Number.isNaN(Date.parse(`${raw}T00:00:00Z`));

function AsOfView() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [, setAsOf] = useSearchParam(ASOF_PARAM, asString);
  const info = useSheetInfo();
  const [date, setDate] = useState('');
  const queryClient = useQueryClient();
  const shown = info.data?.id === sheet ? info.data : undefined;

  const change = async (next: string | undefined) => {
    await setAsOf(next);
    await queryClient.resetQueries({ queryKey: ['sheet', sheet] });
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
        As of <strong data-testid="sheet-asof">{shown?.asOf ?? ''}</strong>
      </span>
      <input data-testid="asof-date" placeholder="YYYY-MM-DD" value={date} onChange={(e) => setDate(e.target.value)} />
      <button type="submit" data-testid="asof-set">
        Set as-of
      </button>
      <button type="button" data-testid="asof-clear" onClick={() => void change(undefined)}>
        Clear as-of
      </button>
    </form>
  );
}

export const contribution = AppSession.fill({ order: 25, Component: AsOfView });

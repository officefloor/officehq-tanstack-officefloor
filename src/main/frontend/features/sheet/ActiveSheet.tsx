import { useQuery } from '@tanstack/react-query';
import { SheetBody } from '../../slots/defs/sheetBody';
import { claimsKey, listClaims } from '../claims/api';
import { listNotes, notesKey } from '../notes/api';
import { listPeople, peopleKey } from '../people/api';
import { useSheetId, useUser } from '../session/session';
import { openSheet, sheetKey } from './api';
import { SheetToday } from './SheetToday';
import { AsOf } from './AsOf';

/** The loaded sheet: its id, then everything kept on it (the sheet.body region). It shows once its
 *  people, claims and notes are read too, so the sheet never shows without the rows it holds. */
export function ActiveSheet() {
  const [user] = useUser();
  const [sheetId] = useSheetId();
  const sheet = useQuery({
    queryKey: sheetKey(sheetId),
    queryFn: () => openSheet(sheetId, user),
    enabled: sheetId !== '',
  });
  const people = useQuery({
    queryKey: peopleKey(sheetId),
    queryFn: () => listPeople(sheetId),
    enabled: sheet.isSuccess,
  });
  const claims = useQuery({
    queryKey: claimsKey(sheetId),
    queryFn: () => listClaims(sheetId),
    enabled: sheet.isSuccess,
  });
  const notes = useQuery({
    queryKey: notesKey(sheetId),
    queryFn: () => listNotes(sheetId),
    enabled: sheet.isSuccess,
  });

  if (sheetId === '') return <p data-testid="home-empty">No sheet loaded.</p>;
  if (sheet.isError) return <p data-testid="sheet-error">The sheet could not be loaded.</p>;
  if (!sheet.data || (!people.data && !people.isError) || (!claims.data && !claims.isError)
    || (!notes.data && !notes.isError)) return <p data-testid="sheet-loading">Loading…</p>;

  return (
    <section>
      <h2>
        Sheet <span data-testid="sheet-current-id">{sheet.data.id}</span>
      </h2>
      <SheetToday key={sheet.data.id} sheet={sheet.data} user={user} />
      <AsOf />
      <SheetBody.Slot sheetId={sheet.data.id} user={user} />
    </section>
  );
}

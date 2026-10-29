import { getJson } from '../../api/http';
import { peopleKey } from '../people/peopleQuery';

/** A crew of the sheet: its name, its lead's person id, and its members' person ids in the order added. */
export type Crew = { name: string; leadId: number; memberIds: number[] };

/** Under the people's key, so re-loading the sheet, or any change to its people, re-reads the crews. */
export const crewsKey = (sheetId: string) => [...peopleKey(sheetId), 'crews'] as const;

export const crewsQuery = (sheetId: string) => ({
  queryKey: crewsKey(sheetId),
  queryFn: () => getJson<Crew[]>(`/api/sheets/${encodeURIComponent(sheetId)}/crews`),
});

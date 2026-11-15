import { getJson } from '../../api/http';

/** The sheet's audit lines exactly as the audit file holds them, newest first; with a name (case and
 *  surrounding spaces ignored; blank: all), only those carrying name=<it> or person=<it>, followed, when that
 *  person was transferred here, by the lines about them from the sheet they came from, newest first. */
export function listAudit(sheetId: string, name: string): Promise<string[]> {
  return getJson<string[]>(`/api/sheets/${encodeURIComponent(sheetId)}/audit?name=${encodeURIComponent(name)}`);
}

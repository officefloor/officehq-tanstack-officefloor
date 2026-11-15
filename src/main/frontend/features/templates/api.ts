import { getJson, postJson } from '../../api/http';
import { sheetKey } from '../sheet/api';
import type { Claim } from '../claims/api';

/** A claim template: its name (trimmed), amount and note. */
export type ClaimTemplate = { sheetId: string; name: string; amount: string; note: string; by: string };

export type TemplateInput = { name: string; amount: string; note: string };

const sheetBase = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}`;

export const templatesKey = (sheetId: string) => [...sheetKey(sheetId), 'templates'] as const;

/** The sheet's claim templates, by name. */
export function listTemplates(sheetId: string): Promise<ClaimTemplate[]> {
  return getJson<ClaimTemplate[]>(`${sheetBase(sheetId)}/templates`);
}

/** Save a template, replacing one of the same name; refused (rejects) for a blank name or an amount the
 *  claim form would refuse. */
export function saveTemplate(sheetId: string, by: string, input: TemplateInput): Promise<ClaimTemplate> {
  return postJson<ClaimTemplate>(`${sheetBase(sheetId)}/templates?by=${encodeURIComponent(by)}`, input);
}

/** Add a DRAFT claim to a person from a template: its amount and note as they stand now, dated like a
 *  claim with no date; refused (rejects) like an add from the claim form, or for an unknown template. */
export function claimFromTemplate(sheetId: string, by: string, personId: number, template: string): Promise<Claim> {
  return postJson<Claim>(`${sheetBase(sheetId)}/people/${personId}/template-claims?by=${encodeURIComponent(by)}`, { template });
}

// The app's fetch surface. A non-2xx REJECTS, so useQuery/useMutation surface the failure instead
// of a silent `if (res.ok)` that leaves the UI showing stale data. The rejection's message is the
// server's own `error` text when it sent one.
//
// Every request carries the signed-in name in X-User (the server records it as `by=<user>`), and,
// while this screen shows the sheet as of a date, that date in X-As-Of: reads answer as the sheet stood
// then, and every change is refused; and, while this screen previews the sheet at a date, that date in
// X-Preview on every read: reads answer as the sheet is now, with that date standing in for its today.
// Changes are never sent with it, so they use the sheet's real today.
let signedInUser = '';
let asOfDate = '';
let previewDate = '';

/** A non-2xx answer: the server's message, and its status (409 when someone else saved first). */
export class HttpError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
  }
}

/** Called by the session (features/session) whenever the signed-in name changes. */
export function setSignedInUser(name: string): void {
  signedInUser = name;
}

/** Called by the as-of control (features/asof) whenever the screen's as-of date changes ('' when none). */
export function setAsOfDate(date: string): void {
  asOfDate = date;
}

/** The as-of date requests are sent with ('' when none). */
export function currentAsOfDate(): string {
  return asOfDate;
}

/** Called by the preview control (features/preview) whenever the screen's preview date changes ('' when none). */
export function setPreviewDate(date: string): void {
  previewDate = date;
}

/** The preview date reads are sent with ('' when none). */
export function currentPreviewDate(): string {
  return previewDate;
}

async function send<T>(path: string, init?: RequestInit, asOf = asOfDate, preview = ''): Promise<T> {
  const headers = new Headers(init?.headers);
  if (signedInUser) headers.set('X-User', signedInUser);
  if (asOf) headers.set('X-As-Of', asOf);
  if (preview) headers.set('X-Preview', preview);
  const res = await fetch(path, { ...init, headers });
  if (!res.ok) {
    let message = `${init?.method ?? 'GET'} ${path} -> ${res.status}`;
    try {
      const body = (await res.json()) as { error?: string };
      if (body?.error) message = body.error;
    } catch {
      /* no JSON body: keep the status message */
    }
    throw new HttpError(message, res.status);
  }
  return res.status === 204 ? (undefined as T) : ((await res.json()) as T);
}

export function getJson<T>(path: string): Promise<T> {
  return send<T>(path, undefined, asOfDate, previewDate);
}

/** A read that answers at the sheet's real today, whatever date this screen previews it at. */
export function getJsonUnpreviewed<T>(path: string): Promise<T> {
  return send<T>(path);
}

/** A read that answers as the store holds the sheet now, whatever date this screen shows it as of. */
export function getJsonNow<T>(path: string): Promise<T> {
  return send<T>(path, undefined, '');
}

export function postJson<T>(path: string, body: unknown): Promise<T> {
  return send<T>(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
}

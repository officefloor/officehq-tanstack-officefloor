// One definition of "a proper email address" for the clients feature: a local part, an @, and a
// domain with a dot — no surrounding whitespace. The form blocks submit on anything else, and the
// server (ClientsPostLogic) + DB CHECK enforce the same rule so a client cannot be saved without it.
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function isValidEmail(email: string): boolean {
  return EMAIL_PATTERN.test(email.trim());
}

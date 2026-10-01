import { useState } from 'react';
import { isValidEmail } from './email';
import { useCreateClient } from './clients';

// Add-a-client form. useState holds only what the user is currently typing; on submit it validates
// the email, and only a proper address POSTs and invalidates the ['clients'] key. A blank or
// malformed email surfaces client-form-email-error and never creates a row.
export function ClientForm() {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [emailError, setEmailError] = useState(false);
  const create = useCreateClient();

  return (
    <form
      data-testid="client-form"
      onSubmit={(event) => {
        event.preventDefault();
        if (!isValidEmail(email)) {
          setEmailError(true);
          return;
        }
        setEmailError(false);
        create.mutate(
          { name, email },
          {
            onSuccess: () => {
              setName('');
              setEmail('');
            },
          },
        );
      }}
    >
      <input
        data-testid="client-form-name"
        placeholder="Name"
        value={name}
        onChange={(event) => setName(event.target.value)}
      />
      <input
        data-testid="client-form-email"
        placeholder="Email"
        value={email}
        onChange={(event) => {
          setEmail(event.target.value);
          if (emailError) {
            setEmailError(false);
          }
        }}
      />
      {emailError && (
        <span data-testid="client-form-email-error" role="alert">
          Enter a valid email address.
        </span>
      )}
      <button data-testid="client-form-submit" type="submit">
        Add client
      </button>
    </form>
  );
}

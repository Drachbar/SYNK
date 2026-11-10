# Inloggning

Sessionsbaserad inloggning med Spring Security. Användarna läses från tabellen `app_user`
(se [databas.md](databas.md)) och lösenorden kontrolleras mot BCrypt-hashen i `password_hash`.
Konfigurationen finns i
[`WebSecurityConfig`](../backend/src/main/java/se/findersson/synk/config/WebSecurityConfig.java).

## Vad som kräver inloggning

- Allt under `/api/` kräver inloggning och svarar annars `401`.
- Allt annat (SPA:ns filer och sidor) är öppet. Frontend avgör själv vad som visas genom att
  fråga `/api/auth/me`.

## API

| Anrop                   | Kropp                                                                    | Svar                                         |
|-------------------------|--------------------------------------------------------------------------|----------------------------------------------|
| `POST /api/auth/login`  | `application/x-www-form-urlencoded` med `username` och `password`        | `204` vid lyckad inloggning, `401` annars    |
| `POST /api/auth/logout` | –                                                                        | `204`                                        |
| `GET /api/auth/me`      | –                                                                        | `200` med `{ "id", "name" }`, `401` om utloggad |

`username` är användarens `name` i `app_user`.

Sessionen identifieras av kakan `SESSION` (HttpOnly, SameSite=Lax), som webbläsaren skickar
med automatiskt. Den gäller i 30 dagar utan aktivitet (`spring.session.timeout`), och kakan
ligger kvar lika länge även om webbläsaren stängs.

## Sessioner i databasen

Sessionerna sparas i PostgreSQL av Spring Session, i tabellerna `spring_session` och
`spring_session_attributes` (se [databas.md](databas.md)). Servern håller alltså inget
inloggningstillstånd i minnet:

- Appen kan köras i flera poddar utan sticky sessions. Man kan logga in via en pod och få
  nästa anrop besvarat av en annan.
- Inloggningen överlever omstarter och deployer.
- Utgångna sessioner städas bort av ett schemalagt jobb i varje pod (en gång i minuten).

Det som sparas i sessionen är Java-serialiserat, bland annat
[`AuthenticatedUser`](../backend/src/main/java/se/findersson/synk/security/AuthenticatedUser.java).
Om den klassen ändras på ett sätt som inte är bakåtkompatibelt går gamla sessioner inte att
läsa. Töm då `spring_session` i samband med deployen, så får alla logga in igen.

CSRF-token ligger i en kaka och inte i sessionen, så den fungerar också oberoende av pod.

## CSRF

Alla anrop som ändrar något (`POST`, `PUT`, `PATCH`, `DELETE`), även inloggning och
utloggning, måste skicka med en CSRF-token. Annars blir svaret `403`.

1. Servern sätter kakan `XSRF-TOKEN` på varje svar där den saknas.
2. Frontend läser kakans värde och skickar det i headern `X-XSRF-TOKEN`.

```ts
const token = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/)?.[1] ?? '';

await fetch('/api/auth/login', {
	method: 'POST',
	headers: { 'X-XSRF-TOKEN': decodeURIComponent(token) },
	body: new URLSearchParams({ username, password })
});
```

Token byts vid inloggning och utloggning. Läs därför kakan på nytt inför varje anrop i
stället för att spara värdet, och gör ett `GET`-anrop (t.ex. `/api/auth/me`) efter inloggning
så att den nya kakan hinner sättas före nästa skrivande anrop.

## Skapa användare

Det finns ingen registrering. Användare läggs in direkt i `app_user` med en BCrypt-hash i
`password_hash`.

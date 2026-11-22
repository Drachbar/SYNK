# API

Allt under `/api/` kräver inloggning, och anrop som ändrar något kräver CSRF-token.
Se [inloggning.md](inloggning.md).

## Veckoanteckningar

Det finns en gemensam anteckning per ISO-vecka (se [databas.md](databas.md#entry)).

| Anrop                               | Svar                                                           |
|-------------------------------------|----------------------------------------------------------------|
| `GET /api/entries`                  | `200` med alla veckor som har en anteckning, nyast först       |
| `GET /api/entries/{år}/{vecka}`     | `200` med anteckningen, `204` utan kropp om veckan saknar anteckning |
| `PUT /api/entries/{år}/{vecka}`     | `200` med den sparade anteckningen, se nedan                   |

`{år}` och `{vecka}` är ISO-veckoår och veckonummer. En vecka som inte finns (t.ex. vecka 53
ett år som bara har 52) ger `400`.

### Anteckning

```json
{
  "isoYear": 2026,
  "isoWeek": 47,
  "schemaVersion": 1,
  "content": { "summary": "Lugn vecka", "meals": "Tacos" },
  "version": 3,
  "updatedAt": "2026-11-22T12:14:05Z",
  "updatedBy": "mattias"
}
```

`GET /api/entries` ger samma sak utan `schemaVersion`, `content` och `version`.

### Spara

```json
{ "version": 3, "schemaVersion": 1, "content": { "summary": "Lugn vecka, men regnig" } }
```

`content` ersätter hela det tidigare innehållet. `version` är versionen som ändringen bygger
på: värdet från `GET`, eller `null` om veckan inte hade någon anteckning. Om någon annan har
sparat sedan dess (versionen stämmer inte, eller anteckningen har skapats under tiden) blir
svaret `409` och inget sparas. Klienten får då hämta anteckningen igen.

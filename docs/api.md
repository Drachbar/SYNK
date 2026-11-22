# API

Allt under `/api/` kräver inloggning, och anrop som ändrar något kräver CSRF-token.
Se [inloggning.md](inloggning.md).

## Veckoanteckningar

Det finns en gemensam anteckning per ISO-vecka (se [databas.md](databas.md#entry)).

| Anrop                               | Svar                                                           |
|-------------------------------------|----------------------------------------------------------------|
| `GET /api/entries`                  | `200` med alla veckor som har en anteckning, nyast först       |
| `GET /api/entries/{år}/{vecka}`     | `200` med anteckningen, `204` utan kropp om veckan saknar anteckning |
| `GET /api/entries/{år}/{vecka}/previous` | `200` med den senaste anteckningen före veckan (behöver inte vara veckan innan), `204` om det inte finns någon |
| `PUT /api/entries/{år}/{vecka}`     | `200` med den sparade anteckningen, se nedan                   |

`{år}` och `{vecka}` är ISO-veckoår och veckonummer. En vecka som inte finns (t.ex. vecka 53
ett år som bara har 52) ger `400`.

### Anteckning

```json
{
  "isoYear": 2026,
  "isoWeek": 47,
  "formVersion": 2,
  "content": { "next_week": [{ "id": "b65d44353b", "text": "Panta", "done": false }] },
  "version": 3,
  "updatedAt": "2026-11-22T12:14:05Z",
  "updatedBy": "mattias"
}
```

`GET /api/entries` ger samma sak utan `formVersion`, `content` och `version`.

### Spara

```json
{ "version": 3, "formVersion": 2, "content": { "menu": { "mon": "Tacos" } } }
```

`content` ersätter hela det tidigare innehållet. Svarens format beskrivs i
[databas.md](databas.md#innehållet-i-content). `formVersion` måste finnas, och alla nycklar i
`content` måste vara fält i den versionen; annars blir svaret `400`. `version` är versionen som ändringen bygger
på: värdet från `GET`, eller `null` om veckan inte hade någon anteckning. Om någon annan har
sparat sedan dess (versionen stämmer inte, eller anteckningen har skapats under tiden) blir
svaret `409` och inget sparas. Klienten får då hämta anteckningen igen.

## Formulär

Formulärsversionerna beskrivs i [databas.md](databas.md#form_version).

| Anrop                       | Svar                                                                 |
|-----------------------------|----------------------------------------------------------------------|
| `GET /api/forms/latest`     | `200` med den senaste versionen, den som nya veckor får              |
| `GET /api/forms/{version}`  | `200` med versionen, `404` om den inte finns                         |
| `POST /api/forms`           | Sparar en definition som nästa version, se nedan                     |

```json
{ "version": 2, "definition": { "sections": [ … ] }, "createdAt": "2026-11-22T19:00:00Z" }
```

`POST` tar en definition (`{ "sections": [ … ] }`) och svarar med den nya versionen. Är
definitionen likadan som den senaste versionen skapas ingen ny, utan den senaste returneras.
En ogiltig definition (inga avsnitt, avsnitt utan rubrik, dubbla eller ogiltiga id:n, okänd typ,
`carryFrom` på något annat än en checklista eller till ett fält som inte finns) ger `400` med
`{ "message": "…" }`. Sparar två personer en ny version samtidigt får den ena `409`.

## Användare

`GET /api/users` ger alla användare som `[{ "id": "…", "name": "mattias" }]`, sorterade på
namn. Används för fälten med ett svar per person.

# Drift

SYNK körs som en jar-fil på en VM (app-VM:en), bakom nginx på en annan VM (proxy-VM:en) som
sköter HTTPS och Let's Encrypt. PostgreSQL körs på en egen databas-VM.

```
webbläsare ──HTTPS──▶ nginx (proxy-VM) ──HTTP──▶ synk.jar :8080 (app-VM) ──▶ PostgreSQL :5432 (databas-VM)
```

Vart appen ansluter styrs av `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` och
`SPRING_DATASOURCE_PASSWORD` i `/etc/synk/synk.env`. Byt värdena och kör
`sudo systemctl restart synk` för att peka om appen till en annan databas.

Allt innehåll kräver inloggning (se [inloggning.md](inloggning.md)); bara SPA:ns statiska filer
och `/login` är öppna.

## Krav på app-VM:en

- Java 25 eller senare (`java -version`).
- Nätverksåtkomst till databas-VM:en på port 5432.
- Port 8080 ska bara gå att nå från proxy-VM:en. Stäng den mot internet i brandväggen.

## Första installationen

### 1. Databasen (på databas-VM:en)

PostgreSQL 13 eller senare (för `gen_random_uuid()`). Skapa en användare och en databas som
användaren äger:

```sh
sudo -u postgres psql -n    # -n: inget i psql-historiken
```

```sql
CREATE ROLE synk LOGIN PASSWORD '<långt slumpat lösenord>';
CREATE DATABASE synk OWNER synk;
\c synk
CREATE EXTENSION IF NOT EXISTS pgcrypto;   -- för att skapa användare med SQL, se steg 4
```

Släpp in app-VM:en, och bara den:

- `postgresql.conf`: `listen_addresses` ska innehålla en adress som app-VM:en når.
- `pg_hba.conf`: `host  synk  synk  <app-VM:ens IP>/32  scram-sha-256`
- Brandväggen: port 5432 öppen bara från app-VM:en.

Ladda om Postgres (`sudo systemctl reload postgresql`). Testa från app-VM:en med
`psql -h <databas-VM> -U synk synk` om psql finns där.

Tabellerna skapas av appen (Liquibase) när den startar första gången.

Om trafiken mellan VM:arna går över ett nät du inte litar på, slå på TLS i Postgres och lägg
till `?sslmode=require` sist i `SPRING_DATASOURCE_URL`.

### 2. Appen

Bygg på din egen dator och kopiera upp:

```sh
./mvnw -DskipTests package
scp backend/target/synk-backend-0.0.1-SNAPSHOT.jar app-vm:/tmp/synk.jar
scp deploy/synk.service deploy/synk.env.example app-vm:/tmp/
```

På app-VM:en:

```sh
sudo useradd --system --home /opt/synk --shell /usr/sbin/nologin synk
sudo install -d -o synk -g synk /opt/synk
sudo install -o synk -g synk -m 644 /tmp/synk.jar /opt/synk/synk.jar

sudo install -d -m 755 /etc/synk
sudo install -m 600 /tmp/synk.env.example /etc/synk/synk.env
sudoedit /etc/synk/synk.env      # databasens adress, användare och lösenord

sudo install -m 644 /tmp/synk.service /etc/systemd/system/synk.service
sudo systemctl daemon-reload
sudo systemctl enable --now synk
journalctl -u synk -f            # vänta på "Started SynkApplication"
```

### 3. nginx på proxy-VM:en

Se [`deploy/nginx-synk.conf`](../deploy/nginx-synk.conf). Det viktiga är att `Host`,
`X-Forwarded-For` och `X-Forwarded-Proto` skickas vidare. Appen litar på de headrarna från
privata adresser (10.x, 172.16–31.x, 192.168.x, 100.64.x, localhost). Om proxy-VM:en ansluter
från en publik adress, ange den i `SERVER_TOMCAT_REMOTEIP_INTERNAL_PROXIES` i `synk.env`.

Kontrollera efteråt: `curl -sI https://<domän>/api/auth/me` ska ge `401` och en
`Set-Cookie: XSRF-TOKEN=...; Secure`.

### 4. Användare

Det finns ingen registrering. Användare läggs in direkt i databasen med en BCrypt-hash,
efter att appen har startat en gång (så att tabellerna finns).

Enklast är skriptet [`scripts/create-user.sh`](../scripts/create-user.sh). Det frågar efter
databasens adress, användare och lösenord, och sedan efter användarnamn och lösenord i SYNK.
Finns användaren redan byts lösenordet. Det kräver `psql` (`sudo apt install postgresql-client`)
och ska köras från en maskin som får ansluta till databasen enligt `pg_hba.conf`, t.ex.
app-VM:en:

```sh
scp scripts/create-user.sh app-vm:
ssh -t app-vm ./create-user.sh
```

Eller för hand på databas-VM:en:

```sh
sudo -u postgres psql -n synk
```

```sql
INSERT INTO app_user (name, password_hash) VALUES ('mattias', crypt('<lösenord>', gen_salt('bf', 10)));
```

Byta lösenord:

```sql
UPDATE app_user SET password_hash = crypt('<nytt lösenord>', gen_salt('bf', 10)), updated_at = now()
WHERE name = 'mattias';
```

Eftersom appen ligger öppen mot internet: använd långa lösenord. nginx-exemplet begränsar
antalet inloggningsförsök per IP.

## Uppdatera

```sh
./mvnw -DskipTests package
scp backend/target/synk-backend-0.0.1-SNAPSHOT.jar app-vm:/tmp/synk.jar
ssh app-vm 'sudo install -o synk -g synk -m 644 /tmp/synk.jar /opt/synk/synk.jar && sudo systemctl restart synk'
```

Databasmigreringar körs automatiskt vid start. Inloggningarna överlever omstarten.

## Säkerhetskopiering

All data ligger i databasen. Har databas-VM:en redan backup av alla databaser räcker det.
Annars en enkel daglig dump, t.ex. från cron på databas-VM:en:

```sh
sudo -u postgres pg_dump synk | gzip > /var/backups/synk-$(date +%F).sql.gz
```

Kopiera gärna dumparna till en annan maskin. Återställning: skapa databasen som i steg 1 och kör
`gunzip -c fil.sql.gz | sudo -u postgres psql synk`.

## Loggar

`journalctl -u synk`.

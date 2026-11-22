#!/usr/bin/env bash
# Skapar en användare i SYNK, eller byter lösenord om användaren redan finns.
# Kräver psql (Ubuntu: sudo apt install postgresql-client) och att appen har
# startat minst en gång, så att tabellen app_user finns. Se docs/drift.md.
#
# Lösenorden skickas till psql via standard in, inte på kommandoraden, så de
# syns inte i processlistan eller i skalets historik.

set -euo pipefail

ask() { # ask <fråga> <standardvärde>
	local answer
	read -rp "$1 [$2]: " answer
	printf '%s' "${answer:-$2}"
}

ask_secret() { # ask_secret <fråga>
	local answer
	read -rsp "$1: " answer
	echo >&2
	printf '%s' "$answer"
}

# Citerar ett värde för psql:s \set: backslash och ' dubbleras.
psql_quote() {
	local s=${1//\\/\\\\}
	printf "'%s'" "${s//\'/\'\'}"
}

command -v psql >/dev/null || { echo "psql saknas. Installera t.ex. med: sudo apt install postgresql-client" >&2; exit 1; }

echo "== Databasen"
db_host=$(ask "Värd (IP eller namn)" "localhost")
db_port=$(ask "Port" "5432")
db_name=$(ask "Databas" "synk")
db_user=$(ask "Databasanvändare" "synk")
db_password=$(ask_secret "Lösenord för $db_user")

echo
echo "== Användaren i SYNK"
name=$(ask "Användarnamn (används vid inloggning)" "")
[[ -n $name ]] || { echo "Användarnamnet får inte vara tomt." >&2; exit 1; }
password=$(ask_secret "Lösenord för $name")
[[ ${#password} -ge 8 ]] || { echo "Lösenordet måste vara minst 8 tecken." >&2; exit 1; }
[[ $password == "$(ask_secret "Upprepa lösenordet")" ]] || { echo "Lösenorden matchar inte." >&2; exit 1; }

# pgcrypto behövs för crypt()/gen_salt(). Den är "trusted" sedan PostgreSQL 13,
# så databasens ägare får skapa den.
PGPASSWORD=$db_password psql -X -q -v ON_ERROR_STOP=1 \
	-h "$db_host" -p "$db_port" -U "$db_user" -d "$db_name" <<SQL
\set name $(psql_quote "$name")
\set password $(psql_quote "$password")
CREATE EXTENSION IF NOT EXISTS pgcrypto;
INSERT INTO app_user (name, password_hash)
VALUES (:'name', crypt(:'password', gen_salt('bf', 10)))
ON CONFLICT ((lower(name))) DO UPDATE
    SET password_hash = EXCLUDED.password_hash, updated_at = now()
RETURNING CASE WHEN xmax = 0 THEN 'Skapade användaren ' ELSE 'Bytte lösenord för ' END || name AS resultat;
SQL

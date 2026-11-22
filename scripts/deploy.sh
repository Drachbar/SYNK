#!/usr/bin/env bash
# Bygger SYNK och driftsätter den på app-VM:en. Körs på din egen dator, från
# var som helst i projektet. Se docs/drift.md.
#
#   ./scripts/deploy.sh                    bygg med tester, driftsätt till ubuntu_general
#   ./scripts/deploy.sh --skip-tests       bygg utan tester
#   ./scripts/deploy.sh annan-vm           driftsätt till en annan ssh-värd
#
# Värden kan också sättas med SYNK_DEPLOY_HOST. Den ska vara ett namn i
# ~/.ssh/config (eller user@ip). sudo-lösenordet på VM:en efterfrågas en gång.

set -euo pipefail

host=${SYNK_DEPLOY_HOST:-ubuntu_general}
skip_tests=false
for arg in "$@"; do
	case $arg in
		--skip-tests) skip_tests=true ;;
		-h | --help) sed -n '2,11p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
		-*) echo "Okänd flagga: $arg" >&2; exit 1 ;;
		*) host=$arg ;;
	esac
done

root=$(cd "$(dirname "$0")/.." && pwd)
cd "$root"
jar=backend/target/synk-backend-0.0.1-SNAPSHOT.jar
version=$(git describe --always --dirty 2>/dev/null || echo okänd)

echo "==> Bygger $version"
if $skip_tests; then
	./mvnw -q -DskipTests package
else
	# Backend-testerna kör mot den lokala databasen i compose.yaml.
	./mvnw -q verify || {
		echo "Bygget eller testerna misslyckades. Är den lokala databasen igång (podman compose up -d)?" >&2
		echo "Hoppa över testerna med --skip-tests." >&2
		exit 1
	}
fi

echo "==> Kopierar till $host"
scp -q "$jar" "$host:/tmp/synk.jar"

echo "==> Installerar och startar om"
# Allt på VM:en i en session, så att sudo bara frågar en gång.
ssh -t "$host" '
	set -euo pipefail
	sudo install -o synk -g synk -m 644 /tmp/synk.jar /opt/synk/synk.jar
	rm -f /tmp/synk.jar
	sudo systemctl restart synk

	port=$(sudo sed -n "s/^SERVER_PORT=//p" /etc/synk/synk.env)
	port=${port:-8080}
	printf "Väntar på att appen svarar på port %s" "$port"
	for _ in $(seq 1 60); do
		# 401 = uppe och kräver inloggning, som den ska.
		if [ "$(curl -s -o /dev/null -w "%{http_code}" "localhost:$port/api/auth/me")" = 401 ]; then
			echo " klart."
			exit 0
		fi
		printf .
		sleep 1
	done
	echo " gav upp efter 60 s. Senaste loggen:"
	sudo journalctl -u synk -n 40 --no-pager
	exit 1
'

echo "==> $version är driftsatt på $host"

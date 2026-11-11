#!/bin/bash
# Controllo sintattico degli sketch Waspmote con avr-g++ (senza IDE).
# Non produce un binario da caricare: per quello usate la Waspmote PRO IDE.
#
# Requisiti: gcc-avr, avr-libc e le API Libelium
#   git clone --depth 1 https://github.com/Libelium/waspmoteapi /opt/waspmoteapi
# Uso: ./firmware/check.sh [file.pde ...]
set -euo pipefail

API="${WASPMOTE_API:-/opt/waspmoteapi}"
if [ ! -d "$API/waspmote-api" ]; then
  echo "API Waspmote non trovate in $API (impostare WASPMOTE_API)" >&2
  exit 1
fi

cd "$(dirname "$0")"
if [ $# -eq 0 ]; then
  set -- */*.pde
fi

INC=(-I"$API/waspmote-api")
for d in "$API"/libraries/*/; do INC+=(-I"$d"); done

status=0
for sketch in "$@"; do
  # La IDE tratta i .pde come C++ e aggiunge in testa WaspClasses.h
  if avr-g++ -x c++ -mmcu=atmega1281 -DF_CPU=14745600L -Os -fsyntax-only \
       -include WaspClasses.h "${INC[@]}" "$sketch"; then
    echo "OK      $sketch"
  else
    echo "ERRORE  $sketch"
    status=1
  fi
done
exit $status

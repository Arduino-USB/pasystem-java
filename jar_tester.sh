#!/bin/bash

JAR=""
SCRIPT=""

while [[ $# -gt 0 ]]; do
	case "$1" in
		--jar)
			JAR="$2"
			shift 2
			;;
		--script)
			SCRIPT="$2"
			shift 2
			;;
		*)
			echo "Unknown argument: $1"
			echo "Usage: $0 --jar <jar-file> --script <jsh-file>"
			exit 1
			;;
	esac
done

if [ -z "$JAR" ] || [ -z "$SCRIPT" ]; then
	echo "Usage: $0 --jar <jar-file> --script <jsh-file>"
	exit 1
fi

TMP=$(mktemp -d)

trap 'rm -rf "$TMP"' EXIT


(
	cd "$TMP"
	jar xf "$OLDPWD/$JAR" BOOT-INF/classes BOOT-INF/lib
)

jshell -q \
	--class-path "$TMP/BOOT-INF/classes:$TMP/BOOT-INF/lib/*" \
	"$SCRIPT"

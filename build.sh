#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
export JAVA_HOME="${JAVA_HOME:-$PWD/tools/jdk}"
export ANDROID_HOME="${ANDROID_HOME:-$PWD/tools/android}"
export PATH="$JAVA_HOME/bin:$PATH"
if [[ -z "${SC_KEYSTORE_PASSWORD:-}" ]]; then
 echo "Set SC_KEYSTORE_PASSWORD to your signing keystore password before building." >&2
 exit 1
fi
export SC_KEYSTORE_PASSWORD
BT="$ANDROID_HOME/build-tools/35.0.0"
JAR="$ANDROID_HOME/platforms/android-35/android.jar"
mkdir -p build/compiled build/generated build/classes build/dex dist
# Delete only build outputs owned by this project.
find build/compiled build/generated build/classes build/dex -type f -delete
"$BT/aapt2" compile --dir res -o build/compiled
"$BT/aapt2" link -o build/resources.apk -I "$JAR" --manifest AndroidManifest.xml --java build/generated --min-sdk-version 26 --target-sdk-version 34 build/compiled/*.flat
find src build/generated -name '*.java' > build/sources.txt
javac --release 8 -Xlint:-options -encoding UTF-8 -cp "$JAR" -d build/classes @build/sources.txt
find build/classes -name '*.class' > build/classes.txt
"$BT/d8" --lib "$JAR" --min-api 26 --output build/dex @build/classes.txt
python3 - <<'PY'
from zipfile import ZipFile
with ZipFile('build/resources.apk','a') as z:
 z.write('build/dex/classes.dex','classes.dex')
PY
"$BT/zipalign" -f 4 build/resources.apk dist/ShortCircuit-unsigned.apk
SIGNING_KEY="${SIGNING_KEY:-$PWD/build.keystore}"
if [ ! -f "$SIGNING_KEY" ]; then
 keytool -genkeypair -keystore "$SIGNING_KEY" -storepass:env SC_KEYSTORE_PASSWORD -keypass:env SC_KEYSTORE_PASSWORD -alias shortcircuit -keyalg RSA -keysize 3072 -validity 3650 -dname 'CN=Short Circuit,O=Personal Android Tools,C=NL'
fi
"$BT/apksigner" sign --ks "$SIGNING_KEY" --ks-key-alias shortcircuit --ks-pass env:SC_KEYSTORE_PASSWORD --key-pass env:SC_KEYSTORE_PASSWORD --out dist/ShortCircuit-v1.5.apk dist/ShortCircuit-unsigned.apk
"$BT/apksigner" verify --verbose --print-certs dist/ShortCircuit-v1.5.apk
"$BT/aapt2" dump badging dist/ShortCircuit-v1.5.apk > build/apk-info.txt
head -12 build/apk-info.txt

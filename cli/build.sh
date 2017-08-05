#/bin/sh

tx pull -a
# Workaround for Android not supporting this language (no ISO-639-2 support?)
rm locale/*-ast.po
# Previously,  I needed to define on OSX: export LC_CTYPE=en_US.utf-8
a2po import

buck build //:app
buck build //:zipalign

VERSION=$(aapt d badging ./buck-out/gen/zipalign/zipalign.apk | grep 'versionName=' | awk -F: 'match($0,"versionName="){ print substr($2,RSTART+4)}' | tr -d "'")
cp ./buck-out/gen/zipalign/zipalign.apk Android-Autostarts-Full-$VERSION.apk
cp ./buck-out/gen/zipalign/zipalign.apk ./buck-out/gen/zipalign/Android-Autostarts-Full.apk


( cd ./buck-out/gen/zipalign/ && bundlecop submit \
  --projectKey $BUNDLECOP_AUOSTARTS_PROJECT_KEY \
  --bundleset 268318521568755716 \
  Android-Autostarts-Full.apk )

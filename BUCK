android_build_config(
  name = 'build_config',
  package = 'com.elsdoerfer.android.autostarts',
  values = [
  ],
)


android_resource(
  name = 'res',
  res = 'res',
  package = 'com.elsdoerfer.android.autostarts',
  visibility = [
    'PUBLIC'
  ],
)

android_library(
  name = 'java',
  srcs = glob([
    'src/**/*.java',
    'src-opt/default/**/*.java',
  ]),
  deps = [':res', ':build_config'],
  visibility = [ 'PUBLIC' ],
)


android_binary(
  name = 'app',
  manifest = 'AndroidManifest.xml',
  keystore = ':production_keystore',
  deps = [
    ':java',
    ':res',
  ],
)

keystore(
  name = 'production_keystore',
  store = 'production.keystore',
  properties = 'production.keystore.properties',
)

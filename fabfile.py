import time, re, tempfile
from os import path, unlink
import logging

from fabric.api import local, env, put, run
from fabric.main import load_settings
from android.build import AndroidProject, get_platform, ProgramFailedError


settings = load_settings('.fabricrc')
if not settings:
    raise RuntimeError('.fabricrc is needed')
env.update(settings)
env.hosts = [env.deploy_host]


logging.getLogger('py-androidbuild').addHandler(logging.StreamHandler())


def locales():
    local('tx pull -a')
    # Workaround for Android not supporting this language (no ISO-639-2 support?)
    local('rm locale/*-ast.po')


def _make_market_manifest(manifest):
    """Stupid Android Market fails with mysterious error message
    ('cannot process apk' or something of the like) if the manifest
    contains empty uses-configuration and uses-feature tags.
    Amazon market fails if they do NOT exist.
    """
    manifest_text = open(manifest, 'r').read()
    manifest_text = re.sub(r'<uses-configuration\s*/>', '', manifest_text)
    manifest_text = re.sub(r'<uses-feature\s*/>', '', manifest_text)
    # aapt will fail if the Manifest is not named AndroidManifest.xml
    new_manifest = path.join(tempfile.mkdtemp(), 'AndroidManifest.xml')
    with open(new_manifest, 'w') as f:
        f.write(manifest_text)
    return new_manifest


BUILDS = {
    'amazon': {
        'extra-paths': ['src-opt/amazon'],
        'template': 'com.elsdoerfer.android.autostarts-%(version)s-AD.apk',
        'sign': False,
        'deploy': False,
    },
    'vodaphone': {
        'extra-paths': ['src-opt/vodaphone'],
        'template': 'Android-Autostarts-Vodaphone-%(version)s.apk',
        # Vodaphone QA complains about incomplete translations
        'a2po_options': '--require-min-complete 1',
    },
    'default': {
        'extra-paths': ['src-opt/default'],
        'template': 'Android-Autostarts-Full-%(version)s.apk',
        'manifest_maker': _make_market_manifest
    }
}


def build(build_only=None, clean=False):
    apks = []
    for build_name, build_options in BUILDS.items():
        if build_only and not build_name == build_only:
            continue

        print
        header = "Building APK for version '%s'..." % build_name
        print header
        print "=" * len(header)
        print

        try:
            # Import appropriate translations for this build
            local('a2po import %s' % build_options.get('a2po_options', ''))

            # Determine manifest file, build can run a hook
            manifest = 'AndroidManifest.xml'
            if build_options.get('manifest_maker', False):
                manifest = build_options['manifest_maker'](manifest)

            # Setup a build project
            p = AndroidProject(manifest, 'Android-Autostarts',
                               project_dir='.',
                               sdk_dir=env.sdk_dir)
            p.extra_source_dirs = build_options['extra-paths']
            p.extra_jars = ['%s/extras/android/support/v4/android-support-v4.jar' % env.sdk_dir]

            # Build the project
            if clean:
                p.clean()
            version = p.manifest_parsed.attrib[
                '{http://schemas.android.com/apk/res/android}versionName']
            apk = p.build('%s' % build_options['template'] % {
                'build': build_name.capitalize(),
                'version': version
            })

            # Optionally sign the API file
            if build_options.get('sign', True):
                password = raw_input('Enter keystore password:')
                if not password:
                    print "Not signing the package."
                else:
                    apk.sign(env.keystore, env.keyalias, password)

            # Align
            apk.align()

            apks.append((apk, build_options))
            print
            print "==> Generated %s" % apk.filename
        except ProgramFailedError, e:
            print e
            print e.stdout
    return apks


def deploy(build_only=None):
    if raw_input('Have you updated the locales? [y/n] ') != 'y':
        print "Then do that first, and commit them...."
        return

    # Build all APKs, upload them all the the server
    for apk, build_options in build(build_only=build_only, clean=True):
        if build_options.get('deploy', True):
            put(apk.filename, env.deploy_dir)

    put('CHANGES', env.deploy_dir)
    run('sudo /etc/init.d/lighttpd restart')

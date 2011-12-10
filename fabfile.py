import time
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
     local('a2po import')


BUILDS = {
    'amazon': {
        'extra-paths': ['src-opt/amazon'],
        'template': 'com.elsdoerfer.android.autostarts-%(version)s-AD.apk',
        'sign': False,
        'deploy': False,
    },
    'default': {
        'extra-paths': ['src-opt/amazon'],
        'template': 'Android-Autostarts-Full-%(version)s.apk',
    }
}


def build(clean=False):
     apks = []
     for build_name, build_options in BUILDS.items():
         print "Building APK for version '%s'..." % build_name
         try:
             p = AndroidProject('AndroidManifest.xml', 'Android-Autostarts',
                                sdk_dir=env.sdk_dir)
             p.extra_source_dirs = build_options['extra-paths']
             p.extra_jars = ['%s/extras/android/support/v4/android-support-v4.jar' % env.sdk_dir]

             if clean:
                 p.clean()

             version = p.manifest_parsed.attrib['{http://schemas.android.com/apk/res/android}versionName']
             apk = p.build('bin/%s' % build_options['template'] % {
                 'build': build_name.capitalize(),
                 'version': version
             })

             if build_options.get('sign', True):
                 password = raw_input('Enter keystore password:')
                 if not password:
                      print "Not signing the package."
                 else:
                      apk.sign(env.keystore, env.keyalias, password)
             apk.align()
             apks.append((apk, build_options))
         except ProgramFailedError, e:
             print e
             print e.stdout
     return apks


def deploy():
    locales()

    for apk, build_options in build(clean=True):
        if build_options.get('deploy', True):
            put(apk.filename, env.deploy_dir)

    put('CHANGES', env.deploy_dir)
    run('sudo /etc/init.d/lighttpd restart')


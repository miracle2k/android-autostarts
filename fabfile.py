import time
from os import path, unlink
import logging

from fabric.api import local, env
from fabric.main import load_settings
from android.build import AndroidProject, get_platform, ProgramFailedError


env.raw_apk = 'bin/Android-Autostarts-%s.apk'
settings = load_settings('.fabricrc')
if not settings:
     raise RuntimeError('.fabricrc is needed')
env.update(settings)


logging.getLogger('py-androidbuild').addHandler(logging.StreamHandler())


def locales():
     local('tx pull -a')
     # Workaround for Android not supporting this language (no ISO-639-2 support?)
     local('rm locale/*-ast.po')
     local('a2po import')


BUILDS = {
    'amazon': {
        'extra-paths': ['src-opt/amazon'],
    },
    'default': {
        'extra-paths': ['src-opt/amazon'],
    }
}


def build():
     apks = []
     for build_name, build_options in BUILDS.items():
         print "Building APK for version '%s'..." % build_name
         try:
             p = AndroidProject('AndroidManifest.xml', 'Android-Autostarts',
                                sdk_dir=env.sdk_dir)
             p.extra_source_dirs = build_options['extra-paths']
             p.extra_jars = ['%s/extras/android/support/v4/android-support-v4.jar' % env.sdk_dir]

             apk = p.build(env.raw_apk % build_name.capitalize())

             password = raw_input('Enter keystore password:')
             if not password:
                  print "Not signing the package."
             else:
                  apk.sign(env.keystore, env.keyalias, password,)
             apk.align()
             apks.append(_versionize(apk.filename))
         except ProgramFailedError, e:
             print e
             print e.stdout
     return apks


def _versionize(filename):
    if not getattr(env, 'app_version', False):        
        temp_project = AndroidProject('AndroidManifest.xml', sdk_dir=env.sdk_dir)
        env.app_version = temp_project.manifest_parsed.attrib['{http://schemas.android.com/apk/res/android}versionName']

    # add version number to file name
    base, ext = path.splitext(filename)
    target_apk = '%s-%s%s' % (base, env.app_version, ext)

    # move to final location
    local('mv %s %s' % (filename, target_apk))
    return target_apk


def deploy():
    locales()
    for apk_filename in build():
        local('scp -P 2211 %s %s' % (apk_filename, env.deploy_target))


import time
from os import path, unlink
import logging

from fabric.api import local, env
from fabric.main import load_settings
from android.build import AndroidProject, get_platform, ProgramFailedError


env.raw_apk = 'bin/Android-Autostarts-Full.apk'
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


def build():
     print "Building the APK."
     try:
         p = AndroidProject('AndroidManifest.xml', 'Android-Autostarts',
                            sdk_dir=env.sdk_dir, target='10')
         apk = p.build(env.raw_apk)
         password = raw_input('Enter keystore password:')
         if not password:
              print "Not signing the package."
         else:
              apk.sign(env.keystore, env.keyalias, password,)
         apk.align()
     except ProgramFailedError, e:
         print e
         print e.stdout


def make_version():
    # determine the version number
    import subprocess, re
    manifest = subprocess.Popen(
        ["aapt", "dump", "xmltree", env.raw_apk, "AndroidManifest.xml"],
        stdout=subprocess.PIPE).communicate()[0]
    env.version = version = re.search(r'^\s*A: android:versionName\([^)]+\)="([^"]+)" \(Raw: "[^"]+"\)\s*$', manifest, re.M).groups()[0]
    # add version number to file name
    base, ext = path.splitext(env.raw_apk)
    env.target_apk = '%s-%s%s' % (base, version, ext)

    # move to final location
    local('mv %s %s' % (env.raw_apk, env.target_apk))


def make_ini():
    # Create an .ini section for our site
    base, ext = path.splitext(env.target_apk)
    ini = open('%s.ini' % base, 'w')
    ini.write("[%s]\n" % env.version)
    ini.write("version=%s\n" % env.version)
    ini.write("timestamp=%s\n" % int(time.time()))
    ini.write("size_apk=%s\n" % path.getsize(env.target_apk))
    ini.close()
    pass


def deploy():
    locales()
    build()
    make_version()
    make_ini()

    local('scp -P 2211 %s %s' % (env.target_apk, env.deploy_target))


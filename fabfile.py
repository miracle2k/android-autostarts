from fabric.api import local, env

env.raw_apk = 'bin/Android-Autostarts-release.apk'


import time
from os import path, unlink


def build():
    # build the app
    local('ant release', capture=False)
    

def version():
    build()
   
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
    

def make():
    version()

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
    make()

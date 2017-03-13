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

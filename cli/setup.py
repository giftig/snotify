import os
import re
from setuptools import setup


def read_version():
    cur_path = os.path.abspath(os.path.dirname(__file__))
    init_path = os.path.join(cur_path, 'snotty/__init__.py')

    with open(init_path, 'rb') as f:
        for l in f.readlines():
            l = l.strip()
            if '__version__' not in l:
                continue

            version = re.match('__version__ *= *[\'"]([^\'$]+)[\'"]$', l)
            if version:
                return version.string

    raise Exception('Could not identify version')


setup(
    name='snotty',
    version=read_version(),
    packages=['snotty'],
    entry_points={
        'console_scripts': [
            'snotty=snotty.send_message:main'
        ]
    },
    install_requires=['requests']
)

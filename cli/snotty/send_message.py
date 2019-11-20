#!/usr/bin/env python2.7

import argparse
import datetime
import fileinput
import os
import re
import sys
import traceback
import uuid

import requests

RELATIVE_TIME_PATTERN = re.compile(
    r'^(?:\+|in | )?([0-9]+) ?(m|h|d|s)(.*)$'
)
TIME_UNITS = {
    'd': 'days',
    'm': 'minutes',
    'h': 'hours',
    's': 'seconds'
}

DATE_FMT = '%Y-%m-%dT%H:%M:%SZ'


class Snotty(object):
    """Send a message to gnotify"""

    def __init__(self, host='localhost', port=6666):
        self.host = host
        self.port = port

    def send_message(
        self, msg_id, title, body, priority, recipient, when=None
    ):
        """
        Send a notification to gnotify with the given details

        N.B. Snotify assumes all timestamps to be UTC, so a UTC datetime
        must be provided.
        """
        now = datetime.datetime.utcnow()
        when = when or now

        data = {
            'id': msg_id,
            'title': title,
            'body': body.strip(),
            'priority': priority,
            'targets': [recipient],
            'complete': False,
            'trigger_time': when.strftime(DATE_FMT),
            'creation_time': now.strftime(DATE_FMT)
        }

        response = requests.post(
            'http://%s:%d/notification' % (self.host, self.port),
            json=data
        )

        status = response.status_code

        if status < 200 or status > 299:
            raise Exception('Unexpected response from server: %d' % status)


def _clean_time(t):
    """Be smart interpreting time strings"""
    if not t or t == 'now':
        return datetime.datetime.utcnow()

    try:
        return datetime.datetime.strptime(t, DATE_FMT)
    except ValueError:
        pass

    try:
        return datetime.datetime.combine(
            datetime.datetime.today(),
            datetime.datetime.strptime(t, '%H:%M:%S').time()
        )
    except ValueError:
        pass

    def _extract_delta(s):
        match = RELATIVE_TIME_PATTERN.match(s)
        if not match:
            return None

        n = int(match.group(1))
        units = match.group(2)[0]
        remainder = match.group(3)

        delta = datetime.timedelta(**{TIME_UNITS[units]: n})

        if remainder:
            delta += _extract_delta(remainder)

        return delta

    delta = _extract_delta(t)
    if delta:
        return datetime.datetime.utcnow() + delta

    raise Exception('Could not interpret time "%s"' % t)


def main():
    parser = argparse.ArgumentParser(
        description='Send a message to the given user. Processes stdin.'
    )
    parser.add_argument(
        '--host', dest='host',
        default=os.environ.get('SNOTTY_HOST', 'localhost')
    )
    parser.add_argument(
        '--port', dest='port', type=int,
        default=int(os.environ.get('SNOTTY_PORT', 6666))
    )
    parser.add_argument('-i', '--id', dest='id', default=str(uuid.uuid4()))
    parser.add_argument('-t', '--title', dest='title', required=True)
    parser.add_argument(
        '-p', '--priority', dest='priority', type=int, default=50
    )
    parser.add_argument(
        '-r', '--recipient', dest='recipient',
        default=os.environ.get('SNOTTY_RECIPIENT')
    )
    parser.add_argument(
        '-w', '--when', '--time', dest='time', default='now',
        help=(
            'Must be UTC and in format %%Y-%%m-%%dT%%H:%%M:%%SZ OR an '
            'expression representing a time relative to now'
        )
    )
    parser.add_argument(
        '-f', '--file', dest='file', type=str, default=None,
        help='Read message from file instead of providing -m'
    )
    parser.add_argument(
        '-m', '--message', dest='msg', type=str, default=None,
        help='The message body'
    )

    args = parser.parse_args()

    if args.msg and args.file:
        raise Exception('Cannot use -m and -f together')

    if not args.msg and not args.file:
        raise Exception('Must specify a message body with either -m or -f')

    body = args.msg or (
        ''.join([line for line in fileinput.input(args.file)])
    )

    try:
        Snotty(host=args.host, port=args.port).send_message(
            msg_id=args.id,
            title=args.title,
            body=body,
            priority=args.priority,
            recipient=args.recipient,
            when=_clean_time(args.time)
        )
    except Exception:
        print('\x1b[1;31m')
        traceback.print_exc()
        print('\x1b[0m')
        sys.exit(1)


if __name__ == '__main__':
    main()

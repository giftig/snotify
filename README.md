# Snotify

A notification application written in Scala with akka actors, streams, and alpakka. Consumes
notification messages from a rabbitmq queue and schedules various forms of alerts to happen
to notify the recipients, eg. play a sound, use notify-send, push to a phone via the
Pushover app, etc.

This is essentially a rewrite of gnotify, a similar application I wrote a few years ago using
Golang. It's currently missing a couple more features I haven't ported yet, including
integrating with a google calendar in order to produce notifications, providing a REST API to
easily schedule a message, and clustering capabilities to pass on messages amongst peers,
but will gain those features shortly.

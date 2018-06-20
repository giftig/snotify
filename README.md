# Snotify

A notification application written in Scala with akka actors, streams, and alpakka. Consumes
notification messages from a rabbitmq queue and schedules various forms of alerts to happen
to notify the recipients, eg. play a sound, use notify-send, push to a phone via the
Pushover app, etc.

## TODO
* Provide a proper database layer; file-based persistence is probably ok for basic use as a
  desktop client, but a server will need a decent db
* Add note searching capabilities backed by whatever database is used
* Support google calendar integration as a notification source
* Finish the cluster logic -- there are a couple of unhandled cases around acking messages
  when they've been passed on to peers only, and I need to ensure other suitable notification
  sources get pushed through the peer routing component (e.g. I should be able to register
  a notification via REST on node X and have it passed on to node Y, not just scheduled
  on the local node)
* Add e-mail as an alert type
* Support templates for notification display (especially relevant to e-mails)

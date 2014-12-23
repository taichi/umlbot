# UMLbot

UML genrator bot for Slack on top PlantUML

## Let's try

1. setup [outgoing-webhook](https://my.slack.com/services/new/outgoing-webhook)
2. [![Deploy](https://www.herokucdn.com/deploy/button.png)](https://heroku.com/deploy)
3. set URL to Heroku. URL same as Heroku application name or your domain name.
4. set TOKEN to Heroku from outgoing-webhook
5. set URL(s) to outgoing-webhook same as URL

## License

GPL v3

## Required Environment

Java8 or higher

## Build

    ./gradlew installApp

## Test

    ./gradlew test

## CI status

[![wercker status](https://app.wercker.com/status/c1ba9b381bde8b76b181c3d4a1cc90d0/m "wercker status")](https://app.wercker.com/project/bykey/c1ba9b381bde8b76b181c3d4a1cc90d0)

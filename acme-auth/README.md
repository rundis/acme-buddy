# Acme Auth

This sample service application provides rest services for handlng user authentication.


## Preconditions
* Leiningen > 2.0
* Java jdk 1.8

## Running the app

``` lein ring server-headless```

The app will by default start on port 6001

You will also need to:
* Start the [acme-webstore](../acme-webstore/) app
* Start the [acme-catalog](../acme-catalog/) app


## Tech specs
* Uses a h2 in memory database for users/roles/accessrights
* db schema and sample data seeded on startup

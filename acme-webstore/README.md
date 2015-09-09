# Acme Webstore

This sample app will talk to the authentication and catalogue applications - acting as a front end for the user.


## Preconditions
* Leiningen > 2.0
* Java jdk 1.7

## Running the app

``` lein ring server```

The app will by default start on port 6002, and your browser shoud open to http://localhost:6002


You will also need to:
* Start the [acme-auth](../acme-auth/) app
* Start the [acme-catalog](../acme-catalog/) app



## Using
* To act as a regular customer login with "test/secret"
* To act as an store admin use "admin/secret"

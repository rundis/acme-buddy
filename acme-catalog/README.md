# Acme Catalog

This sample app will provide a catalog of goods. The acme-webstore app gets data from here to present to the user


## Preconditions
* Leiningen > 2.0
* Java jdk 1.7

## Running the app

``` lein ring server-headless```

The app will by default start on port 6003. The acme-webstore app will then communicate with it.


You will also need to:
* Start the [acme-auth](../acme-auth/) app
* Start the [acme-webstore](../acme-webstore/) app



## Using
* No interaction necessary from you.

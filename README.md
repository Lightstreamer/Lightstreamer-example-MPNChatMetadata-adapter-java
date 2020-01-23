# Lightstreamer - MPN Basic Chat Demo - Java Adapter

<!-- START DESCRIPTION lightstreamer-example-mpnchat-adapter-java -->

This project includes the resources needed to develop a Metadata Adapter for the [Lightstreamer - Basic Chat Demo - iOS Client - Swift](https://github.com/Lightstreamer/Lightstreamer-example-Chat-client-ios-swift) that is pluggable into Lightstreamer Server.

The *Lightstreamer Basic Chat Demo* is a very simple chat application based on [Lightstreamer](http://www.lightstreamer.com) for its real-time communication needs. It supports Mobile Push Notifications (MPN).

## Details

The project is comprised of source code and a deployment example.

### Dig the Code

The MPN Chat Metadata Adapter is comprised of one Java class.

#### MPNChatMetadataAdapter

The Metadata Adapter inherits from the reusable [LiteralBasedProvider](https://github.com/Lightstreamer/Lightstreamer-example-ReusableMetadata-adapter-java) and just adds a simple support for message submission. It should not be used as a reference for a real case of client-originated message handling, as no guaranteed delivery and no clustering support is shown.

In addition the code shows a few best practices to ensure safety of Mobile Push Notifications (MPN) activity. See the source code comments for further details.

<!-- END DESCRIPTION lightstreamer-example-mpnchat-adapter-java -->

### The Adapter Set Configuration

The Data Adapter functionalities are absolved by the `ChatDataAdapter`, a full implementation of a Data Adapter, explained in [Lightstreamer - Basic Chat Demo - Java Adapter](https://github.com/Lightstreamer/Lightstreamer-example-Chat-adapter-java).

This Adapter Set is configured and will be referenced by the clients as `CHAT`.

The `adapters.xml` file for the Basic Chat Demo, should look like:

```xml
<?xml version="1.0"?>

<adapters_conf id="CHAT">

  <!--
    Not all configuration options of an Adapter Set are exposed by this file.
    You can easily expand your configurations using the generic template,
    `DOCS-SDKs/sdk_adapter_java_inprocess/doc/adapter_conf_template/adapters.xml`,
    as a reference.
  -->

    <metadata_adapter_initialised_first>Y</metadata_adapter_initialised_first>

    <metadata_provider>

        <adapter_class>chat_demo.adapters.MPNChatMetadataAdapter</adapter_class>

        <!-- Optional for MPNChatMetadataAdapter.
             Configuration file for the Adapter's own logging.
             Logging is managed through log4j. -->
        <param name="log_config">adapters_log_conf.xml</param>
        <param name="log_config_refresh_seconds">10</param>

        <!-- Optional, managed by the inherited LiteralBasedProvider.
             See LiteralBasedProvider javadoc. -->
        <!--
        <param name="max_bandwidth">40</param>
        <param name="max_frequency">3</param>
        <param name="buffer_size">30</param>
        <param name="prefilter_frequency">5</param>
        <param name="allowed_users">user123,user456</param>
        -->
        <param name="distinct_snapshot_length">30</param>

        <!-- Optional, managed by the inherited LiteralBasedProvider.
             See LiteralBasedProvider javadoc. -->
        <param name="item_family_1">chat_room.*</param>
        <param name="modes_for_item_family_1">DISTINCT</param>

    </metadata_provider>


    <data_provider name="CHAT_ROOM">

        <adapter_class>chat_demo.adapters.ChatDataAdapter</adapter_class>

        <!-- Optional for ChatDataAdapter.
             Configuration file for the Adapter's own logging.
             Leans on the Metadata Adapter for the configuration refresh.
             Logging is managed through log4j. -->
        <param name="log_config">adapters_log_conf.xml</param>

        <!-- Optional for ChatDataAdapter.
             Configuration flag for periodic flush of the snapshot.
             Default: false. -->
        <param name="flush_snapshot">true</param>

        <!-- Optional for ChatDataAdapter.
             Configuration interval in millis for snapshot flush.
             Default: 30 minutes. -->
        <!-- <param name="flush_snapshot_interval">1800000</param> -->
    </data_provider>


</adapters_conf>
```

<i>NOTE: not all configuration options of an Adapter Set are exposed by the file suggested above.
You can easily expand your configurations using the generic template, `DOCS-SDKs/sdk_adapter_java_inprocess/doc/adapter_conf_template/adapters.xml`, as a reference.</i>

Please refer to the *General Concepts* document (available under `DOCS-SDKs` in your Lightstreamer Server installation) for more details about Lightstreamer Adapters.

## Install

If you want to install a version of the *Chat Demo* in your local Lightstreamer Server, follow these steps:

* Download *Lightstreamer Server Version 7.0 or greater* (Lightstreamer Server comes with a free non-expiring demo license for 20 connected users) from [Lightstreamer Download page](http://www.lightstreamer.com/download.htm), and install it, as explained in the `GETTING_STARTED.TXT` file in the installation home directory.
* Make sure that Lightstreamer Server is not running.

### Installing the Adapter

* Get the `deploy.zip` file of the [Metadata Adapter latest release](https://github.com/Lightstreamer/Lightstreamer-example-MPNChatMetadata-adapter-java/releases), unzip it, and copy the `Chat` folder into the `adapters` folder of your Lightstreamer Server installation.
* Get the `deploy.zip` file of the [Data Adapter latest release](https://github.com/Lightstreamer/Lightstreamer-example-Chat-adapter-java/releases), unzip it, and copy the content of `Chat/lib` folder into the `adapters/Chat/lib` folder of your Lightstreamer Server installation.

### Enabling the MPN Module

The Mobile Push Notifications (MPN) module of Lightstreamer is not enabled by default and requires configuration.

* Open the `lightstreamer_conf.xml` file under the `conf` directory of your Lightstreamer Server installation.
* Find the `<mpn>` tag.
* Set the `<enabled>` tag to `Y`.

### Configuring the MPN Service Provider

The MPN module currently supports two MPN providers:

* Apple&trade; APNs for iOS, macOS, tvOS, watchOS and Safari
* Google&trade; FCM for Android, Chrome and Firefox

Currently the MPN Basic Chat Demo only supports the iOS Client, hence you only to configure the `apple_notifier_conf.xml` file under `conf/mpn/apple`. For an example of configuration of the MPN module for Google FCM, refer to the MPN Stock-List Demos and in particular the [Lightstreamer - MPN Stock-List Demo Metadata - Java Adapter](https://github.com/Lightstreamer/Lightstreamer-example-MPNStockListMetadata-adapter-java).

#### Configuring the APNs Provider

For the APNs provider, you need the following material in order to configure it correctly:

* the *app ID* of your app;
* a development or production *APNs client certificate*, exported in *p12* format and related to the app ID above;
* the *password* for the p12 client certificate above.

All this may be obtained on the [Apple Developer Center](https://developer.apple.com/membercenter/index.action). Exporting the client certificate in p12 format may be done easily from the *Keychain Access* system app. This [guide](https://code.google.com/p/javapns/wiki/GetAPNSCertificate) describes the full procedure in details.

Once you have the required material, add the following segment to the `apple_notifier_conf.xml` file:

```xml
   <app id="your.app.id">
      <service_level>development</service_level>
      <keystore_file>your_client_certificate.p12</keystore_file>
      <keystore_password>your certificate password</keystore_password>
   </app>
```

Replace `your.app.id`, `your_client_certificate.p12` and `your certificate password` with the corresponding informations. The certificate file must be located in the same folder of `apple_notifier_conf.xml`, unless an absolute path is specified. The `<service_level>` tag must be set accordingly to your client certificate type: `development` (sandbox) or `production`. For more informations on the meaning of these tags please consult the `apple_notifier_conf.xml` itself or the *Mobile Push Notifications* section of the *General Concepts* document (available under `DOCS-SDKs` in your Lightstreamer Server installation).

### Configuring the MPN Database

The MPN module requires a working SQL database in order to store persistent data regarding devices and subscriptions. The database configuration must be specified in the `hibernate.cfg.xml` file under `conf/mpn` in your Lightstreamer Server installation.

If you don't have a working database instance, an HSQL test database may be installed and configured quickly following these steps:

* Download the latest stable release of HSQL from [hsqldb.org](http://hsqldb.org) and unzip it in a folder of your choice.
* Copy the `hsqldb.jar` file from the `lib` folder of your HSQL installation to the `lib/mpn/hibernate` folder of your Lightstreamer Server installation.
* Launch the HSQL instance by running the `runServer.sh` or `runServer.bat` script in the `bin` folder of your HSQL installation.
* Open the `hibernate.cfg.xml` file and locate the pre-enabled section indicated by *Sample database connection settings for HSQL*.
* If your HSQL instance is running on a separate machine than the Lightstreamer Server, specify its IP address in place of `localhost` in the following property: `<property name="connection.url">jdbc:hsqldb:hsql://localhost</property>`.

If you have a working database instance, follow these steps:

* Copy the JDBC driver jar file (or files) to the `lib/mpn/hibernate` folder of your Lightstreamer Server installation.
* Open the `hibernate.cfg.xml` file.
* Specify the appropriate connection properties and SQL dialect (samples are provided for MySQL, HSQL and Oracle), including the IP address.

A complete guide on configuring the Hibernate JDBC connection may be found [here](https://docs.jboss.org/hibernate/orm/5.0/manual/en-US/html/ch03.html#configuration-hibernatejdbc) (and [here](https://docs.jboss.org/hibernate/orm/5.0/manual/en-US/html/ch03.html#configuration-optional-dialects) is a list of available SQL dialects). Avoid introducing optional parameters, like those from tables 3.3 - 3.7, if they are not already present in the `hibernate.cfg.xml` file, as they may have not been tested and may lead to unexpected behavior. Do it only if you know what you are doing.

### Compiling and Building the Clients

You may download the source code for the Chat Demo iOS Client here:

* [Lightstreamer - Chat Demo - iOS Client - Swift](https://github.com/Lightstreamer/Lightstreamer-example-Chat-client-ios-swift)

The project must be modified in order to work with your app ID and certificate and to point to your Lightstreamer Server:

* your app ID must be set as the *Bundle Identifier* of the project (in Xcode, it may be found in the General tab of the project);
* the IP address of your Lightstreamer Server must be set in the `SERVER_URL` constant in the `SwiftChat/ViewController.swift` file.

Also, remember to install an appropriate *provisioning profile* for the app, enabled for push notifications, before building or running the code.

### Finishing Installation

Done all this, the installation is finished and ready to be tested:

* Launch the Lightstreamer Server.
* Launch the iOS Client on your device (remember the iOS Simulator does not support push notifications).
* Accept push notifications.

If everything is correct, as soon as someone writes on the chat, you will receive a push notification within. You may try for yourself running the app on both the emulator and a device.

In case of any problem, first double check all the steps above, then check for any errors reported on the Lightstreamer Server log.

## Build

To build your own version of `LS_MPN_chat_metadata_adapter.jar`, instead of using the one provided in the `deploy.zip` file from the [Install](https://github.com/Lightstreamer/Lightstreamer-example-MPNChatMetadata-adapter-java#install) section above, follow these steps:

* Download this project.
* Get the `ls-adapter-interface.jar` file from the `/lib` folder of a [Lightstreamer distribution 7.0 or greater](http://www.lightstreamer.com/download), and copy it into the `lib` folder.
* Get the `log4j-1.2.17.jar` file from [Apache log4j](https://logging.apache.org/log4j/1.2/) and copy it into the `lib` folder.
* Get the `LS_chat_data_adapter.jar` from the deploy zip file of latest [Chat Data Adapter release](https://github.com/Lightstreamer/Lightstreamer-example-Chat-adapter-java/releases) and copy it into the `lib` folder.
* Create the jar `LS_MPN_StockListDemo_MetadataAdapter.jar` with commands like these:

```sh
 >javac -source 1.8 -target 1.8 -nowarn -g -classpath lib/log4j-1.2.17.jar;lib/ls-adapter-interface.jar;lib/LS_chat_data_adapter.jar -sourcepath src -d tmp_classes src/chat_demo/adapters/MPNChatMetadataAdapter.java

 >jar cvf LS_MPN_chat_metadata_adapter.jar -C tmp_classes src
```

* Copy the just compiled `LS_MPN_chat_metadata_adapter.jar` in the `adapters/Chat/lib` folder of your Lightstreamer Server installation.

## See Also

### Clients Using This Adapter

<!-- START RELATED_ENTRIES -->

* [Lightstreamer - Chat Demo - iOS Client - Swift](https://github.com/Lightstreamer/Lightstreamer-example-Chat-client-ios-swift)

<!-- END RELATED_ENTRIES -->

### Related Projects

* [Lightstreamer - Basic Chat Demo - Node.js Adapter](https://github.com/Lightstreamer/Lightstreamer-example-Chat-adapter-node)
* [Lightstreamer - Reusable Metadata Adapters - Java Adapter](https://github.com/Lightstreamer/Lightstreamer-example-ReusableMetadata-adapter-java)
* [Lightstreamer - Basic Messenger Demo - Java Adapter](https://github.com/Lightstreamer/Lightstreamer-example-Messenger-adapter-java)
* [Lightstreamer - Basic Messenger Demo - HTML Client](https://github.com/Lightstreamer/Lightstreamer-example-Messenger-client-javascript)

## Lightstreamer Compatibility Notes

- Compatible with Lightstreamer SDK for Java Adapters Since 7.0
- For an example compatible with Lightstreamer SDK for Java Adapters version 6.x, please refer to the [non-MPN version of the Chat Data and Metadata Adapters](https://github.com/Lightstreamer/Lightstreamer-example-Chat-adapter-java).

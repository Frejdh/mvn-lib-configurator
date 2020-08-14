Property configurator and injector
-
<strong>Work in progress! No release has been done yet</strong><br>

This module handles property/environmental variables. Works similiarly to Spring-boot's `@Value` properties but more lightweight
and support for `static` members. 

## Loading the properties
By default there will be an attempt to load the `a` and `a` files that is used by Spring-boot and Vertx. 
Environment variables set during the execution will also be loaded and will override the given properties in the files.
Additional property files can be added either inside one of the default property files or as an environment variable with the key `property.sources`.
<br>
Example for the `application.properties` file: `property.sources=application-dev.properties, application-prod.properties`



## Adding the dependency
```
<dependencies>
    <dependency>
        <groupId>com.frejdh.util.environment</groupId>
        <artifactId>property-injector</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>

<repositories> <!-- Required in order to resolve this package -->
    <repository>
        <id>mvn-lib-property-injector</id>
        <url>https://raw.github.com/Frejdh/mvn-lib-property-injector/mvn-repo/</url>
    </repository>
</repositories>
```

## Other libraries
[Search for my other public libraries here](https://github.com/search?q=Frejdh%2Fmvn-lib-).

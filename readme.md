Property configurator
-
This module handles property/environmental variables for default Java, `application.properties` like files that are used in Spring-boot (profiles can still be used), 
or `conf/config.json` files that may exist for Vertx. Also added support for `.json5` files to support comments in JSON files.

## Loading the properties & runtime configuration
By default, there will be an attempt to load the files that are commonly used by Spring-boot and Vertx. 
Environment variables set during the execution will also be loaded and will override the given properties in the files.
Additional property files can be added either inside one of the default property files or as an environment variable with the key `config.sources` (string).
Runtime configuration can be enabled with the variable `property.runtime.enabled` (boolean).
<br>
Example for adding more property source files: `config.sources=myOtherFile.properties, test.json, testWithCommentSupport.json5`

## Default property files
Other than the loaded environmental variables, the following files are always attempted to be loaded:
* `application.properties` or `application-[PROFILE].properties`
* `conf/config.json`
* `conf/config.json5` 

## Adding the dependency
```
<dependencies>
    <dependency>
        <groupId>com.frejdh.util.environment</groupId>
        <artifactId>configurator</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>

<repositories> <!-- Required in order to resolve this package -->
    <repository>
        <id>mvn-lib-configurator</id>
        <url>https://raw.github.com/Frejdh/mvn-lib-configurator/mvn-repo/</url>
    </repository>
</repositories>
```

## Other libraries
[Search for my other public libraries here](https://github.com/search?q=Frejdh%2Fmvn-lib-).

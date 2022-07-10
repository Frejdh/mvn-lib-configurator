Property configurator
-
This module handles property/environmental variables for default Java, `application.properties` like files that are used in Spring-boot (profiles can still be used), 
or `conf/config.json` files that may exist for Vertx. Also added support for `.json5` files to support comments in JSON files.

## Loading the properties & runtime configuration
By default, there will be an attempt to load the files that are commonly used by Spring-boot and Vertx. 
Environment variables set during the execution will also be loaded and will override the given properties in the files.
Additional property files can be added either inside one of the default property files or as an environment variable with the key `config.sources` (string).
Runtime configuration can be enabled with the variable `config.runtime.enabled` (boolean).
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
        <version>2.1.0</version>
    </dependency>
</dependencies>

<repositories> <!-- Required in order to resolve this package -->
    <repository>
        <id>mvn-lib-configurator</id>
        <url>https://raw.github.com/Frejdh/mvn-lib-configurator/releases/</url>
    </repository>
</repositories>
```

## Usage
Use the class `Config`. For instance:
```java
// ... other imports
import com.frejdh.util.environment.Config;

public class Sample {
    private String myString = Config.getString("my-property.string");
    private List<Integer> myInt = Config.getIntegerList("my-property.list-of-integers");
}
```

## Test-helper classes
This dependency also includes some helpful classes for writing tests.

### Property annotation
`@TestProperty`

Usage example:
```java
import com.frejdh.util.environment.Config;
import com.frejdh.util.environment.test.TestProperty;
import com.frejdh.util.environment.test.TestPropertyExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TestPropertyExtension.class)
public class SampleTest {

    @Test
    @TestProperty(key = "my-property.example", value = "some value")
    public void annotationWorks() {
        Assert.assertEquals("some value", Config.getString("my-property.example"));
    }

}
```

## Other libraries
[Search for my other public libraries here](https://github.com/search?q=Frejdh%2Fmvn-lib-).

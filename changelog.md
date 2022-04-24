# Changelog
Changes for each version.

## 2.1.0 (Not released)
- #### TODO: Variable substitution for `${example.my-variable:default-value}` syntax.
- Split `getMap` into `getHashMap` and `getMultiMap` methods.
- Fixed bug where to many entries could appear in a fetched map.
- Added optional class conversion parameters for `getMap` and `getList`.

## 2.0.0 
- Added `getMap` and `getObject` implementations.
- Now supports loading multiple spring profiles.
- Drastically improved implementation logic.
    - Created custom linked MultiMap implementation for property keys. No more "loose" property storing for keys.
    - Custom wrapper object for properties. Has references to parent/children properties.
    - Much simpler parsing logic for each file standard. Most of the parsing now done by the Map instead with a shared implementation.
    - Native support of multiple values thanks to the new MultiMap. No more complicated/error-prone parsing now required.
- Divided project into multiple modules.
    - First module is the configurator implementation.
    - Second module contains helpful environment classes that can be used when writing tests.
- Moved from Junit 4 to 5. Also updated Configurator test-helper module to Junit 5.
  - `@Rule` with class `TestPropertyRule` has been replaced with `@ExtendWith` and `TestPropertyExtension`
- Removed kotlin dependency since it was only used for tests

## 1.1.0
- Now uses my [Storage Watcher / File & Directory Watcher](https://github.com/Frejdh/mvn-lib-file-watcher/) as a dependency to detect runtime changes to files.
- Better separation of classes.
- Cleanup of keys (to work with spring syntax).
    - Example 1: "example.key.value" == "example_key_value".
    - Example 2: "example.this-works" == "example.thisWorks".
- Array support for `.properties|.yml` files.
- Support for absolute file paths.

## 1.0.0
First version

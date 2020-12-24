Changelog

# 1.1.0-SNAPSHOT
- Now uses my [Storage Watcher / File & Directory Watcher](https://github.com/Frejdh/mvn-lib-file-watcher/) as a dependency to detect runtime changes to files.
- Better separation of classes.
- Cleanup of keys (to match spring syntax).
    - Example 1: "example.key.value" == "example_key_value".
    - Example 2: "example.this-works" == "example.thisWorks" (work in progress).
- Array support for `.properties|.yml` files.
- Support for absolute file paths.
- Get methods that throws an exception

# 1.0.0-SNAPSHOT
First version

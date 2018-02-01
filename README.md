StreamSDKDemoApp
================

This is a copy of Stream Unlimited's demo app source.

Requirements
------------

- OpenJDK 8
- Android SDK 26

Local dependencies
------------------

- [streamconlib][]

[streamconlib]: https://gitlab.com/tiohome/streamconlib

Build
-----

```sh
$ ./tool/build.sh
```

Notes
-----

Stream's demo app has a legacy (Ant/Java 7) codebase.  To minimize churn
and make their changes easier to integrate, it's better that we **don't**
refactor this code.

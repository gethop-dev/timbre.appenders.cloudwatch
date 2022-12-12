[![ci-cd](https://github.com/gethop-dev/timbre.appenders.cloudwatch/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/gethop-dev/timbre.appenders.cloudwatch/actions/workflows/ci-cd.yml)
[![Clojars Project](https://img.shields.io/clojars/v/dev.gethop/timbre.appenders.cloudwatch.svg)](https://clojars.org/dev.gethop/timbre.appenders.cloudwatch)

# timbre.appenders.cloudwatch

Duct module that provides a Cloudwatch Timbre log appender. The
library does batching and rate limiting considering Cloudwatch
PutLogEvents API service quota limitations.

## Installation

[![Clojars Project](https://clojars.org/dev.gethop/timbre.appenders.cloudwatch/latest-version.svg)](https://clojars.org/dev.gethop/timbre.appenders.cloudwatch)


## Usage
To use this module just include the `dev.gethop.timbre.appenders/cloudwatch` key to your modules configuration in the `config.edn`.
The module requires the following mandatory keys:

* `log-group-name`: The Cloudwatch's log group name.
* `batch-config`: The batch configuration requires the following mandatory keys:
  * `size`: The maximum number of `log-events` to be sent in one batch.
  * `timeout`: The amount of time in ms that the `batch-log-queue` should wait before sending the batch if the maximum batch `size` has not been reached. The timeout resets every time there is a new `log-event` entry in the `batch-log-queue`.

The module also accepts the following optional keys:

* `appender-config`: Configuration for the Timbre's appender:
  * `min-level`: Minimum logging level. It defaults to `:info`.
* `client-config`: Extra configuration for the
  [aws-api client](https://cognitect-labs.github.io/aws-api/cognitect.aws.client.api-api.html#cognitect.aws.client.api/client)
  used to interact with the Cloudwatch API:

### Example

``` clojure
{:dev.gethop.timbre.appenders/cloudwatch
 {:log-group-name "/aws/eb/hop-test"
  :batch-config {:size 1000 :timeout 300}}
```

## License

Copyright (c) 2022 HOP Technologies

This Source Code Form is subject to the terms of the Mozilla Public License,
v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain
one at https://mozilla.org/MPL/2.0/

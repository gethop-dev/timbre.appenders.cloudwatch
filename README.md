# timbre.appenders.cloudwatch

Duct module that provides a Cloudwatch Timbre log appender. The
library does batching and rate limiting considering Cloudwatch
PutLogEvents API service quota limitations.

## Usage
To use this module just include the `dev.gethop.timbre.appenders/cloudwatch` key to your modules configuration in the `config.edn`.
The module requires the following mandatory keys:

* `log-group-name`: The Cloudwatch's log group name.
* `batch-config`: The batch configuration requires the following mandatory keys:
  * `size`: The maximum number of `log-events` to be sent in one batch.
  * `timeout`: The amount of time in ms that the `batch-log-queue` should wait before sending the batch if the maximum batch `size` has not been reached. The timeout resets every time there is a new `log-event` entry in the `batch-log-queue`.

The module also accepts the following optional keys:

* `appender-config`: Configuration for the Timbre's appender. Currently it only uses the following key:

## License

Copyright (c) 2022 HOP Technologies

This Source Code Form is subject to the terms of the Mozilla Public License,
v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain
one at https://mozilla.org/MPL/2.0/

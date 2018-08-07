/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zeebe.exporter.spi;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;
import org.slf4j.Logger;

public interface Context {
  /** @return pre-configured logger for an exporter */
  Logger getLogger();

  /** @return Zeebe working directory * */
  Path getWorkingDirectory();

  /** @return ID of the exporter * */
  String getId();

  /** @return Map of arguments specified in the configuration file */
  Map<String, Object> getArgs();

  /** @return a consumer which should be called with the latest exported event position */
  Consumer<Long> getLastPositionUpdater();
}

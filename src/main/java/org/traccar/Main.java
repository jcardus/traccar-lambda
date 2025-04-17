/*
 * Copyright 2012 - 2024 Anton Tananaev (anton@traccar.org)
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
package org.traccar;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.ProvisionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.storage.DatabaseModule;
import org.traccar.web.WebModule;
import org.traccar.web.WebServer;

import java.util.Locale;
import java.util.concurrent.ExecutorService;

public final class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private static Injector injector;

    public static Injector getInjector() {
        return injector;
    }

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ENGLISH);
        run(args[args.length - 1]);
    }

    public static void run(String configFile) {
        try {
            injector = Guice.createInjector(new MainModule(configFile), new DatabaseModule(), new WebModule());
            LOGGER.info("Starting lambda server...");

            var service = injector.getInstance(WebServer.class);
            if (service != null) {
                service.start();
            }

            Thread.setDefaultUncaughtExceptionHandler((t, e) -> LOGGER.error("Thread exception", e));

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("Stopping server...");
                try {
                    assert service != null;
                    service.stop();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                injector.getInstance(ExecutorService.class).shutdown();
            }));
        } catch (Exception e) {
            Throwable unwrapped;
            if (e instanceof ProvisionException) {
                unwrapped = e.getCause();
            } else {
                unwrapped = e;
            }
            LOGGER.error("Main method error", unwrapped);
            System.exit(1);
        }
    }

}

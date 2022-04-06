/*
 * Copyright 2014 The British Library/SCAPE Project Consortium
 * Authors: William Palmer (William.Palmer@bl.uk)
 *          Alecs Geuder (Alecs.Geuder@bl.uk)
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package au.gov.nla.flint.checks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * A helper class to run timed validations using Callables inheriting from {@link au.gov.nla.flint.checks.TimedTask}.
 *
 * Every task comes with a timeout[seconds], if this is reached, or another Exception
 * has occurred, the task is killed and a  {@link au.gov.nla.flint.checks.CheckCategory}
 * is being created with the task's name as name.
 */
public class TimedValidation {

    private static Logger LOGGER = LoggerFactory.getLogger(TimedValidation.class);

    private TimedValidation(){}

    /**
     * Run a timed validation of a TimedTask against a file
     * @param task task to run
     * @param contentFile file to run against the TimedTask
     * @return output from the TimedTask
     */
    public static LinkedHashMap<String, CheckCategory> validate(TimedTask task, File contentFile) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        task.setContentFile(contentFile);
        LinkedHashMap<String, CheckCategory> cMap = new LinkedHashMap<String, CheckCategory>();
        Future<LinkedHashMap<String, CheckCategory>> future = executor.submit(task);
        LOGGER.info("calling time-limited validation task {}, timeout: {} seconds", task.name, task.timeout);
        try {
            cMap.putAll(future.get(task.timeout, TimeUnit.SECONDS));
        } catch (Exception e) {
            LOGGER.error("Exception during validation: {}", e);
            CheckCategory cc = new CheckCategory(task.name);
            cc.add(new CheckCheck(task.name, false, null));
            cMap.put(task.name, cc);
            LOGGER.warn("Added validation error category '{}'", task.name);
        } catch (StackOverflowError e) {
            LOGGER.error("StackOverflowError during validation: {}", e);
            CheckCategory cc = new CheckCategory(task.name);
            cc.add(new CheckCheck(task.name, false, null));
            cMap.put(task.name, cc);
            LOGGER.warn("Added validation error category '{}'", task.name);
        }
        executor.shutdownNow();
        return cMap;
    }

}

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
package au.gov.nla.flint.pdf.checks;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import au.gov.nla.flint.checks.CheckCategory;
import au.gov.nla.flint.checks.CheckCheck;
import au.gov.nla.flint.checks.TimedTask;
import au.gov.nla.flint.wrappers.CalibreWrapper;
import au.gov.nla.flint.wrappers.Jhove1Wrapper;
import au.gov.nla.flint.wrappers.PDFBoxWrapper;

import java.util.LinkedHashMap;
import java.util.Set;

/**
 * Wrapper around additional specific WellFormdness checks that produces an error message
 * in case of a timing out after PDFFormat#Wrapper_TIMEOUT seconds.
 */
public class Wellformedness extends TimedTask {

    private Logger logger;
    private Set<String> patternFilter;

    /**
     * Constructor for Wellformedness.
     *
     * @param timeout the time [s] after which a TimeOutException is thrown and logged as
     *                an 'erroneous' {@link au.gov.nla.flint.checks.CheckCategory}
     * @param patternFilter a set of strings that represent patterns to be included
     *                      in following operations.
     */
    public Wellformedness(long timeout, Set<String> patternFilter) {
        super(FixedCategories.WELL_FORMED.toString(), timeout);
        this.patternFilter = patternFilter;
        this.logger = LoggerFactory.getLogger(this.getClass());
    }

    @Override
    public LinkedHashMap<String, CheckCategory> call() throws Exception {
        String catName = FixedCategories.WELL_FORMED.toString();
        LinkedHashMap<String, CheckCategory> cmap = new LinkedHashMap<String, CheckCategory>();
        if (patternFilter == null || patternFilter.contains(catName) ) {
            logger.info("Adding additional well-formedness checks for {}", contentFile);
            CheckCategory cc = new CheckCategory(catName);

            if (!System.getProperty("enable-pdfbox", "true").equalsIgnoreCase("false")) {
                cc.add(new CheckCheck("isValidPDFBox", new PDFBoxWrapper().isValid(contentFile), null));
                logger.debug(cc.get("isValidPDFBox").toString());
            }

            CalibreWrapper calibreWrapper = new CalibreWrapper();
            if (calibreWrapper.calibreIsAvailable()) {
                try {
                    cc.add(new CheckCheck("isValid_Calibre", calibreWrapper.isValid(contentFile), null));
                } catch (CalibreWrapper.CalibreMissingException e) {
                    // this shouldn't happen due to availability check
                    e.printStackTrace();
                }
                logger.debug(cc.get("isValid_Calibre").toString());
            }

            // Jhove is passing files that should not pass
            // therefore only add a result if it is negative
            boolean jhove = new Jhove1Wrapper().isValid(contentFile);
            if (!jhove) {
                cc.add(new CheckCheck("isValidJhove1", false, null));
                logger.debug(cc.get("isValidJhove1").toString());
            }
            cmap.put(cc.getName(), cc);
        }
        return cmap;
    }
}

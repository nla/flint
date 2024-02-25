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
import au.gov.nla.flint.wrappers.PDFBoxWrapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Scanner;
import java.util.Set;

/**
 * Wrapper around additional specific DRM checks that produces an error message
 * in case of a timing out after PDFFormat#Wrapper_TIMEOUT seconds.
 */
public class SpecificDrmChecks extends TimedTask {

    private Logger logger;
    private Set<String> patternFilter;

    /**
     * Create a SpeficDRMChecks Object that times out if calls take longer than expected
     * @param pTimeout timeout to use
     * @param pPatternFilter a set of strings indicating which categories to use and not
     */
    public SpecificDrmChecks(long pTimeout, Set<String> pPatternFilter) {
        super(FixedCategories.NO_DRM.toString(), pTimeout);
        this.patternFilter = pPatternFilter;
        this.logger = LoggerFactory.getLogger(this.getClass());
    }

    @Override
    public LinkedHashMap<String, CheckCategory> call() throws Exception {
        LinkedHashMap<String, CheckCategory> cmap = new LinkedHashMap<String, CheckCategory>();
        if (patternFilter == null || patternFilter.contains(FixedCategories.NO_DRM.toString()) ) {
            PDFBoxWrapper pdfBoxWrapper = new PDFBoxWrapper();
            
            logger.info("Adding specific DRM checks for {} to check-result", contentFile);
            CheckCategory cc = new CheckCategory(FixedCategories.NO_DRM.toString());
            cc.add(new CheckCheck("checkDRMPDFBoxAbsolute", !pdfBoxWrapper.hasDRM(contentFile), null));
            logger.debug(cc.get("checkDRMPDFBoxAbsolute").toString());
            cmap.put(cc.getName(), cc);
        }
        return cmap;
    }
}

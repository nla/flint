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
package au.gov.nla.flint.formats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import au.gov.nla.flint.checks.CheckCategory;
import au.gov.nla.flint.checks.CheckCheck;
import uk.bl.dpt.utils.schematron.Validator;
import uk.bl.dpt.utils.schematron.ValidatorFactory;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathExpressionException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * An extension to the format interface which knows about schematron based
 * policy validation.
 */
public abstract class PolicyAware {

    protected Logger logger;

    protected Set<String> patternFilter = null;
    private static ValidatorFactory valFac;

    /**
     * Create a new PolicyAware object
     */
    public PolicyAware() {
        logger = LoggerFactory.getLogger(getClass());
        valFac = new ValidatorFactory();
    }

    /**
     * Get an InputStream that represents the policy to check against
     * @return InputStream that represents the policy to check against
     */
    abstract public InputStream getPolicy();

    /**
     * Validates the xml-report of the format-specific third-party validator using
     * the policy schematron file.
     *
     * @param resultToBeValidated a stream source representing the output of a third-party
     *                             validation process
     * @return a report with resulting assertion errors and their frequency.
     * @throws Exception 
     */
    public static LinkedHashMap<String, CheckCategory> policyValidationResult(StreamSource resultToBeValidated, StreamSource schema, Set<String> pFilter) throws Exception {
        LinkedHashMap<String, CheckCategory> ccMap = new LinkedHashMap<String, CheckCategory>();
        Validator validator = valFac.newValidator(schema, pFilter);
        validator.validate(resultToBeValidated);
        LinkedHashMap<String, ? extends Map<String, Integer>> report = validator.getReport();
        for (Map.Entry<String, String> test : valFac.getAssertPatternMap(schema, pFilter).entrySet()) {
            CheckCategory cc;
            if (ccMap.containsKey(test.getValue())) {
                cc = ccMap.get(test.getValue());
            } else {
                cc = new CheckCategory(test.getValue());
            }
            boolean passed = true;
            int errorCount = 0;
            if (report.get(test.getValue()).containsKey(test.getKey())) {
                passed = false;
                errorCount = report.get(test.getValue()).get(test.getKey());
            }
            cc.add(new CheckCheck(test.getKey(), passed, errorCount));
            ccMap.put(cc.getName(), cc);
        }
        return ccMap;
    }

    /**
     * Reads a properties file with pattern element names that are relevant for the
     * policy validation and adds the patterns to {@link #patternFilter}.
     *
     * @throws IOException
     */
    public void setPatternFilter(String policyProperties) throws IOException {
        if (policyProperties == null) {
            logger.debug("no policy properties set. PatternFilter will not be used then.");
        } else {
            Properties props = new Properties();
            props.load(new FileInputStream(policyProperties));
            logger.debug("loading policy properties file: {}", policyProperties);
            for (Map.Entry<Object, Object> prop : props.entrySet()) {
                if (Boolean.parseBoolean((String) prop.getValue())) {
                    logger.debug("adding pattern: {}", prop.getKey());
                    patternFilter.add((String) prop.getKey());
                } else {
                    logger.debug("found a de-activated pattern: {}", prop.getKey());
                }
            }
        }
    }

    /**
     * Set a patternFilter directly.
     *
     * @param pFilter a set of strings that represent patterns to be included
     *                      in following operations.
     */
    public void setPatternFilter(Set<String> pFilter) {
        patternFilter = pFilter;
    }

    /**
     * Gets the names of the policy categories as a collection of Strings.
     *
     * @return a Collection of Strings with the names of the policy categories
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws XPathExpressionException
     * @throws IOException
     */
    public Collection<String> requestPolicyPatternNames(StreamSource schema) throws ParserConfigurationException, SAXException, XPathExpressionException, IOException {
        return valFac.getPatternNames(schema, patternFilter);
    }
}

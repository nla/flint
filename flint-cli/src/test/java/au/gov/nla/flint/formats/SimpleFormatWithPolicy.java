package au.gov.nla.flint.formats;

import com.google.common.io.Files;
import org.apache.commons.lang3.StringUtils;
import au.gov.nla.flint.checks.CheckResult;

import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class SimpleFormatWithPolicy extends PolicyAware implements Format {

    static String policy = "" +
            "<?xml version='1.0'?><s:schema xmlns:s='http://purl.oclc.org/dsdl/schematron'>" +
            "<s:pattern name='Body count'>" +
            "<s:rule context='head'>" +
            "<s:assert test='count(beard) = 1'>You shall be no beardless being</s:assert>" +
            "<s:assert test='count(piercing) = 1'>You shall not be missing a piercing</s:assert>" +
            "</s:rule>" +
            "<s:rule context='body'>" +
            "<s:assert test='count(belly) = 1'>A belly has to be</s:assert>" +
            "</s:rule>" +
            "</s:pattern>" +
            "</s:schema>";

    @Override
    public boolean canCheck(File pFile, String pMimetype) {
        String[] nameArray = pFile.getName().split("\\.");
        return nameArray[nameArray.length-1].equals(getFormatName());
    }

    @Override
    public boolean canCheck(String pMimetype) {
        return false;
    }

    @Override
    public Collection<String> acceptedMimeTypes() {
        return null;
    }

    @Override
    public CheckResult validationResult(File contentFile) {
        CheckResult checkResult;
        try {
            checkResult = new CheckResult(contentFile.getName(), this.getFormatName(), this.getVersion(), getAllCategoryNames());
            String xml = StringUtils.join(Files.readLines(contentFile, Charset.defaultCharset()), "");
            checkResult.addAll(PolicyAware.policyValidationResult(new StreamSource(new ByteArrayInputStream(xml.getBytes())),
                    new StreamSource(getPolicy()), new HashSet<String>()));
        } catch (Exception e) {
            throw new RuntimeException("could not initialise check-result! reason: {}", e);
        }
        Long startTime = System.currentTimeMillis();


        checkResult.setTime(System.currentTimeMillis() - startTime);
        logger.info("all checks done for {}", this.getFormatName());
        return checkResult;
    }

    @Override
    public Map<String, Map<String, Set<String>>> getFixedCategories() {
        return new HashMap<>();
    }

    @Override
    public Collection<String> getAllCategoryNames() throws Exception {
        Collection<String> cats = new HashSet<>();
        cats.addAll(getFixedCategories().keySet());
        cats.addAll(requestPolicyPatternNames(new StreamSource(getPolicy())));
        return cats;
    }

    @Override
    public String getFormatName() {
        return "SIMPLE_FORMAT_WITH_POLICY";
    }

    @Override
    public String getVersion() {
        return "0.1.2.3.4.5";
    }

    @Override
    public InputStream getPolicy() {
        return new ByteArrayInputStream(policy.getBytes());
    }

    public String getPolicyAsString() {
        return policy;
    }
}

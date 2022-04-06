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
package au.gov.nla;

import org.junit.Test;
import au.gov.nla.flint.checks.CheckCategory;
import au.gov.nla.flint.checks.CheckCheck;
import au.gov.nla.flint.checks.CheckResult;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import static org.fest.assertions.Assertions.assertThat;


public class CheckResultTest {

    @Test
    public void testResultPassed() {
        CheckCategory cc1 = new CheckCategory("testCc1");
        cc1.add(new CheckCheck("testCheck1", true, null));
        CheckCategory cc2 = new CheckCategory("testCc2");
        cc2.add(new CheckCheck("testCheck2", true, null));
        CheckResult result = new CheckResult("someFilename", "aFormat", "aVersion");
        result.add(cc1);
        result.add(cc2);

        assertThat(result.get("testCc1")).isEqualTo(cc1);
        assertThat(result.get("testCc2")).isEqualTo(cc2);

        assertThat(result.isErroneous()).isEqualTo(false);
        assertThat(result.isHappy()).isEqualTo(true);
        assertThat(result.getResult()).isEqualTo("passed");
    }

    @Test
    public void testResultFailed() {
        CheckCategory cc1 = new CheckCategory("testCc1");
        cc1.add(new CheckCheck("testCheck1", true, null));
        CheckCategory cc2 = new CheckCategory("testCc2");
        cc2.add(new CheckCheck("testCheck2", false, null));
        CheckResult result = new CheckResult("someFilename", "aFormat", "aVersion");
        result.add(cc1);
        result.add(cc2);

        assertThat(result.isErroneous()).isEqualTo(false);
        assertThat(result.isHappy()).isEqualTo(false);
        assertThat(result.getResult()).isEqualTo("failed");
    }

    @Test
    public void testResultErroneous() {
        // two cases for a check-result being erroneous:
        CheckResult result = new CheckResult("someFilename", "aFormat", "aVersion");

        // (1) there aren't any check-categories at all
        assertThat(result.isErroneous()).isEqualTo(true);
        assertThat(result.isHappy()).isEqualTo(null);
        assertThat(result.getResult()).isEqualTo("erroneous");

        CheckCategory cc1 = new CheckCategory("testCc1");
        cc1.add(new CheckCheck("testCheck1", null, null));
        CheckCategory cc2 = new CheckCategory("testCc2");
        cc2.add(new CheckCheck("testCheck2", null, null));
        result.add(cc1);
        result.add(cc2);

        assertThat(result.isErroneous()).isEqualTo(true);
        assertThat(result.isHappy()).isEqualTo(null);
        assertThat(result.getResult()).isEqualTo("erroneous");

        //    ..if at least one category is erroneous the result is also
        CheckCategory cc3 = new CheckCategory("testCc3");
        cc3.add(new CheckCheck("testCheck3", false, null));
        result.add(cc3);
        assertThat(result.isErroneous()).isEqualTo(true);
    }

    @Test
    public void testToXMLAndFunnyCharacters() {
        // create a result with a single quote in the name
        CheckResult result = new CheckResult("Mr Mac Meier's new toys", "aFormat", "aVersion");
        CheckCategory cc1 = new CheckCategory("Other funny characters: $%&");
        cc1.add(new CheckCheck("Even here: @¬?", false, null));
        result.add(cc1);
        result.setTime(System.currentTimeMillis());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(output);
        result.toXML(pw, "", "");
        pw.close();
        assertThat(output.toString())
                .startsWith("<checkedFile name='Mr Mac Meier&apos;s new toys' result='failed' format='aFormat' version='aVersion' ")
                .contains("<checkCategory name='Other funny characters: $%&amp;' result='failed'")
                .endsWith(String.format("</checkedFile>%n"));
    }
}

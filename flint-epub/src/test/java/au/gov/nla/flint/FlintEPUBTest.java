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
package au.gov.nla.flint;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import au.gov.nla.flint.checks.CheckResult;
import au.gov.nla.flint.epub.checks.FixedCategories;

import java.io.File;

/**
 * Test collection for epub validation.
 */
// Finding/creating Creative Commons DRM files is difficult, so
// ignore these tests until we can find them.
@Ignore("Sample files provided aren't actually DRM")
public class FlintEPUBTest {

    private Flint flint;
    private static String DRM_EPUBCHECK = "DRMDetectionEpubCheck";
    private static String DRM_RIGHTSFILE = FixedCategories.NO_DRM_RIGHTS_FILE.toString();

    @Before
    public void setUp() throws Exception {
        flint = new Flint();
    }

    @Test
    public final void testEpubSamplesWastelandWoffObf() {
        File toTest = new File(FlintEPUBTest.class.getResource("/epub_samples/wasteland-woff-obf-20120118.epub").getPath());
        CheckResult result = flint.check(toTest).get(0);
        Assert.assertFalse("drm not found when it should be", result.get(DRM_EPUBCHECK).isHappy());
        Assert.assertFalse("epupcheck didn't find drm when it should have", result.get(DRM_EPUBCHECK).get("Encryption detected").isHappy());
        Assert.assertFalse("drm not found when it should be", result.get(DRM_RIGHTSFILE).isHappy());
        Assert.assertFalse("rights-file check didn't find drm when it should have", result.get(DRM_RIGHTSFILE).get("checkForRightsFile").isHappy());
    }

    @Test
    public final void testEpubSamplesWastelandOtfObf() {
        File toTest = new File(FlintEPUBTest.class.getResource("/epub_samples/wasteland-otf-obf-20120118.epub").getPath());
        CheckResult result = flint.check(toTest).get(0);
        Assert.assertFalse("drm not found when it should be", result.get(DRM_EPUBCHECK).isHappy());
        Assert.assertFalse("epupcheck didn't find drm when it should have", result.get(DRM_EPUBCHECK).get("Encryption detected").isHappy());
        Assert.assertFalse("drm not found when it should be", result.get(DRM_RIGHTSFILE).isHappy());
        Assert.assertFalse("rights-file check didn't find drm when it should have", result.get(DRM_RIGHTSFILE).get("checkForRightsFile").isHappy());
    }

}

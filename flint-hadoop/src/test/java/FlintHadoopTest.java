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
import com.google.common.io.Files;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mrunit.mapreduce.MapDriver;
import org.apache.hadoop.mrunit.mapreduce.MapReduceDriver;
import org.apache.hadoop.mrunit.types.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import au.gov.nla.flint.Flint;
import au.gov.nla.flint.checks.CheckResult;
import au.gov.nla.flint.hadoop.FlintHadoop;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedHashMap;

/**
 * Tests for reduce, map and both combined.
 */
public class FlintHadoopTest {

    MapDriver<LongWritable, Text, Text, FlintHadoop.CheckResultText> mapDriver;
    //ReduceDriver<Text, FlintHadoop.CheckResultText, Text, Text> reduceDriver;
    MapReduceDriver<LongWritable, Text, Text, FlintHadoop.CheckResultText, Text, Text> mapRedDriver;

    final static String testPdf1Name = "encryption_openpassword.pdf";
    final static String testPdf1Path = FlintHadoopTest.class.getResource("/format_corpus/" + testPdf1Name).getPath();

    final static String testPdf2Name = "encryption_notextaccess.pdf";
    final static String testPdf2Path = FlintHadoopTest.class.getResource("/format_corpus/" + testPdf2Name).getPath();

    final static File tmpDir = Files.createTempDir();

    static CheckResult testPdf1CheckResult;
    static CheckResult testPdf2CheckResult;

    /**
     * Set up the FlintHadoop tests
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    @Before
    public void setUp() throws InstantiationException, IllegalAccessException {
        FlintHadoop.FlintMap mapper = new FlintHadoop.FlintMap();
       // FlintHadoop.FlintReduce reducer = new FlintHadoop.FlintReduce();
        mapDriver = MapDriver.newMapDriver(mapper);
        //reduceDriver = ReduceDriver.newReduceDriver(reducer);
        //mapRedDriver = MapReduceDriver.newMapReduceDriver(mapper, reducer);

        mapDriver.getConfiguration().set("mapred.output.dir", tmpDir.getAbsolutePath());
        //reduceDriver.getConfiguration().set("mapred.output.dir", tmpDir.getAbsolutePath());
        //mapRedDriver.getConfiguration().set("mapred.output.dir", tmpDir.getAbsolutePath());

        testPdf1CheckResult =  new Flint().check(new File(testPdf1Path)).get(0);
        testPdf2CheckResult =  new Flint().check(new File(testPdf2Path)).get(0);
    }

    /**
     * Test the Map class
     * @throws IOException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    @Test
    public void testMap() throws IOException, InstantiationException, IllegalAccessException {
        // Oh dear. So on Windows, this happens -- https://cwiki.apache.org/confluence/display/HADOOP2/WindowsProblems
        // To resolve it, download the winutils binary from https://github.com/steveloughran/winutils/tree/master/hadoop-3.0.0/bin
        // and drop it inside of a bin directory under flint-hadoop
        // This property makes hadoop look for it at {hadoop.home.dir}/bin/winutils.exe
        //
        // No, I will not be committing strange exes to the repo for Windows development. Sorry.
        //
        // If you have the misfortune to develop under Windows, uncomment the below silliness.
        //if (System.getProperty("os.name").toLowerCase().contains("win")) {
        //    System.setProperty("hadoop.home.dir", Paths.get("./flint-hadoop").toAbsolutePath().toString());
        //}
        mapDriver.withInput(new LongWritable(0), new Text(testPdf1Path));
        assertOutputMatchesRecord(mapDriver.run().get(0), testPdf1CheckResult, testPdf1Name);
    }

    /**
     * Test the Reduce class
     * @throws IOException
     */
//    @SuppressWarnings("serial")
//	@Test
//    public void testReduce() throws IOException {
//        List<FlintHadoop.CheckResultText> records = new ArrayList<FlintHadoop.CheckResultText>() {{
//            add(new FlintHadoop.CheckResultText(testPdf1CheckResult));
//        }};
//        reduceDriver.withInput(new Text(testPdf1Name), records);
//        List<Pair<Text, Text>> output = reduceDriver.run();
//
//        assertOutputMatchesRecord(output.get(0), testPdf1CheckResult, testPdf1Name);
//    }

    /**
     * Test the Map and Reduce classes together
     * @throws Exception
     */
//    @Test
//    public void testMapReduce() throws Exception {
//        mapRedDriver.withInput(new LongWritable(0), new Text(testPdf1Path));
//        mapRedDriver.withInput(new LongWritable(1), new Text(testPdf2Path));
//
//        final List<Pair<Text, Text>> output = mapRedDriver.run();
//
//        assertThat(output)
//                .isNotNull()
//                .hasSize(2);
//
//        // hadoop doesn't care too much about order, so we have to check
//        Pair<Text, Text> first = output.get(0);
//        Pair<Text, Text> second = output.get(1);
//        if (first.getFirst().toString().equals(testPdf2Name)) {
//            first = output.get(1);
//            second = output.get(0);
//        }
//        assertOutputMatchesRecord(first, testPdf1CheckResult, testPdf1Name);
//        assertOutputMatchesRecord(second, testPdf2CheckResult, testPdf2Name);
//    }

    /**
     * helper method to compare rows of output against what is expected.
     */
    private void assertOutputMatchesRecord(Pair<Text,? extends Text> row, CheckResult result, String name) {
        // Flint's check-result production shall be tested somewhere else, so we can safely use
        // it here to make things easy for us.
        LinkedHashMap<String, String> expected = result.toMap();

        // 1. check the filename is there
        Assert.assertEquals(name, row.getFirst().toString());
        // 2. check all results are there
        String[] keys = expected.keySet().toArray(new String[expected.size()]);
        String [] outputResults = row.getSecond().toString().split("\t");
        for (int i=0;i<keys.length;i++) {
            if (keys[i].equals("timeTaken")) continue; // skip as it is necessarily different
            String value = expected.values().toArray(new String[expected.size()])[i];
            Assert.assertEquals(value, outputResults[i]);
        }
    }
}

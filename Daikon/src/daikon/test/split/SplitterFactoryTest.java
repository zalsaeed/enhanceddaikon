// ***** This file is automatically generated by SplitterFactoryTestUpdater.java

package daikon.test.split;

import junit.framework.*;
import daikon.split.*;
import daikon.*;
import java.util.*;
import java.io.*;
import plume.*;
import gnu.getopt.*;

/*>>>
import org.checkerframework.checker.nullness.qual.*;
*/

/**
 * THIS CLASS WAS GENERATED BY SplitterFactoryTestUpdater.
 * Therefore, it is a bad idea to directly edit this class's
 * code for all but temporary reasons.  Any permanent changes
 * should be made through SplitterFactoryUpdater.
 * <p>
 * This class contains regression tests for the SplitterFactory class.
 * The tests directly test the java files produced by the
 * load_splitters method by comparing them against goal files.
 * Note that it is normal for some classes not to compile during this test.
 * <p>
 * These tests assume that the goal files are contained in the directory:
 * "daikon/test/split/targets/"
 * These tests ignore extra white spaces.
 */
public class SplitterFactoryTest extends TestCase {
  // Because the SplitterFactory sequentially numbers the
  // java files it produces, changing the order that the setUpTests
  // commands are run will cause the tests to fail.

  private static String targetDir = "daikon/test/split/targets/";

  private static /*@Nullable*/ String tempDir = null;

  private static boolean saveFiles = false;

    private static String usage =
      UtilMDE.joinLines(
        "Usage:  java daikon.tools.CreateSpinfo FILE.java ...",
        "  -s       Save (do not delete) the splitter java files in the temp directory",
        "  -h       Display this usage message"
      );

  public static void main(String[] args) {
    Getopt g =
      new Getopt("daikon.test.split.SplitterFactoryTest", args, "hs");
    int c;
    while ((c = g.getopt()) != -1) {
      switch(c) {
      case 's':
        saveFiles = true;
        break;
      case 'h':
        System.out.println(usage);
        System.exit(1);
        break;
      case '?':
        break;
      default:
        System.out.println("getopt() returned " + c);
        break;
      }
    }
    junit.textui.TestRunner.run(suite());
  }

  private static void setUpTests() {
    List<String> spinfoFiles;
    List<String> declsFiles;
    createSplitterFiles("daikon/test/split/targets/StreetNumberSet.spinfo", "daikon/test/split/targets/StreetNumberSet.decls");
    createSplitterFiles("daikon/test/split/targets/Fib.spinfo", "daikon/test/split/targets/Fib.decls");
    createSplitterFiles("daikon/test/split/targets/QueueAr.spinfo", "daikon/test/split/targets/QueueAr.decls");
    createSplitterFiles("daikon/test/split/targets/BigFloat.spinfo", "daikon/test/split/targets/BigFloat.decls");
  }

  public SplitterFactoryTest(String name) {
    super(name);
  }

  /**
   * Sets up the test by generating the needed splitter java files.
   */
  private static void createSplitterFiles(String spinfo, String decl) {
    List<String> spinfoFiles = new ArrayList<String>();
    spinfoFiles.add(spinfo);
    List<String> declsFiles = Collections.singletonList(decl);
    createSplitterFiles(spinfoFiles, declsFiles);
  }

  /**
   * Sets up the test by generating the needed splitter java files.
   */
  private static void createSplitterFiles(List<String> spinfos, List<String> decls) {
    Set<File> spFiles = new HashSet<File>();
    PptMap allPpts = new PptMap();
    for (String spinfo : spinfos) {
      spFiles.add(new File(spinfo));
    }
    try {
      if (saveFiles) {
        SplitterFactory.dkconfig_delete_splitters_on_exit = false;
      }
      PptSplitter.dkconfig_suppressSplitterErrors = true;
      Daikon.create_splitters(spFiles);
      for (String declsFile : decls) {
        FileIO.resetNewDeclFormat();
        FileIO.read_data_trace_file(declsFile, allPpts);
      }
      tempDir = SplitterFactory.getTempDir();
    } catch(IOException e) {
        throw new RuntimeException(e);
    }
  }

  /**
   * Returns true iff files are the same (ignoring extra white space).
   */

  public static void assertEqualFiles(String f1, String f2) {
    if (!UtilMDE.equalFiles(f1, f2)) {
      fail("Files " + f1 + " and " + f2 + " differ.");
    }
  }

  public static void assertEqualFiles(String f1) {
    assertEqualFiles(tempDir + f1,
                     targetDir + f1 + ".goal");
  }

  public static void testMapQuick1_StreetNumberSet_contains_6() {
    assertEqualFiles("MapQuick1_StreetNumberSet_contains_6.java");
  }

  public static void testMapQuick1_StreetNumberSet_orderStatistic_14() {
    assertEqualFiles("MapQuick1_StreetNumberSet_orderStatistic_14.java");
  }

  public static void testDataStructures_QueueAr_isFull_21() {
    assertEqualFiles("DataStructures_QueueAr_isFull_21.java");
  }

  public static void testMath__BigFloat_bmul__40() {
    assertEqualFiles("Math__BigFloat_bmul__40.java");
  }

  public static void testMapQuick1_StreetNumberSet_contains_10() {
    assertEqualFiles("MapQuick1_StreetNumberSet_contains_10.java");
  }

  public static void testMapQuick1_StreetNumberSet_contains_9() {
    assertEqualFiles("MapQuick1_StreetNumberSet_contains_9.java");
  }

  public static void testDataStructures_QueueAr_enqueue_32() {
    assertEqualFiles("DataStructures_QueueAr_enqueue_32.java");
  }

  public static void testDataStructures_QueueAr_dequeue_25() {
    assertEqualFiles("DataStructures_QueueAr_dequeue_25.java");
  }

  public static void testMath__BigFloat_bdiv__38() {
    assertEqualFiles("Math__BigFloat_bdiv__38.java");
  }

  public static void testMath__BigFloat_bdiv__33() {
    assertEqualFiles("Math__BigFloat_bdiv__33.java");
  }

  public static void testMath__BigFloat_bdiv__36() {
    assertEqualFiles("Math__BigFloat_bdiv__36.java");
  }

  public static void testDataStructures_QueueAr_getFront_24() {
    assertEqualFiles("DataStructures_QueueAr_getFront_24.java");
  }

  public static void testMapQuick1_StreetNumberSet_contains_11() {
    assertEqualFiles("MapQuick1_StreetNumberSet_contains_11.java");
  }

  public static void testMapQuick1_StreetNumberSet_size_17() {
    assertEqualFiles("MapQuick1_StreetNumberSet_size_17.java");
  }

  public static void testMath__BigFloat_bmul__42() {
    assertEqualFiles("Math__BigFloat_bmul__42.java");
  }

  public static void testMath__BigFloat_bdiv__39() {
    assertEqualFiles("Math__BigFloat_bdiv__39.java");
  }

  public static void testDataStructures_QueueAr_enqueue_31() {
    assertEqualFiles("DataStructures_QueueAr_enqueue_31.java");
  }

  public static void testDataStructures_QueueAr_enqueue_29() {
    assertEqualFiles("DataStructures_QueueAr_enqueue_29.java");
  }

  public static void testMath__BigFloat_bmul__41() {
    assertEqualFiles("Math__BigFloat_bmul__41.java");
  }

  public static void testMath__BigFloat_bmul__43() {
    assertEqualFiles("Math__BigFloat_bmul__43.java");
  }

  public static void testDataStructures_QueueAr_dequeue_26() {
    assertEqualFiles("DataStructures_QueueAr_dequeue_26.java");
  }

  public static void testMath__BigFloat_bdiv__34() {
    assertEqualFiles("Math__BigFloat_bdiv__34.java");
  }

  public static void testMapQuick1_StreetNumberSet_checkRep_4() {
    assertEqualFiles("MapQuick1_StreetNumberSet_checkRep_4.java");
  }

  public static void testMapQuick1_StreetNumberSet_contains_13() {
    assertEqualFiles("MapQuick1_StreetNumberSet_contains_13.java");
  }

  public static void testMapQuick1_StreetNumberSet_StreetNumberSet_1() {
    assertEqualFiles("MapQuick1_StreetNumberSet_StreetNumberSet_1.java");
  }

  public static void testMapQuick1_StreetNumberSet_contains_12() {
    assertEqualFiles("MapQuick1_StreetNumberSet_contains_12.java");
  }

  public static void testDataStructures_QueueAr_dequeue_27() {
    assertEqualFiles("DataStructures_QueueAr_dequeue_27.java");
  }

  public static void testMapQuick1_StreetNumberSet_size_16() {
    assertEqualFiles("MapQuick1_StreetNumberSet_size_16.java");
  }

  public static void testDataStructures_QueueAr_dequeue_28() {
    assertEqualFiles("DataStructures_QueueAr_dequeue_28.java");
  }

  public static void testMapQuick1_StreetNumberSet_StreetNumberSet_2() {
    assertEqualFiles("MapQuick1_StreetNumberSet_StreetNumberSet_2.java");
  }

  public static void testDataStructures_QueueAr_isEmpty_19() {
    assertEqualFiles("DataStructures_QueueAr_isEmpty_19.java");
  }

  public static void testMapQuick1_StreetNumberSet_StreetNumberSet_0() {
    assertEqualFiles("MapQuick1_StreetNumberSet_StreetNumberSet_0.java");
  }

  public static void testDataStructures_QueueAr_enqueue_30() {
    assertEqualFiles("DataStructures_QueueAr_enqueue_30.java");
  }

  public static void testMath__BigFloat_bdiv__37() {
    assertEqualFiles("Math__BigFloat_bdiv__37.java");
  }

  public static void testMapQuick1_StreetNumberSet_orderStatistic_15() {
    assertEqualFiles("MapQuick1_StreetNumberSet_orderStatistic_15.java");
  }

  public static void testmisc_Fib_main_18() {
    assertEqualFiles("misc_Fib_main_18.java");
  }

  public static void testDataStructures_QueueAr_isFull_22() {
    assertEqualFiles("DataStructures_QueueAr_isFull_22.java");
  }

  public static void testDataStructures_QueueAr_isEmpty_20() {
    assertEqualFiles("DataStructures_QueueAr_isEmpty_20.java");
  }

  public static void testMapQuick1_StreetNumberSet_checkRep_5() {
    assertEqualFiles("MapQuick1_StreetNumberSet_checkRep_5.java");
  }

  public static void testDataStructures_QueueAr_getFront_23() {
    assertEqualFiles("DataStructures_QueueAr_getFront_23.java");
  }

  public static void testMath__BigFloat_bdiv__35() {
    assertEqualFiles("Math__BigFloat_bdiv__35.java");
  }

  public static void testMapQuick1_StreetNumberSet_contains_8() {
    assertEqualFiles("MapQuick1_StreetNumberSet_contains_8.java");
  }

  public static void testMapQuick1_StreetNumberSet_StreetNumberSet_3() {
    assertEqualFiles("MapQuick1_StreetNumberSet_StreetNumberSet_3.java");
  }

  public static void testMapQuick1_StreetNumberSet_contains_7() {
    assertEqualFiles("MapQuick1_StreetNumberSet_contains_7.java");
  }

  public static Test suite() {
    setUpTests();
    TestSuite suite = new TestSuite();
    suite.addTest(new SplitterFactoryTest("testMapQuick1_StreetNumberSet_contains_6"));
    suite.addTest(new SplitterFactoryTest("testMapQuick1_StreetNumberSet_orderStatistic_14"));
    suite.addTest(new SplitterFactoryTest("testDataStructures_QueueAr_isFull_21"));
    suite.addTest(new SplitterFactoryTest("testMath__BigFloat_bmul__40"));
    suite.addTest(new SplitterFactoryTest("testMapQuick1_StreetNumberSet_contains_10"));
    suite.addTest(new SplitterFactoryTest("testMapQuick1_StreetNumberSet_contains_9"));
    suite.addTest(new SplitterFactoryTest("testDataStructures_QueueAr_enqueue_32"));
    suite.addTest(new SplitterFactoryTest("testDataStructures_QueueAr_dequeue_25"));
    suite.addTest(new SplitterFactoryTest("testMath__BigFloat_bdiv__38"));
    suite.addTest(new SplitterFactoryTest("testMath__BigFloat_bdiv__33"));
    suite.addTest(new SplitterFactoryTest("testMath__BigFloat_bdiv__36"));
    suite.addTest(new SplitterFactoryTest("testDataStructures_QueueAr_getFront_24"));
    suite.addTest(new SplitterFactoryTest("testMapQuick1_StreetNumberSet_contains_11"));
    suite.addTest(new SplitterFactoryTest("testMapQuick1_StreetNumberSet_size_17"));
    suite.addTest(new SplitterFactoryTest("testMath__BigFloat_bmul__42"));
    suite.addTest(new SplitterFactoryTest("testMath__BigFloat_bdiv__39"));
    suite.addTest(new SplitterFactoryTest("testDataStructures_QueueAr_enqueue_31"));
    suite.addTest(new SplitterFactoryTest("testDataStructures_QueueAr_enqueue_29"));
    suite.addTest(new SplitterFactoryTest("testMath__BigFloat_bmul__41"));
    suite.addTest(new SplitterFactoryTest("testMath__BigFloat_bmul__43"));
    suite.addTest(new SplitterFactoryTest("testDataStructures_QueueAr_dequeue_26"));
    suite.addTest(new SplitterFactoryTest("testMath__BigFloat_bdiv__34"));
    suite.addTest(new SplitterFactoryTest("testMapQuick1_StreetNumberSet_checkRep_4"));
    suite.addTest(new SplitterFactoryTest("testMapQuick1_StreetNumberSet_contains_13"));
    suite.addTest(new SplitterFactoryTest("testMapQuick1_StreetNumberSet_StreetNumberSet_1"));
    suite.addTest(new SplitterFactoryTest("testMapQuick1_StreetNumberSet_contains_12"));
    suite.addTest(new SplitterFactoryTest("testDataStructures_QueueAr_dequeue_27"));
    suite.addTest(new SplitterFactoryTest("testMapQuick1_StreetNumberSet_size_16"));
    suite.addTest(new SplitterFactoryTest("testDataStructures_QueueAr_dequeue_28"));
    suite.addTest(new SplitterFactoryTest("testMapQuick1_StreetNumberSet_StreetNumberSet_2"));
    suite.addTest(new SplitterFactoryTest("testDataStructures_QueueAr_isEmpty_19"));
    suite.addTest(new SplitterFactoryTest("testMapQuick1_StreetNumberSet_StreetNumberSet_0"));
    suite.addTest(new SplitterFactoryTest("testDataStructures_QueueAr_enqueue_30"));
    suite.addTest(new SplitterFactoryTest("testMath__BigFloat_bdiv__37"));
    suite.addTest(new SplitterFactoryTest("testMapQuick1_StreetNumberSet_orderStatistic_15"));
    suite.addTest(new SplitterFactoryTest("testmisc_Fib_main_18"));
    suite.addTest(new SplitterFactoryTest("testDataStructures_QueueAr_isFull_22"));
    suite.addTest(new SplitterFactoryTest("testDataStructures_QueueAr_isEmpty_20"));
    suite.addTest(new SplitterFactoryTest("testMapQuick1_StreetNumberSet_checkRep_5"));
    suite.addTest(new SplitterFactoryTest("testDataStructures_QueueAr_getFront_23"));
    suite.addTest(new SplitterFactoryTest("testMath__BigFloat_bdiv__35"));
    suite.addTest(new SplitterFactoryTest("testMapQuick1_StreetNumberSet_contains_8"));
    suite.addTest(new SplitterFactoryTest("testMapQuick1_StreetNumberSet_StreetNumberSet_3"));
    suite.addTest(new SplitterFactoryTest("testMapQuick1_StreetNumberSet_contains_7"));
    return suite;
  }

}

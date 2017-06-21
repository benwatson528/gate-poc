package uk.co.hadoopathome.gatepoc;

import org.junit.Test;

import java.io.File;
import java.util.Collections;

public class ANNIETutorialTest {
    @Test
    public void testProcessDocuments() throws Exception {
        ANNIETutorial annieTutorial = new ANNIETutorial();
        String input = new File("src/test/resources/input/business-text.txt").toURI().toURL().toString();
        annieTutorial.processDocuments(Collections.singletonList(input));
    }
}
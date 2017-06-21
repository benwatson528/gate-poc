package uk.co.hadoopathome.gatepoc;

import gate.*;
import gate.corpora.RepositioningInfo;
import gate.creole.ResourceInstantiationException;
import gate.util.GateException;
import gate.util.Out;
import gate.util.persistence.PersistenceManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * This class illustrates how to use ANNIE as a sausage machine
 * in another application - put ingredients in one end (URLs pointing
 * to documents) and get sausages (e.g. Named Entities) out the
 * other end.
 * <P><B>NOTE:</B><BR>
 * For simplicity's sake, we don't do any exception handling.
 */
public class ANNIETutorial {

    private static final String TAG_OPEN = "<span GateID=\"";
    private static final String TAG_TITLE = "\" title=\"";
    private static final String TAG_STYLE = "\" style=\"background:Red;\">";
    private static final String TAG_CLOSE = "</span>";
    private CorpusController annieController;

    /**
     * Sets up a new ANNIE pipeline. This initialisation can take
     * several minutes so this object should live on in the
     * background.
     */
    ANNIETutorial() throws IOException, GateException {
        Gate.init();
        initAnnie();
        Out.prln("Finished initialising ANNIE");
    }

    /**
     * Initialise the ANNIE system. This creates a "corpus pipeline"
     * application that can be used to run sets of documents through
     * the extraction system.
     */
    private void initAnnie() throws GateException, IOException {
        Out.prln("Initialising ANNIE");
        File pluginsHome = Gate.getPluginsHome();
        File anniePlugin = new File(pluginsHome, "ANNIE");
        File annieGapp = new File(anniePlugin, "ANNIE_with_defaults.gapp");
        this.annieController = (CorpusController) PersistenceManager.loadObjectFromFile(annieGapp);
    }

    /**
     * Run from the command-line, with a list of URLs as argument.
     * <P><B>NOTE:</B><BR>
     * This code will run with all the documents in memory - if you
     * want to unload each from memory after use, add code to store
     * the corpus in a DataStore.
     */
    public void processDocuments(List<String> documents) throws GateException, IOException {
        Corpus corpus = Factory.newCorpus("StandAloneAnnie corpus");
        addDocumentsToCorpus(documents, corpus);

        this.annieController.setCorpus(corpus);
        Out.prln("Executing");
        this.annieController.execute();

        Iterator iter = corpus.iterator();
        int count = 0;

        while (iter.hasNext()) {
            Document doc = (Document) iter.next();
            Set<Annotation> peopleAndPlaces = defineAnnotations(doc);

            FeatureMap features = doc.getFeatures();
            String originalContent = (String) features.get(GateConstants.ORIGINAL_DOCUMENT_CONTENT_FEATURE_NAME);
            RepositioningInfo info = (RepositioningInfo)
                    features.get(GateConstants.DOCUMENT_REPOSITIONING_INFO_FEATURE_NAME);

            ++count;
            File file = new File("StANNIE_" + count + ".HTML");
            Out.prln("Output file name: '" + file.getAbsolutePath() + "'");
            if (originalContent != null && info != null) {
                annotateWithInfo(peopleAndPlaces, originalContent, info, file);
            } else if (originalContent != null) {
                annotateWithoutInfo(peopleAndPlaces, originalContent, file);
            } else {
                Out.prln("Repositioning: " + info);
            }

            String xmlDocument = doc.toXml(peopleAndPlaces, false);
            String fileName = "StANNIE_toXML_" + count + ".HTML";
            FileWriter writer = new FileWriter(fileName);
            writer.write(xmlDocument);
            writer.close();
        }
    }

    private Set<Annotation> defineAnnotations(Document doc) {
        AnnotationSet defaultAnnotSet = doc.getAnnotations();
        Set<String> annotTypesRequired = new HashSet<>();
        annotTypesRequired.add("Person");
        annotTypesRequired.add("Location");
        return new HashSet<>(defaultAnnotSet.get(annotTypesRequired));
    }

    private void addDocumentsToCorpus(List<String> documents, Corpus corpus) throws MalformedURLException,
            ResourceInstantiationException {
        for (String document : documents) {
            URL u = new URL(document);
            FeatureMap params = Factory.newFeatureMap();
            params.put("sourceUrl", u);
            params.put("preserveOriginalContent", Boolean.TRUE);
            params.put("collectRepositioningInfo", Boolean.TRUE);
            Out.prln("Creating doc for " + u);
            Document doc = (Document) Factory.createResource("gate.corpora.DocumentImpl", params);
            corpus.add(doc);
            Out.prln("Added doc to corpus");
        }
    }

    private void annotateWithoutInfo(Set<Annotation> peopleAndPlaces, String originalContent, File file)
            throws IOException {
        Out.prln("OrigContent existing. Generate file...");

        Iterator it = peopleAndPlaces.iterator();
        Annotation currAnnot;
        SortedAnnotationList sortedAnnotations = new SortedAnnotationList();

        while (it.hasNext()) {
            currAnnot = (Annotation) it.next();
            sortedAnnotations.addSortedExclusive(currAnnot);
        }

        StringBuilder editableContent = new StringBuilder(originalContent);
        long insertPositionEnd;
        long insertPositionStart;

        Out.prln("Unsorted annotations count: " + peopleAndPlaces.size());
        Out.prln("Sorted annotations count: " + sortedAnnotations.size());
        for (int i = sortedAnnotations.size() - 1; i >= 0; --i) {
            currAnnot = (Annotation) sortedAnnotations.get(i);
            insertPositionStart = currAnnot.getStartNode().getOffset();
            insertPositionEnd = currAnnot.getEndNode().getOffset();
            if (insertPositionEnd != -1 && insertPositionStart != -1) {
                editableContent.insert((int) insertPositionEnd, TAG_CLOSE);
                editableContent.insert((int) insertPositionStart, TAG_STYLE);
                editableContent.insert((int) insertPositionStart, currAnnot.getType());
                editableContent.insert((int) insertPositionStart, TAG_TITLE);
                editableContent.insert((int) insertPositionStart, currAnnot.getId().toString());
                editableContent.insert((int) insertPositionStart, TAG_OPEN);
            }
        }

        writeToFile(file, editableContent);
    }

    private void annotateWithInfo(Set<Annotation> peopleAndPlaces, String originalContent, RepositioningInfo info,
                                  File file) throws IOException {
        Out.prln("OrigContent and reposInfo existing. Generate file...");

        Iterator it = peopleAndPlaces.iterator();
        Annotation currAnnot;
        SortedAnnotationList sortedAnnotations = new SortedAnnotationList();

        while (it.hasNext()) {
            currAnnot = (Annotation) it.next();
            sortedAnnotations.addSortedExclusive(currAnnot);
        }

        StringBuilder editableContent = new StringBuilder(originalContent);
        long insertPositionEnd;
        long insertPositionStart;
        Out.prln("Unsorted annotations count: " + peopleAndPlaces.size());
        Out.prln("Sorted annotations count: " + sortedAnnotations.size());
        for (int i = sortedAnnotations.size() - 1; i >= 0; --i) {
            currAnnot = (Annotation) sortedAnnotations.get(i);
            insertPositionStart = currAnnot.getStartNode().getOffset();
            insertPositionStart = info.getOriginalPos(insertPositionStart);
            insertPositionEnd = currAnnot.getEndNode().getOffset();
            insertPositionEnd = info.getOriginalPos(insertPositionEnd, true);
            if (insertPositionEnd != -1 && insertPositionStart != -1) {
                editableContent.insert((int) insertPositionEnd, TAG_CLOSE);
                editableContent.insert((int) insertPositionStart, TAG_STYLE);
                editableContent.insert((int) insertPositionStart, currAnnot.getType());
                editableContent.insert((int) insertPositionStart, TAG_TITLE);
                editableContent.insert((int) insertPositionStart, currAnnot.getId().toString());
                editableContent.insert((int) insertPositionStart, TAG_OPEN);
            }
        }

        writeToFile(file, editableContent);
    }

    private void writeToFile(File file, StringBuilder editableContent) throws IOException {
        FileWriter writer = new FileWriter(file);
        writer.write(editableContent.toString());
        writer.close();
    }

    public static class SortedAnnotationList extends Vector {
        SortedAnnotationList() {
            super();
        }

        void addSortedExclusive(Annotation annot) {
            Annotation currAnot;

            for (Object o : this) {
                currAnot = (Annotation) o;
                if (annot.overlaps(currAnot)) {
                    return;
                }
            }

            long annotStart = annot.getStartNode().getOffset();
            long currStart;

            for (int i = 0; i < size(); ++i) {
                currAnot = (Annotation) get(i);
                currStart = currAnot.getStartNode().getOffset();
                if (annotStart < currStart) {
                    insertElementAt(annot, i);
                    return;
                }
            }

            int size = size();
            insertElementAt(annot, size);
        }
    }
}
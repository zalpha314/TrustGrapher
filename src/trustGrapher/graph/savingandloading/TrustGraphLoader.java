package trustGrapher.graph.savingandloading;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import trustGrapher.graph.*;
import trustGrapher.visualizer.eventplayer.TrustLogEvent;

import utilities.ChatterBox;

public class TrustGraphLoader {

    private LinkedList<TrustLogEvent> logList;
    private TrustGraph hiddenGraph;
    private TrustGraph visibleGraph;
    private List<LoadingListener> loadingListeners;

    //[start] Constructor
    public TrustGraphLoader() {
        logList = new LinkedList<TrustLogEvent>();
        ChatterBox.debug(this, "P2PNetworkGraphLoader()", "A new graph was instanciated.  I have set it to feedback history by default.");
        hiddenGraph = new TrustGraph(TrustGraph.FEEDBACK_HISTORY);
        visibleGraph = new TrustGraph(TrustGraph.FEEDBACK_HISTORY);
        loadingListeners = new LinkedList<LoadingListener>();
    }
    //[end] Constructor

    //[start] Loading Method
    /**
     * @return <code>true</code> if file loaded successfully
     */
    public boolean doLoad() {
        String[] acceptedExtensions = {"xml", "arff", "txt"};
        File file = chooseLoadFile(".xml , .arff and .txt only", acceptedExtensions);
        if (file != null) {
            if (file.getAbsolutePath().endsWith(".txt") || file.getAbsolutePath().endsWith(".arff")) {

                try {
                    loadingStarted(1, "Log Files");
                    TrustEventLoader logBuilder = new TrustEventLoader();
                    for (LoadingListener l : loadingListeners) {
                        logBuilder.addLoadingListener(l);
                    }

                    logList = logBuilder.createList(file);

                    hiddenGraph = logBuilder.getHiddenGraph(); //load hidden graph but keep visible graph empty
                    loadingComplete();
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, e.getMessage(), "Failure in doLoad()", JOptionPane.ERROR_MESSAGE);
                }
            } else if (file.getAbsolutePath().endsWith(".xml")) {
                try {
                    SAXBuilder builder = new SAXBuilder();
                    final Document networkDoc = builder.build(file);
                    graphBuilder(networkDoc);
                    logList.addFirst(TrustLogEvent.getStartEvent());
                    logList.addLast(TrustLogEvent.getEndEvent(logList.getLast()));
                    return true;
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, e.getMessage(), "Failure", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        return false;
    }
    //[end] Loading Method

    //[start] Listener Methods
    public void addLoadingListener(LoadingListener listener) {
        loadingListeners.add(listener);
    }

    private void loadingStarted(int numberLines, String whatIsLoading) {
        for (LoadingListener l : loadingListeners) {
            l.loadingStarted(numberLines, whatIsLoading);
        }
    }

    private void loadingProgress(int progress) {
        for (LoadingListener l : loadingListeners) {
            l.loadingProgress(progress);
        }
    }

    private void loadingChanged(int numberLines, String whatIsLoading) {
        for (LoadingListener l : loadingListeners) {
            l.loadingChanged(numberLines, whatIsLoading);
        }
    }

    private void loadingComplete() {
        for (LoadingListener l : loadingListeners) {
            l.loadingComplete();
        }
    }
    //[end] Listener Methods

    //[start] Graph Builder
    private void graphBuilder(Document networkDoc) {
        if (networkDoc.getRootElement().getName().equals("network")) {
            int edgeCounter = 0;
            TrustGraph startGraph = new TrustGraph(TrustGraph.FEEDBACK_HISTORY);
            ChatterBox.debug(this, "P2PNetworkGraphLoader()", "A new graph was instanciated.  I have set it to feedback history by default.");
            Element networkElem = networkDoc.getRootElement();
            int counter = 0;
            //[start] Create Graph
            Element graphElem = networkElem.getChild("graph");
            if (graphElem != null) {
                //[start] Add Vertices to graph
                Element nodeMap = graphElem.getChild("nodemap");
                loadingStarted(nodeMap.getChildren().size(), "Vertices");

                for (Object o : nodeMap.getChildren()) {
                    Element elem = (Element) o;
                    String type = elem.getAttribute("type").getValue();

                    if (type.equals("PeerVertex")) {
                        int key = Integer.parseInt(elem.getChild("key").getText());
                        hiddenGraph.addVertex(new TrustVertex(key));
                        startGraph.addVertex(new TrustVertex(key));
                    } else if (type.equals("DocumentVertex")) {
                        int key = Integer.parseInt(elem.getChild("key").getText());
                        hiddenGraph.addVertex(new DocumentVertex(key));
                        startGraph.addVertex(new DocumentVertex(key));
                    }
                    loadingProgress(++counter);
                }
                //[end] Add Vertices to graph

                //[start] Add Edges to graph
                Element edgeMap = graphElem.getChild("edgemap");
                loadingChanged(edgeMap.getChildren().size(), "Edges");
                counter = 0;

                for (Object o : edgeMap.getChildren()) {
                    Element elem = (Element) o;
                    String type = elem.getAttribute("type").getValue();

                    if (type.equals("PeerToPeer")) { //Peer to Peer
                        int v1Key = Integer.parseInt(elem.getChild("v1").getText());
                        int v2Key = Integer.parseInt(elem.getChild("v2").getText());
                        TrustVertex peer1 = hiddenGraph.getVertexInGraph(new TrustVertex(v1Key));
                        TrustVertex peer2 = hiddenGraph.getVertexInGraph(new TrustVertex(v2Key));
                        startGraph.addEdge(new TrustConnection(edgeCounter), peer1, peer2);
                        hiddenGraph.addEdge(new TrustConnection(edgeCounter), peer1, peer2);
                        edgeCounter++;
                    } else {
                        ChatterBox.debug(this, "graphBuilder()", "no diea what to do here.");
                    }
                    loadingProgress(++counter);
                }
                //[end] Add Edges to graph
            }
            //[end] Create Graph

            //[start] Create Logs
            Element logElem = networkElem.getChild("logevents");
            if (logElem != null) {
                loadingChanged(logElem.getChildren().size(), "Events");
                counter = 0;
                for (Object o : logElem.getChildren()) {

                    Element event = (Element) o;
                    String type = event.getAttribute("type").getValue();
                    if (type.equals("start") || type.equals("end")) {
                        continue;
                    }
                    long timeDifference = Integer.parseInt(event.getChildText("timedifference"));
                    int paramOne = Integer.parseInt(event.getChildText("param1"));
                    int paramTwo = Integer.parseInt(event.getChildText("param2"));

                    ChatterBox.debug(this, "graphBuilder()", "A new LogEvent was created but I don't know how to get the feedback.  So it has a rating of +1");
                    TrustLogEvent evt = new TrustLogEvent(timeDifference, paramOne, paramTwo, 1.0);

                    //Asuuming all events are feedback events
                    TrustVertex assessor = hiddenGraph.getVertexInGraph(new TrustVertex(evt.getAssessee()));
                    TrustVertex assessee = hiddenGraph.getVertexInGraph(new TrustVertex(evt.getAssessor()));

                    //If the peers don't exist, add them
                    if (hiddenGraph.getPeer(evt.getAssessee()) == null) {
                        hiddenGraph.addPeer(evt.getAssessee());
                    }
                    if (hiddenGraph.getPeer(evt.getAssessor()) == null) {
                        hiddenGraph.addPeer(evt.getAssessor());
                    }
                    TrustConnection edge = null;
                    if (hiddenGraph.getType() == TrustGraph.FEEDBACK_HISTORY) {
                        edge = new TrustConnection(edgeCounter, TrustConnection.FEEDBACK, evt.getFeedback());
                    } else {
                        ChatterBox.debug(this, "graphBuilder()", "I haven't implemented adding edges to different types of graphs yet");
                    }
                    edgeCounter++;
                    hiddenGraph.addEdge(edge, assessor, assessee);

                    logList.add(evt);
                    loadingProgress(++counter);
                }
            }
            visibleGraph = startGraph;
            //[end] Create Logs
            loadingComplete();
        }
    }

    private void addEventsToGraph(Document networkDoc) {
        if (networkDoc.getRootElement().getName().equals("network")) {
            int edgeCounter = hiddenGraph.getEdgeCount();
            Element networkElem = networkDoc.getRootElement();
            Element logElem = networkElem.getChild("logevents");
            if (logElem != null) {
                loadingChanged(logElem.getChildren().size(), "Events");
                for (Object o : logElem.getChildren()) {

                    Element event = (Element) o;
                    String type = event.getAttribute("type").getValue();
                    if (type.equals("start") || type.equals("end")) {
                        continue;
                    }
                    long timeDifference = Integer.parseInt(event.getChildText("timedifference"));
                    int paramOne = Integer.parseInt(event.getChildText("param1"));
                    int paramTwo = Integer.parseInt(event.getChildText("param2"));
                    ChatterBox.debug(this, "addEventsToGraph()", "A new LogEvent was created but I don't know how to get the rating.  So it has a rating of +1");
                    TrustLogEvent evt = new TrustLogEvent(timeDifference, paramOne, paramTwo, 1.0);

                    //Asuuming all events are feedback events
                    TrustVertex assessor = hiddenGraph.getVertexInGraph(new TrustVertex(evt.getAssessee()));
                    TrustVertex assessee = hiddenGraph.getVertexInGraph(new TrustVertex(evt.getAssessor()));

                    //If the peers don't exist, add them
                    if (hiddenGraph.getPeer(evt.getAssessee()) == null) {
                        hiddenGraph.addPeer(evt.getAssessee());
                    }
                    if (hiddenGraph.getPeer(evt.getAssessor()) == null) {
                        hiddenGraph.addPeer(evt.getAssessor());
                    }
                    TrustConnection edge = null;
                    if (hiddenGraph.getType() == TrustGraph.FEEDBACK_HISTORY) {
                        edge = new TrustConnection(edgeCounter, TrustConnection.FEEDBACK, evt.getFeedback());
                    } else {
                        ChatterBox.debug(this, "addEventsToGraph()", "I haven't implemented adding edges to different types of graphs yet");
                    }
                    edgeCounter++;
                    hiddenGraph.addEdge(edge, assessor, assessee);
                    logList.add(evt);
                }
            }
        }
    }
    //[end] Graph Builder

    //[start] Getters
    public LinkedList<TrustLogEvent> getLogList() {
        return logList;
    }

    public TrustGraph getHiddenP2PNetworkGraph() {
        return hiddenGraph;
    }

    public TrustGraph getVisibleP2PNetworkGraph() {
        return visibleGraph;
    }
    //[end] Getters

    //[start] Static Methods
    public static File chooseLoadFile(String filterDescription, String[] acceptedExtensions) {
        JFileChooser fileNamer = new JFileChooser();
        fileNamer.setFileFilter(new ExtensionFileFilter(filterDescription, acceptedExtensions));
        int returnVal = fileNamer.showOpenDialog(null);


        if (returnVal == JFileChooser.APPROVE_OPTION) {
            for (String extension : acceptedExtensions) {
                if (fileNamer.getSelectedFile().getAbsolutePath().endsWith(extension)) {

                    return fileNamer.getSelectedFile();
                }
            }
            JOptionPane.showMessageDialog(null, "Error: Incorrect extension.", "Error", JOptionPane.ERROR_MESSAGE);

            return null;
        } else if (returnVal == JFileChooser.ERROR_OPTION) {
            JOptionPane.showMessageDialog(null, "Error: Could not load file.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    public static TrustGraphLoader buildGraph(InputStream inStream) throws JDOMException, IOException {

        SAXBuilder parser = new SAXBuilder();
        Document doc = parser.build(inStream);

        TrustGraphLoader loader = new TrustGraphLoader();
        loader.logList.addFirst(TrustLogEvent.getStartEvent());
        loader.logList.addLast(TrustLogEvent.getEndEvent(loader.logList.getLast()));
        loader.graphBuilder(doc);

        return loader;
    }

    public static LinkedList<TrustLogEvent> buildLogs(InputStream inStream, TrustGraph hiddenGraph) throws JDOMException, IOException {

        SAXBuilder parser = new SAXBuilder();
        Document doc = parser.build(inStream);
        TrustGraphLoader loader = new TrustGraphLoader();

        loader.hiddenGraph = hiddenGraph;
        loader.addEventsToGraph(doc);
        return loader.logList;
    }
    //[end] Static Methods
}

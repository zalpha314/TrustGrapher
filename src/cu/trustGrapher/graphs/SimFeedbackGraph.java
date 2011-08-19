///////////////////////////////SimFeedbackGraph/////////////////////////////
package cu.trustGrapher.graphs;

import cu.trustGrapher.eventplayer.TrustLogEvent;
import cu.trustGrapher.graphs.edges.SimFeedbackEdge;
import cu.repsystestbed.entities.Agent;
import cu.repsystestbed.graphs.FeedbackHistoryEdgeFactory;
import cu.repsystestbed.graphs.FeedbackHistoryGraph;
import cu.trustGrapher.loading.GraphConfig;

import org.jgrapht.graph.SimpleDirectedGraph;

/**
 * A graph that displays individual feedbacks grouped together into edges
 * @author Andrew O'Hara
 */
public class SimFeedbackGraph extends SimAbstractGraph {

//////////////////////////////////Constructor///////////////////////////////////
    /**
     * Creates a feedback graph
     * @param graphPair The graphPair that is to hold this graph
     */
    public SimFeedbackGraph(GraphConfig graphConfig) {
        super(graphConfig, (SimpleDirectedGraph) new FeedbackHistoryGraph(new FeedbackHistoryEdgeFactory()));
    }
    
///////////////////////////////////Methods//////////////////////////////////////
    public void graphEvent(TrustLogEvent event, boolean forward) {
        Agent src = ensureAgentExists(event.getAssessor(), this), sink = ensureAgentExists(event.getAssessee(), this);
        SimFeedbackEdge dynEdge = (SimFeedbackEdge) ensureEdgeExists(src, sink, this);
        SimFeedbackEdge fullEdge = (SimFeedbackEdge) fullGraph.findEdge(src, sink);
        double feedback = event.getFeedback();
        if (forward) {
            //Add the feedback to the full edge so it can be seen in the viewer            
            dynEdge.addFeedback(src, sink, feedback);
            fullEdge.addFeedback(src,sink, feedback);
        } else {
            dynEdge.removeFeedback(feedback);
            fullEdge.removeFeedback(feedback);
            if (dynEdge.feedbacks.isEmpty()){
                removeEdgeAndVertices(dynEdge); //Remove the dynamic edge
            }
        }
    }

    /**
     * Creates an edge but does not yet add the feedback to it.  As the visible edges, are added, the feedbacks will be added to the hidden edges
     * @param event The current event being processed that contains the ids of the assessor and assesse and feedback given
     */
    @Override
    public void graphConstructionEvent(TrustLogEvent event) {
        if (event != null) {
            Agent src = ensureAgentExists(event.getAssessor(), fullGraph);
            Agent sink = ensureAgentExists(event.getAssessee(), fullGraph);
            ensureEdgeExists(src, sink, fullGraph);
        }
       
    }
}
////////////////////////////////////////////////////////////////////////////////


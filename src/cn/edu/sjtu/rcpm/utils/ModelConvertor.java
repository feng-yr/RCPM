package cn.edu.sjtu.rcpm.utils;

import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.acceptingpetrinet.models.impl.AcceptingPetriNetFactory;
import org.processmining.framework.packages.PackageManager.Canceller;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.InductiveMiner.reduceacceptingpetrinet.ReduceAcceptingPetriNetKeepLanguage;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.conversion.ProcessTree2Petrinet;
import org.processmining.processtree.conversion.ProcessTree2Petrinet.InvalidProcessTreeException;
import org.processmining.processtree.conversion.ProcessTree2Petrinet.NotYetImplementedException;
import org.processmining.processtree.conversion.ProcessTree2Petrinet.PetrinetWithMarkings;

import com.raffaeleconforti.conversion.petrinet.PetriNetToBPMNConverter;

@SuppressWarnings("deprecation")
public class ModelConvertor {
	
	public static Object[] processTree2Petrinet(ProcessTree tree, Canceller canceller) {
		
		PetrinetWithMarkings pn = null;
		try {
			pn = ProcessTree2Petrinet.convert(tree);
		} catch (NotYetImplementedException e) {
			e.printStackTrace();
		} catch (InvalidProcessTreeException e) {
			e.printStackTrace();
		}

		AcceptingPetriNet a = AcceptingPetriNetFactory.createAcceptingPetriNet(pn.petrinet, pn.initialMarking,
				pn.finalMarking);

		ReduceAcceptingPetriNetKeepLanguage.reduce(a, canceller);

		return new Object[] { a.getNet(), a.getInitialMarking(), a.getFinalMarkings().iterator().next() };
	}
	
	public static BPMNDiagram processTree2BPMN(ProcessTree tree, Canceller canceller) {
		
		Object[] petriNet = processTree2Petrinet(tree, canceller);
		return PetriNetToBPMNConverter.convert((Petrinet) petriNet[0], (Marking) petriNet[1], (Marking) petriNet[2], true);
	}
}

package cn.edu.sjtu.rcpm.plugins.dialogs;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.InductiveMiner.ClassifierChooser;
import com.fluxicon.slickerbox.factory.SlickerFactory;

import cn.edu.sjtu.rcpm.parameters.RuleMiningParameters;
import cn.edu.sjtu.rcpm.parameters.RuleMiningParametersIMf;

public class RuleMiningDialog extends JPanel {
	
	private static final long serialVersionUID = -8814094265284857398L;
	private final ParametersWrapper p = new ParametersWrapper();
	private final JComboBox<Variant> variantCombobox;
	private final JLabel noiseLabel;
	private final JSlider noiseSlider;
	private final JLabel noiseValue;
	private final JLabel ruleLabel;
	private final JSlider ruleSlider;
	private final JLabel ruleValue;
	private final JLabel doiLabel;
	private final JLabel doiValue;

	public static final String email = "feng-yr@sjtu.edu.cn";
	public static final String affiliation = "Shanghai Jiao Tong University";
	public static final String author = "Yingrui Feng";

	public class ParametersWrapper {
		public RuleMiningParameters parameters;
		public Variant variant;
	}

	public abstract class Variant {
		@Override
		public abstract String toString();

		public abstract boolean hasNoise();

		public abstract boolean noNoiseImpliesFitness();

		public abstract RuleMiningParameters getMiningParameters();

		public abstract int getWarningThreshold();

		public String getDoi() {
			return null;
		}
	}

	public class VariantIMf extends Variant {
		public String toString() {
			return "Inductive Miner - infrequent (IMf)";
		}

		public boolean hasNoise() {
			return true;
		}

		public RuleMiningParameters getMiningParameters() {
			return new RuleMiningParametersIMf();
		}

		public boolean noNoiseImpliesFitness() {
			return true;
		}

		public String getDoi() {
			return null;
		}

		public int getWarningThreshold() {
			return 0;
		}
	}

	@SuppressWarnings("unchecked")
	public RuleMiningDialog(XLog log, boolean hasThreshold) {
		p.parameters = new RuleMiningParametersIMf();
		p.variant = new VariantIMf();
		SlickerFactory factory = SlickerFactory.instance();

		int gridy = 1;

		setLayout(new GridBagLayout());

		//algorithm
		final JLabel variantLabel = factory.createLabel("Variant");
		{
			GridBagConstraints cVariantLabel = new GridBagConstraints();
			cVariantLabel.gridx = 0;
			cVariantLabel.gridy = gridy;
			cVariantLabel.weightx = 0.4;
			cVariantLabel.anchor = GridBagConstraints.NORTHWEST;
			add(variantLabel, cVariantLabel);
		}

		variantCombobox = factory.createComboBox(new Variant[] { new VariantIMf() });
		{
			GridBagConstraints cVariantCombobox = new GridBagConstraints();
			cVariantCombobox.gridx = 1;
			cVariantCombobox.gridy = gridy;
			cVariantCombobox.anchor = GridBagConstraints.NORTHWEST;
			cVariantCombobox.fill = GridBagConstraints.HORIZONTAL;
			cVariantCombobox.weightx = 0.6;
			add(variantCombobox, cVariantCombobox);
			variantCombobox.setSelectedIndex(0);
		}

		gridy++;

		{
			JLabel spacer = factory.createLabel(" ");
			GridBagConstraints cSpacer = new GridBagConstraints();
			cSpacer.gridx = 0;
			cSpacer.gridy = gridy;
			cSpacer.anchor = GridBagConstraints.WEST;
			add(spacer, cSpacer);
		}

		gridy++;

		//noise threshold
		noiseLabel = factory.createLabel("Noise threshold");
		{
			GridBagConstraints cNoiseLabel = new GridBagConstraints();
			cNoiseLabel.gridx = 0;
			cNoiseLabel.gridy = gridy;
			cNoiseLabel.anchor = GridBagConstraints.WEST;
			add(noiseLabel, cNoiseLabel);
		}

		noiseSlider = factory.createSlider(SwingConstants.HORIZONTAL);
		{
			noiseSlider.setMinimum(0);
			noiseSlider.setMaximum(1000);
			noiseSlider.setValue((int) (p.parameters.getNoiseThreshold() * 1000));
			GridBagConstraints cNoiseSlider = new GridBagConstraints();
			cNoiseSlider.gridx = 1;
			cNoiseSlider.gridy = gridy;
			cNoiseSlider.fill = GridBagConstraints.HORIZONTAL;
			add(noiseSlider, cNoiseSlider);
		}

		noiseValue = factory.createLabel(String.format("%.2f", p.parameters.getNoiseThreshold()));
		{
			GridBagConstraints cNoiseValue = new GridBagConstraints();
			cNoiseValue.gridx = 2;
			cNoiseValue.gridy = gridy;
			add(noiseValue, cNoiseValue);
		}

		gridy++;
		
		final JLabel noiseExplanation = factory.createLabel("If set to 0.00, perfect log fitness is guaranteed.");
		{
			GridBagConstraints cNoiseExplanation = new GridBagConstraints();
			cNoiseExplanation.gridx = 1;
			cNoiseExplanation.gridy = gridy;
			cNoiseExplanation.gridwidth = 3;
			cNoiseExplanation.anchor = GridBagConstraints.WEST;
			add(noiseExplanation, cNoiseExplanation);
		}

		gridy++;
		
		// rule threshold
		ruleLabel = factory.createLabel("Rule threshold");
		{
			GridBagConstraints cRuleLabel = new GridBagConstraints();
			cRuleLabel.gridx = 0;
			cRuleLabel.gridy = gridy;
			cRuleLabel.anchor = GridBagConstraints.WEST;
			add(ruleLabel, cRuleLabel);
		}

		ruleSlider = factory.createSlider(SwingConstants.HORIZONTAL);
		{
			ruleSlider.setMinimum(0);
			ruleSlider.setMaximum(1000);
			ruleSlider.setValue((int) (p.parameters.getRuleThreshold() * 1000));
			GridBagConstraints cRuleSlider = new GridBagConstraints();
			cRuleSlider.gridx = 1;
			cRuleSlider.gridy = gridy;
			cRuleSlider.fill = GridBagConstraints.HORIZONTAL;
			add(ruleSlider, cRuleSlider);
		}

		ruleValue = factory.createLabel(String.format("%.2f", p.parameters.getRuleThreshold()));
		{
			GridBagConstraints cRuleValue = new GridBagConstraints();
			cRuleValue.gridx = 2;
			cRuleValue.gridy = gridy;
			add(ruleValue, cRuleValue);
		}
		
		ruleLabel.setVisible(hasThreshold);
		ruleSlider.setVisible(hasThreshold);
		ruleValue.setVisible(hasThreshold);

		gridy++;

		//spacer
		{
			JLabel spacer = factory.createLabel(" ");
			GridBagConstraints cSpacer = new GridBagConstraints();
			cSpacer.gridx = 0;
			cSpacer.gridy = gridy;
			cSpacer.anchor = GridBagConstraints.WEST;
			add(spacer, cSpacer);
		}

		gridy++;

		//spacer
		{
			JLabel spacer = factory.createLabel(" ");
			GridBagConstraints cSpacer = new GridBagConstraints();
			cSpacer.gridx = 0;
			cSpacer.gridy = gridy;
			cSpacer.anchor = GridBagConstraints.WEST;
			add(spacer, cSpacer);
		}

		gridy++;

		//classifiers
		{
			final JLabel classifierLabel = factory.createLabel("Event classifier");
			GridBagConstraints cClassifierLabel = new GridBagConstraints();
			cClassifierLabel.gridx = 0;
			cClassifierLabel.gridy = gridy;
			cClassifierLabel.weightx = 0.4;
			cClassifierLabel.anchor = GridBagConstraints.NORTHWEST;
			add(classifierLabel, cClassifierLabel);
		}

		final ClassifierChooser classifiers = new ClassifierChooser(log);
		{
			GridBagConstraints cClassifiers = new GridBagConstraints();
			cClassifiers.gridx = 1;
			cClassifiers.gridy = gridy;
			cClassifiers.anchor = GridBagConstraints.NORTHWEST;
			cClassifiers.fill = GridBagConstraints.HORIZONTAL;
			cClassifiers.weightx = 0.6;
			add(classifiers, cClassifiers);
		}

		gridy++;

		//spacer
		{
			JLabel spacer = factory.createLabel(" ");
			GridBagConstraints cSpacer = new GridBagConstraints();
			cSpacer.gridx = 0;
			cSpacer.gridy = gridy;
			cSpacer.anchor = GridBagConstraints.WEST;
			add(spacer, cSpacer);
		}

		gridy++;

		//doi
		{
			doiLabel = factory.createLabel("More information");
			GridBagConstraints cDoiLabel = new GridBagConstraints();
			cDoiLabel.gridx = 0;
			cDoiLabel.gridy = gridy;
			cDoiLabel.weightx = 0.4;
			cDoiLabel.anchor = GridBagConstraints.NORTHWEST;
			add(doiLabel, cDoiLabel);
		}

		{
			doiValue = factory.createLabel("doi doi");
			GridBagConstraints cDoiValue = new GridBagConstraints();
			cDoiValue.gridx = 1;
			cDoiValue.gridy = gridy;
			cDoiValue.anchor = GridBagConstraints.NORTHWEST;
			cDoiValue.weightx = 0.6;
			add(doiValue, cDoiValue);
		}

		gridy++;

		{
			GridBagConstraints gbcFiller = new GridBagConstraints();
			gbcFiller.weighty = 1.0;
			gbcFiller.gridy = gridy;
			gbcFiller.fill = GridBagConstraints.BOTH;
			add(Box.createGlue(), gbcFiller);
		}

		variantCombobox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Variant variant = (Variant) variantCombobox.getSelectedItem();
				float noise = p.parameters.getNoiseThreshold();
				XEventClassifier classifier = p.parameters.getClassifier();
				p.parameters = variant.getMiningParameters();
				p.parameters.setNoiseThreshold(noise);
				p.parameters.setClassifier(classifier);
				p.variant = variant;
				if (variant.hasNoise()) {
					noiseValue.setText(String.format("%.2f", p.parameters.getNoiseThreshold()));
				} else {
					int width = noiseValue.getWidth();
					int height = noiseValue.getHeight();
					noiseValue.setText("  ");
					noiseValue.setPreferredSize(new Dimension(width, height));
				}

				if (variant.getDoi() != null) {
					doiLabel.setVisible(true);
					doiValue.setVisible(true);
					doiValue.setText(variant.getDoi());
				} else {
					doiLabel.setVisible(false);
					doiValue.setVisible(false);
				}

				noiseLabel.setVisible(variant.hasNoise());
				noiseSlider.setVisible(variant.hasNoise());
				noiseExplanation.setVisible(variant.noNoiseImpliesFitness());
			}
		});

		noiseSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				p.parameters.setNoiseThreshold((float) (noiseSlider.getValue() / 1000.0));
				noiseValue.setText(String.format("%.2f", p.parameters.getNoiseThreshold()));
			}
		});
		
		ruleSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				p.parameters.setRuleThreshold((float) (ruleSlider.getValue() / 1000.0));
				ruleValue.setText(String.format("%.2f", p.parameters.getRuleThreshold()));
			}
		});

		classifiers.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				p.parameters.setClassifier(classifiers.getSelectedClassifier());
			}
		});
		p.parameters.setClassifier(classifiers.getSelectedClassifier());

		doiValue.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				String doi = ((Variant) variantCombobox.getSelectedItem()).getDoi();
				if (doi != null) {
					openWebPage(doi);
				}
			}
		});
		doiValue.setText(((Variant) variantCombobox.getSelectedItem()).getDoi());
	}

	public RuleMiningParameters getMiningParameters() {
		return p.parameters;
	}

	public Variant getVariant() {
		return p.variant;
	}

	public static void openWebPage(String url) {
		if (Desktop.isDesktopSupported()) {
			Desktop desktop = Desktop.getDesktop();
			try {
				desktop.browse(new URI(url));
			} catch (IOException | URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			Runtime runtime = Runtime.getRuntime();
			try {
				runtime.exec("xdg-open " + url);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}

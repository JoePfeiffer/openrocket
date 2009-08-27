package net.sf.openrocket.gui.plot;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import net.sf.openrocket.document.Simulation;
import net.sf.openrocket.simulation.FlightDataBranch;
import net.sf.openrocket.simulation.FlightEvent;
import net.sf.openrocket.unit.Unit;
import net.sf.openrocket.unit.UnitGroup;
import net.sf.openrocket.util.GUIUtil;
import net.sf.openrocket.util.MathUtil;
import net.sf.openrocket.util.Pair;
import net.sf.openrocket.util.Prefs;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYImageAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.text.TextUtilities;
import org.jfree.ui.LengthAdjustmentType;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.TextAnchor;

public class PlotDialog extends JDialog {
	
	private static final Color DEFAULT_EVENT_COLOR = new Color(0,0,0);
	private static final Map<FlightEvent.Type, Color> EVENT_COLORS =
		new HashMap<FlightEvent.Type, Color>();
	static {
		EVENT_COLORS.put(FlightEvent.Type.LAUNCH, new Color(255,0,0));
		EVENT_COLORS.put(FlightEvent.Type.LIFTOFF, new Color(0,80,196));
		EVENT_COLORS.put(FlightEvent.Type.LAUNCHROD, new Color(0,100,80));
		EVENT_COLORS.put(FlightEvent.Type.IGNITION, new Color(230,130,15));
		EVENT_COLORS.put(FlightEvent.Type.BURNOUT, new Color(80,55,40));
		EVENT_COLORS.put(FlightEvent.Type.EJECTION_CHARGE, new Color(80,55,40));
		EVENT_COLORS.put(FlightEvent.Type.STAGE_SEPARATION, new Color(80,55,40));
		EVENT_COLORS.put(FlightEvent.Type.APOGEE, new Color(15,120,15));
		EVENT_COLORS.put(FlightEvent.Type.RECOVERY_DEVICE_DEPLOYMENT, new Color(0,0,128));
		EVENT_COLORS.put(FlightEvent.Type.GROUND_HIT, new Color(0,0,0));
		EVENT_COLORS.put(FlightEvent.Type.SIMULATION_END, new Color(128,0,0));
	}

	private static final Map<FlightEvent.Type, Image> EVENT_IMAGES =
		new HashMap<FlightEvent.Type, Image>();
	static {
		loadImage(FlightEvent.Type.LAUNCH, "pix/spheres/red-16x16.png");
		loadImage(FlightEvent.Type.LIFTOFF, "pix/eventicons/event-liftoff.png");
		loadImage(FlightEvent.Type.LAUNCHROD, "pix/eventicons/event-launchrod.png");
		loadImage(FlightEvent.Type.IGNITION, "pix/eventicons/event-ignition.png");
		loadImage(FlightEvent.Type.BURNOUT, "pix/eventicons/event-burnout.png");
		loadImage(FlightEvent.Type.EJECTION_CHARGE, "pix/spheres/green-16x16.png");
		loadImage(FlightEvent.Type.STAGE_SEPARATION, "pix/eventicons/event-stage-separation.png");
		loadImage(FlightEvent.Type.APOGEE, "pix/eventicons/event-apogee.png");
		loadImage(FlightEvent.Type.RECOVERY_DEVICE_DEPLOYMENT, "pix/spheres/blue-16x16.png");
		loadImage(FlightEvent.Type.GROUND_HIT, "pix/spheres/gray-16x16.png");
		loadImage(FlightEvent.Type.SIMULATION_END, "pix/eventicons/event-simulation-end.png");
	}

    private static void loadImage(FlightEvent.Type type, String file) {
    	InputStream is;
 
    	is = ClassLoader.getSystemResourceAsStream(file);
    	if (is == null) {
    		System.out.println("ERROR: File " + file + " not found!");
    		return;
    	}
    	
    	try {
    		Image image = ImageIO.read(is);
    		EVENT_IMAGES.put(type, image);
    	} catch (IOException ignore) {
    		ignore.printStackTrace();
    	}
    }
    
    
    
    
    private final List<ModifiedXYItemRenderer> renderers =
    	new ArrayList<ModifiedXYItemRenderer>();
	
	private PlotDialog(Window parent, Simulation simulation, PlotConfiguration config) {
		super(parent, "Flight data plot");
		this.setModalityType(ModalityType.DOCUMENT_MODAL);
		
		final boolean initialShowPoints = Prefs.getBoolean(Prefs.PLOT_SHOW_POINTS, false);
		
		
		// Fill the auto-selections
		FlightDataBranch branch = simulation.getSimulatedData().getBranch(0);
		PlotConfiguration filled = config.fillAutoAxes(branch);
		List<Axis> axes = filled.getAllAxes();


		// Create the data series for both axes
		XYSeriesCollection[] data = new XYSeriesCollection[2];
		data[0] = new XYSeriesCollection();
		data[1] = new XYSeriesCollection();
		
		
		// Get the domain axis type
		final FlightDataBranch.Type domainType = filled.getDomainAxisType();
		final Unit domainUnit = filled.getDomainAxisUnit();
		if (domainType == null) {
			throw new IllegalArgumentException("Domain axis type not specified.");
		}
		List<Double> x = branch.get(domainType);
		
		
		// Get plot length (ignore trailing NaN's)
		int typeCount = filled.getTypeCount();
		int dataLength = 0;
		for (int i=0; i<typeCount; i++) {
			FlightDataBranch.Type type = filled.getType(i);
			List<Double> y = branch.get(type);
			
			for (int j = dataLength; j < y.size(); j++) {
				if (!Double.isNaN(y.get(j)) && !Double.isInfinite(y.get(j)))
					dataLength = j;
			}
		}
		dataLength = Math.min(dataLength, x.size());
		
		
		// Create the XYSeries objects from the flight data and store into the collections
		String[] axisLabel = new String[2];
		for (int i = 0; i < typeCount; i++) {
			// Get info
			FlightDataBranch.Type type = filled.getType(i);
			Unit unit = filled.getUnit(i);
			int axis = filled.getAxis(i);
			String name = getLabel(type, unit);
			
			// Store data in provided units
			List<Double> y = branch.get(type);
			XYSeries series = new XYSeries(name, false, true);
			for (int j=0; j < dataLength; j++) {
				series.add(domainUnit.toUnit(x.get(j)), unit.toUnit(y.get(j)));
			}
			data[axis].addSeries(series);

			// Update axis label
			if (axisLabel[axis] == null)
				axisLabel[axis] = type.getName();
			else
				axisLabel[axis] += "; " + type.getName();
		}
		
		
		// Create the chart using the factory to get all default settings
        JFreeChart chart = ChartFactory.createXYLineChart(
            "Simulated flight",
            null, 
            null, 
            null,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );
		
        chart.addSubtitle(new TextTitle(config.getName()));
        
		// Add the data and formatting to the plot
		XYPlot plot = chart.getXYPlot();
		int axisno = 0;
		for (int i=0; i<2; i++) {
			// Check whether axis has any data
			if (data[i].getSeriesCount() > 0) {
				// Create and set axis
				double min = axes.get(i).getMinValue();
				double max = axes.get(i).getMaxValue();
				NumberAxis axis = new PresetNumberAxis(min, max);
				axis.setLabel(axisLabel[i]);
//				axis.setRange(axes.get(i).getMinValue(), axes.get(i).getMaxValue());
				plot.setRangeAxis(axisno, axis);
				
				// Add data and map to the axis
				plot.setDataset(axisno, data[i]);
				ModifiedXYItemRenderer r = new ModifiedXYItemRenderer();
				r.setBaseShapesVisible(initialShowPoints);
				r.setBaseShapesFilled(true);
				renderers.add(r);
				plot.setRenderer(axisno, r);
				plot.mapDatasetToRangeAxis(axisno, axisno);
				axisno++;
			}
		}
		
		plot.getDomainAxis().setLabel(getLabel(domainType,domainUnit));
		plot.addDomainMarker(new ValueMarker(0));
		plot.addRangeMarker(new ValueMarker(0));
		
		
		
		// Create list of events to show (combine event too close to each other)
		ArrayList<Double> timeList = new ArrayList<Double>();
		ArrayList<String> eventList = new ArrayList<String>();
		ArrayList<Color> colorList = new ArrayList<Color>();
		ArrayList<Image> imageList = new ArrayList<Image>();
		
		HashSet<FlightEvent.Type> typeSet = new HashSet<FlightEvent.Type>();
		
		double prevTime = -100;
		String text = null;
		Color color = null;
		Image image = null;
		
		List<Pair<Double, FlightEvent>> events = branch.getEvents();
		for (int i=0; i < events.size(); i++) {
			Pair<Double, FlightEvent> event = events.get(i);
			double t = event.getU();
			FlightEvent.Type type = event.getV().getType();
			
			if (type != FlightEvent.Type.ALTITUDE && config.isEventActive(type)) {
				if (Math.abs(t - prevTime) <= 0.01) {
					
					if (!typeSet.contains(type)) {
						text = text + ", " + event.getV().getType().toString();
						color = getEventColor(type);
						image = EVENT_IMAGES.get(type);
						typeSet.add(type);
					}
					
				} else {
					
					if (text != null) {
						timeList.add(prevTime);
						eventList.add(text);
						colorList.add(color);
						imageList.add(image);
					}
					prevTime = t;
					text = type.toString();
					color = getEventColor(type);
					image = EVENT_IMAGES.get(type);
					typeSet.clear();
					typeSet.add(type);
					
				}
			}
		}
		if (text != null) {
			timeList.add(prevTime);
			eventList.add(text);
			colorList.add(color);
			imageList.add(image);
		}
		
		
		// Create the event markers
		
		if (config.getDomainAxisType() == FlightDataBranch.TYPE_TIME) {
			
			// Domain time is plotted as vertical markers
			for (int i=0; i < eventList.size(); i++) {
				double t = timeList.get(i);
				String event = eventList.get(i);
				color = colorList.get(i);
				
				ValueMarker m = new ValueMarker(t);
				m.setLabel(event);
				m.setPaint(color);
				m.setLabelPaint(color);
				m.setAlpha(0.7f);
				plot.addDomainMarker(m);
			}
			
		} else {
			
			// Other domains are plotted as image annotations
			List<Double> time = branch.get(FlightDataBranch.TYPE_TIME);
			List<Double> domain = branch.get(config.getDomainAxisType());
			
			for (int i=0; i < eventList.size(); i++) {
				final double t = timeList.get(i);
				String event = eventList.get(i);
				image = imageList.get(i);
				
				if (image == null)
					continue;
				
				// Calculate index and interpolation position a
				final double a;
				int tindex = Collections.binarySearch(time, t);
				if (tindex < 0) {
					tindex = -tindex -1;
				}
				if (tindex >= time.size()) {
					// index greater than largest value in time list
					tindex = time.size()-1;
					a = 0;
				} else if (tindex <= 0) {
					// index smaller than smallest value in time list
					tindex = 0;
					a = 0;
				} else {
					assert(tindex > 0);
					tindex--;
					double t1 = time.get(tindex);
					double t2 = time.get(tindex+1);
					
					if ((t1 > t) || (t2 < t)) {
						throw new RuntimeException("BUG: t1="+t1+" t2="+t2+" t="+t);
					}
					
					if (MathUtil.equals(t1, t2)) {
						a = 0;
					} else {
						a = 1 - (t-t1) / (t2-t1);
					}
				}
				
				final double xcoord;
				if (a == 0) {
					xcoord = domain.get(tindex);
				} else {
					xcoord = a * domain.get(tindex) + (1-a) * domain.get(tindex+1);
				}
				
				for (int index = 0; index < config.getTypeCount(); index++) {
					FlightDataBranch.Type type = config.getType(index);
					List<Double> range = branch.get(type);
					
					final double ycoord;
					if (a == 0) {
						ycoord = range.get(tindex);
					} else {
						ycoord = a * range.get(tindex) + (1-a) * range.get(tindex+1);
					}
					
					XYImageAnnotation annotation = 
						new XYImageAnnotation(xcoord, ycoord, image, RectangleAnchor.CENTER);
					annotation.setToolTipText(event);
					plot.addAnnotation(annotation);
				}
			}
		}
		
		
		// Create the dialog
		
		JPanel panel = new JPanel(new MigLayout("fill"));
		this.add(panel);
		
		ChartPanel chartPanel = new ChartPanel(chart,
				false, // properties
				true,  // save
				false, // print
				true,  // zoom
				true); // tooltips
		chartPanel.setMouseWheelEnabled(true);
		chartPanel.setEnforceFileExtensions(true);
		chartPanel.setInitialDelay(500);
		
		chartPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
		
		panel.add(chartPanel, "grow, wrap 20lp");
		
		final JCheckBox check = new JCheckBox("Show points");
		check.setSelected(initialShowPoints);
		check.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean show = check.isSelected();
				Prefs.putBoolean(Prefs.PLOT_SHOW_POINTS, show);
				for (ModifiedXYItemRenderer r: renderers) {
					r.setBaseShapesVisible(show);
				}
			}
		});
		panel.add(check, "split, left");
		
		panel.add(new JPanel(), "growx");

		JButton button = new JButton("Close");
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				PlotDialog.this.dispose();
			}
		});
		panel.add(button, "right");

		this.setLocationByPlatform(true);
		this.pack();
		GUIUtil.installEscapeCloseOperation(this);
		GUIUtil.setDefaultButton(button);
	}
	
	
	private String getLabel(FlightDataBranch.Type type, Unit unit) {
		String name = type.getName();
		if (unit != null  &&  !UnitGroup.UNITS_NONE.contains(unit)  &&
				!UnitGroup.UNITS_COEFFICIENT.contains(unit) && unit.getUnit().length() > 0)
			name += " ("+unit.getUnit() + ")";
		return name;
	}
	

	
	private class PresetNumberAxis extends NumberAxis {
		private final double min;
		private final double max;
		
		public PresetNumberAxis(double min, double max) {
			this.min = min;
			this.max = max;
			autoAdjustRange();
		}
		
		@Override
		protected void autoAdjustRange() {
			this.setRange(min, max);
		}
	}
	
	
	/**
	 * Static method that shows a plot with the specified parameters.
	 * 
	 * @param parent		the parent window, which will be blocked.
	 * @param simulation	the simulation to plot.
	 * @param config		the configuration of the plot.
	 */
	public static void showPlot(Window parent, Simulation simulation, PlotConfiguration config) {
		new PlotDialog(parent, simulation, config).setVisible(true);
	}
	
	
	
	private static Color getEventColor(FlightEvent.Type type) {
		Color c = EVENT_COLORS.get(type);
		if (c != null)
			return c;
		return DEFAULT_EVENT_COLOR;
	}
	
	

	
	
	/**
	 * A modification to the standard renderer that renders the domain marker
	 * labels vertically instead of horizontally.
	 */
	private static class ModifiedXYItemRenderer extends StandardXYItemRenderer {

		@Override
		public void drawDomainMarker(Graphics2D g2, XYPlot plot, ValueAxis domainAxis,
				Marker marker, Rectangle2D dataArea) {

			if (!(marker instanceof ValueMarker)) {
				// Use parent for all others
				super.drawDomainMarker(g2, plot, domainAxis, marker, dataArea);
				return;
			}

			/*
			 * Draw the normal marker, but with rotated text.
			 * Copied from the overridden method.
			 */
			ValueMarker vm = (ValueMarker) marker;
			double value = vm.getValue();
			Range range = domainAxis.getRange();
			if (!range.contains(value)) {
				return;
			}

			double v = domainAxis.valueToJava2D(value, dataArea, plot.getDomainAxisEdge());

			PlotOrientation orientation = plot.getOrientation();
			Line2D line = null;
			if (orientation == PlotOrientation.HORIZONTAL) {
				line = new Line2D.Double(dataArea.getMinX(), v, dataArea.getMaxX(), v);
			} else {
				line = new Line2D.Double(v, dataArea.getMinY(), v, dataArea.getMaxY());
			}

			final Composite originalComposite = g2.getComposite();
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, marker
					.getAlpha()));
			g2.setPaint(marker.getPaint());
			g2.setStroke(marker.getStroke());
			g2.draw(line);

			String label = marker.getLabel();
			RectangleAnchor anchor = marker.getLabelAnchor();
			if (label != null) {
				Font labelFont = marker.getLabelFont();
				g2.setFont(labelFont);
				g2.setPaint(marker.getLabelPaint());
				Point2D coordinates = calculateDomainMarkerTextAnchorPoint(g2,
						orientation, dataArea, line.getBounds2D(), marker
								.getLabelOffset(), LengthAdjustmentType.EXPAND, anchor);
				
				// Changed:
				TextAnchor textAnchor = TextAnchor.TOP_RIGHT;
				TextUtilities.drawRotatedString(label, g2, (float) coordinates.getX()+2,
						(float) coordinates.getY(), textAnchor,
						-Math.PI/2, textAnchor);
			}
			g2.setComposite(originalComposite);
		}

	}

}
package com.intel.openiot.lwm2m.client;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.general.AbstractDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.intel.openiot.lwm2m.resource.FloatValueResource;
import com.intel.openiot.lwm2m.resource.ResourceEventListener;

public class ValueUI extends JTabbedPane implements ResourceEventListener {

	JLabel wIcon = new JLabel();
	final JTextField temperature;
	JCheckBox autoGenerateValueBox;
	Timer timer;
	
	final FloatValueResource resource;
	private JButton modify;
	JCheckBox generateAbnornalValueBox;
	private XYSeriesCollection dataSet;
	private int dataNum=1;
	
	public ValueUI(String icon, FloatValueResource resource) {
		this.resource = resource;
		JTabbedPane tabbedPane = this;

	// value panel
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        
        
        BufferedImage wPic;
		try {
			URL url = this.getClass().getResource(icon);
			wPic = ImageIO.read(url);
			wIcon.setIcon(new ImageIcon(wPic));			
			panel.add(wIcon, c);
		} catch (Exception e1) {
			panel.add(new JLabel("Temperature:"), c);
		}
        
        
		temperature  = new JTextField(String.valueOf(resource.getValue()), 4);
		temperature.setEditable(false);
		temperature.setForeground(Color.BLUE);
		temperature.setFont(new Font("Arial", Font.BOLD, 30));
	        c.gridx += 1;
        panel.add(temperature, c);

        modify = new JButton("Modify...");
        c.gridx = 0;
        c.gridy = 1;
        panel.add(modify, c);
        modify.addActionListener(new ActionListener() {

//            @Override
            public void actionPerformed(ActionEvent e) {
            	String newValue = JOptionPane.showInputDialog(ValueUI.this, "Please input new value:");
            	if (newValue != null) {
            		try {
            			float value = Float.parseFloat(newValue);
            			ValueUI.this.resource.setValue(value);
            			dataSet.getSeries(0).add(dataNum++, value);
            		} catch (Exception ex) {
            			JOptionPane.showMessageDialog(ValueUI.this,
            					newValue + " is not a valid value",
            				    "error",
            				    JOptionPane.ERROR_MESSAGE);
            		}
            	}
            }

        });
        
        c.gridx = 1;
        c.gridy = 1;
        c.anchor=GridBagConstraints.EAST;
        panel.add(new JLabel("Auto Gen Value:"), c);
        
        autoGenerateValueBox = new JCheckBox();
        autoGenerateValueBox.addActionListener(new ActionListener(){
	    public void actionPerformed(ActionEvent e)
	    {
		if(autoGenerateValueBox.isSelected())
		{
		    modify.setEnabled(false);
		    if(timer==null) timer = new Timer();
		    timer.scheduleAtFixedRate(new TimerTask() {
		     @Override
		     public void run() {
//			 float random = (float)(Math.random() * 50 + 1);
			 float random = (float)(Math.sin((dataNum++)*0.3)*25+25);
			 if(generateAbnornalValueBox.isSelected() && dataNum%10==0)
			 {
			     random = (float) (Math.random()*10 + 50);
			 }
			 DecimalFormat decimalFormat = new DecimalFormat("#.#");
			 random = Float.valueOf(decimalFormat.format(random));
			 ValueUI.this.resource.setValue(random);
			 dataSet.getSeries(0).add(dataNum, random);
		     }
		    }, 0, 2000);		
		}
		else
		{
		    modify.setEnabled(true);
		    if(timer!=null)
		    {
			timer.cancel();
			timer = null;
		    }
		}
	    }});
        c.gridx = 2;
        c.gridy = 1;
        c.anchor=GridBagConstraints.WEST;
        panel.add(autoGenerateValueBox, c);

        tabbedPane.addTab("Value", null, panel, "Temperature Information");
        
        
	//2. chart panel
        JPanel chartPanel = new JPanel();
        chartPanel.setLayout(new GridBagLayout());
        GridBagConstraints polylinePanelConstrain = new GridBagConstraints();
        polylinePanelConstrain.gridx = 0;
        polylinePanelConstrain.gridy = 0;
     // Create a chart:  
        final XYSeries dataSeries = new XYSeries("");
        dataSeries.setMaximumItemCount(65);
	dataSet=new XYSeriesCollection();
	((XYSeriesCollection)dataSet).addSeries(dataSeries);
	final JFreeChart chart = createChart(dataSet);
        final ChartPanel chartPan = new ChartPanel(chart);
        chartPan.setPreferredSize(new java.awt.Dimension(250, 100));       
        chartPanel.add(chartPan, polylinePanelConstrain);
        
        polylinePanelConstrain.gridx = 0;
        polylinePanelConstrain.gridy = 1;
        polylinePanelConstrain.anchor=GridBagConstraints.EAST;
        chartPanel.add(new JLabel("Gen Abnormal Value:"), polylinePanelConstrain);
        
        generateAbnornalValueBox = new JCheckBox();
        polylinePanelConstrain.gridx = 1;
        polylinePanelConstrain.gridy = 1;
        polylinePanelConstrain.anchor=GridBagConstraints.WEST;
        chartPanel.add(generateAbnornalValueBox, polylinePanelConstrain);
        
        tabbedPane.addTab("Chart", null, chartPanel, "Temperature Chart");

        // 3. add value listener
        resource.addValueListner(this);
	}
	
	private JFreeChart createChart(AbstractDataset dataset)
	{
	    final JFreeChart chart = ChartFactory.createXYLineChart(
		    "Temperature",      // chart title
		    "Index",                      // x axis label
		    "Degree",                      // y axis label
		    (XYDataset) dataset,                  // data
		    PlotOrientation.VERTICAL,
		    false,                     // include legend
		    false,                     // tooltips
		    false                     // urls
		    );

	    // get a reference to the plot for further customisation...
	    final XYPlot plot = chart.getXYPlot();
	    plot.setBackgroundPaint(Color.lightGray);
	    plot.setDomainGridlinePaint(Color.white);
	    plot.setRangeGridlinePaint(Color.white);

	    final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
	    renderer.setSeriesLinesVisible(0, false);
	    renderer.setSeriesShapesVisible(1, false);
	    plot.setRenderer(renderer);

	    // change the auto tick unit selection to integer units only...
	    final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
	    rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
	    // OPTIONAL CUSTOMISATION COMPLETED.

	    return chart;
	}

	//	@Override
	public void resourceChanged(int resourceID, int event) {
	    switch(event)
	    {
	    case ResourceEventListener.EVENT_VALUE_CHANGED:
		temperature.setText(String.valueOf(resource.getValue()));
		break;
	    }
		
	}

}

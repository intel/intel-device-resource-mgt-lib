package com.intel.openiot.lwm2m.client;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;

import com.intel.openiot.lwm2m.resource.IntegerValueResource;
import com.intel.openiot.lwm2m.resource.ResourceEventListener;

public class SwitchUI extends JPanel implements ResourceEventListener {
	
	JRadioButton onRadioButton, offRadioButton;
	
	JLabel iconLabel = new JLabel();
	
	ImageIcon onImageIcon;
	
	ImageIcon offImageIcon;
	
	final IntegerValueResource resource;
	
	public SwitchUI(String onIcon, String offIcon, IntegerValueResource resource) {
		this.resource = resource;
		
		Border border = BorderFactory.createTitledBorder("Switch Status");
	    this.setBorder(border);
		//this.setLayout(new GridLayout(1,2));
		
		try {
			
			URL url = this.getClass().getResource(offIcon);
			BufferedImage wPic = ImageIO.read(url);
			offImageIcon = new ImageIcon(wPic);
						
			onImageIcon = new ImageIcon(this.getClass().getResource(
                    onIcon));
				
			iconLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
			this.add(iconLabel);
		} catch (Exception e1) {
			this.add(new JLabel("Switch Status:"));
		}
		
		JPanel panel = new JPanel();
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		this.add(panel);
		
		BoxLayout layout1 = new BoxLayout(panel, BoxLayout.Y_AXIS);
		
		panel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		panel.setLayout(layout1);
	    
	    //   Create group
	    ButtonGroup group = new ButtonGroup();	    
	    
	    onRadioButton = new JRadioButton("On");
	    onRadioButton.setFont(new Font("Arial", Font.BOLD, 15));
	    onRadioButton.setAlignmentX(Component.CENTER_ALIGNMENT);
	    panel.add(onRadioButton);
	    group.add(onRadioButton);
	    onRadioButton.addActionListener(new ActionListener() {

//			@Override
			public void actionPerformed(ActionEvent e) {
				on();
			}
	    	
	    });
	    
	    offRadioButton = new JRadioButton("Off");
	    offRadioButton.setFont(new Font("Arial", Font.BOLD, 15));
	    offRadioButton.setAlignmentX(Component.CENTER_ALIGNMENT);
	    panel.add(offRadioButton);
	    group.add(offRadioButton);
	    offRadioButton.addActionListener(new ActionListener() {

//			@Override
			public void actionPerformed(ActionEvent e) {
				off();
				
			}
	    	
	    });
	    updateStatus();
	    
	    this.resource.addValueListner(this);
	}

	public void resourceChanged(int resourceID, int event) {
	    switch(event)
	    {
	    case ResourceEventListener.EVENT_VALUE_CHANGED:
		updateStatus();
		break;
	    }
	}
	
	private void on() {
		resource.setValue(1);
		updateStatus();
	}
	
	private void off() {
		resource.setValue(0);
		updateStatus();
	}
	
	private void updateStatus() {
		if (resource.getValue() != 0) {
	    	onRadioButton.setSelected(true);
	    	onRadioButton.setForeground(Color.RED);
	    	offRadioButton.setSelected(false);
	    	offRadioButton.setForeground(Color.LIGHT_GRAY);
	    	iconLabel.setIcon(onImageIcon);
	    } else {
	    	onRadioButton.setSelected(false);
	    	onRadioButton.setForeground(Color.LIGHT_GRAY);
	    	offRadioButton.setSelected(true);
	    	offRadioButton.setForeground(Color.RED);
	    	iconLabel.setIcon(offImageIcon);
	    }
	}
}

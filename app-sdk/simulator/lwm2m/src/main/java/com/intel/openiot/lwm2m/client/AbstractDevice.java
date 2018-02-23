package com.intel.openiot.lwm2m.client;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import leshan.client.LwM2mClient;
import leshan.client.register.RegisterUplink;
import leshan.client.resource.LwM2mClientObjectDefinition;
import leshan.client.response.OperationResponse;

public abstract class AbstractDevice {
	
    // UI components
//    JTextField clientIP = new JTextField("0.0.0.0");
    JTextField clientPort = new JTextField("6113");
    JTextField serverIP = new JTextField("127.0.0.1");
//    JTextField serverPort = new JTextField("5688");// listening port by iAgent on gateway 
    int serverPort = 5688; // listening port by iAgent on gateway 
    JTextField deviceEndpointName = new JTextField("");
    
    JTextArea textArea = new JTextArea(5, 20);
    
    //lwm2m related 
    protected RegisterUplink registerUplink;
    protected LwM2mClientObjectDefinition objectDevice;
    protected LwM2mClient client;
    protected String deviceLocation;
    
    protected final int TIMEOUT_MS = 10000;
    private JButton regButton;
    private JButton deregButton;
   
    protected AbstractDevice() {
    	clientPort.setText(String.valueOf(findFreePort()));
    }    
    
    protected JFrame createMainFrame(AbstractDevice device) {
        final JFrame frame = new JFrame();
        frame.setTitle(getDeviceUUID());
        frame.setSize(200, 220);

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        Container contentPane = frame.getContentPane();
        contentPane.setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        panel.add(createServerPanel(), c);
        c.gridx = 1;
        c.gridy = 0;
        panel.add(createDevicePanel(), c);

        contentPane.add(panel, BorderLayout.CENTER);

        contentPane.add(createStatusPanel(), BorderLayout.SOUTH);

        frame.pack();
        
        this.register();
        
        return frame;
    }
    
    protected abstract JComponent createDevicePanel();
    
    protected abstract String getDeviceUUID();
    
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(textArea);
        // textArea.setEditable(false);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton clear = new JButton("Clear");
        panel.add(clear, BorderLayout.SOUTH);
        clear.addActionListener(new ActionListener(){
	    public void actionPerformed(ActionEvent e)
	    {
		textArea.setText("");
	    }});
        return panel;
    }
    
    protected JPanel createServerPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

//        c.gridx = 0;
//        c.gridy = 0;
//        panel.add(new JLabel("Client IP:"), c);
//        c.gridx = 1;
//        c.gridy = 0;
//        c.fill = GridBagConstraints.HORIZONTAL;
//        panel.add(clientIP, c);
//
//        c.gridx = 0;
//        c.gridy += 1;
//        c.fill = GridBagConstraints.NONE;
//        panel.add(new JLabel("Client Port:"), c);
//        c.gridx = 1;
//        c.fill = GridBagConstraints.HORIZONTAL;
//        panel.add(clientPort, c);

        c.gridx = 0;
        c.gridy += 1;
        c.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Gw IP:"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(serverIP, c);

//        c.gridx = 0;
//        c.gridy += 1;
//        c.fill = GridBagConstraints.NONE;
//        panel.add(new JLabel("Gateway Port:"), c);
//        c.gridx = 1;
//        c.fill = GridBagConstraints.HORIZONTAL;
//        panel.add(serverPort, c);
//
        c.gridx = 0;
        c.gridy += 1;
        c.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Dev ID:"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(deviceEndpointName, c);

        c.gridx = 0;
        c.gridy += 1;
        c.fill = GridBagConstraints.NONE;
        regButton = new JButton("Reg");
        regButton.addActionListener(new ActionListener() {
//            @Override
	    public void actionPerformed(ActionEvent e)
	    {
                register();
	    }
        });
        panel.add(regButton, c);
        c.gridx = 1;
        deregButton = new JButton("UnReg");
        panel.add(deregButton, c);
        deregButton.addActionListener(new ActionListener() {

//            @Override
            public void actionPerformed(ActionEvent e) {
                deregister();

            }
        });
        deregButton.setEnabled(false);
        
      //init ui component
        deviceEndpointName.setText(getDeviceUUID());
        return panel;
    }
    
    protected void register() {
        textArea.append("Try to connect gateway ("+serverIP.getText()+") ... ... ... ...");
        // Connect to the server provided
        final InetSocketAddress clientAddress = new InetSocketAddress("0.0.0.0", findFreePort());
        final InetSocketAddress serverAddress = new InetSocketAddress(serverIP.getText(), 5688);
//        textArea.append("\nclient: "+clientAddress.toString()+"    server: "+serverAddress.toString());
        registerUplink = client.startRegistration(clientAddress, serverAddress);

        final OperationResponse operationResponse = registerUplink.register(deviceEndpointName.getText(), new HashMap<String, String>(),
                TIMEOUT_MS);

        // final OperationResponse operationResponse = registerUplink.register(UUID.randomUUID().toString(),
        // new HashMap<String, String>(), TIMEOUT_MS);

        // Report registration response.
        textArea.append("\nDevice Registration: "+ (operationResponse.isSuccess()?" Successful!":" Fail!"));
        if (operationResponse.isSuccess()) {
            textArea.append("\nDevice: Registered Client Location '" + operationResponse.getLocation() + "'\n\n");
            textArea.setCaretPosition(textArea.getDocument().getLength());
            deviceLocation = operationResponse.getLocation();
            regButton.setEnabled(false);
            serverIP.setEditable(false);
            deviceEndpointName.setEditable(false);
            deregButton.setEnabled(true);
        } else {
//            System.err.println("\tDevice: " + operationResponse.getErrorMessage()+"   from "+gatewayHostName+":"+serverPort);
            textArea.append("\nDevice: " + operationResponse.getErrorMessage());
            textArea.append("\nPlease check if gateway IP is correct and connect with this device correctly!!!\n\n");
            textArea.setCaretPosition(textArea.getDocument().getLength());
        }

        // Deregister on shutdown.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                deregister();
            }
        });
    }

    private void deregister() {
        if (deviceLocation != null) {
            textArea.append("\nDevice: Deregistering Client '" + deviceLocation + "'... ... ... ...");
            registerUplink.deregister(deviceLocation, TIMEOUT_MS);
            textArea.append("\nDevice De-Register successfull!\n\n");
        }
        regButton.setEnabled(true);
        serverIP.setEditable(true);
        deviceEndpointName.setEditable(true);
        deregButton.setEnabled(false);
    }

    
    private int findFreePort() {
    	ServerSocket socket= null;
    	try {
    		socket= new ServerSocket(0);
			return socket.getLocalPort();
    	} catch (Exception e) {
    		//ignore
    		return -1;
    	} finally {
    		if (socket != null)
				try {
					socket.close();
				} catch (IOException e) {					
					e.printStackTrace();
				}
    	}
    }

    protected int getInedxByID(String deviceID)
    {
	int charIndex = deviceID.indexOf("-");
	if((charIndex<0) || charIndex==(deviceID.length()-1)) return 1;
	    
	String id =deviceID.substring(charIndex+1);
	Integer index = Integer.valueOf(id);
	return index;
    }
}

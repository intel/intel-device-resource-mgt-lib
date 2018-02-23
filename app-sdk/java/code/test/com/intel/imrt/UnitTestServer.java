


package com.intel.imrt;

import java.util.logging.Level;

import org.eclipse.californium.core.CaliforniumLogger;

public class UnitTestServer {

	static {
        CaliforniumLogger.initialize();
        CaliforniumLogger.setLevel(Level.ALL);
      }

	public static void main(String[] args) {
        // autocontrol  controlfansontemperature controlmodbusdevice
        String testCaseName = "monitorfirstdevice";
        if (args != null && args.length > 0) {
            testCaseName = args[0];
        }
        ImrtCoapServer server = new ImrtCoapServer(testCaseName);
        server.start();
	}

}

/**********************************************************************************
 * SCENARIO: 
 * The device connected to gateway is special, user need to a self defined 
 * parser for resource data to control the special device
 * 
 * SOLUTION: 
 * step 1. add a subclass of ResourceDataGeneral
 * step 2. add a self define parser by API IAgentManager.addPayloadParser()
 * step 3. get device and resource info by query API IAgentManager.DoDeviceQuery()
 * step 4. get resource info by API IAgentManager.DoResourceGET() which will use self defined parser to parse data
 * step 5. control the light according to the suer configure property "interval" and "algorithm" of this device
 *         algorithm=1: on-off-on-off...  
 * 
 ***************************************/
package com.intel.idrml.iagent.examples;


import java.util.logging.Level;

import org.eclipse.californium.core.CaliforniumLogger;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;

import com.intel.idrml.iagent.framework.IAgentManager;
import com.intel.idrml.iagent.utilities.LogUtil;


public class AppUnitTestServer extends CoapServer {

//	static {
//        CaliforniumLogger.initialize();
//        CaliforniumLogger.setLevel(Level.ALL);
//      }

	public static void main(String[] args) {
		AppUnitTestServer app = new AppUnitTestServer();
		app.run();
	}

	private void run() {
		for(int i=1; i<11; i++)
		{
			CoapResource resource=new ImrtCoapResource("test"+i, null);
			add(resource);
		}
        NetworkConfig networkConfig = NetworkConfig.getStandard();
        networkConfig.setInt(NetworkConfig.Keys.MAX_RESOURCE_BODY_SIZE, 204800);
        addEndpoint(new CoapEndpoint(5699, networkConfig));
		start();
	}

	public interface ImrtResponse {
	    public void get(CoapExchange exchange);
	    public void post(CoapExchange exchange);
	    public void put(CoapExchange exchange);
	    public void delete(CoapExchange exchange);
	}
	
	public class ImrtBaseResponseImpl implements ImrtResponse {
//	    @Override
	    public void get(CoapExchange exchange) {
	    	LogUtil.log("Coap GET: request from ["+exchange.getSourceAddress()+":"+exchange.getSourcePort()+"]"+" with requestCode: "+exchange.getRequestCode());
	    	LogUtil.log(" option: "+exchange.getRequestOptions());
	    	LogUtil.log(" requesttext: "+exchange.getRequestText());
	        exchange.respond(ResponseCode.CHANGED);
	    	LogUtil.log("Coap GET: response with ResponseCode.CHANGED");
	    }
//	    @Override
	    public void post(CoapExchange exchange) {
	    	System.out.println("\n=================================================================================");
	    	System.out.println("Coap POST: from ["+exchange.getSourceAddress()+":"+exchange.getSourcePort()+"]"+" "+exchange.getRequestCode()+" option: "+exchange.getRequestOptions()+" payload [bytes]: "+exchange.getRequestPayload().length);
	    	byte[] data = exchange.getRequestPayload();
	    	short checksum = 0;
	    	for(int i=0; i<data.length-2; i++) checksum+=data[i];
	    	short checksumInData = (short)(data[data.length-1]+data[data.length-2]*256);
	    	if(((byte)(checksum & 0xff) == data[data.length-1]) && ((byte)((checksum & 0xff00)>>8) == data[data.length-2]))
	    	{
	    		System.out.println("Case Success:  payload data checksum is right value: "+checksumInData);
	    	}
	    	else
	    	{
	    		System.out.format("Case Fail:  checksum in payload: 0x%02X%02X [%d]  V.S.  checksum calculated: 0x%04X [%d]\n", data[data.length-2], data[data.length-1],checksumInData, checksum, checksum);
	    	}
	        exchange.respond(ResponseCode.CHANGED);
	        System.out.println("Coap POST: response with ResponseCode.CHANGED "+ResponseCode.CHANGED+"\n=================================================================================\n");
	    }
//	    @Override
	    public void put(CoapExchange exchange) {
	    	System.out.println("\n=================================================================================");
	    	System.out.println("Coap PUT: from ["+exchange.getSourceAddress()+":"+exchange.getSourcePort()+"]"+" "+exchange.getRequestCode()+" option: "+exchange.getRequestOptions()+" payload [bytes]: "+exchange.getRequestPayload().length);
	    	byte[] data = exchange.getRequestPayload();
	    	short checksum = 0;
	    	for(int i=0; i<data.length-2; i++) checksum+=data[i];
	    	short checksumInData = (short)(data[data.length-1]+data[data.length-2]*256);
	    	if(((byte)(checksum & 0xff) == data[data.length-1]) && ((byte)((checksum & 0xff00)>>8) == data[data.length-2]))
	    	{
	    		System.out.println("Case Success:  payload data checksum is right value: "+checksumInData);
	    	}
	    	else
	    	{
	    		System.out.format("Case Fail:  checksum in payload: 0x%02X%02X [%d]  V.S.  checksum calculated: 0x%04X [%d]\n", data[data.length-2], data[data.length-1],checksumInData, checksum, checksum);
	    	}
	        exchange.respond(ResponseCode.CHANGED);
	        System.out.println("Coap PUT: response with ResponseCode.CHANGED "+ResponseCode.CHANGED+"\n=================================================================================\n");
	    }
//	    @Override
	    public void delete(CoapExchange exchange) {
	    	LogUtil.log("Coap DELETE: from ["+exchange.getSourceAddress()+":"+exchange.getSourcePort()+"]"+" with requestCode: "+exchange.getRequestCode());
	    	LogUtil.log(" option: "+exchange.getRequestOptions());
	    	LogUtil.log(" requesttext: "+exchange.getRequestText());
	        exchange.respond(ResponseCode.CHANGED);
	    	LogUtil.log("Coap DELETE: response with ResponseCode.CHANGED");
	    }
	}
	
    /*
     * Definition of the Hello-World Resource
     */
    class ImrtCoapResource extends CoapResource {

        private ImrtResponse imrtResponse;

        public ImrtCoapResource(String name, ImrtResponse imrtResponse) {
            super(name);
            this.imrtResponse = imrtResponse;
            if (this.imrtResponse == null) {
                this.imrtResponse = new ImrtBaseResponseImpl();
            }
            getAttributes().setTitle("Imrt-" + name);
        }

        @Override
        public Resource getChild(String name) {
            Resource resource = super.getChild(name);
            if(resource==null){
                resource = super.getChild("*");
            }
            return resource;
        }

        @Override
        public void handleGET(CoapExchange exchange) {
            imrtResponse.get(exchange);
        }

        @Override
        public void handlePOST(CoapExchange exchange) {
            imrtResponse.post(exchange);
        }

        @Override
        public void handleDELETE(CoapExchange exchange) {
            imrtResponse.delete(exchange);
        }

        @Override
        public void handlePUT(CoapExchange exchange) {
            imrtResponse.put(exchange);
        }

        @Override
        public void handleRequest(Exchange exchange) {
            String s = org.eclipse.californium.core.Utils.prettyPrint(exchange.getRequest());
//            LogUtil.log(s);
            super.handleRequest(exchange);
        }
    }	
}

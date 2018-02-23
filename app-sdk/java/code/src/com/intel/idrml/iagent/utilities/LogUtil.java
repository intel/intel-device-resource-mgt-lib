/*
 * Copyright (C) 2017 Intel Corporation.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intel.idrml.iagent.utilities;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class LogUtil {
    public enum LEVEL
    {
	DEBUG(0),
	INFO(1),
	WARN(2),
	ERR(3),
	CRITICAL(4);
	
	private int value;
	
	LEVEL(int val)
	{
	    value = val;
	}
	
	public int compare( LEVEL level)
	{
	    if(value>level.value) return 1;
	    else if(value==level.value) return 0;
	    else return -1;
	}
    }

    private static DateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm:ss");
    public static boolean logCodeInfo=false;
    public static LogUtil.LEVEL level = LEVEL.ERR;
	//private static DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    private static String getCodeAddress() {
        StackTraceElement[] sts = Thread.currentThread().getStackTrace();
        if (sts == null) {
            return null;
        }
        for (StackTraceElement st : sts) {
            if (!st.isNativeMethod() && 
            	!st.getClassName().equals(Thread.class.getName()) && 
            	!st.getClassName().equals(LogUtil.class.getName())
            	) 
            {
            	return st.toString();  
//            	 return st.getMethodName()+"(" + st.getFileName() + ":" + st.getLineNumber() + ")";
            }
        }
        return null;
    }

    private static void displayCurrentStack(int numStack)
    {
        //always the 3th stack is the caller of public method getCurrentMethodName()
        //notes: 
        //       stack[0]: getStackTrace()
        //       stack[1]: displayCurrentStack()
        //       stack[2]: log()
        //       stack[3]: caller()
        for(int i=4; i<Thread.currentThread().getStackTrace().length; i++)
        {
        	if((i-4) < numStack)
        	{
        		System.out.println(Thread.currentThread().getStackTrace()[i].toString());
        	}
        }
    }

    public static void log(LEVEL logLevel, String info)
    {
		if(logLevel.compare(level)>=0) System.out.println(dateFormat.format(Calendar.getInstance().getTime())+":"+(logCodeInfo?(getCodeAddress()+" "):" ")+info);
    }

    public static void log(LEVEL logLevel, String info, int numStack)
    {
	if(logLevel.compare(level)>=0)
	{
	    System.out.println(dateFormat.format(Calendar.getInstance().getTime())+":"+(logCodeInfo?(getCodeAddress()+" "):" ")+info);
	    displayCurrentStack(numStack);
	}
    }

    public static void log(String info)
    {
	    System.out.println(dateFormat.format(Calendar.getInstance().getTime())+":"+(logCodeInfo?(getCodeAddress()+" "):" ")+info);
    }

    public static void log(String info, int numStack)
    {
	System.out.println(dateFormat.format(Calendar.getInstance().getTime())+":"+(logCodeInfo?(getCodeAddress()+" "):" ")+info);
	displayCurrentStack(numStack);
    }

}

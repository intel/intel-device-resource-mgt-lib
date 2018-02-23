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


package com.intel.imrt.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.intel.imrt.iagent.utilities.LogUtil;

public class FileUtil {
    private static final String CONTEXT_PATH = "test-case-resources/";
    public static final String FILENAME_JSON_DATA_ = "json_data.txt";
    public static final String FILENAME_DEVICES_DATA = "rd_get_devices.txt";
    //public static String testCaseName = "";

    public static String readFile(String fileName) {
        StringBuilder result = new StringBuilder();
        try {
            String testCaseName = loadTestCaseName();
            File file = new File(CONTEXT_PATH + testCaseName + fileName);
            if (!file.exists()) {
                file = new File(CONTEXT_PATH + fileName);
            }

            LogUtil.log("read file file path : " + file.getAbsolutePath());
            BufferedReader br = new BufferedReader(new FileReader(file));
            String s = null;
            while ((s = br.readLine()) != null) {
                result.append(System.lineSeparator() + s);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    /** The default name for the configuration. */
    public static final String DEFAULT_FILE_NAME = "gateway-sdk.properties";
    public static final String TEST_CASE_TIME = "TEST_CASE_NAME";

    private static String loadTestCaseName() {
        File file = new File(CONTEXT_PATH + DEFAULT_FILE_NAME);
        if (file.exists()) {
            LogUtil.log(String.format("loading properties from file %s", file.getAbsolutePath()));
            InputStream inStream = null;
            Properties properties = new Properties();
            try {
                inStream = new FileInputStream(file);
                properties.load(inStream);
            } catch (IOException e) {
                LogUtil.log(String.format("cannot load properties from file %s: %s",
                        new Object[] { file.getAbsolutePath(), e.getMessage() }));
            } finally {
                closeStream(inStream);
            }
            String testCaseName = "";
            if (properties.containsKey(TEST_CASE_TIME)) {
                testCaseName = properties.getProperty(TEST_CASE_TIME);
            }
            if (testCaseName != null) {
                testCaseName = testCaseName.trim();
                if (testCaseName.length() > 0)
                    testCaseName = testCaseName.trim() + "/";
            }
            LogUtil.log(String.format("test case name %s", testCaseName));
            return testCaseName;
        } else {
            LogUtil.log("properties file does not exist");
            return "";
        }
    }

    public static void setTestCaseName(String name) {
        if (name != null && !"".equals(name)) {
            setProperty(name);
        }
    }

    private static void setProperty(String value) {
        File file = new File(CONTEXT_PATH + DEFAULT_FILE_NAME);
        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
            Properties properties = new Properties();
            properties.put(TEST_CASE_TIME, value);
            properties.store(writer, "gateway sdk properties file,set TEST_CASE_NAME value");
        } catch (IOException e) {
            LogUtil.log(String.format("cannot write properties to file %s: %s", file.getAbsolutePath(), e.getMessage()));
        } finally {
            closeStream(writer);
        }
    }

    private static void closeStream(final Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}

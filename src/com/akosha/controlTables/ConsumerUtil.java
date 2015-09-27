package com.akosha.controlTables;

import java.util.ResourceBundle;

public class ConsumerUtil {

    public static Object readPropertyFile(String fileName, String key){
        ResourceBundle rb = ResourceBundle.getBundle(fileName);
        return rb!=null?rb.getObject(key):null;
    }

}

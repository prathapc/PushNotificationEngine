package com.akosha.controlTables;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class PushNotificationSender {
	 public static void main(String[] args) {  
		 	ConnectionFactory factory = new ConnectionFactory();
			factory.setHost((String)ConsumerUtil.readPropertyFile("olahack", "RABBITMQ_CONNECTION_ADDRESS"));
			//factory.setUsername((String)AkoshaUtility.readPropertyFile("akosha", "RABBITMQ_CONNECTION_USERNAME"));
			//factory.setPassword((String)AkoshaUtility.readPropertyFile("akosha", "RABBITMQ_CONNECTION_PASSWORD"));
			Connection connection = null;
			try {
				connection = factory.newConnection();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(0);
			}
			
	        ExecutorService executor = Executors.newFixedThreadPool(2);//creating a pool of 2 threads  
	       
	        Runnable db_worker = new NotificationConsumer("ControlTableDBConsumer_Thread", connection, (String)ConsumerUtil.readPropertyFile("olahack", "EXCHANGE_NAME"), (String)ConsumerUtil.readPropertyFile("olahack", "DB_QUEUE_NAME"), executor);  
	        executor.execute(db_worker);//calling execute method of ExecutorService  
	        
	    } 
}

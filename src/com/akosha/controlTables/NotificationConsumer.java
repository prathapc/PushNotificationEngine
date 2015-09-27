package com.akosha.controlTables;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.concurrent.ExecutorService;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.QueueingConsumer;

public class NotificationConsumer implements Runnable{

	private Thread t;
	private String threadName;
	private Connection connection;
	private String EXCHANGE_NAME;
	private String DB_QUEUE_NAME;
	private ExecutorService executor;
	private static final Logger controlTableLogs = Logger.getLogger("ControlTableDB");
	public NotificationConsumer(String threadName, Connection connection, String EXCHANGE_NAME, String DB_QUEUE_NAME, ExecutorService executor) {
		this.threadName = threadName;
		this.connection = connection;
		this.EXCHANGE_NAME = EXCHANGE_NAME;
		this.DB_QUEUE_NAME = DB_QUEUE_NAME;
		this.executor = executor;
	}

	public void start () {
		System.out.println("Starting " +  threadName );
		if (t == null)
		{
			t = new Thread (this, threadName);
			t.start ();
		}
	}
	@SuppressWarnings("all")
	@Override
	public void run() {
		// TODO Auto-generated method stub
		Channel channel;
		java.sql.Connection dbConnection = null;
		try{
			int prefetchCount = 1;
			channel = connection.createChannel();
			channel.basicQos(prefetchCount);
			channel.queueDeclare(DB_QUEUE_NAME, true, false, false, null);
			//String queueName = channel.queueDeclare().getQueue(); //queue created nd given by server
			channel.queueBind(DB_QUEUE_NAME, EXCHANGE_NAME, "");

			System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

			QueueingConsumer consumer = new QueueingConsumer(channel);
			channel.basicConsume(DB_QUEUE_NAME, consumer);
			int count = 0;
			String output;
			while (true) {
				count++;
				QueueingConsumer.Delivery delivery = consumer.nextDelivery(); //channel.basicGet

				byte[] bs = delivery.getBody();
				String str = new String(bs);
				controlTableLogs.info("Got msg from queue::"+str);
				JSONObject notificationJson = new JSONObject(str);
				
				int customerId = notificationJson.getInt("customerId");
				Date notificationTime = new Date(notificationJson.getString("notificationTime"));
				if(notificationTime.after(new Date())) {
					if(pushNotification(customerId)) {
						System.out.println("Notification sent!!");
						channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
					}
				}else {
					Thread.sleep(60000);
				}
			}
		}catch (Exception e) {
			e.printStackTrace();
			executor.shutdown();
			System.exit(0);
		}
	}

	private boolean pushNotification(int customerId) throws ClientProtocolException, IOException, JSONException {
		// TODO Auto-generated method stub
		HttpClient client = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost("https://gcm-http.googleapis.com/gcm/send");
		httpPost.addHeader("Content-Type", "application/json");
		httpPost.addHeader("Authorization", "key= AIzaSyBhHFgBFONFOAYzn8iNVRvwdh54BHvnl0k");
		JSONObject gcmBody  = new JSONObject();
		JSONArray regArray = new JSONArray();
		regArray.put(0, "APA91bGxfcFj2fcQkFlVI5Fc-U2QjiD3co7Q6d3_HRD2x_n1jmNxilxyRtL4HWuJ2mAGY9utbsbSADjX3lzs9OjjgjmERPuGqGIgr4qcv0kTUoXlYGQyA44mgV8ZQOGtsZM6B1LeiRnP");
		gcmBody.put("registration_ids", regArray);
		
		JSONObject gcmData = new JSONObject();
		gcmData.put("id", customerId);
		gcmBody.put("data", gcmData);
		StringEntity data = new StringEntity(gcmBody.toString());
		httpPost.setEntity(data);
		
		HttpResponse response = client.execute(httpPost);
		System.out.println(response.getStatusLine());
		if(response.getStatusLine().getStatusCode() == 200) {
			BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
			String output = "";
			while ((output = br.readLine()) != null) {
				System.out.println(output);
				if(output.contains("success")) {
					return true;
				}else {
					System.exit(0);
				}
			}
	
		}
		else {
			System.exit(0);
		}
		return false;
	}

	public Thread getT() {
		return t;
	}

	public void setT(Thread t) {
		this.t = t;
	}

	public String getThreadName() {
		return threadName;
	}

	public void setThreadName(String threadName) {
		this.threadName = threadName;
	}

	public Connection getConnection() {
		return connection;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}
}

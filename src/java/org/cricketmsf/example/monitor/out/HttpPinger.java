/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cricketmsf.example.monitor.out;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import org.cricketmsf.Adapter;
import org.cricketmsf.Event;
import org.cricketmsf.Kernel;
import org.cricketmsf.out.OutboundAdapter;

/**
 *
 * @author greg
 */
public class HttpPinger extends OutboundAdapter implements HttpPingerIface, Adapter {

    private final String USER_AGENT = "Mozilla/5.0";

    private HashMap<String, String> delayDefinitions = new <String, String>HashMap();

    @Override
    public void loadProperties(HashMap<String, String> properties) {
        String urls = properties.get("urls");
        String[] tokens = urls.split(";");
        String url = null;
        String delay = null;
        String[] urlTokens;
        for (String token : tokens) {
            urlTokens = token.split("\\s+"); //split by space
            int i = 0;
            for (String t : urlTokens) {
                if (i == 0) {
                    url = t;
                } else if (i == 1) {
                    delay = t;
                }
                i++;
            }
            delayDefinitions.put(url, delay);
        }
        //sendEvent(new Event(this.getClass().getSimpleName(),Event.LOG_INFO,"",null,"urls: " + urls));
        //System.out.println("urls: " + urls);
    }

    @Override
    public PingResult sendGET(String urlToPing) {
        PingResult result = new PingResult(-1, -1, -1);
        try {
            
            Kernel.getInstance().handleEvent(new Event(this.getClass().getSimpleName(),Event.CATEGORY_LOG,Event.LOG_FINE,null,"pinging " + urlToPing));
            
            long startPoint = System.currentTimeMillis();
            URL obj = new URL(urlToPing);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", USER_AGENT);
            result.setCode(con.getResponseCode());
            result.setResponseTime(System.currentTimeMillis() - startPoint);
            if (result.getCode() == HttpURLConnection.HTTP_OK) { // success
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                result.setContentLength(response.length());
                // print result
                //System.out.println(response.toString());
            } else {
                //System.out.println("GET request not worked");
            }
        } catch (Exception e) {
            //e.printStackTrace();
            return new PingResult(-1, -1, -1);
        }
        return result;
    }

    @Override
    public HashMap<String,String> getDefinitions(){
        return delayDefinitions;
    }
}

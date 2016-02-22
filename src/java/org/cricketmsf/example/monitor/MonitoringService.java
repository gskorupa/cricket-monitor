/*
 * Copyright 2016 Grzegorz Skorupa <g.skorupa at gmail.com>.
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
package org.cricketmsf.example.monitor;

import org.cricketmsf.example.monitor.in.ApiAdapterIface;
import org.cricketmsf.example.monitor.out.HttpPingerIface;
import org.cricketmsf.example.monitor.out.PingResult;
import org.cricketmsf.example.monitor.out.PingResultStoreIface;
import java.util.HashMap;
import java.util.Map;
import org.cricketmsf.Event;
import org.cricketmsf.EventHook;
import org.cricketmsf.HttpAdapterHook;
import org.cricketmsf.Kernel;
import org.cricketmsf.RequestObject;
import org.cricketmsf.in.http.EchoHttpAdapterIface;
import org.cricketmsf.in.http.FileResult;
import org.cricketmsf.in.http.HtmlGenAdapterIface;
import org.cricketmsf.in.http.HttpAdapter;
import org.cricketmsf.in.http.ParameterMapResult;
import org.cricketmsf.in.http.Result;
import org.cricketmsf.in.scheduler.SchedulerIface;
import org.cricketmsf.out.db.KeyValueCacheAdapterIface;
import org.cricketmsf.out.html.HtmlReaderAdapterIface;
import org.cricketmsf.out.log.LoggerAdapterIface;

/**
 * EchoService
 *
 * @author greg
 */
public class MonitoringService extends Kernel {

    // adapterClasses - built in (do not remove)
    LoggerAdapterIface logAdapter = null;
    EchoHttpAdapterIface httpAdapter = null;
    KeyValueCacheAdapterIface cache = null;
    SchedulerIface scheduler = null;
    HtmlGenAdapterIface htmlAdapter = null;
    HtmlReaderAdapterIface htmlReaderAdapter = null;
    
    // adapter classes - dedicated
    HttpPingerIface pinger = null;
    PingResultStoreIface results = null;
    ApiAdapterIface apiAdapter = null;
    
    public MonitoringService() {
        // built in aapters
        registerAdapter(logAdapter, LoggerAdapterIface.class);
        registerAdapter(httpAdapter, EchoHttpAdapterIface.class);
        registerAdapter(cache, KeyValueCacheAdapterIface.class);
        registerAdapter(scheduler, SchedulerIface.class);
        registerAdapter(htmlAdapter, HtmlGenAdapterIface.class);
        registerAdapter(htmlReaderAdapter, HtmlReaderAdapterIface.class);
        // dedicated adapters
        registerAdapter(pinger, HttpPingerIface.class);
        registerAdapter(results, PingResultStoreIface.class);
        registerAdapter(apiAdapter, ApiAdapterIface.class);
    }

    @Override
    public void getAdapters() {
        logAdapter = (LoggerAdapterIface) getRegistered(LoggerAdapterIface.class);
        httpAdapter = (EchoHttpAdapterIface) getRegistered(EchoHttpAdapterIface.class);
        cache = (KeyValueCacheAdapterIface) getRegistered(KeyValueCacheAdapterIface.class);
        scheduler = (SchedulerIface) getRegistered(SchedulerIface.class);
        htmlAdapter = (HtmlGenAdapterIface) getRegistered(HtmlGenAdapterIface.class);
        htmlReaderAdapter = (HtmlReaderAdapterIface) getRegistered(HtmlReaderAdapterIface.class);
        pinger = (HttpPingerIface) getRegistered(HttpPingerIface.class);
        results = (PingResultStoreIface) getRegistered(PingResultStoreIface.class);
        apiAdapter =(ApiAdapterIface) getRegistered(ApiAdapterIface.class);
    }

    @Override
    public void runOnce() {
        super.runOnce();
        System.out.println("Hello from BasicService.runOnce()");
    }

    @HttpAdapterHook(handlerClassName = "EchoHttpAdapterIface", requestMethod = "GET")
    public Object doGetEcho(Event requestEvent) {
        return sendEcho((RequestObject) requestEvent.getPayload());
    }
    
    @HttpAdapterHook(handlerClassName = "HtmlGenAdapterIface", requestMethod = "GET")
    public Object doGet(Event event) {
        RequestObject request = (RequestObject) event.getPayload();
        Result result = getFile(request);
        HashMap<String, String> data = new HashMap();
        //copy parameters from request to response data without modification
        Map<String, Object> map = request.parameters;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            data.put(entry.getKey(), (String) entry.getValue());
        }
        result.setData(data);
        return result;
    }

    @HttpAdapterHook(handlerClassName = "ApiAdapterIface", requestMethod = "GET")
    public Object doGetResults(Event event) {
        RequestObject request = (RequestObject) event.getPayload();
        HashMap list = new HashMap();
        for(Object key: results.getKeySet()){
            list.put((String)key, results.get((String)key));
        }
        ParameterMapResult result = new ParameterMapResult();
        result.setData(list);
        result.setCode(200);
        return result;
    }
    
    @EventHook(eventCategory = "LOG")
    public void logEvent(Event event) {
        logAdapter.log(event);
    }

    @EventHook(eventCategory = "*")
    public void processEvent(Event event) {
        if (event.getTimePoint() != null) {
            scheduler.handleEvent(event);
        } else {
            System.out.println(event.getPayload().toString());
        }
    }

    @EventHook(eventCategory = "ping")
    public void pingExternalService(Event event) {
        if (event.getTimePoint() != null) {
            scheduler.handleEvent(event);
        } else {
            // send ping using adapter
            PingResult result = pinger.sendGET(event.getPayload().toString());
            
            logEvent(new Event(this.getClass().getSimpleName(),Event.CATEGORY_LOG,Event.LOG_FINE,null,
                    "PING " + ": "
                    + event.getPayload() + " "
                    + result.getCode() + " "
                    + result.getResponseTime() + " "
                    + result.getContentLength()
            ));
            String url=(String)event.getPayload();
            // save response time to db
            results.put(url, result);
            // create new event for scheduler
            // find delay for url
            String delay=pinger.getDefinitions().get(url);
            scheduler.handleEvent(new Event("BasicService", "ping", "", delay, url));
        }
    }

    @Override
    protected void runInitTasks() {

        String urls = getConfiguration(this.getClass().getName())
                .getAdapterConfiguration("HttpPingerIface").getProperty("urls");
        if (!scheduler.isRestored()) {
            String delay;
            for(String url: pinger.getDefinitions().keySet()){
                delay=pinger.getDefinitions().get(url);
                scheduler.handleEvent(new Event("BasicService", "ping", "", delay, url));
            }
        }
    }

    public Object sendEcho(RequestObject request) {
        //
        Long counter;
        counter = (Long) cache.get("counter", new Long(0));
        counter++;
        cache.put("counter", counter);

        ParameterMapResult r = new ParameterMapResult();
        HashMap<String, Object> data = new HashMap();
        Map<String, Object> map = request.parameters;
        data.put("request.method", request.method);
        data.put("request.pathExt", request.pathExt);
        data.put("echo counter", cache.get("counter"));
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            //System.out.println(entry.getKey() + "=" + entry.getValue());
            data.put(entry.getKey(), (String) entry.getValue());
        }
        if (data.containsKey("error")) {
            r.setCode(HttpAdapter.SC_INTERNAL_SERVER_ERROR);
            data.put("error", "error forced by request");
        } else {
            r.setCode(HttpAdapter.SC_OK);
        }
        r.setData(data);
        return r;
    }
    
    private Result getFile(RequestObject request) {
        logEvent(new Event("EchoService", Event.CATEGORY_LOG, Event.LOG_FINEST, "", "STEP1"));
        byte[] fileContent = {};
        String filePath = request.pathExt;
        logEvent(new Event("EchoService", Event.CATEGORY_LOG, Event.LOG_FINEST, "", "pathExt=" + filePath));
        String fileExt = "";
        if (!(filePath.isEmpty() || filePath.endsWith("/")) && filePath.indexOf(".") > 0) {
            fileExt = filePath.substring(filePath.lastIndexOf("."));
        }
        Result result;
        switch (fileExt.toLowerCase()) {
            case ".jpg":
            case ".jpeg":
            case ".gif":
            case ".png":
                result = new FileResult();
                break;
            default:
                fileExt = ".html";
                result = new ParameterMapResult();
        }
        try {
            byte[] b = htmlReaderAdapter.readFile(filePath);
            result.setPayload(b);
            result.setFileExtension(fileExt);
            result.setCode(HttpAdapter.SC_OK);
            result.setMessage("");
        } catch (Exception e) {
            logEvent(new Event("EchoService", Event.CATEGORY_LOG, Event.LOG_WARNING, "", e.getMessage()));
            result.setPayload(fileContent);
            result.setFileExtension(fileExt);
            result.setCode(HttpAdapter.SC_NOT_FOUND);
            result.setMessage("file not found");
        }
        return result;
    }

}

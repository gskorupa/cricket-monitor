/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cricketmsf.example.monitor.out;

/**
 *
 * @author greg
 */
public class PingResult {
    public int code;
    public long responseTime;
    public long contentLength;

    public PingResult(){
        
    }
    
    public PingResult(int code, long time, long length){
        setCode(code);
        setResponseTime(time);
        setContentLength(length);
    }
    
    /**
     * @return the code
     */
    public int getCode() {
        return code;
    }

    /**
     * @param code the code to set
     */
    public void setCode(int code) {
        this.code = code;
    }

    /**
     * @return the responseTime
     */
    public long getResponseTime() {
        return responseTime;
    }

    /**
     * @param responseTime the responseTime to set
     */
    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
    }

    /**
     * @return the contentLength
     */
    public long getContentLength() {
        return contentLength;
    }

    /**
     * @param contentLength the contentLength to set
     */
    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }
}

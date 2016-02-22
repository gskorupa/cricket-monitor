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
public class PingDefinition {
    
    String url;
    protected String delayDefinition;

    /**
     * @return the delayDefinition
     */
    public String getDelayDefinition() {
        return delayDefinition;
    }

    /**
     * @param delayDefinition the delayDefinition to set
     */
    public void setDelayDefinition(String delayDefinition) {
        this.delayDefinition = delayDefinition;
    }
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cricketmsf.example.monitor.out;

import java.util.HashMap;

/**
 *
 * @author greg
 */
public interface HttpPingerIface {
    
    public PingResult sendGET(String urlToPing);
    public HashMap<String,String> getDefinitions();
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webrtc.server.Files;

/**
 *
 * @author gaelph
 */
public class RootFolderNotFoundException extends Exception {
    
    private static final long serialVersionUID = -6642630653319118260L;
    
    public RootFolderNotFoundException() {
        super();
    }
    
    public RootFolderNotFoundException(String path) {
        super(path + " doesn't exist or is not a folder");
    }
     
}

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
public class NoRootFolderException extends Exception {
    
    private static final long serialVersionUID = 6856298911333334521L;
    
    public NoRootFolderException() {
        super("Root folder isn't set");
    }
}

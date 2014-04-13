package com.example.services;

import android.util.Log;

public class NativeWrapper {

	// Declare native method (and make it public to expose it directly)
    public static native int is_synch_proc_switch();
    
    // Provide additional functionality, that &quot;extends&quot; the native method
    public static int is_in_emu()
    {
        return is_synch_proc_switch();
    }
     
    
    // Load library
    static {
        try{
        	System.loadLibrary("bt_detect2");
        }catch(UnsatisfiedLinkError e){
            //nothing to do
        	System.out.println("NativeWrapper : ");
            System.out.println("Couldn't load CallApi");
            System.out.println(e.getMessage());
        }
        catch(ExceptionInInitializerError e){
            //nothing to d
        	System.out.println("NativeWrapper : ");
            Log.d("NativeWrapper", e.getMessage());
             Log.d("NativeWrapper",e.getCause().toString());
        }
    }
	
}

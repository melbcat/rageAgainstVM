package com.example.services;

public class BinaryTranslationDetection {
	
	// Declare native method (and make it public to expose it directly)
    public static native int is_bt();
    
 // Provide additional functionality, that &quot;extends&quot; the native method
    public static int is_in_emu()
    {
        return is_bt();
    }
     
    
    // Load library
    static {
     try{
    	System.loadLibrary("bt_detect");
     
    }catch(UnsatisfiedLinkError e){
        //nothing to do
    	System.out.println("BinaryTranslationDetection : ");
        System.out.println("UnsatisfiedLinkError Exception : Couldn't load CallApi");
        System.out.println(e.getMessage());
    }
    catch(ExceptionInInitializerError e){
        
    	System.out.println("BinaryTranslationDetection : ");
        System.out.println("ExceptionInInitializerError : Couldn't load CallApi");
        System.out.println(e.getMessage());
    }
    }

}

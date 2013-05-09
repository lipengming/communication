package task;



import javax.sound.sampled.LineUnavailableException;
/**
 * 
 * @author lpm
 *
 */
public class VoiceChat {
	VoiceSend vs;
	VoiceReceive vr;
	public VoiceChat(String send_ip){
		
		try {
			vs=new VoiceSend(send_ip);
			vs.start();
		} catch (SecurityException e) {			
			e.printStackTrace();
		} catch (LineUnavailableException e) {
			
			e.printStackTrace();
		}
		
		
        try {
			vr = new VoiceReceive();
		} catch (SecurityException e1) {
			
			e1.printStackTrace();
		} catch (LineUnavailableException e1) {
			
			e1.printStackTrace();
		}
        vr.start();
		
	}
	
	void stop(){
		vs.stop();
		vr.stop();
	}
}

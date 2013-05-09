package task;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
/**
 * 
 * @author lpm
 *
 */
public class VoiceReceive implements Runnable{
	
	final static int _BUFFSIZE = 512;
	byte _dataBuf[] = new byte[_BUFFSIZE];
	int _remotePort = 0;
	String _ipAdd;
	InetAddress _remoteHost = null;
	DatagramSocket _UDPClientSocket = null;
	DatagramPacket _ClientPacket = null;

	private final javax.sound.sampled.AudioFormat.Encoding _ENC;
	AudioFormat _format = null;
	Thread _thread = null;
	TargetDataLine _tdLine = null;
	SourceDataLine _sdLine = null;
	DataLine.Info _inInfo = null;
	DataLine.Info _outInfo = null;
	public  VoiceReceive()throws  LineUnavailableException, SecurityException {
		_ENC = javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED; 
		_format = new AudioFormat(_ENC, 8000F, 8, 1, 1, 8000F, true);
		_inInfo = new javax.sound.sampled.DataLine.Info(TargetDataLine.class,
				_format);
		_thread = null;
		_outInfo = new DataLine.Info(SourceDataLine.class, _format);
	
	}
	/**
	 * 将字节流压缩成音频格式，通过Clip类进行播放
	 * @param bytes
	 */
	public void outPutSound(byte[] bytes){
		 DataLine.Info dlInfo = new DataLine.Info(Clip.class,_format, bytes.length);
		 try {
			 Clip clip = (Clip) AudioSystem.getLine(dlInfo);
			 clip.open(_format,bytes,0,bytes.length);
			 clip.start();
		 } catch (LineUnavailableException e) {
			 e.printStackTrace();
		 }
		} 
	/**
	 * 建立udp连接，从连接管道中获取数据返回一个自己数组
	 */
	public byte[] receive() {		
		try {
			try 
			 {			
				_UDPClientSocket=new DatagramSocket(3333);
			 } catch (SocketException e) {
				e.printStackTrace();
			 }
			//System.out.println("Start");
			_ClientPacket=new DatagramPacket(_dataBuf,_BUFFSIZE);			
			_UDPClientSocket.receive(_ClientPacket);
			//System.out.println("ok");
			//System.out.println(ClientPacket.getData().toString());
			_UDPClientSocket.close();
			_UDPClientSocket=null;
		} 
		catch (NullPointerException e) {
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		return(_dataBuf);
	
	}	
	
	public void start()
	{
	  _thread = new Thread(this);
	  _thread.start();
	 }
	 public void stop()
   {
		 _thread = null;
   }
	
	@Override
	public void run() {
		Thread thisThread = Thread.currentThread();
		try {
			VoiceReceive vr = new VoiceReceive();
			while(_thread==thisThread){
				byte[] inbyte=vr.receive();
				vr.outPutSound(inbyte);
			}			
		} catch (SecurityException e) {
			
			e.printStackTrace();
		} catch (LineUnavailableException e) {
			
			e.printStackTrace();
		}
	}
	
}

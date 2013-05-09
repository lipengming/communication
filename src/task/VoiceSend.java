package task;

import java.io.IOException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

public class VoiceSend implements Runnable, LineListener {
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

	public VoiceSend(String ip) throws LineUnavailableException,
			SecurityException {
		_ipAdd = ip;
		_ENC = javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;
		_format = new AudioFormat(_ENC, 8000F, 8, 1, 1, 8000F, true);
		_inInfo = new javax.sound.sampled.DataLine.Info(TargetDataLine.class,
				_format);
		_thread = null;
		_outInfo = new DataLine.Info(SourceDataLine.class, _format);
		//开启搜集音频，以_ENC格式放在缓冲区
		try {

			_tdLine = (TargetDataLine) AudioSystem.getLine(_inInfo);
			_tdLine.open(_format, _tdLine.getBufferSize());
			_tdLine.addLineListener(this);

		} catch (Exception exception) {
			System.out.println(exception.toString());
		}
	}
/**
 * 发送指定字节长度的自己流到udp通道
 * @param abyte0
 * @param inBytes
 */
	public void send(byte abyte0[], int inBytes) {
		try {
			_dataBuf = abyte0;
			try {
				_UDPClientSocket = new DatagramSocket(3000);
			} catch (SocketException e) {
				e.printStackTrace();
			}
			_ClientPacket = new DatagramPacket(_dataBuf, inBytes,
					InetAddress.getByName(_ipAdd), 3333);
			_UDPClientSocket.send(_ClientPacket);
			for (int i = 0; i < _dataBuf.length; i++)
				_dataBuf[i] = 0;

			_UDPClientSocket.close();
			_UDPClientSocket = null;
		} catch (NullPointerException e) {
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
//开启录制
	public void start() {
		_thread = new Thread(this);
		_thread.start();
	}
//停止录制
	public void stop() {
		_thread = null;
	}
//关闭录制
	public void shutdown() {
		if (_tdLine != null) {
			if (_tdLine.isActive())
				_tdLine.stop();
			_tdLine.close();
			_tdLine = null;
		}
	}

	@Override
	public void update(LineEvent event) {
	}

	@Override
	public void run() {
		Thread thisThread = Thread.currentThread();
		byte abyte0[] = new byte[_BUFFSIZE];
		_tdLine.start();
		while (_thread == thisThread) {
			try {
				int i;
				if ((i = _tdLine.read(abyte0, 0, _BUFFSIZE)) == -1)
					break;
				// outPutSound(abyte0);
				this.send(abyte0, i);
				// td.stop();
			} catch (ArrayIndexOutOfBoundsException e) {
			} catch (NullPointerException er) {
				System.out.println(er.toString());
			}
		}

	}

}

package tun2socks.core;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;




/**
 * Created by zengzheying on 15/12/23.
 */
public class LocalVpnService extends VpnService implements Runnable {

	public static LocalVpnService Instance;
	public static String ConfigUrl;
	public static boolean IsRunning = false;

	private static int ID;
	private static int LOCAL_IP;
	private static ConcurrentHashMap<onStatusChangedListener, Object> m_OnStatusChangedListeners = new
			ConcurrentHashMap<onStatusChangedListener, Object>();

	private Thread m_VPNThread;
	private ParcelFileDescriptor m_VPNInterface;
	private TcpProxyServer m_TcpProxyServer;
	private DnsProxy m_DnsProxy;
	private FileOutputStream m_VPNOutputStream;

	private byte[] m_Packet;
	private IPHeader m_IPHeader;
	private TCPHeader m_TCPHeader;
	private UDPHeader m_UDPHeader;
	private ByteBuffer m_DNSBuffer;
	private Handler m_Handler;
	private long m_SentBytes;
	private long m_ReceivedBytes;

	public LocalVpnService() {
		ID++;
		m_Handler = new Handler();
		m_Packet = new byte[20000];
		m_IPHeader = new IPHeader(m_Packet, 0);
		// 20 = ip报文头部
		m_TCPHeader = new TCPHeader(m_Packet, 20);
		m_UDPHeader = new UDPHeader(m_Packet, 20);
		// 28 = ip报文头部 + udp报文头部
		m_DNSBuffer = ((ByteBuffer) ByteBuffer.wrap(m_Packet).position(28)).slice();
		Instance = this;

		if (ProxyConfig.IS_DEBUG) {
			DebugLog.i("New VPNService(%d)\n", ID);
		}
	}

	public static void addOnStatusChangedListener(onStatusChangedListener listener) {
		if (!m_OnStatusChangedListeners.containsKey(listener)) {
			m_OnStatusChangedListeners.put(listener, 1);
		}
	}

	public static void removeOnStatusChangedListener(onStatusChangedListener listener) {
		if (m_OnStatusChangedListeners.containsKey(listener)) {
			m_OnStatusChangedListeners.remove(listener);
		}
	}

	@Override
	public void onCreate() {
		if (ProxyConfig.IS_DEBUG) {
			DebugLog.i("VPNService(%s) created.\n", ID);
		}
		// Start a new session by creating a new thread.
		m_VPNThread = new Thread(this, "VPNServiceThread");
		m_VPNThread.start();
		super.onCreate();
	}


	//只设置IsRunning标志位
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		IsRunning = true;
		return super.onStartCommand(intent, flags, startId);
	}

	//终止未停止的VPN线程
	@Override
	public void onDestroy() {
		if (ProxyConfig.IS_DEBUG) {
			DebugLog.i("VPNService(%s) destroyed.\n", ID);
		}
		if (m_VPNThread != null) {
			m_VPNThread.interrupt();
		}
	}

	private void onStatusChanged(final String status, final boolean isRunning) {
		m_Handler.post(new Runnable() {
			@Override
			public void run() {
				for (Map.Entry<onStatusChangedListener, Object> entry : m_OnStatusChangedListeners.entrySet()) {
					entry.getKey().onStatusChanged(status, isRunning);
				}
			}
		});
	}

	public void writeLog(final String format, Object... args) {
		if (ProxyConfig.IS_DEBUG) {
			DebugLog.i(format, args);
		}
		final String logString = String.format(format, args);
		m_Handler.post(new Runnable() {
			@Override
			public void run() {
				for (Map.Entry<onStatusChangedListener, Object> entry : m_OnStatusChangedListeners.entrySet()) {
					entry.getKey().onLogReceived(logString);
				}
			}
		});
	}

	//发送UDP数据报
	public void sendUDPPacket(IPHeader ipHeader, UDPHeader udpHeader) {
		try {
			CommonMethods.ComputeUDPChecksum(ipHeader, udpHeader);
			this.m_VPNOutputStream.write(ipHeader.m_Data, ipHeader.m_Offset, ipHeader.getTotalLength());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	String getAppInstallID() {
		SharedPreferences preferences = getSharedPreferences("SmartProxy", MODE_PRIVATE);
		String appInstallID = preferences.getString("AppInstallID", null);
		if (appInstallID == null || appInstallID.isEmpty()) {
			appInstallID = UUID.randomUUID().toString();
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString("AppInstallID", appInstallID);
			editor.apply();
		}
		return appInstallID;
	}

	String getVersionName() {
		try {
			PackageManager packageManager = getPackageManager();
			// getPackageName()是你当前类的包名，0代表是获取版本信息
			PackageInfo packInfo = packageManager.getPackageInfo(getPackageName(), 0);
			String version = packInfo.versionName;
			return version;
		} catch (Exception e) {
			return "0.0";
		}
	}

	//建立VPN，同时监听网络输入输出流的数据
	private void runVPN() throws Exception {
		this.m_VPNInterface = establishVPN();
		this.m_VPNOutputStream = new FileOutputStream(m_VPNInterface.getFileDescriptor());
		FileInputStream in = new FileInputStream(m_VPNInterface.getFileDescriptor());
		int size = 0;
		while (size != -1 && IsRunning) {
			while ((size = in.read(m_Packet)) > 0 && IsRunning) {
				if (m_DnsProxy.Stopped || m_TcpProxyServer.Stopped) {
					in.close();
					throw new Exception("LocalServer stopped.");
				}
				onIPPacketReceived(m_IPHeader, size);
			}
			Thread.sleep(100);
		}
		in.close();
		disconnectVPN();
	}

	void onIPPacketReceived(IPHeader ipHeader, int size) throws IOException {

//		if (ProxyConfig.IS_DEBUG) {
//			DebugLog.i("Source IP: %s -> Destination IP: %s", CommonMethods.ipIntToString(ipHeader.getSourceIP()),
//					CommonMethods.ipIntToString(ipHeader.getDestinationIP()));
//		}

		switch (ipHeader.getProtocol()) {
			case IPHeader.TCP:
				TCPHeader tcpHeader = m_TCPHeader;
				tcpHeader.m_Offset = ipHeader.getHeaderLength();
				if (ipHeader.getSourceIP() == LOCAL_IP) {  //本地TCP服务器发过来的ip数据包
					if (tcpHeader.getSourcePort() == m_TcpProxyServer.Port) {// 收到本地TCP服务器数据
						NatSession session = NatSessionManager.getSession(tcpHeader.getDestinationPort());
						if (session != null) {
							ipHeader.setSourceIP(ipHeader.getDestinationIP());  //修改ip，欺骗客户端
							tcpHeader.setSourcePort(session.RemotePort);
							ipHeader.setDestinationIP(LOCAL_IP);

							CommonMethods.ComputeTCPChecksum(ipHeader, tcpHeader);
							m_VPNOutputStream.write(ipHeader.m_Data, ipHeader.m_Offset, size);
							m_ReceivedBytes += size;
						} else {
							DebugLog.i("NoSession: %s %s\n", ipHeader.toString(), tcpHeader.toString());
						}
					} else {

						// 添加端口映射
						int portKey = tcpHeader.getSourcePort();
						NatSession session = NatSessionManager.getSession(portKey);
						if (session == null || session.RemoteIP != ipHeader.getDestinationIP() || session.RemotePort
								!= tcpHeader.getDestinationPort()) {
							session = NatSessionManager.createSession(portKey, ipHeader.getDestinationIP(), tcpHeader
									.getDestinationPort());
						}

						session.LastNanoTime = System.nanoTime();
						session.PacketSent++;//注意顺序

						int tcpDataSize = ipHeader.getDataLength() - tcpHeader.getHeaderLength();
						if (session.PacketSent == 2 && tcpDataSize == 0) {
							return;//丢弃tcp握手的第二个ACK报文。因为客户端发数据的时候也会带上ACK，这样可以在服务器Accept之前分析出HOST信息。
						}

						//分析数据，找到host
						if (session.BytesSent == 0 && tcpDataSize > 10) {
							int dataOffset = tcpHeader.m_Offset + tcpHeader.getHeaderLength();
							String host = HttpHostHeaderParser.parseHost(tcpHeader.m_Data, dataOffset, tcpDataSize);
							if (host != null) {
								session.RemoteHost = host;
								if (ProxyConfig.IS_DEBUG) {
									DebugLog.i("Host: %s", host);
								}
							}
						}

						// 转发给本地TCP服务器
						ipHeader.setSourceIP(ipHeader.getDestinationIP());
						ipHeader.setDestinationIP(LOCAL_IP);
						tcpHeader.setDestinationPort(m_TcpProxyServer.Port);

						CommonMethods.ComputeTCPChecksum(ipHeader, tcpHeader);
						m_VPNOutputStream.write(ipHeader.m_Data, ipHeader.m_Offset, size);
						session.BytesSent += tcpDataSize;//注意顺序
						m_SentBytes += size;
					}
				}
				break;
			case IPHeader.UDP:
				// 转发DNS数据包：
				UDPHeader udpHeader = m_UDPHeader;
				udpHeader.m_Offset = ipHeader.getHeaderLength();
				if (ProxyConfig.IS_DEBUG && udpHeader.getDestinationPort() == 53) {
					DebugLog.i("Dns Query %s --> %s", CommonMethods.ipIntToString(ipHeader.getSourceIP()),
							CommonMethods.ipIntToString(ipHeader.getDestinationIP()));
				}
				if (ipHeader.getSourceIP() == LOCAL_IP && udpHeader.getDestinationPort() == 53) {
					m_DNSBuffer.clear();
					m_DNSBuffer.limit(ipHeader.getDataLength() - 8);
					DnsPacket dnsPacket = DnsPacket.FromBytes(m_DNSBuffer);
					if (dnsPacket != null && dnsPacket.Header.QuestionCount > 0) {
						m_DnsProxy.onDnsRequestReceived(ipHeader, udpHeader, dnsPacket);
					}
				}
				break;
		}
	}

	private void waitUntilPrepared() {
		while (prepare(this) != null) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public synchronized void run() {
		try {
//			System.out.printf("VPNService(%s) work thread is runing...\n", ID);
			if (ProxyConfig.IS_DEBUG) {
				DebugLog.i("VPNService(%s) work thread is running...\n", ID);
			}

			ProxyConfig.AppInstallID = getAppInstallID();//获取安装ID
			ProxyConfig.AppVersion = getVersionName();//获取版本号
//			System.out.printf("AppInstallID: %s\n", ProxyConfig.AppInstallID);
			if (ProxyConfig.IS_DEBUG) {
				DebugLog.i("AppInstallID: %s\n", ProxyConfig.AppInstallID);
			}
			writeLog("Android version: %s", Build.VERSION.RELEASE);
			writeLog("App version: %s", ProxyConfig.AppVersion);


			ChinaIpMaskManager.loadFromFile(getResources().openRawResource(R.raw.ipmask));//加载中国的IP段，用于IP分流。
			waitUntilPrepared();//检查是否准备完毕。

			m_TcpProxyServer = new TcpProxyServer(0);
			m_TcpProxyServer.start();
			writeLog("LocalTcpServer started.");

			m_DnsProxy = new DnsProxy();
			m_DnsProxy.start();
			writeLog("LocalDnsProxy started.");

			while (true) {
				if (IsRunning) {
					//加载配置文件
					writeLog("Load config from %s ...", ConfigUrl);
					try {
						ProxyConfig.Instance.loadFromUrl(ConfigUrl);
						if (ProxyConfig.Instance.getDefaultProxy() == null) {
							throw new Exception("Invalid config file.");
						}
						writeLog("PROXY %s", ProxyConfig.Instance.getDefaultProxy());
					} catch (Exception e) {
						String errString = e.getMessage();
						if (errString == null || errString.isEmpty()) {
							errString = e.toString();
						}

						IsRunning = false;
						onStatusChanged(errString, false);
						continue;
					}


					writeLog("Load config success.");
					String welcomeInfoString = ProxyConfig.Instance.getWelcomeInfo();
					if (welcomeInfoString != null && !welcomeInfoString.isEmpty()) {
						writeLog("%s", ProxyConfig.Instance.getWelcomeInfo());
					}

					runVPN();
				} else {
					Thread.sleep(100);
				}
			}
		} catch (InterruptedException e) {
			System.out.println(e);
		} catch (Exception e) {
			e.printStackTrace();
			writeLog("Fatal error: %s", e.toString());
		} finally {
			writeLog("SmartProxy terminated.");
			dispose();
		}
	}

	private ParcelFileDescriptor establishVPN() throws Exception {
		Builder builder = new Builder();
		builder.setMtu(ProxyConfig.Instance.getMTU());
		if (ProxyConfig.IS_DEBUG) {
//			System.out.printf("setMtu: %d\n", ProxyConfig.Instance.getMTU());
			DebugLog.i("setMtu: %d\n", ProxyConfig.Instance.getMTU());
		}

		ProxyConfig.IPAddress ipAddress = ProxyConfig.Instance.getDefaultLocalIP();
		LOCAL_IP = CommonMethods.ipStringToInt(ipAddress.Address);
		builder.addAddress(ipAddress.Address, ipAddress.PrefixLength);
		if (ProxyConfig.IS_DEBUG) {
//			System.out.printf("addAddress: %s/%d\n", ipAddress.Address, ipAddress.PrefixLength);
			DebugLog.i("addAddress: %s/%d\n", ipAddress.Address, ipAddress.PrefixLength);
		}

		for (ProxyConfig.IPAddress dns : ProxyConfig.Instance.getDnsList()) {
			builder.addDnsServer(dns.Address);
			if (ProxyConfig.IS_DEBUG) {
//				System.out.printf("addDnsServer: %s\n", dns.Address);
				DebugLog.i("addDnsServer: %s\n", dns.Address);
			}
		}

		if (ProxyConfig.Instance.getRouteList().size() > 0) {
			for (ProxyConfig.IPAddress routeAddress : ProxyConfig.Instance.getRouteList()) {
				builder.addRoute(routeAddress.Address, routeAddress.PrefixLength);
				if (ProxyConfig.IS_DEBUG) {
//					System.out.printf("addRoute: %s/%d\n", routeAddress.Address, routeAddress.PrefixLength);
					DebugLog.i("addRoute: %s/%d\n", routeAddress.Address, routeAddress.PrefixLength);
				}
			}
			builder.addRoute(CommonMethods.ipIntToString(ProxyConfig.FAKE_NETWORK_IP), 16);

			if (ProxyConfig.IS_DEBUG) {
//				System.out.printf("addRoute for FAKE_NETWORK: %s/%d\n", CommonMethods.ipIntToString(ProxyConfig
//						.FAKE_NETWORK_IP), 16);
				DebugLog.i("addRoute for FAKE_NETWORK: %s/%d\n", CommonMethods.ipIntToString(ProxyConfig
						.FAKE_NETWORK_IP), 16);
			}
		} else {
			builder.addRoute("0.0.0.0", 0);
			if (ProxyConfig.IS_DEBUG) {
//				System.out.printf("addDefaultRoute: 0.0.0.0/0\n");
				DebugLog.i("addDefaultRoute: 0.0.0.0/0\n");
			}
		}


		Class<?> SystemProperties = Class.forName("android.os.SystemProperties");
		Method method = SystemProperties.getMethod("get", new Class[]{String.class});
		ArrayList<String> servers = new ArrayList<String>();
		for (String name : new String[]{"net.dns1", "net.dns2", "net.dns3", "net.dns4",}) {
			String value = (String) method.invoke(null, name);
			if (value != null && !"".equals(value) && !servers.contains(value)) {
				servers.add(value);
				builder.addRoute(value, 32);
				if (ProxyConfig.IS_DEBUG) {
//					System.out.printf("%s=%s\n", name, value);
					DebugLog.i("%s=%s\n", name, value);
				}
			}
		}

		Intent intent = new Intent(this, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
		builder.setConfigureIntent(pendingIntent);

		builder.setSession(ProxyConfig.Instance.getSessionName());
		ParcelFileDescriptor pfdDescriptor = builder.establish();
		onStatusChanged(ProxyConfig.Instance.getSessionName() + getString(R.string.vpn_connected_status), true);
		return pfdDescriptor;
	}

	public void disconnectVPN() {
		try {
			if (m_VPNInterface != null) {
				m_VPNInterface.close();
				m_VPNInterface = null;
			}
		} catch (Exception e) {
			// ignore
		}
		onStatusChanged(ProxyConfig.Instance.getSessionName() + getString(R.string.vpn_disconnected_status), false);
		this.m_VPNOutputStream = null;
	}

	private synchronized void dispose() {
		// 断开VPN
		disconnectVPN();

		// 停止TcpServer
		if (m_TcpProxyServer != null) {
			m_TcpProxyServer.stop();
			m_TcpProxyServer = null;
			writeLog("LocalTcpServer stopped.");
		}

		// 停止DNS解析器
		if (m_DnsProxy != null) {
			m_DnsProxy.stop();
			m_DnsProxy = null;
			writeLog("LocalDnsProxy stopped.");
		}

		stopSelf();
		IsRunning = false;
		System.exit(0);
	}

	public interface onStatusChangedListener {
		public void onStatusChanged(String status, Boolean isRunning);

		public void onLogReceived(String logString);
	}


}

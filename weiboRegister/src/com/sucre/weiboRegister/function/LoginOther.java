package com.sucre.weiboRegister.function;

import com.sucre.weiboRegister.common.CommonVer;
import com.sucre.weiboRegister.mainUtil.MyUtil;
import com.sucre.weiboRegister.net.Nets;
import com.sucre.weiboRegister.thread.Thread4Net;

public class LoginOther extends Thread4Net {

	protected LoginOther(int u, boolean isCircle) {
		super(u, isCircle);
	}

	@Override
	public int doWork(int index) {
		Nets nets=new Nets();
		String ret=nets.GoHttp("www.51zggj.com", 9180, login(CommonVer.otherId,CommonVer.otherPassword));
		if(!MyUtil.isEmpty(ret)) {
		CommonVer.tokenCode=MyUtil.midWord("\r\n\r\n", "end", ret+"end");
		}
		return 0;
	}
    
	//登录打码平台 www.51zggj.com:9180
	private byte[] login(String id,String password) {
		StringBuilder data = new StringBuilder(900);
		data.append("GET http://www.51zggj.com/service.asmx/UserLoginStr?name="+ id +"&psw="+ password +" HTTP/1.1\r\n");
		data.append("Host: www.51zggj.com\r\n");
		data.append("Connection: keep-alive\r\n");
		data.append("Upgrade-Insecure-Requests: 1\r\n");
		data.append("User-Agent: Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36\r\n");
		data.append("Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\r\n");
		data.append("Referer: http://www.51kmf.com/osapijk.html\r\n");
		data.append("Accept-Language: zh-CN,zh;q=0.8\r\n");
		data.append("\r\n");
		data.append("\r\n");
		return data.toString().getBytes();
	}
}
